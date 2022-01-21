package com.example.mybleapplication;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "my.ble.app.EXTRA_MESSAGE";


    private TextView tvStatus;
    private Button btScan;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION));
        System.out.println(this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION));
        System.out.println(this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION));

        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        }

        BluetoothManager bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bleAdapter = bleManager.getAdapter();

        ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                    }
                });

        if (!bleAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            someActivityResultLauncher.launch(enableBtIntent);
        }

        MyBLEScanner myBLEScanner = new MyBLEScanner(this, bleAdapter.getBluetoothLeScanner());

        this.tvStatus = findViewById(R.id.tvStatus);

        this.btScan = findViewById(R.id.btScan);
        this.btScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myBLEScanner.scanLeDevice();
            }
        });

        myBLEScanner.scanLeDevice();
    }

    public void startDeviceControlActivity(String deviceAddress) {
        Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(EXTRA_MESSAGE, deviceAddress);
        startActivity(intent);
    }

    public void onFound() {
        this.tvStatus.setText(getResources().getString(R.string.device_found));
    }

    public void onStartScanning() {
        this.tvStatus.setText(getResources().getString(R.string.scanning));
        this.btScan.setEnabled(false);

    }

    public void onStopScanning() {
        this.tvStatus.setText(getResources().getString(R.string.stop_scan));
        this.btScan.setEnabled(true);

    }
}