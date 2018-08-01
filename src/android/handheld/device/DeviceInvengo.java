package com.izouma.handheld.device;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.izouma.handheld.ReadTagOptions;
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

public class DeviceInvengo extends Device implements Barcode2DWithSoft.ScanCallback, IMessageNotificationReceivedHandle {
    private static final String TAG     = "DeviceInvengo";
    private final        Object lockObj = new Object();

    private static final int SCAN_STATE_OFF    = 0;
    private static final int SCAN_STATE_IDLE   = 1;
    private static final int SCAN_STATE_DECODE = 2;
    private static final int RFID_STATE_OFF    = 0;
    private static final int RFID_STATE_IDLE   = 1;
    private static final int RFID_STATE_READ   = 2;

    private static final int RFID_MSG_CONNECT    = 0;
    private static final int RFID_MSG_DISCONNECT = 1;
    private static final int RFID_MSG_READ       = 2;
    private static final int RFID_MSG_STOP       = 3;

    private static final byte SYS_CONF_POWER    = 0x65;
    private static final byte SYS_CONF_MODE     = 0x6D;
    private static final byte SYS_CONF_INTERVAL = 0x12;

    private static final int KEYCODE_TRIGGER = 300; //扳机键

    private int scanState = SCAN_STATE_OFF;
    private int rfidState = RFID_STATE_OFF;

    private Context           context;
    private ScanCodeListener  scanCodeListener;
    private ReadTagListener   readTagListener;
    private AlertDialog       dialog;
    private Barcode2DWithSoft bcr;
    private VoiceManager      mVoiceManager;
    private Reader            reader;
    private ProgressDialog    progressDialog;

