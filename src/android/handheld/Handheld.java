package com.izouma.handheld;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;

import com.google.gson.Gson;
import com.izouma.handheld.device.Device;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * This class echoes a string called from JavaScript.
 */
public class Handheld extends CordovaPlugin {
    private Device          device;
    private CallbackContext callbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        device = DeviceFactory.getDevice(this);
        device.init();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(0);
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        device.onResume();
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        device.onPause();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("pause")) {
            device.onPause();
            callbackContext.success();
            return true;
        } else if (action.equals("resume")) {
            device.onResume();
            callbackContext.success();
            return true;
        } else if (action.equals("scanCode")) {
            this.scanCode(callbackContext);
            return true;
        } else if (action.equals("stopScan")) {
            device.stopScan();
            callbackContext.success();
            return true;
        } else if (action.equals("readTag")) {
            ReadTagOptions options = null;
            try {
                JSONObject jsonObject = args.getJSONObject(0);
                Gson gson = new Gson();
                options = gson.fromJson(jsonObject.toString(), ReadTagOptions.class);
                this.readTag(callbackContext, options);
            } catch (Exception e) {
                e.printStackTrace();
                callbackContext.error(e.getMessage());
            }
            return true;
        } else if (action.equals("stopRead")) {
            device.stopRead();
            callbackContext.success();
            return true;
        } else if (action.equals("writeTag")) {
            try {
                String tagData = args.getString(0);
                writeTag(tagData, callbackContext);
            } catch (Exception e) {
                e.printStackTrace();
                callbackContext.error(e.getMessage());
            }
            return true;
        } else if (action.equals("getInfo")) {
            getInfo(callbackContext);
            return true;
        }
        return false;
    }

    private void scanCode(final CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
        device.scanCode(result -> {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
            pluginResult.setKeepCallback(false);
            callbackContext.sendPluginResult(pluginResult);
        });
    }

    private void readTag(final CallbackContext callbackContext, ReadTagOptions options) {
        if (options == null) {
            options = new ReadTagOptions();
        }
        device.readTag(options, new Device.ReadTagListener() {
            @Override
            public void onReadData(TagData tagData) {
                Gson gson = new Gson();
                try {
                    JSONObject jsonObject = new JSONObject(gson.toJson(tagData));
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
                    pluginResult.setKeepCallback(false);
                    callbackContext.sendPluginResult(pluginResult);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onReadData(List<TagData> list) {
                Gson gson = new Gson();
                try {
                    JSONArray jsonArray = new JSONArray(gson.toJson(list));
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonArray);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void writeTag(String tagData, final CallbackContext callbackContext) {
        if (TextUtils.isEmpty(tagData)) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "数据格式错误");
            pluginResult.setKeepCallback(false);
            callbackContext.sendPluginResult(pluginResult);
            return;
        }
        if (tagData.length() != 24) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "长度必须为24");
            pluginResult.setKeepCallback(false);
            callbackContext.sendPluginResult(pluginResult);
            return;
        }
        device.writeTag(tagData, new Device.WriteTagListener() {
            @Override
            public void onWriteSuccess() {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                pluginResult.setKeepCallback(false);
                callbackContext.sendPluginResult(pluginResult);
            }

            @Override
            public void onWriteFail(String error) {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, error);
                pluginResult.setKeepCallback(false);
                callbackContext.sendPluginResult(pluginResult);
            }
        });
    }

    private void getInfo(CallbackContext callbackContext) {
        try {
            PackageInfo info = cordova.getActivity().getPackageManager().getPackageInfo(cordova.getActivity().getPackageName(), 0);
            String versionName = info.versionName;
            String versionCode = String.valueOf(info.versionCode);
            List<String> ipList = new ArrayList<>();
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        ipList.add(inetAddress.getHostAddress());
                    }
                }
            }

            Map<String, String> wifiInfo = NetworkTool.getWifiInfo(cordova.getActivity());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("versionName", versionName);
            jsonObject.put("versionCode", versionCode);
            jsonObject.put("mac", NetworkTool.getMac(cordova.getActivity()));
            jsonObject.put("rssi", wifiInfo.get("rssi"));
            jsonObject.put("ip", wifiInfo.get("ip"));

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
            pluginResult.setKeepCallback(false);
            callbackContext.sendPluginResult(pluginResult);
        } catch (Exception e) {
            e.printStackTrace();
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
            pluginResult.setKeepCallback(false);
            callbackContext.sendPluginResult(pluginResult);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        device.destroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        device.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public void requestPermissions(int requestCode) {
        super.requestPermissions(requestCode);
        cordova.requestPermissions(this, requestCode, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
                Manifest.permission.ACCESS_WIFI_STATE});
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) {
            if (requestCode == 1) {
                device.scanCode(result -> {
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                    pluginResult.setKeepCallback(false);
                    callbackContext.sendPluginResult(pluginResult);
                });
            }
        } else {
            new AlertDialog.Builder(cordova.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                    .setTitle("提示")
                    .setMessage("未能获得权限")
                    .setPositiveButton("打开设置", (dialog, which) -> {
                        dialog.dismiss();
                        Intent localIntent = new Intent();
                        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                        localIntent.setData(Uri.fromParts("package", cordova.getActivity().getPackageName(), null));
                        cordova.getActivity().startActivity(localIntent);
                    })
                    .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
        }
    }
}
