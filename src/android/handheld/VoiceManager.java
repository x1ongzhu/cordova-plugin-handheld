package com.izouma.handheld;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.SparseIntArray;

public class VoiceManager {
    public static final int SOUND_NO_LOOP_MODE = 0;
    public static final int SOUND_LOOP_MODE = -1;

    public static final int TAG_SOUND = 0;
    public static final int SUCCESS_SOUND = 1;
    public static final int ERROR_SOUND = 2;
    public static final int BEEP_SOUND = 3;
    public static final int FAIL_SOUND = 4;

    private SoundPool mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
    private SparseIntArray mSoundPoolMap = new SparseIntArray();

    private void initSoundPool(Context context) {
        this.context = context;
        mSoundPoolMap.append(TAG_SOUND, mSoundPool.load(context, context.getResources().getIdentifier("tag", "raw", context.getPackageName()), 1));
        mSoundPoolMap.append(SUCCESS_SOUND, mSoundPool.load(context, context.getResources().getIdentifier("success", "raw", context.getPackageName()), 1));
        mSoundPoolMap.append(ERROR_SOUND, mSoundPool.load(context, context.getResources().getIdentifier("error", "raw", context.getPackageName()), 1));
        mSoundPoolMap.append(BEEP_SOUND, mSoundPool.load(context, context.getResources().getIdentifier("beep", "raw", context.getPackageName()), 1));
        mSoundPoolMap.append(FAIL_SOUND, mSoundPool.load(context, context.getResources().getIdentifier("fail", "raw", context.getPackageName()), 1));
    }

    private Context context;
    private static VoiceManager mVoiceManager = null;

    private VoiceManager(Context context) {
        initSoundPool(context);
    }

    public static synchronized VoiceManager getInstance(Context context) {
        if (null == mVoiceManager) {
            mVoiceManager = new VoiceManager(context);
        }
        return mVoiceManager;
    }

    public void playSound(final int sound, final int loop) {
        AudioManager audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        float streamVolumeCurrent = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float streamVolumeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        final float volume = streamVolumeCurrent / streamVolumeMax;
//		mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
//			@Override
//			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        mSoundPool.play(mSoundPoolMap.get(sound), volume, volume, 1, loop, 1f);
//			}
//		});
    }

}
