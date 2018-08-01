package com.izouma.handheld.device;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;


/**
 * Created by xiongzhu on 2018/3/20.
 */

public class DeviceHoneywell extends Device {
    public static final String ACTION_SCAN = "com.android.server.scannerservice.broadcast";
    private Context context;
    private BroadcastReceiver receiver;
    private ScanCodeListener listener;
    private boolean handleResult = false;
    private AlertDialog dialog;

    public DeviceHoneywell(Context context) {
        this.context = context;
    }

    @Override
    public void init() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_SCAN.equals(intent.getAction())) {
                    String result = intent.getStringExtra("scannerdata");
                    if (!TextUtils.isEmpty(result) && listener != null && handleResult) {
                        listener.onScanResult(result);
                    }
                    stopScan();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SCAN);
        context.registerReceiver(receiver, filter);
    }

    @Override
    public void scanCode(ScanCodeListener listener) {
        this.listener = listener;
        handleResult = true;
        sendBroadcast(true);
        dialog = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setCancelable(false)
                .setMessage("请按住扫描按钮，将激光对准 二维码/条形码 进行扫描")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        stopScan();
                    }
                }).show();
    }

    @Override
    public void stopScan() {
        sendBroadcast(false);
        handleResult = false;
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public void destroy() {
        context.unregisterReceiver(receiver);
        stopScan();
    }

    private void sendBroadcast(boolean type) {
        Intent intent = new Intent();
        intent.setAction(type ? "com.izouma.scanservice.start" : "com.izouma.scanservice.stop");
        context.sendBroadcast(intent);
    }
}
