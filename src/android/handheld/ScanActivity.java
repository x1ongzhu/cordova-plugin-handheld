package com.izouma.handheld;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;

public class ScanActivity extends Activity implements QRCodeView.Delegate, View.OnClickListener {

    private QRCodeView mQRCodeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(getResources().getIdentifier("activity_scan", "layout", getPackageName()));
        findViewById(getResources().getIdentifier("btn_flash", "id", getPackageName())).setOnClickListener(this);
        findViewById(getResources().getIdentifier("btn_back", "id", getPackageName())).setOnClickListener(this);
        mQRCodeView = (ZXingView) findViewById(getResources().getIdentifier("zxingview", "id", getPackageName()));
        mQRCodeView.setDelegate(this);
        mQRCodeView.startSpotAndShowRect();
    }

    @Override
    public void onScanQRCodeSuccess(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        intent.putExtra("result", s);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).setMessage("无法打开摄像头").create().show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == getResources().getIdentifier("btn_flash", "id", getPackageName())) {
            if (v.isActivated()) {
                mQRCodeView.closeFlashlight();
                v.setActivated(false);
            } else {
                mQRCodeView.openFlashlight();
                v.setActivated(true);
            }
        } else if (v.getId() == getResources().getIdentifier("btn_back", "id", getPackageName())) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mQRCodeView.stopCamera();
        mQRCodeView.closeFlashlight();
    }
}
