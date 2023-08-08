package com.wireless.kernel;

import android.content.Context;
import android.os.Looper;
import android.os.Handler;

import com.example.lin.soundlab.LogThread;
import com.wireless.kernel.nativeinterface.DeviceInfo;
import com.wireless.kernel.nativeinterface.EngineConstant;
import com.wireless.kernel.nativeinterface.IGlobalAdapter;
import com.wireless.kernel.nativeinterface.IWSWrapperEngine;
import com.wireless.kernel.nativeinterface.WrapperEngineGlobalConfig;
import com.wireless.kernel.utils.SoLoadUtil;

import java.util.HashMap;

public class KernelSetter {
    private static final String TAG = "KernelService";
    private static Handler mHandler = null;

    public static void initWrapper(Context context) {
        mHandler = new Handler(Looper.getMainLooper());

        SoLoadUtil.loadNativeLibrary(context, "kernel");

        WrapperEngineGlobalConfig config = new WrapperEngineGlobalConfig(
                "",
                EngineConstant.KPLATFORMTYPEANDROID,
                EngineConstant.KAPPTYPEDEMO,
                "1.0.0",
                ""
        );
        IWSWrapperEngine.get().initWithConfig(config, new IGlobalAdapter() {

            @Override
            public void onLog(int level, String msg) {
                LogThread.debugLog(level, TAG, msg);
            }

            @Override
            public Long onGetSrvCalTime() {
                return Long.valueOf((long) 0.0);
            }

            @Override
            public void onShowErrUITips(String errMsg) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
//                        showToast("onShowErrUITips", "err_msg"to errMsg)
                    }
                });
            }

            @Override
            public DeviceInfo getDeviceInfo() {
                return initDeviceInfo();
            }

            @Override
            public void onDataReport(String uid, String eventCode, boolean isSuc, HashMap<String, String> params, boolean isRealTime) {

            }
        });
    }

    private static DeviceInfo initDeviceInfo() {
        return new DeviceInfo();
    }
}
