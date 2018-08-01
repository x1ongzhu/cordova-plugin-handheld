package com.izouma.handheld;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

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

import java.util.List;

/**
 * This class echoes a string called from JavaScript.
 */
public class Handheld extends CordovaPlugin {
    private Device device;
    private CallbackContext callbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        device = DeviceFactory.getDevice(this);
        device.init();
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
        if (action.equals("scanCode")) {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.readTag(callbackContext, options);
            return true;
        } else if (action.equals("stopRead")) {
            device.stopRead();
            callbackContext.success();
            return true;
        } else if (action.equals("pause")) {
            device.onPause();
            callbackContext.success();
            return true;
        } else if (action.equals("resume")) {
            device.onResume();
            callbackContext.success();
            return true;
        }
        return false;
    }

    private void scanCode(final CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
        device.scanCode(new Device.ScanCodeListener() {
            @Override
            public void onScanResult(String result) {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                pluginResult.setKeepCallback(false);
                callbackContext.sendPluginResult(pluginResult);
            }
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
        cordova.requestPermissions(this, requestCode, new String[]{Manifest.permission.CAMERA});
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            device.scanCode(new Device.ScanCodeListener() {
                @Override
                public void onScanResult(String result) {
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                    pluginResult.setKeepCallback(false);
                    callbackContext.sendPluginResult(pluginResult);
                }
            });
        } else {
            new AlertDialog.Builder(cordova.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                    .setTitle("提示")
                    .setMessage("需要相机权限")
                    .setPositiveButton("打开设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent localIntent = new Intent();
                            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                            localIntent.setData(Uri.fromParts("package", cordova.getActivity().getPackageName(), null));
                            cordova.getActivity().startActivity(localIntent);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();
        }
    }
}
