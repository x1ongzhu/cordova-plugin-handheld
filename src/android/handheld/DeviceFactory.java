package com.izouma.handheld;

import android.os.Build;

import com.izouma.handheld.device.Device;
import com.izouma.handheld.device.DeviceC5000;
import com.izouma.handheld.device.DeviceCommon;
import com.izouma.handheld.device.DeviceHoneywell;
import com.izouma.handheld.device.DeviceInvengo;
import com.izouma.handheld.device.DeviceSupoin;

import org.apache.cordova.CordovaPlugin;


/**
 * Created by xiongzhu on 2018/3/20.
 */

public class DeviceFactory {
    public static Device getDevice(CordovaPlugin cordovaPlugin) {
        String model = Build.MODEL;
        String product = Build.PRODUCT;
        String manufacture = Build.MANUFACTURER;
        if ("SHT".equals(model) && "SHT".equals(product) && "SUPOIN".equals(manufacture)) {
            return new DeviceSupoin(cordovaPlugin.cordova.getActivity());
        } else if (model.contains("XC2903")) {
            return new DeviceInvengo(cordovaPlugin.cordova.getActivity());
        } else if ("full_rlk6735m_65c_1_l1".equals(product)) {
            return new DeviceHoneywell(cordovaPlugin.cordova.getActivity());
        } else if ("SKU-C5000".equals(model)) {
            return new DeviceC5000(cordovaPlugin.cordova.getActivity());
        } else {
            return new DeviceCommon(cordovaPlugin);
        }
    }
}
