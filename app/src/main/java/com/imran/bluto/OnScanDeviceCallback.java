package com.imran.bluto;

import android.bluetooth.BluetoothDevice;

import java.util.List;

public interface OnScanDeviceCallback {

    void onReceive(List<BluetoothDevice> devices);
}
