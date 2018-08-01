package com.izouma.handheld.device;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.Toast;

import com.izouma.handheld.TagData;
import com.izouma.handheld.VoiceManager;
import com.zebra.adc.decoder.Barcode2DWithSoft;

import invengo.javaapi.core.BaseReader;
import invengo.javaapi.core.IMessageNotification;
import invengo.javaapi.core.Util;
import invengo.javaapi.handle.IMessageNotificationReceivedHandle;
import invengo.javaapi.protocol.IRP1.Buzzer_500;
import invengo.javaapi.protocol.IRP1.IntegrateReaderManager;
import invengo.javaapi.protocol.IRP1.PowerOff;
import invengo.javaapi.protocol.IRP1.RXD_TagData;
import invengo.javaapi.protocol.IRP1.ReadTag;
import invengo.javaapi.protocol.IRP1.Reader;
import invengo.javaapi.protocol.IRP1.SysConfig_800;


/**
 * Created by xiongzhu on 2018/3/20.
 */

public class DeviceC5000 extends Device {
    private static final String TAG         = "DeviceC5000";
    private static final String ACTION_SCAN = "shmaker.android.intent.action.SCANER_DECODE_DATA";

    private Context           context;
    private ScanCodeListener  scanCodeListener;
    private BroadcastReceiver receiver;
    private AlertDialog       dialog;
    private boolean handleResult = false;

    public DeviceC5000(Context context) {
        this.context = context;
    }

    @Override
    public void init() {
        super.init();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_SCAN.equals(intent.getAction())) {
                    String result = intent.getStringExtra("extra_decode_data");
                    if (!TextUtils.isEmpty(result) && scanCodeListener != null && handleResult) {
                        scanCodeListener.onScanResult(result);
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
        this.scanCodeListener = listener;
        handleResult = true;
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

    private void showToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
