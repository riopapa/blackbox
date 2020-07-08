package com.urrecliner.blackbox;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.view.View;
import android.widget.Toast;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.DistanceSinceCCCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.SpacesOffCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.urrecliner.blackbox.Vars.FORMAT_DATE;
import static com.urrecliner.blackbox.Vars.SNAP_SHOT_INTERVAL;
import static com.urrecliner.blackbox.Vars.editor;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.sharedPref;
import static com.urrecliner.blackbox.Vars.todayStr;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextSpeed;
import static com.urrecliner.blackbox.Vars.vPreviewView;
import static com.urrecliner.blackbox.Vars.vTodayKms;
import static com.urrecliner.blackbox.Vars.viewFinder;

class OBDAccess {

    String logID = "OBD";
    private BluetoothAdapter btAdapter = null;
    //            utils.log(TAG, "connectOBD  "+chosenDeviceName+" : "+chosenDeviceAddress);
    private BluetoothSocket bSocket;
    private BluetoothDevice bluetoothDevice;
    private String chosenDeviceName = null, chosenDeviceAddress = null;
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    String speedNow = "speed", speedOld = "old";

    //    private ObdCommand rpmCommand = new RPMCommand();
    private ObdCommand speedCommand = null;
    private ObdCommand distanceSinceCCCommand = null;
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
                    bluetoothDevice = device;
                    chosenDeviceAddress = device.getAddress();
                    chosenDeviceName = device.getName();
                    btAdapter.cancelDiscovery();
                    new Timer().schedule(new TimerTask() {  // autoStart
                        public void run() {
                        buildSocket(bluetoothDevice, uuid);
//                        readyOBDDevice();
//                        getOBDInfoTimeBased();
                            if (!connectOBD())
                                connectOBD();
                            getOBDInfoTimeBased();
                        }
                    }, 100);
                }
            }
        } else{
            Toast.makeText(mContext, "No paired devices found", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean connectOBD()  {
//        utils.logBoth(logID, "connect OBD");
        if (!buildSocket(bluetoothDevice, uuid)) return false;
        if (!resetOBD()) return false;
        for (int cmdType = 0; cmdType < 4; cmdType++) {
            for (int l = 0; l < 3; l++) {
                if (obdCommand(cmdType)) {
                    break;
                }
                try {
                    bSocket.connect();
                } catch (Exception e) {
                    utils.logE("connectOBD","reConnect Error", e);
                }
                obdCommand(cmdType);
            }
        }
//        obdCommand(3);
        try {
            distanceSinceCCCommand = new DistanceSinceCCCommand();
            distanceSinceCCCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
            utils.logBoth("OBDDist",distanceSinceCCCommand.getFormattedResult());
            utils.logBoth("OBDKms", distanceSinceCCCommand.getCalculatedResult()+" <><><><>");
            beginKms = Integer.parseInt(distanceSinceCCCommand.getCalculatedResult());
            todayStr = new SimpleDateFormat(FORMAT_DATE, Locale.US).format(System.currentTimeMillis());
            if (!todayStr.equals(sharedPref.getString("todayStr",""))) {
                editor = sharedPref.edit();
                editor.putString("todayStr", todayStr).apply();
                editor.putInt("beginKms", beginKms).apply();    // today's starting Kms
            }
            else
                beginKms = sharedPref.getInt("beginKms",beginKms);
            utils.logBoth(logID, todayStr +" Starting with "+beginKms+" Kms");
            return true;
        } catch (IllegalArgumentException e) {
            utils.logE(logID, "IllegalArgumentException  ", e);
        } catch (IOException e) {
            utils.logBoth(logID, "/// IOException  distanceSinceCCCommand ///");
        } catch (Exception e){
            utils.logE(logID, "General Exception  ", e);
        }
        return false;
    }

    private boolean resetOBD() {
        ObdResetCommand obdResetCommand = new ObdResetCommand();
        try {
            for (int i = 0; i < 2; i++) {
                obdResetCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
                utils.logBoth("OBD Reset", obdResetCommand.getFormattedResult());
                Thread.sleep(200);
                //                SystemClock.sleep(100);
            }
            return true;
        } catch (IllegalArgumentException e) {
            utils.logE(logID, "IllegalArgumentException  ", e);
        } catch (IOException e) {
            utils.logBoth(logID, "*** OBD not READY ***");
        } catch (Exception e){
            utils.logE(logID, "General Exception  ", e);
        }
        return false;
    }

    private boolean buildSocket(BluetoothDevice bluetoothDevice, UUID uuid) {
        try {
            bSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bSocket.connect();
            if (bSocket.isConnected()) {
                utils.logBoth(logID, chosenDeviceName + " connected ^^ ");
                return true;
            }
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
        try {
//            if (!bluetoothSocket.isConnected()) {
//                utils.logBoth("obdCommand","reconnect "+cmd);
//                bluetoothSocket.connect();
//                new ObdResetCommand().run(bluetoothSocket.getInputStream(), bluetoothSocket.getOutputStream());
//                utils.logBoth("obdCommand","reset requested "+cmd);
//            }
            Thread.sleep(200);
            switch (cmd) {
                case 0:
                    EchoOffCommand echoOffCommand = new EchoOffCommand();
                    echoOffCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
                    utils.logBoth("OBD Echo Good",echoOffCommand.getFormattedResult());
                    break;
                case 1:
                    LineFeedOffCommand lineFeedOffCommand = new LineFeedOffCommand();
                    lineFeedOffCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
                    utils.logBoth("OBD Line Good",lineFeedOffCommand.getFormattedResult());
                    break;
                case 2:
                    SelectProtocolCommand selectProtocolCommand = new SelectProtocolCommand(ObdProtocols.AUTO);
                    selectProtocolCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
                    utils.logBoth("OBD Proto",selectProtocolCommand.getFormattedResult());
                    break;
                case 3:
                    SpacesOffCommand spacesOffCommand = new SpacesOffCommand();
                    spacesOffCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
                    utils.logBoth("OBD space",spacesOffCommand.getFormattedResult());
                    break;
            }
            return true;
        } catch (IllegalArgumentException e) {
            utils.logE(logID+cmd, "IllegalArgumentException "+cmd, e);
        } catch (IOException e) {
            utils.logE(logID+cmd, "IOException  obdCommand "+cmd, e);
        } catch (Exception e){
            utils.logE(logID+cmd, "General Exception  "+cmd, e);
        }
        return false;
    }

    private Timer obdTimer = null;
    private int beginKms, nowKms;
    private boolean noPreview = false;
    private void getOBDInfoTimeBased() {
        utils.logBoth(logID, "start get OBD Speed");

        try {
            bSocket.connect();
        } catch (Exception e) {
            utils.logBoth("getOBDInfoTimeBased connect", e.toString());
        }
        speedCommand = new SpeedCommand();
        obdTimer = new Timer();
        final TimerTask obdTask = new TimerTask() {
            @Override
            public void run() {
                try {
//                    rpmCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
//                    obdRPM = rpmCommand.getCalculatedResult();
                    speedCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
                    speedNow = speedCommand.getCalculatedResult();
                    if (!speedNow.equals(speedOld)) {
//                   distanceSinceCCCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
//                   nowKms = Integer.parseInt(distanceSinceCCCommand.getCalculatedResult());
                        mActivity.runOnUiThread(() -> {
                            vTextSpeed.setText(speedNow);
//                            String s = (nowKms - beginKms) + "";
//                            vTodayKms.setText(s);
                            boolean offPrevView = Integer.parseInt(speedNow) > 50;
                            if (viewFinder && offPrevView != noPreview) {
                                noPreview = offPrevView;
                                vPreviewView.setVisibility((noPreview) ? View.INVISIBLE : View.VISIBLE);
                            }
                        });
                        speedOld = speedNow;
                    }
//                    loadCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
//                    utils.log(TAG,"3) "+ loadCommand.getCalculatedResult());
                } catch (Exception e) {
                    utils.logE(logID, "obdTask Exception  ", e);
                }
            }
        };
        obdTimer.schedule(obdTask, 100, SNAP_SHOT_INTERVAL);
    }

//
//    private static final int DELAY_FIFTEEN_SECOND = 15000;
//    private static final int DELAY_TWO_SECOND = 2000;
//    private boolean mIsRunningSuccess;
//    void readyOBDDevice() {
//        final Thread newThread = new Thread(new Runnable() {
//            String cmd = "none";
//            @Override
//            public void run() {
//                try {
//                    // this thread is required because in Headunit command.run method block infinitly ,
//                    // therefore this thread life is maximum 15 second so that block can be handled.
//                    mIsRunningSuccess = false;
//                    cmd = "ObdResetCommand 1";
//                    new ObdResetCommand().run(bSocket.getInputStream(), bSocket.getOutputStream());
//                    Thread.sleep(1000);
//                    cmd = "ObdResetCommand 2";
//                    new ObdResetCommand().run(bSocket.getInputStream(), bSocket.getOutputStream());
//                    Thread.sleep(1000);
//                    cmd = "EchoOffCommand";
//                    new EchoOffCommand().run(bSocket.getInputStream(), bSocket.getOutputStream());
//                    Thread.sleep(200);
//                    cmd = "LineFeedOffCommand";
//                    new LineFeedOffCommand().run(bSocket.getInputStream(), bSocket.getOutputStream());
//                    Thread.sleep(200);
//                    cmd = "SpacesOffCommand";
//                    new SpacesOffCommand().run(bSocket.getInputStream(), bSocket.getOutputStream());
//                    Thread.sleep(200);
//                    cmd = "SpacesOffCommand";
//                    new SpacesOffCommand().run(bSocket.getInputStream(), bSocket.getOutputStream());
//                    Thread.sleep(200);
//                    cmd = "TimeoutCommand";
//                    new TimeoutCommand(125).run(bSocket.getInputStream(), bSocket.getOutputStream());
//                    //  updateNotification(getString(R.string.searching_protocol));
//                    Thread.sleep(200);
//                    cmd = "SelectProtocolCommand";
//                    new SelectProtocolCommand(ObdProtocols.AUTO).run(bSocket.getInputStream(), bSocket.getOutputStream());
//                    Thread.sleep(200);
//                    cmd = "EchoOffCommand";
//                    new EchoOffCommand().run(bSocket.getInputStream(), bSocket.getOutputStream());
//                    //  updateNotification(getString(R.string.searching_supported_sensor));
//                    Thread.sleep(200);
//                    mIsRunningSuccess = true;
//                    // checkPid0To20(true);
//
//                } catch (Exception e) {
//                    mIsRunningSuccess = false;
//                    utils.logBoth("@ "+cmd,"In new thread reset command  exception :: " + e != null ? e.getMessage() : "");
//                }
//
//            }
//        });
//        try {
//            Thread.sleep(DELAY_TWO_SECOND);
//                  /*  if (mLastNotificationType != INIT_OBD) {
//                        mLastNotificationType = INIT_OBD;
//                        updateNotification(getString(R.string.connecting_to_ecu));
//                    }*/
//            utils.logBoth("ready","Executing reset command in new Thread :: " + Thread.currentThread().getId());
//
//            newThread.start();
//            newThread.join(DELAY_FIFTEEN_SECOND);
//            utils.logBoth("newJoin","Thread wake to check reset command status  i.e  :: " + Thread.currentThread().getId() + ",  mIsRunningSuccess :: " + mIsRunningSuccess);
//            isSockedConnected = mIsRunningSuccess;
//
//        } catch (Exception e) {
//            utils.logBoth("except","reset command Exception  :: " + e.getMessage());
//            isSockedConnected = false;
//        }
//
//    }

    void stop() {
        if (obdTimer != null)
            obdTimer.cancel();
        obdTimer = null;
//        String s = "today "+(nowKms-beginKms)+" Kms moved";
//        utils.logBoth(logID, s);
//        Toast.makeText(mContext,s,Toast.LENGTH_LONG).show();
    }
}
