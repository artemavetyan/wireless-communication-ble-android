package com.example.mybleapplication;

import static android.content.ContentValues.TAG;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceControlActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private MyBluetoothService bluetoothService;
    private String deviceAddress;

    private TextView tvConnectionStatus;
    private Spinner spinner;
    private ArrayAdapter<CharSequence> songsArrayAdapter;
    private Button btPlay;
    private EditText etDisplayMessage;
    private Button btSendMessage;

    private TextView tvTemperature;
    private Button btGetTemperature;
    private Button btSubscribeToTemperature;


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
                Toast.makeText(DeviceControlActivity.this, "Sending song #" + currentSongIndex, Toast.LENGTH_SHORT).show();
            }
        });

        this.btPlay.setEnabled(false);

        this.etDisplayMessage = (EditText) findViewById(R.id.etDisplayMessage);
        this.etDisplayMessage.setEnabled(false);

        this.btSendMessage = (Button) findViewById(R.id.btSend);

        this.btSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothService.sendMessage(etDisplayMessage.getText().toString());
                Toast.makeText(DeviceControlActivity.this, "Sending message: " + etDisplayMessage.getText(), Toast.LENGTH_SHORT).show();

            }
        });

        this.btSendMessage.setEnabled(false);

        this.tvTemperature = (TextView) findViewById(R.id.tvTemperature);
        this.tvTemperature.setEnabled(false);

        this.btGetTemperature = (Button) findViewById(R.id.btGetTemperature);
        this.btGetTemperature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothService.readTemperature();
                Toast.makeText(DeviceControlActivity.this, "Reading temperature", Toast.LENGTH_SHORT).show();
            }
        });
        this.btGetTemperature.setEnabled(false);

        this.btSubscribeToTemperature = (Button) findViewById(R.id.btSubsribeTemperature);
        this.btSubscribeToTemperature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothService.subscribeToTemperatureReadings();
                Toast.makeText(DeviceControlActivity.this, "Reading temperature", Toast.LENGTH_SHORT).show();
            }
        });
        this.btSubscribeToTemperature.setEnabled(false);

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

    @RequiresApi(api = Build.VERSION_CODES.M)
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
            System.out.println("BroadcastReceiver");
            final String action = intent.getAction();
            System.out.println(action);
            if (MyBluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                tvConnectionStatus.setText(getResources().getString(R.string.connected));
            } else if (MyBluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                changeUIState(false);
                tvConnectionStatus.setText(getResources().getString(R.string.disconnected));
                if (bluetoothService != null) {
                    tvConnectionStatus.setText(getResources().getString(R.string.reconnecting));
                    final boolean result = bluetoothService.connect(deviceAddress);
                    Log.d(TAG, "Connect request result=" + result);
                }
            } else if (MyBluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                tvConnectionStatus.setText(getResources().getString(R.string.services_discovered));
                // Show all the supported services and characteristics on the user interface.
                changeUIState(true);
            } else if (MyBluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                System.out.println("ACTION_DATA_AVAILABLE");
                String textToShow = intent.getStringExtra(MyBluetoothService.EXTRA_DATA) + "°C";
                tvTemperature.setText(textToShow);
            } else if (MyBluetoothService.DISPLAY_MESSAGE_SENT.equals(action)) {
                System.out.println("MyBluetoothService.DISPLAY_MESSAGE_SENT");
                etDisplayMessage.setText("");
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyBluetoothService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(MyBluetoothService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(MyBluetoothService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(MyBluetoothService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(MyBluetoothService.DISPLAY_MESSAGE_SENT);

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

    private void changeUIState(boolean isEnabled) {
        this.spinner.setEnabled(isEnabled);
        this.btPlay.setEnabled(isEnabled);
        this.etDisplayMessage.setEnabled(isEnabled);
        this.btSendMessage.setEnabled(isEnabled);
        this.tvTemperature.setEnabled(isEnabled);
        this.btGetTemperature.setEnabled(isEnabled);
        this.btSubscribeToTemperature.setEnabled(isEnabled);
    }
}