package com.cloudwebrtc.webrtc.audio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.twilio.audioswitch.AudioDevice;
import com.twilio.audioswitch.AudioSwitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class AudioSwitchManager {
    @SuppressLint("StaticFieldLeak")
    public static AudioSwitchManager instance;
    @NonNull
    private final Context context;
    @NonNull
    private final AudioManager audioManager;

    public boolean loggingEnabled;
    private boolean isActive = false;
    @NonNull
    public Function2<
            ? super List<? extends AudioDevice>,
            ? super AudioDevice,
            Unit> audioDeviceChangeListener = (devices, currentDevice) -> null;

    @NonNull
    public AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = (i -> {});

    @NonNull
    public List<Class<? extends AudioDevice>> preferredDeviceList;

    // AudioSwitch is not threadsafe, so all calls should be done on the main thread.
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    private AudioSwitch audioSwitch;

    public AudioSwitchManager(@NonNull Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        preferredDeviceList = new ArrayList<>();
        preferredDeviceList.add(AudioDevice.BluetoothHeadset.class);
        preferredDeviceList.add(AudioDevice.WiredHeadset.class);
        preferredDeviceList.add(AudioDevice.Speakerphone.class);
        preferredDeviceList.add(AudioDevice.Earpiece.class);
        initAudioSwitch();
    }

    private void initAudioSwitch() {
        try {
        if (audioSwitch == null) {
                    handler.removeCallbacksAndMessages(null);
                    handler.postAtFrontOfQueue(() -> {
                        audioSwitch = new AudioSwitch(
                                context,
                                loggingEnabled,
                                audioFocusChangeListener,
                                preferredDeviceList
                        );
                        audioSwitch.start(audioDeviceChangeListener);
                    });
                }
}
catch(Exception e) {
   Log.e("FlutterWebRTCPlugin", "MY Error initAudioSwitch ", e);
}

        
        
    }

    public void start() {
        if (audioSwitch != null) {
            handler.removeCallbacksAndMessages(null);
            handler.postAtFrontOfQueue(() -> {
                if (!isActive) {
                    try {
Objects.requireNonNull(audioSwitch).activate();
                    isActive = true;
                    } catch(Exception e) {
   Log.e("FlutterWebRTCPlugin", "MY Error start ", e);
}
                    
                }
            });
        }
    }

    public void stop() {
        if (audioSwitch != null) {
            handler.removeCallbacksAndMessages(null);
            handler.postAtFrontOfQueue(() -> {
                if (isActive) {
                    Objects.requireNonNull(audioSwitch).deactivate();
                    isActive = false;
                }
            });
        }
    }

    public void setMicrophoneMute(boolean mute){
        audioManager.setMicrophoneMute(mute);
    }

    @Nullable
    public AudioDevice selectedAudioDevice() {
        return Objects.requireNonNull(audioSwitch).getSelectedAudioDevice();
    }

    @NonNull
    public List<AudioDevice> availableAudioDevices() {
        return Objects.requireNonNull(audioSwitch).getAvailableAudioDevices();
    }

    public void selectAudioOutput(@NonNull Class<? extends AudioDevice> audioDeviceClass) {
        handler.post(() -> {
            List<AudioDevice> devices = availableAudioDevices();
            AudioDevice audioDevice = null;
            for (AudioDevice device : devices) {
                if (device.getClass().equals(audioDeviceClass)) {
                    audioDevice = device;
                    break;
                }
            }
            if (audioDevice != null) {
                Objects.requireNonNull(audioSwitch).selectDevice(audioDevice);
            }
        });
    }

    public void enableSpeakerphone(boolean enable) {
        audioManager.setSpeakerphoneOn(enable);
    }

    public void enableSpeakerButPreferBluetooth() {
        AudioDeviceInfo bluetoothDevice = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    bluetoothDevice = device;
                    break;
                }
            }
        }
        if (bluetoothDevice == null) {
            audioManager.setSpeakerphoneOn(true);
        }
    }

    public void selectAudioOutput(@Nullable AudioDeviceKind kind) {
        if (kind != null) {
            selectAudioOutput(kind.audioDeviceClass);
        }
    }
}
