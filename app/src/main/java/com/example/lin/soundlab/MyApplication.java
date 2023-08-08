package com.example.lin.soundlab;

import android.app.Application;

import com.wireless.kernel.KernelService;
import com.wireless.kernel.KernelSetter;
import com.wireless.kernel.api.IKernelCreateListener;
import com.wireless.kernel.nativeinterface.IKernelAudioListener;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        KernelSetter.initWrapper(this);
    }
}
