package com.imran.bluto;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class AndroidBluetoothController implements BluetoothController{

    private static final String TAG = "AndroidBluetoothControl";

    private final Context context;

    private final BluetoothAdapter bluetoothAdapter;

    private final List<BluetoothDevice> scannedDevices;

    private final List<BluetoothDevice> pairedDevices;

    private final OnScanDeviceCallback onScanDeviceCallback;

    private BluetoothServerSocket currentServerSocket;

    private BluetoothSocket currentClientSocket;


    public AndroidBluetoothController(Context context, OnScanDeviceCallback onScanDeviceCallback) {
        this.context = context;
        this.onScanDeviceCallback = onScanDeviceCallback;

        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            throw new SecurityException("Bluetooth unsupported!");
        }

        scannedDevices = new ArrayList<>(2);
        pairedDevices = new ArrayList<>(2);
    }

    private void registerReceiver() {
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(foundDeviceReceiver, filter);
    }

    private final BroadcastReceiver foundDeviceReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                scannedDevices.add(device);
                onScanDeviceCallback.onReceive(scannedDevices);
            }
        }
    };

    @Override
    public List<BluetoothDevice> getPairedDevices() {
        Set<BluetoothDevice> pairedDeviceSet = bluetoothAdapter.getBondedDevices();
        if (pairedDeviceSet.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            pairedDevices.addAll(pairedDeviceSet);
        }
        return pairedDevices;
    }

    @Override
    public void startDiscovery() {
        if(!hasPermission(android.Manifest.permission.BLUETOOTH_SCAN)) {
            return;
        }
        Log.d(TAG, "startDiscovery: Starting");
        registerReceiver();
        bluetoothAdapter.startDiscovery();
        Log.d(TAG, "startDiscovery: Started");
    }

    @Override
    public void stopDiscovery() {
        if(!hasPermission(android.Manifest.permission.BLUETOOTH_SCAN)) {
            return;
        }
        bluetoothAdapter.cancelDiscovery();
        Log.d(TAG, "stopDiscovery: Canceled");
    }

    @Override
    public void startBluetoothServer() throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                throw new SecurityException("No BLUETOOTH_CONNECT permission");
            }
        }

        currentServerSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                "chat_service",
                UUID.fromString(Constant.BLUETOOTH_DEVICE_UUID)
        );

        boolean shouldLoop = true;
        while(shouldLoop) {
            try {
                Log.d(TAG, "startBluetoothServer: Starting");
                currentClientSocket = currentServerSocket.accept();
                Log.d(TAG, "startBluetoothServer: Started");
            } catch(IOException e) {
                shouldLoop = false;
            }
            if (currentClientSocket != null) {
                currentServerSocket.close();
                Log.d(TAG, "startBluetoothServer: Closed");
            }
        }
    }

    @Override
    public void connectWithDevice(BluetoothDevice bluetoothDevice) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                throw new SecurityException("No BLUETOOTH_CONNECT permission");
            }
        }

        currentClientSocket = bluetoothAdapter.getRemoteDevice(bluetoothDevice.getAddress())
                .createRfcommSocketToServiceRecord(
                UUID.fromString(Constant.BLUETOOTH_DEVICE_UUID)
        );
        stopDiscovery();

        if (currentClientSocket != null){
            try {
                Log.d(TAG, "connectToDevice: Connecting");
                currentClientSocket.connect();
                Log.d(TAG, "connectToDevice: Connected");
            } catch(IOException e) {
                currentClientSocket.close();
                currentClientSocket = null;
                Log.d(TAG, "connectToDevice: Closed");
            }
        }
    }

    @Override
    public void stopConnection() throws IOException {
        if (currentClientSocket != null){
            currentClientSocket.close();
        }
        if (currentServerSocket != null){
            currentServerSocket.close();
        }
        currentClientSocket = null;
        currentServerSocket = null;
        Log.d(TAG, "closeConnection: Closed");
    }

    @Override
    public void dispose() throws IOException {
        context.unregisterReceiver(foundDeviceReceiver);
        stopConnection();
    }

    @Override
    public boolean isEnabled() {
        return bluetoothAdapter.isEnabled();
    }

    @Override
    public boolean hasPermission(String permission) {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void sendMessage(String message) {

    }
}
