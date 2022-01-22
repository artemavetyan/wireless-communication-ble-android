package com.example.mybleapplication;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

public class MyBLEScanner {

    private MainActivity mainActivity;
    private BluetoothLeScanner bleScanner;
    private boolean scanning;
    private Handler handler;

    // Stops scanning after 30 seconds.
    private static final long SCAN_PERIOD = 10000;

    public MyBLEScanner(MainActivity mainActivity, BluetoothLeScanner bleScanner) {
        this.mainActivity = mainActivity;
        this.bleScanner = bleScanner;
        this.handler = new Handler();
    }

    public void scanLeDevice() {
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
//                    mainActivity.updateStatus("Stopped scanning");
                    mainActivity.onStopScanning();
                    scanning = false;
                    bleScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            scanning = true;
//            this.mainActivity.updateStatus("Scanning");
            this.mainActivity.onStartScanning();
            ScanSettings settings = new ScanSettings.Builder().build();
            ArrayList<ScanFilter> filters = new ArrayList<>();
            filters.add(new ScanFilter.Builder().setDeviceAddress("ED:16:18:78:E5:51").build());

            bleScanner.startScan(filters, settings,leScanCallback);
        } else {
            scanning = false;
            bleScanner.stopScan(leScanCallback);
        }
    }

    private final ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    mainActivity.onFound();
//                    mainActivity.updateStatus("Found!");
                    mainActivity.startDeviceControlActivity(result.getDevice().getAddress());
                    bleScanner.stopScan(leScanCallback);
                }
            };
}
