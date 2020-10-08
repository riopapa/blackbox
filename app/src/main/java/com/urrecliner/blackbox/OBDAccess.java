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
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.urrecliner.blackbox.Vars.ASK_SPEED_INTERVAL;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.speedInt;
import static com.urrecliner.blackbox.Vars.nowIsNear;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextSpeed;
import static com.urrecliner.blackbox.Vars.vPreviewView;
import static com.urrecliner.blackbox.Vars.viewFinder;

class OBDAccess {

    String logID = "OBD";
    //            utils.log(TAG, "connectOBD  "+chosenDeviceName+" : "+chosenDeviceAddress);
    private BluetoothSocket bSocket;
    private BluetoothDevice bluetoothDevice;
    private String chosenDeviceName = null;
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    String speedNow = "speed", speedOld = "old";

    //    private ObdCommand engineRpmCommand = new EngineRpmCommand();
    private ObdCommand speedCommand = null;
//    private ObdCommand distanceSinceCCCommand = null;
//    private ObdCommand loadCommand = new LoadCommand();

    void prepare() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
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
//                    chosenDeviceAddress = device.getAddress();
                    chosenDeviceName = device.getName();
                    btAdapter.cancelDiscovery();
                    new Timer().schedule(new TimerTask() {  // autoStart
                        public void run() {
                            if (connectOBD() && bSocket.isConnected()) {
                                askOBDDistance();
                                loopAskOBDSpeed();
                            }
                        }
                    }, 100);
                }
            }
        } else{
            Toast.makeText(mContext, "\nNo paired devices found\n", Toast.LENGTH_LONG).show();
        }
    }

    private boolean connectOBD()  {
//        utils.logBoth(logID, "connect OBD");
        if (!step1_BuildSocket(bluetoothDevice, uuid)) return false;
        if (!step2_ResetOBD()) return false;
        return step3_Initialize();
//        obdCommand(3);
//        try {
//            distanceSinceCCCommand = new DistanceSinceCCCommand();
//            distanceSinceCCCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
//            utils.logBoth("OBDDist",distanceSinceCCCommand.getFormattedResult());
//            utils.logBoth("OBDKms", distanceSinceCCCommand.getCalculatedResult()+" <><><><>");
//            beginKms = Integer.parseInt(distanceSinceCCCommand.getCalculatedResult());
//            todayStr = new SimpleDateFormat(FORMAT_DATE, Locale.US).format(System.currentTimeMillis());
//            if (!todayStr.equals(sharedPref.getString("todayStr",""))) {
//                editor = sharedPref.edit();
//                editor.putString("todayStr", todayStr).apply();
//                editor.putInt("beginKms", beginKms).apply();    // today's starting Kms
//            }
//            else
//                beginKms = sharedPref.getInt("beginKms",beginKms);
//            utils.logBoth(logID, todayStr +" Starting with "+beginKms+" Kms");
//        } catch (IllegalArgumentException e) {
//            utils.logE(logID, "IllegalArgumentException  ", e);
//        } catch (IOException e) {
//            utils.logBoth(logID, "/// IOException  distanceSinceCCCommand ///");
//        } catch (Exception e){
//            utils.logE(logID, "General Exception  ", e);
//        }
//        return false;
    }

    private boolean step1_BuildSocket(BluetoothDevice bluetoothDevice, UUID uuid) {
        try {
            bSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bSocket.connect();
            if (bSocket.isConnected()) {
                utils.logBoth("socket", chosenDeviceName + " connected");
                return true;
            }
        } catch (IllegalArgumentException e) {
            utils.logE("socket", "IllegalArgumentException  ", e);
//            Toast.makeText(mContext, "Please choose Bluetooth device first", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            utils.logBoth("socket", "*** OBD NOT FOUND ***");
            return true;
        } catch (Exception e){
            utils.logE("socket", "Exception  ", e);
        }
        return false;
    }

    final private int sleepTime = 100;
    private boolean step2_ResetOBD() {
        ObdResetCommand obdResetCommand = new ObdResetCommand();
        try {
            for (int i = 0; i < 3; i++) {
                obdResetCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
//                utils.logBoth("OBD Reset", obdResetCommand.getFormattedResult());
                Thread.sleep(sleepTime);
                //                SystemClock.sleep(100);
            }
            return true;
        } catch (IllegalArgumentException e) {
            utils.logE("resetOBD", "IllegalArgumentException  ", e);
        } catch (IOException e) {
            utils.logBoth("resetOBD", "*** OBD not READY ***");
        } catch (Exception e){
            utils.logE("resetOBD", "General Exception  ", e);
        }
        return false;
    }

    private boolean step3_Initialize() {
        try {
            EchoOffCommand echoOffCommand = new EchoOffCommand();
            echoOffCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
            Thread.sleep(sleepTime);
            LineFeedOffCommand lineFeedOffCommand = new LineFeedOffCommand();
            lineFeedOffCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
            Thread.sleep(sleepTime);
            SelectProtocolCommand selectProtocolCommand = new SelectProtocolCommand(ObdProtocols.AUTO);
            selectProtocolCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
            Thread.sleep(sleepTime);
            SpacesOffCommand spacesOffCommand = new SpacesOffCommand();
            spacesOffCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
            return true;
        } catch (IllegalArgumentException e) {
            utils.logE(logID, "IllegalArgumentException", e);
        } catch (IOException e) {
            utils.logE(logID, "IOException  obdCommand", e);
        } catch (Exception e){
            utils.logE(logID, "General Exception", e);
        }
        return false;
    }

    private Timer obdTimer = null;
    private boolean noPreview = false;
    private void loopAskOBDSpeed() {
//        utils.logBoth(logID, "start get OBD Speed");

        try {
            bSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bSocket.connect();
        } catch (Exception e) {
            utils.logBoth("loopAskOBDSpeed connect Exception", e.toString());
        }
        speedCommand = new SpeedCommand();
        obdTimer = new Timer();
        int HIDE_SPEED = 50;
        int NEAR_FOCUS = 40;
        final TimerTask obdTask = new TimerTask() {
            @Override
            public void run() {
                speedNow = askSpeed();
                if (!speedNow.equals(speedOld)) {
                    mActivity.runOnUiThread(() -> {
                        vTextSpeed.setText(speedNow);
                        speedInt = Integer.parseInt(speedNow);
                        boolean offPrevView =  speedInt > HIDE_SPEED;
                        if (viewFinder && offPrevView != noPreview) {
                            noPreview = offPrevView;
                            vPreviewView.setVisibility((noPreview) ? View.INVISIBLE : View.VISIBLE);
                        }
                        if (!nowIsNear && speedInt < NEAR_FOCUS) {
                            MainActivity.onNearSwitch();
                        }
                        if (nowIsNear && speedInt > NEAR_FOCUS) {
                            MainActivity.offNearSwitch();
                        }
                        speedOld = speedNow;
                    });
                }
            }
        };
        obdTimer.schedule(obdTask, 100, ASK_SPEED_INTERVAL);
    }

    private String askSpeed() {
        try {
            speedCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
            return speedCommand.getCalculatedResult();
        } catch (IllegalArgumentException e) {
            utils.logE("speed", "IllegalArgumentException", e);
        } catch (IOException e) {
            utils.logE("speed", "IOException  obdCommand", e);
        } catch (Exception e){
            utils.logE("speed", "General Exception", e);
        }
        return "speed Err";
    }

    private void askOBDDistance() {
        try {
            DistanceSinceCCCommand distanceSinceCCCommand = new DistanceSinceCCCommand();
            distanceSinceCCCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
            utils.logBoth("OBD Distance Good",distanceSinceCCCommand.getFormattedResult());
        } catch (Exception e){
//            utils.logE("distance", "General Exception", e);
        }
    }

    void stop() {
        if (obdTimer != null)
            obdTimer.cancel();
        obdTimer = null;
    }
}