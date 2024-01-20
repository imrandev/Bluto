package com.imran.bluto;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;

import java.io.IOException;
import java.util.List;

public interface BluetoothController {

    void startDiscovery();

    void stopDiscovery();

    void startBluetoothServer() throws IOException;

    void connectWithDevice(BluetoothDevice bluetoothDevice) throws IOException;

    void stopConnection() throws IOException;

    void dispose() throws IOException;

    boolean isEnabled();

    List<BluetoothDevice> getPairedDevices();

    boolean hasPermission(String permission);

    void sendMessage(String message);
}
