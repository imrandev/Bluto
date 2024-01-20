package com.imran.bluto;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

public interface OnDeviceClickListener {

    void onClick(Context context, BluetoothDevice bluetoothDevice);
}
