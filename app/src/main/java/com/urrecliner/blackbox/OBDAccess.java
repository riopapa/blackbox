package com.urrecliner.blackbox;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.SystemClock;
import android.view.View;
import android.widget.Toast;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.DistanceSinceCCCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.urrecliner.blackbox.Vars.FORMAT_DATE;
import static com.urrecliner.blackbox.Vars.editor;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.sharedPref;
import static com.urrecliner.blackbox.Vars.speedNow;
import static com.urrecliner.blackbox.Vars.speedOld;
import static com.urrecliner.blackbox.Vars.todayStr;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextSpeed;
import static com.urrecliner.blackbox.Vars.vPreviewView;
import static com.urrecliner.blackbox.Vars.vTodayKms;
import static com.urrecliner.blackbox.Vars.viewFinder;

class OBDAccess {

    String logID = "OBD";
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket;
    private String chosenDeviceName = null, chosenDeviceAddress = null;
//    private ObdCommand rpmCommand = new RPMCommand();
    private ObdCommand speedCommand = new SpeedCommand();
    private ObdCommand distanceSinceCCCommand = new DistanceSinceCCCommand();
//    private ObdCommand loadCommand = new LoadCommand();

    void prepare() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
//        utils.logOnly(logID, "btAdaptor is "+btAdapter.toString());
        if(btAdapter == null){
            Toast.makeText(Vars.mContext, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }
//        final ArrayList<String> pairedDevicesNames = new ArrayList<>();
//        final ArrayList<String> pairedDevicesAddresses = new ArrayList<>();

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
//                utils.log(logID, "pairedDevices ="+device.getName()+" type:"+device.getType());
                if (device.getName().contains("OBD")) {
                    chosenDeviceAddress = device.getAddress();
                    chosenDeviceName = device.getName();
                    new Timer().schedule(new TimerTask() {  // autoStart
                        public void run() {
                            connectOBD();
                        }
                    }, 5000);
                }
            }

        } else{
            Toast.makeText(mContext, "No paired devices found", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectOBD() {
//        utils.logBoth(logID, "connect OBD");
        BluetoothDevice device = btAdapter.getRemoteDevice(chosenDeviceAddress);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//            utils.log(TAG, "connectOBD  "+chosenDeviceName+" : "+chosenDeviceAddress);

        if (buildSocket(device, uuid)) return;
        if (resetOBD()) return;
        for (int cmdType = 0; cmdType < 4; cmdType++) {
            for (int l = 0; l < 3; l++) {
                SystemClock.sleep(300);
                if (obdCommand(cmdType)) {
                    break;
                }
                buildSocket(device, uuid);
                resetOBD();
            }
        }
        try {
            distanceSinceCCCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
            beginKms = Integer.parseInt(distanceSinceCCCommand.getCalculatedResult());
            todayStr = new SimpleDateFormat(FORMAT_DATE, Locale.US).format(System.currentTimeMillis());
            if (!todayStr.equals(sharedPref.getString("todayStr",""))) {
                editor = sharedPref.edit();
                editor.putString("todayStr", todayStr).apply();
                editor.putInt("beginKms", beginKms).apply();    // today's starting Kms
            }
            else
                beginKms = sharedPref.getInt("beginKms",beginKms);
            utils.logOnly(logID, todayStr +" Starting with "+beginKms+" Kms");
            getOBDInfoTimeBased();
        } catch (IllegalArgumentException e) {
            utils.logE(logID, "IllegalArgumentException  ", e);
        } catch (IOException e) {
            utils.logBoth(logID, "IOException  distanceSinceCCCommand ");
        } catch (Exception e){
            utils.logE(logID, "General Exception  ", e);
        }
    }

    private boolean resetOBD() {
        try {
            for (int i = 0; i < 4; i++) {
                new ObdResetCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
                SystemClock.sleep(500);
            }
        } catch (IllegalArgumentException e) {
            utils.logE(logID, "IllegalArgumentException  ", e);
            return true;
        } catch (IOException e) {
            utils.logBoth(logID, "*** OBD not READY ***");
            return true;
        } catch (Exception e){
            utils.logE(logID, "Exception  ", e);
            return true;
        }
        return false;
    }

    private boolean buildSocket(BluetoothDevice device, UUID uuid) {
        try {
            btSocket = device.createRfcommSocketToServiceRecord(uuid);
            btSocket.connect();
            if (btSocket.isConnected())
                utils.logBoth(logID,chosenDeviceName+" connected ^^ ");
        } catch (IllegalArgumentException e) {
            utils.logE(logID, "IllegalArgumentException  ", e);
//            Toast.makeText(mContext, "Please choose Bluetooth device first", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            utils.logBoth(logID, "*** OBD NOT FOUND ***");
            return true;
        } catch (Exception e){
            utils.logE(logID, "Exception  ", e);
        }
        return false;
    }

    private boolean obdCommand(int cmd) {
        boolean runOK = false;
        try {
            switch (cmd) {
                case 0:
                    new EchoOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
                    break;
                case 1:
                    new LineFeedOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
                    break;
                case 2:
                    new SelectProtocolCommand(ObdProtocols.AUTO).run(btSocket.getInputStream(), btSocket.getOutputStream());
                    break;
                case 3:
                    new DistanceSinceCCCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
                    break;
            }
            runOK = true;
        } catch (IllegalArgumentException e) {
            utils.logE(logID+cmd, "IllegalArgumentException "+cmd, e);
        } catch (IOException e) {
            utils.logE(logID+cmd, "IOException  obdCommand "+cmd, e);
        } catch (Exception e){
            utils.logE(logID+cmd, "General Exception  "+cmd, e);
        }
        return runOK;
    }

    private Timer timer = null;
    private int beginKms, nowKms;
    private boolean noPreview = false;
    private void getOBDInfoTimeBased() {
        utils.logBoth(logID, "startOBD  ");
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
//                    rpmCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
//                    obdRPM = rpmCommand.getCalculatedResult();
                    speedCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
                    distanceSinceCCCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
                    speedNow = speedCommand.getCalculatedResult();
                    if (!speedNow.equals(speedOld)) {
                        nowKms = Integer.parseInt(distanceSinceCCCommand.getCalculatedResult());
                        mActivity.runOnUiThread(() -> {
                            vTextSpeed.setText(speedNow);
                            String s = (nowKms - beginKms)+"";
                            vTodayKms.setText(s);
                            if (viewFinder && (Integer.parseInt(speedNow) > 50) != noPreview) {
                                noPreview = Integer.parseInt(speedNow) > 50;
                                vPreviewView.setVisibility((noPreview)? View.INVISIBLE:View.VISIBLE);
                            }
                        });
                        speedOld = speedNow;
                    }
//                    loadCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
//                    utils.log(TAG,"3) "+ loadCommand.getCalculatedResult());
                } catch (Exception e) {
                    utils.logE(logID, "Exception  ", e);
                }
            }
        }, 100, 2000);
    }

    void stop() {
        if (timer != null)
            timer.cancel();
        timer = null;
        String s = "today "+(nowKms-beginKms)+" Kms moved";
        utils.logBoth(logID, s);
        Toast.makeText(mContext,s,Toast.LENGTH_LONG).show();
    }
}
//If you are connecting to a Bluetooth serial board then try using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if you are connecting to an Android peer then please generate your own unique UUID.
