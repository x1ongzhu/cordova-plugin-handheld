package com.izouma.handheld.device;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.izouma.handheld.ReadTagOptions;
import com.izouma.handheld.TagData;
import com.izouma.handheld.VoiceManager;
import com.uhf.api.cls.Reader;
import com.uhf.uhf.Common.Comm;
import com.uhf.uhf.Common.InventoryBuffer;
import com.uhf.uhf.UHF1.UHF001;
import com.uhf.uhf.UHF1.UHF1Application;
import com.uhf.uhf.UHF1Function.SPconfig;
import com.uhf.uhf.UHF6.UHF006;

import java.util.ArrayList;
import java.util.List;

import static com.uhf.uhf.Common.Comm.lsTagList;
import static com.uhf.uhf.Common.Comm.operateType.nullOperate;
import static com.uhf.uhf.Common.Comm.soundPool;
import static com.uhf.uhf.Common.Comm.tag;

/**
 * Created by xiongzhu on 2018/3/20.
 */

public class DeviceSupoin extends Device {
    private static final String ACTION_ON_OFF = "com.android.server.scannerservice.onoff";
    private static final String ACTION_SCAN   = "com.android.server.scannerservice.broadcast";

    private Context           context;
    private ScanCodeListener  scanCodeListener;
    private ReadTagListener   readTagListener;
    private BroadcastReceiver receiver;
    private AlertDialog       dialog;
    private VoiceManager      mVoiceManager;
    private boolean           singleMode;
    private int               soundId;
    private boolean handleResult = false;

    @SuppressLint("HandlerLeak")
    private Handler connectH = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                UHF001.mhandler = uhfhandler;
                if (null != Comm.rfidOperate)
                    Comm.rfidOperate.mHandler = uhfhandler;
                if (null != Comm.uhf6)
                    UHF006.UHF6handler = uhfhandler;

                Bundle bd = msg.getData();
                String strMsg = bd.get("Msg").toString();
                if (!TextUtils.isEmpty(strMsg)) {
                    Toast.makeText(context, strMsg, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "模块初始化失败", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("connectH", e.getMessage());
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler uhfhandler = new Handler() {
        @SuppressWarnings({"unchecked", "unused"})
        @Override
        public void handleMessage(Message msg) {
            try {
                Comm.tagListSize = Comm.lsTagList.size();
                Bundle bd = msg.getData();
                int readCount = bd.getInt("readCount");
                if (readCount > 0 && Comm.tagListSize > 0) {
                    List<TagData> list = new ArrayList<TagData>();
                    for (InventoryBuffer.InventoryTagMap tagMap : lsTagList) {
                        TagData tagData = new TagData();
                        tagData.setEpc(tagMap.strEPC);
                        tagData.setRssi(tagMap.strRSSI);
                        list.add(tagData);
                    }
                    readTagListener.onReadData(list);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private android.os.Handler tagOpehandler = new android.os.Handler() {
        @SuppressWarnings({"unchecked", "unused"})
        @Override
        public void handleMessage(Message msg) {
            playBeep();
            try {
                Bundle b = msg.getData();
                switch (Comm.opeT) {
                    case readOpe:
                        String strErr = b.getString("Err");
                        String strEPC = b.getString("readData");
                        if (!TextUtils.isEmpty(strEPC)) {
                            TagData tagData = new TagData();
                            tagData.setEpc(strEPC);
                            readTagListener.onReadData(tagData);
                        } else {
                            Log.e(TAG, "Read Fail: " + strErr);
                        }
                        break;
                    case writeOpe:
                        boolean isWriteSucceed = b.getBoolean("isWriteSucceed");
                        if (isWriteSucceed) {
                            Toast.makeText(context, "Write Succeed", Toast.LENGTH_SHORT).show();
                            Log.d("UHF", "Write Succeed");
                        } else {
                            Toast.makeText(context, "Write Fail", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case writeepcOpe:
                        boolean isWriteEPCSucceed = b.getBoolean("isWriteSucceed");
                        if (isWriteEPCSucceed) {
                            Toast.makeText(context, "Write Succeed", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Write Fail", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case lockOpe:
                        boolean isLockSucceed = b.getBoolean("isLockSucceed");
                        if (isLockSucceed)
                            Toast.makeText(context, "Lock Succeed", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(context, "Lock Fail", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Comm.opeT = nullOperate;
        }
    };

    public DeviceSupoin(Context context) {
        this.context = context;
    }

    @Override
    public void init() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_ON_OFF.equals(intent.getAction())) {
                    // TODO: 2018/3/20  handle on/off action
                } else if (ACTION_SCAN.equals(intent.getAction())) {
                    String result = intent.getStringExtra("scannerdata");
                    if (!TextUtils.isEmpty(result) && scanCodeListener != null && handleResult) {
                        scanCodeListener.onScanResult(result);
                    }
                    stopScan();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ON_OFF);
        filter.addAction(ACTION_SCAN);
        context.registerReceiver(receiver, filter);
        mVoiceManager = VoiceManager.getInstance(context.getApplicationContext());

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
    public void readTag(ReadTagOptions options, ReadTagListener listener) {
        this.readTagListener = listener;
        this.singleMode = options.isSingle();
        if (singleMode) {
            Comm.opeT = Comm.operateType.readOpe;
            Comm.readTag(0, 1, "6", "2", 0);
        } else {
            Comm.startScan();
        }
    }

    @Override
    public void stopRead() {
        Comm.stopScan();
    }

    @Override
    public void onResume() {
        super.onResume();
        Comm.repeatSound = true;
        Comm.app = new UHF1Application();
        Comm.spConfig = new SPconfig(context);
        Comm.soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
        soundId = soundPool.load(context, context.getResources().getIdentifier("success", "raw", context.getPackageName()), 1);
        Comm.checkDevice();
        if (!Comm.powerUp()) {
            Comm.powerDown();
        }
        if (Comm.powerUp()) {
            Toast.makeText(context, "success", Toast.LENGTH_SHORT).show();
            Comm.connecthandler = connectH;
            Comm.mRWLHandler = tagOpehandler;
            Comm.Connect();

            int[] val = new int[]{-1};
            val[0] = 0;
            UHF001.er = Comm.myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAGDATA_RECORDHIGHESTRSSI, val);
            if (UHF001.er == Reader.READER_ERR.MT_OK_ERR) {
                Comm.myapp.Rparams.session = val[0];
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null != UHF001.UHF1handler)
            Comm.stopScan();
        if (null != Comm.myapp.Mreader)
            Comm.myapp.Mreader.CloseReader();
        if (null != Comm.myapp.Rpower)
            Comm.myapp.Rpower.PowerDown();
        if (null != Comm.baseTabFragment.mReader) {
            Comm.baseTabFragment.mReader.free();
        }
        Comm.powerDown();
    }

    @Override
    public void destroy() {
        context.unregisterReceiver(receiver);
        stopScan();
    }

    private void playBeep() {
        soundPool.play(soundId, 1, 1, 1, 0, 1f);
    }
}
