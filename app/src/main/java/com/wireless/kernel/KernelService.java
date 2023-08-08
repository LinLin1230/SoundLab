package com.wireless.kernel;

import android.os.Environment;

import com.example.lin.soundlab.LogThread;
import com.wireless.kernel.api.IKernelCreateListener;
import com.wireless.kernel.nativeinterface.IDependsAdapter;
import com.wireless.kernel.nativeinterface.IKernelAudioService;
import com.wireless.kernel.nativeinterface.IKernelSessionListener;
import com.wireless.kernel.nativeinterface.IOperateCallback;
import com.wireless.kernel.nativeinterface.IWSWrapperSession;
import com.wireless.kernel.nativeinterface.InitSessionConfig;
import com.wireless.kernel.nativeinterface.IKernelSessionListener;
import com.wireless.kernel.nativeinterface.PlatformType;

import java.io.File;

public class KernelService {
    private static final String TAG = "KernelService";
    private static final String clientVer = "1.1";
    private static final String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String appPath = storagePath + "/" + "SoundLab";
    private String kernelPath = appPath + "/" + "Kernel";


    private IWSWrapperSession wrapperSession = null;
    public IKernelAudioService audioService = null;

    public void start(IKernelCreateListener listener) {
        File file;
        file = new File(kernelPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        String sysPath = kernelPath;
        String userPath = kernelPath;
        wrapperSession = IWSWrapperSession.create(new InitSessionConfig(
                        "1", sysPath, userPath, clientVer,
                        "", PlatformType.KANDROID, "",
                        "default"
                ),
                getIDependsAdapter(), getIKernelSessionListener(listener));
        LogThread.debugLog(2, TAG, "startSession: curSessionId" + wrapperSession.getSessionId());
        initService();
        listener.onKernelInitComplete();
    }

    private void initService() {
        if (audioService == null && wrapperSession != null) {
            audioService = wrapperSession.getAudioService();
        }
    }

    public void initUltraSignalFlag(String path, IOperateCallback result) {
        audioService.initUltraSignalFlag(kernelPath + "/" + path, result);
    }

    private final IDependsAdapter getIDependsAdapter() {
        return new IDependsAdapter() {

        };
    }

    private final IKernelSessionListener getIKernelSessionListener(IKernelCreateListener listener) {
        return new IKernelSessionListener() {

            @Override
            public void onSessionCreate(int result, String sessionId){

            }
        };
    }
}
