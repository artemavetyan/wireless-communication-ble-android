package com.example.mybleapplication;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class MyBluetoothService extends Service {

    public final static String ACTION_GATT_CONNECTED =
            "my.ble.app.ACTION_GATT_CONNECTED";

    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "my.ble.app.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_GATT_DISCONNECTED =
            "my.ble.app.ACTION_GATT_DISCONNECTED";
    private static final String ACTION_DATA_AVAILABLE = "my.ble.app.ACTION_DATA_AVAILABLE";

    private static final String EXTRA_DATA = "my.ble.app.EXTRA_DATA";

    public final static String MUSIC_SERVICE_UUID =
            "0b000";
    public final static String PLAY_SONG_CHAR_UUID =
            "0b001";

    public final static String LED_SERVICE_UUID =
            "0a000";
    public final static String LED_CHAR_UUID =
            "0a001";
    private BluetoothGattCharacteristic playSongChar;

    private BluetoothGattCharacteristic ledChar;

    private static final int TRANSPORT_LE = 2;

    private BluetoothGatt bluetoothGatt;

    private Queue<Runnable> commandQueue;
    private boolean commandQueueBusy;

    private final Binder binder = new LocalBinder();

    // TODO: remove this later
    public static final String TAG = "BluetoothLeService";

    private BluetoothAdapter bluetoothAdapter;

    Handler bleHandler = new Handler();
    private boolean isRetrying;
    private int nrTries;


    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;
        return bluetoothGatt.getServices();
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean initialize() {
        Log.w(TAG, "Init called");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        try {
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            // connect to the GATT server on the device
//            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback, TRANSPORT_LE);

            return true;
        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "Device not found with provided address. Unable to connect.");
            return false;
        }
    }

    private void broadcastUpdate(final String action) {
        Log.w(TAG, "ACTION - " + action);
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
//        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
//            int flag = characteristic.getProperties();
//            int format = -1;
//            if ((flag & 0x01) != 0) {
//                format = BluetoothGattCharacteristic.FORMAT_UINT16;
//                Log.d(TAG, "Heart rate format UINT16.");
//            } else {
//                format = BluetoothGattCharacteristic.FORMAT_UINT8;
//                Log.d(TAG, "Heart rate format UINT8.");
//            }
//            final int heartRate = characteristic.getIntValue(format, 1);
//            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
//            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
//        } else {
        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
                    stringBuilder.toString());
        }
//        }
        sendBroadcast(intent);
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                broadcastUpdate(ACTION_GATT_CONNECTED);
                Log.w(TAG, "ACTION_GATT_CONNECTED");
                // Attempts to discover services after successful connection.
                bluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.w(TAG, "onServicesDiscovered START: " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "IF START");
                retrieveServicesAndCharacteristics();
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.w(TAG, "IF END");
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
            Log.w(TAG, "onServicesDiscovered END");
        }

        @Override
        public void onCharacteristicRead(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
    };

    private void retrieveServicesAndCharacteristics() {
        for (BluetoothGattService gattService : getSupportedGattServices()) {
            if (gattService.getUuid().toString().contains(MUSIC_SERVICE_UUID)) {
                System.out.println("Music service found!");
                System.out.println(gattService.getCharacteristics());
                for (BluetoothGattCharacteristic bluetoothGattCharacteristic : gattService.getCharacteristics()) {
                    Log.w(TAG, "CHAR DETECTED - " + bluetoothGattCharacteristic.getUuid().toString());

                    if (bluetoothGattCharacteristic.getUuid().toString().contains(PLAY_SONG_CHAR_UUID)) {
                        this.playSongChar = bluetoothGattCharacteristic;
                    }
                }

            }
            if (gattService.getUuid().toString().contains(LED_SERVICE_UUID)) {
                for (BluetoothGattCharacteristic bluetoothGattCharacteristic : gattService.getCharacteristics()) {
                    Log.w(TAG, "CHAR DETECTED - " + bluetoothGattCharacteristic.getUuid().toString());
                    if (bluetoothGattCharacteristic.getUuid().toString().contains(LED_CHAR_UUID)) {
                        this.ledChar = bluetoothGattCharacteristic;
                    }
                }

                System.out.println("LED service found!");
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.w(TAG, "onBind called");

        return binder;
    }

    public void ledToggle() {
        Log.w(TAG, "LED CHAR write");
        String hexValue = "";
//        byte[] bytes = ByteBuffer.allocate(4).putInt(0).array();
        byte[] bytes = "a".getBytes();

        System.out.println(Arrays.toString(bytes));
        this.ledChar.setValue(bytes);
        this.ledChar.setWriteType(1);

        System.out.println(this.ledChar);
        System.out.println(this.ledChar.getWriteType());
        if (!bluetoothGatt.writeCharacteristic(this.ledChar)) {
            Log.w(TAG, String.format("ERROR: writeCharacteristic failed for characteristic: %s", this.ledChar.getUuid()));
            completedCommand();
        } else {
            Log.w(TAG, String.format("writing <%s> to characteristic <%s>", new String(bytes), this.ledChar.getUuid()));
            nrTries++;
        }
//        this.bluetoothGatt.writeCharacteristic(this.playSongChar);
    }

    public void playSong(int currentSongIndex) {

        this.playSongChar.setValue(currentSongIndex, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        this.playSongChar.setWriteType(1);

        if (!bluetoothGatt.writeCharacteristic(this.playSongChar)) {
            Log.e(TAG, String.format("ERROR: writeCharacteristic failed for characteristic: %s", this.playSongChar.getUuid()));
            completedCommand();
        } else {
            Log.d(TAG, String.format("writing <%s> to characteristic <%s>", currentSongIndex, this.playSongChar.getUuid()));
            nrTries++;
        }
    }

    private void nextCommand() {
        // If there is still a command being executed then bail out
        if (commandQueueBusy) {
            return;
        }

        // Check if we still have a valid gatt object
        if (bluetoothGatt == null) {
            Log.e(TAG, String.format("ERROR: GATT is 'null' for peripheral '%s', clearing command queue"));
            commandQueue.clear();
            commandQueueBusy = false;
            return;
        }

        // Execute the next command in the queue
        if (commandQueue.size() > 0) {
            final Runnable bluetoothCommand = commandQueue.peek();
            commandQueueBusy = true;
            nrTries = 0;

            bleHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        bluetoothCommand.run();
                    } catch (Exception ex) {
                        Log.e(TAG, String.format("ERROR: Command exception"), ex);
                    }
                }
            });
        }
    }

    private void completedCommand() {
        commandQueueBusy = false;
        isRetrying = false;
        commandQueue.poll();
        nextCommand();
    }

    class LocalBinder extends Binder {
        public MyBluetoothService getService() {
            return MyBluetoothService.this;
        }
    }
}
