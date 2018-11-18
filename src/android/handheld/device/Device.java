package com.izouma.handheld.device;

import android.content.Intent;
import android.view.KeyEvent;

import com.izouma.handheld.ReadTagOptions;
import com.izouma.handheld.TagData;

import java.util.List;

/**
 * Created by xiongzhu on 2018/3/20.
 */

public abstract class Device {
    protected final static String TAG = "Handheld Device";

    public void init() {
    }

    public void onPause() {
    }

    public void onResume() {
    }

    public void destroy() {
    }

    public void scanCode(ScanCodeListener listener) {
    }

    public void stopScan() {
    }

    public void readTag(ReadTagOptions options, ReadTagListener listener) {
    }

    public void writeTag(String tagData, WriteTagListener listener) {
    }

    public void stopRead() {
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    }

    public interface ScanCodeListener {
        void onScanResult(String result);
    }

    public interface ReadTagListener {
        void onReadData(TagData tagData);

        void onReadData(List<TagData> list);
    }

    public interface WriteTagListener {
        void onWriteSuccess();

        void onWriteFail(String error);
    }
}
