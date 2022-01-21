package com.example.mybleapplication;

import static android.content.ContentValues.TAG;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class DeviceControlActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private MyBluetoothService bluetoothService;
    private String deviceAddress;
    private TextView tvConnectionStatus;
    private Spinner spinner;
    private ArrayAdapter<CharSequence> songsArrayAdapter;
    private Button btPlay;

    private int currentSongIndex;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);

        this.tvConnectionStatus = findViewById(R.id.tvConnectionStatus);

        this.tvConnectionStatus.setText(getResources().getString(R.string.device_found));

        this.spinner = (Spinner) findViewById(R.id.spinner);
        this.songsArrayAdapter = ArrayAdapter.createFromResource(this,
                R.array.songs_array, android.R.layout.simple_spinner_item);
        this.songsArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinner.setAdapter(songsArrayAdapter);
        this.spinner.setOnItemSelectedListener(this);
        this.spinner.setEnabled(false);

        this.btPlay = (Button) findViewById(R.id.btPLay);

        this.btPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothService.playSong(currentSongIndex);
//                bluetoothService.ledToggle();
                Toast.makeText(DeviceControlActivity.this, "Sending " + currentSongIndex, Toast.LENGTH_SHORT).show();

            }
        });

        this.btPlay.setEnabled(false);

        Intent intent = getIntent();
        this.deviceAddress = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        Intent gattServiceIntent = new Intent(this, MyBluetoothService.class);
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        this.currentSongIndex = position;
        System.out.println(this.currentSongIndex);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bluetoothService != null) {
            final boolean result = bluetoothService.connect(this.deviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (MyBluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                tvConnectionStatus.setText(getResources().getString(R.string.connected));
            } else if (MyBluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                tvConnectionStatus.setText(getResources().getString(R.string.disconnected));
            } else if (MyBluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                tvConnectionStatus.setText(getResources().getString(R.string.services_discovered));
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(bluetoothService.getSupportedGattServices());
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyBluetoothService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(MyBluetoothService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(MyBluetoothService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            tvConnectionStatus.setText(getResources().getString(R.string.gatt_service_connected));

            bluetoothService = ((MyBluetoothService.LocalBinder) service).getService();
            if (bluetoothService != null) {
                if (!bluetoothService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }
                // perform device connection
                bluetoothService.connect(deviceAddress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothService = null;
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        System.out.println("Amount of services - " + gattServices.size());
//        for (BluetoothGattService gattService : gattServices) {
//            if (gattService.getUuid().toString().contains(MUSIC_SERVICE_UUID)) {
        System.out.println("Music service found!");
        this.spinner.setEnabled(true);
        this.btPlay.setEnabled(true);
//            }
//            if (gattService.getUuid().toString().contains(LED_SERVICE_UUID)) {
//        System.out.println("LED service found!");
//            }
//        }
    }

}