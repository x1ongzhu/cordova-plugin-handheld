package com.izouma.handheld.device;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.izouma.handheld.ScanActivity;

import org.apache.cordova.CordovaPlugin;

/**
 * Created by xiongzhu on 2018/3/20.
 */

public class DeviceCommon extends Device {
    private CordovaPlugin cordovaPlugin;
    private ScanCodeListener listener;

    public DeviceCommon(CordovaPlugin cordovaPlugin) {
        this.cordovaPlugin = cordovaPlugin;
    }

    @Override
    public void init() {

    }

    @Override
    public void scanCode(ScanCodeListener listener) {
        if (ContextCompat.checkSelfPermission(cordovaPlugin.cordova.getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cordovaPlugin.requestPermissions(0);
        } else {
            this.listener = listener;
            Intent intent = new Intent(cordovaPlugin.cordova.getActivity(), ScanActivity.class);
            cordovaPlugin.cordova.startActivityForResult(cordovaPlugin, intent, 1);
        }
    }

    @Override
    public void stopScan() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == Activity.RESULT_OK) {
            String result = intent.getStringExtra("result");
            if (!TextUtils.isEmpty(result) && listener != null) {
                listener.onScanResult(result);
            }
        }
    }
}
