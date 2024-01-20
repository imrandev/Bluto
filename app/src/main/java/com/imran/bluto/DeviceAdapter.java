package com.imran.bluto;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.util.List;

public class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {

    private final OnDeviceClickListener deviceClickListener;

    public DeviceAdapter(@NonNull Context context, List<BluetoothDevice> objects, OnDeviceClickListener deviceClickListener) {
        super(context, R.layout.item_device, objects);
        this.deviceClickListener = deviceClickListener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        BluetoothDevice device = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_device, parent, false);
        }

        TextView tvName = convertView.findViewById(R.id.item_name);
        Button btnConnect = convertView.findViewById(R.id.btn_connect);

        btnConnect.setOnClickListener(view -> deviceClickListener.onClick(getContext(), device));

        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return convertView;
        }
        tvName.setText(device.getName());
        return convertView;
    }
}
