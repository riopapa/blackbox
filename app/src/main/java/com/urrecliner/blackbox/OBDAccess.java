package com.urrecliner.blackbox;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
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
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.urrecliner.blackbox.Vars.ChronoLog;
import static com.urrecliner.blackbox.Vars.chronoLogs;
import static com.urrecliner.blackbox.Vars.kiloMeter;
import static com.urrecliner.blackbox.Vars.lNewsLine;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.sharedPref;
import static com.urrecliner.blackbox.Vars.speedInt;
import static com.urrecliner.blackbox.Vars.chronoNowDate;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextKilo;
import static com.urrecliner.blackbox.Vars.vTextSpeed;
import static com.urrecliner.blackbox.Vars.vPreviewView;
import static com.urrecliner.blackbox.Vars.viewFinder;

class OBDAccess {

    final static int ASK_SPEED_INTERVAL = 600;
    String logID = "OBD";
    private BluetoothSocket bSocket;
    private BluetoothDevice bluetoothDevice;
    private String chosenDeviceName = null;
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //    private ObdCommand engineRpmCommand = new EngineRpmCommand();
    private ObdCommand speedCommand = null;
//    private ObdCommand distanceSinceCCCommand = null;
//    private ObdCommand loadCommand = new LoadCommand();

    void start() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
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
                                resetTodayKm(Integer.parseInt(askOBDDistance()));
                                showDistance();
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

    private void resetTodayKm(int kilo) {
        kiloMeter = sharedPref.getInt("kilo", -1);
        chronoNowDate = sharedPref.getString("today","new");
        String tuDay = new SimpleDateFormat("yy/MM/dd", Locale.KOREA).format(System.currentTimeMillis());
        if (chronoNowDate.equals(tuDay))
            return;
        chronoNowDate = tuDay;
        kiloMeter = kilo;
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("today", chronoNowDate);
        editor.putInt("kilo",kiloMeter);
        editor.apply();
        StringBuilder sb = new StringBuilder();
        int oldKilo = 0;
        for (int i = 0; i < chronoLogs.size(); i++) {
            ChronoLog chronoLog = chronoLogs.get(i);
            utils.logBoth("kilo "+i,"date"+chronoLog.chroDate+" > "+chronoLog.chroKilo);
            if (i == 0)
                oldKilo = chronoLog.chroKilo;
            else {
                sb.append(chronoLog.chroDate).append(" = ").append(chronoLog.chroKilo - oldKilo).append("\n");
            }
        }
        utils.logBoth("Kilo", sb.toString());
    }

    private Timer speedTimer = null;
    private boolean noPreview = false;
    private int distCount = 0;
    private String speedString = "speed", speedOld = "old", distOld = "old";

    private void loopAskOBDSpeed() {
//        switch (SUFFIX) {
//            case "8": // Galaxy 8 sometimes disconnected so reconnect
//                try {
//                    bSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
//                    bSocket.connect();
//                } catch (Exception e) {
//                    utils.logBoth("loopAskOBDSpeed connect Exception", e.toString());
//                }
//                break;
//            case "9":
//            case "S":
//                break;
//        }
        speedCommand = new SpeedCommand();
        speedTimer = new Timer();
        final TimerTask obdTask = new TimerTask() {
            @Override
            public void run() {
                speedString = askSpeed();
                if (!speedString.equals(speedOld)) {
                    mActivity.runOnUiThread(() -> {
                        vTextSpeed.setText(speedString);
                        speedInt = Integer.parseInt(speedString);
                        boolean offPrevView =  speedInt > 40; // hide video if over 40 Kms
                        if (viewFinder && offPrevView != noPreview) {
                            noPreview = offPrevView;
                            vPreviewView.setVisibility((noPreview) ? View.INVISIBLE : View.VISIBLE);
                        }
                        if (speedOld.equals("old")) {
                            lNewsLine.setVisibility(View.VISIBLE);
                        }
                        speedOld = speedString;
                    });
                    if (distCount++ > 50) {
                        distCount = 0;
                        showDistance();
                    }
                }
            }
        };
        speedTimer.schedule(obdTask, 200, ASK_SPEED_INTERVAL);
    }

    private void showDistance() {
        String ss = askOBDDistance();
        if (ss.equals(distOld))
            return;
        distOld = ss;
        int kilo = Integer.parseInt(ss) - kiloMeter;
        mActivity.runOnUiThread(() -> {
            String s = ""+kilo;
            vTextKilo.setText(s);
        });
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

    private String askOBDDistance() {
        try {
            DistanceSinceCCCommand distanceSinceCCCommand = new DistanceSinceCCCommand();
            distanceSinceCCCommand.run(bSocket.getInputStream(), bSocket.getOutputStream());
            return distanceSinceCCCommand.getCalculatedResult();
        } catch (Exception e){
//            utils.logE("distance", "General Exception", e);
        }
        return "-1";
    }

    void stop() {
        if (speedTimer != null)
            speedTimer.cancel();
        speedTimer = null;
    }
}