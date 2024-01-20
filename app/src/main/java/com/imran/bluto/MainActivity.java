package com.imran.bluto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ListView scannedLv;

    private ListView pairedLv;

    private BluetoothController bluetoothController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothController =  new AndroidBluetoothController(this, onScanDeviceCallback);

        scannedLv = findViewById(R.id.li_scanned_devices);
        pairedLv = findViewById(R.id.li_paired_devices);

        Button startServerButton = findViewById(R.id.btn_start_server);
        Button stopServerButton = findViewById(R.id.btn_stop_server);

        startServerButton.setOnClickListener(startServerButtonClickListener);
        stopServerButton.setOnClickListener(stopServerButtonClickListener);

        onPermissionRequest();
    }

    private final OnScanDeviceCallback onScanDeviceCallback = new OnScanDeviceCallback() {
        @Override
        public void onReceive(List<BluetoothDevice> devices) {
            setUpScannedList(devices);
        }
    };

    private final View.OnClickListener startServerButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            new Thread(() -> {
                try {
                    bluetoothController.startBluetoothServer();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();

        }
    };

    private final View.OnClickListener stopServerButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            new Thread(() -> {
                try {
                    bluetoothController.stopConnection();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();

        }
    };

    private void onPermissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (bluetoothController.hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                enableBluetooth();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        Constant.BLUETOOTH_CONNECT_REQUEST_CODE
                );
            }
        } else {
            enableBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constant.BLUETOOTH_CONNECT_REQUEST_CODE
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // enable bluetooth
            enableBluetooth();
        } else {
            onPermissionRequest();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!bluetoothController.hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                    onPermissionRequest();
                    return;
                }
            }
            bluetoothController.startDiscovery();
            setUpPairedList(bluetoothController.getPairedDevices());
            Log.d(TAG, "Bluetooth enabled");
        } else {
            enableBluetooth();
        }
    }

    private void enableBluetooth() {
        if (!bluetoothController.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ActivityCompat.startActivityForResult(
                    this, enableBtIntent, Constant.BLUETOOTH_ENABLE_REQUEST_CODE, null);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!bluetoothController.hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                    onPermissionRequest();
                    return;
                }
            }
            bluetoothController.startDiscovery();
            setUpPairedList(bluetoothController.getPairedDevices());
        }
    }

    void setUpScannedList(List<BluetoothDevice> deviceList){
        scannedLv.setAdapter(new DeviceAdapter(MainActivity.this, deviceList, deviceClickListener));
    }

    void setUpPairedList(List<BluetoothDevice> deviceList){
        pairedLv.setAdapter(new DeviceAdapter(MainActivity.this, deviceList, deviceClickListener));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            bluetoothController.dispose();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final OnDeviceClickListener deviceClickListener = (context, bluetoothDevice) -> {
        new Thread(() -> {
            try {
                bluetoothController.connectWithDevice(bluetoothDevice);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    };
}