    private Handler rfidHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case RFID_MSG_CONNECT:
                    switch (msg.arg1) {
                        case 0:
                            break;
                        case 1:
                            showToast("RFID已连接");
                            break;
                        case 2:
                            showToast("RFID连接失败");
                            break;
                    }
                    break;
                case RFID_MSG_DISCONNECT:
                    break;
                case RFID_MSG_READ:
                    break;
                case RFID_MSG_STOP:
                    break;
            }
            return false;
        }
    });


    public DeviceInvengo(Context context) {
        this.context = context;
    }

    @Override
    public void init() {
        bcr = Barcode2DWithSoft.getInstance();
        progressDialog = new ProgressDialog(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        mVoiceManager = VoiceManager.getInstance(context.getApplicationContext());
        new Thread(new Runnable() {
            @Override
            public void run() {
                connectReader();
            }
        }).start();
    }

    @Override
    public void scanCode(ScanCodeListener listener) {
        this.scanCodeListener = listener;
        dialog = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setCancelable(false)
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KEYCODE_TRIGGER) {
                            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                startDecode();
                            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                                bcr.stopScan();
                                scanState = SCAN_STATE_IDLE;
                            }
                        }
                        return false;
                    }
                })
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
        bcr.stopScan();
        if (dialog != null) {
            dialog.dismiss();
        }
        scanState = SCAN_STATE_IDLE;
    }

    @Override
    public void readTag(ReadTagOptions options, ReadTagListener listener) {
        this.readTagListener = listener;
        new Thread(new Runnable() {
            public void run() {
                if (rfidState != RFID_STATE_IDLE)
                    return;
                SysConfig_800 configPower = new SysConfig_800(SYS_CONF_POWER, new byte[]{2, 0, 12});
                reader.send(configPower);

                SysConfig_800 configMode = new SysConfig_800(SYS_CONF_MODE, new byte[]{2});
                reader.send(configMode);

                ReadTag readTag = new ReadTag(ReadTag.ReadMemoryBank.EPC_6C);

                if (reader.send(readTag)) {
                    rfidState = RFID_STATE_READ;
                }
            }
        }).start();
    }

    @Override
    public void stopRead() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                stopReader();
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        openDecoder();
    }

    @Override
    public void onPause() {
        super.onPause();
        new Thread(new Runnable() {
            @Override
            public void run() {
                stopReader();
            }
        }).start();
        stopScan();
        releaseDecoder();
    }

    @Override
    public void destroy() {
        super.destroy();
        stopScan();
        releaseDecoder();
        new Thread(new Runnable() {
            @Override
            public void run() {
                disconnectReader();
            }
        }).start();
    }

    @Override
    public void onScanComplete(int symbology, int length, byte[] data) {
        if (scanState == SCAN_STATE_DECODE)
            scanState = SCAN_STATE_IDLE;

        if (length > 0) {
            bcr.stopScan();

            if (symbology == 0x99) {// type 99?
                symbology = data[0];
                int n = data[1];
                int s = 2;
                int d = 0;
                int len = 0;

                byte d99[] = new byte[data.length];
                for (int i = 0; i < n; ++i) {
                    s += 2;
                    len = data[s++];
                    System.arraycopy(data, s, d99, d, len);
                    s += len;
                    d += len;
                }
                d99[d] = 0;
                data = d99;
            }
            String result = null;
            try {
                result = new String(data).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
            playBeep();
            stopScan();
            if (!TextUtils.isEmpty(result) && scanCodeListener != null) {
                scanCodeListener.onScanResult(result);
            }
        }
    }

    @Override
    public void messageNotificationReceivedHandle(BaseReader baseReader, IMessageNotification msg) {
        playBeep();
        if (msg instanceof RXD_TagData) {
            RXD_TagData data = (RXD_TagData) msg;
            String epc = Util.convertByteArrayToHexString(data.getReceivedMessage().getEPC());
            String rssi = String.valueOf(data.getReceivedMessage().getRSSI()[0] & 0xFF);
            TagData tagData = new TagData();
            tagData.setEpc(epc);
            tagData.setRssi(rssi);
            if (readTagListener != null && rfidState == RFID_STATE_READ) {
                readTagListener.onReadData(tagData);
            }
        }
    }

    private void openDecoder() {
        if (bcr.open(context)) {
            bcr.setScanCallback(this);
            bcr.setParameter(765, 0);
            bcr.setParameter(136, 50);
            bcr.enableAllCodeTypes();
            scanState = SCAN_STATE_IDLE;
        } else {
            showToast("扫码模块连接失败");
        }
    }

    private void startDecode() {
        if (scanState != SCAN_STATE_IDLE)
            return;
        scanState = SCAN_STATE_DECODE;
        bcr.scan(); // start decode (callback gets results)
    }

    private void releaseDecoder() {
        bcr.close();
        scanState = SCAN_STATE_OFF;
    }

    private void connectReader() {
        synchronized (lockObj) {
            Message msg = new Message();
            msg.what = RFID_MSG_CONNECT;
            msg.arg1 = 0;
            rfidHandler.sendMessage(msg);
            reader = new Reader("Reader1", "RS232", IntegrateReaderManager.getPortName() + ",115200");
            if (reader.connect()) {
                reader.onMessageNotificationReceived.add(DeviceInvengo.this);
                rfidState = RFID_STATE_IDLE;
                Message msg1 = new Message();
                msg.what = RFID_MSG_CONNECT;
                msg.arg1 = 1;
                rfidHandler.sendMessage(msg1);
                Buzzer_500 buzzer_500 = new Buzzer_500((byte) 1);
                reader.send(buzzer_500);
            } else {
                Message msg1 = new Message();
                msg.what = RFID_MSG_CONNECT;
                msg.arg1 = 2;
                rfidHandler.sendMessage(msg1);
            }
        }
    }

    private void stopReader() {
        synchronized (lockObj) {
            if (rfidState == RFID_STATE_READ) {
                if (reader.send(new PowerOff())) {
                    rfidState = RFID_STATE_IDLE;
                    Message msg = new Message();
                    msg.what = RFID_MSG_STOP;
                    rfidHandler.sendMessage(msg);
                }
            }
        }
    }

    private void disconnectReader() {
        synchronized (lockObj) {
            if (rfidState != RFID_STATE_OFF) {
                reader.send(new PowerOff());
                reader.disConnect();
                rfidState = RFID_STATE_OFF;
                reader = null;
            }
            Message msg = new Message();
            msg.what = RFID_MSG_DISCONNECT;
            rfidHandler.sendMessage(msg);
        }
    }

    private void showToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private void playBeep() {
        mVoiceManager.playSound(VoiceManager.SUCCESS_SOUND, VoiceManager.SOUND_NO_LOOP_MODE);
    }
}
