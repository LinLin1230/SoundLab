package com.wireless.kernel;

import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.example.lin.soundlab.LogThread;
import com.example.lin.soundlab.R;
import com.wireless.kernel.api.IKernelCreateListener;
import com.wireless.kernel.api.IKernelListener;
import com.wireless.kernel.nativeinterface.IDependsAdapter;
import com.wireless.kernel.nativeinterface.IKernelAudioListener;
import com.wireless.kernel.nativeinterface.IKernelAudioService;
import com.wireless.kernel.nativeinterface.IKernelSessionListener;
import com.wireless.kernel.nativeinterface.IOperateCallback;
import com.wireless.kernel.nativeinterface.IWSWrapperSession;
import com.wireless.kernel.nativeinterface.InitSessionConfig;
import com.wireless.kernel.nativeinterface.PlatformType;
import com.wireless.kernel.nativeinterface.UltraResult;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class KernelService {
    private static final String TAG = "KernelService";
    private static final String clientVer = "1.1";
    private static final String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String appPath = storagePath + "/" + "SoundLab";
    private String kernelPath = appPath + "/" + "Kernel";


    private IWSWrapperSession wrapperSession = null;
    public IKernelAudioService audioService = null;

    private static Handler mHandler = null;

    private AtomicLong curId = new AtomicLong(0);

    private CheckBox checkboxFindMaxVal;
    private CheckBox checkboxNeedFilter;

    private EditText textUltraThre;
    private EditText textUltraSpace;

    private IKernelListener listener;

    private float thre;

    public void setListener(IKernelListener listener) {
        this.listener = listener;
    }

    public static KernelService create(Activity superActivity) {
        mHandler = new Handler(Looper.getMainLooper());
        KernelService kernelService = new KernelService();
        kernelService.start(new IKernelCreateListener() {

            @Override
            public void onKernelInitComplete() {
                kernelService.audioService.addListener(new IKernelAudioListener() {
                    @Override
                    public void onUltraSignalUpdate(UltraResult result) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (result.getHasMaxVal()) {
                                    kernelService.checkboxFindMaxVal.setChecked(true);
                                }
                                if (result.getCurVal() > kernelService.thre) {
                                    kernelService.listener.onFindMaxVal();
                                }
//                                LogThread.debugLog(4, TAG, "onUltraSignalUpdate: " + result.toString());
                            }
                        });
                    }
                });
                kernelService.initUltraSignalFlag("ZCSignalModulated.pcm", new IOperateCallback() {
                    @Override
                    public void onResult(int rc, String msg) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                LogThread.debugLog(1, TAG, "initUltraSignalFlag: " + rc);
                            }
                        });
                    }
                });
            }
        });
        kernelService.initUi(superActivity);
        return kernelService;
    }

    private void initUi(Activity superActivity) {
        checkboxFindMaxVal = superActivity.findViewById(R.id.checkboxFindMaxVal);
        checkboxNeedFilter = superActivity.findViewById(R.id.checkboxNeedFilter);

        Button buttonSet = superActivity.findViewById(R.id.buttonSet);
        textUltraThre = superActivity.findViewById(R.id.textUltraThre);
        String ts = textUltraThre.getText().toString();
        thre = Integer.parseInt(ts);

        textUltraSpace = superActivity.findViewById(R.id.textUltraSpace);
        buttonSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioService == null) {
                    return;
                }
                String ts = textUltraThre.getText().toString();
                if (audioService == null || ts.length() == 0) {
                    return;
                }
                float t = Integer.parseInt(ts);
                audioService.setUltraSignalFlagThre(t, new IOperateCallback() {
                    @Override
                    public void onResult(int rc, String msg) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                LogThread.debugLog(1, TAG, "setUltraSignalFlagThre: " + rc);
                            }
                        });
                    }
                });

                String space = textUltraSpace.getText().toString();
                if (space.length() == 0) {
                    return;
                }
                int ss = Integer.parseInt(space);
                audioService.ultraSetSpace(ss, new IOperateCallback() {
                    @Override
                    public void onResult(int rc, String msg) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                LogThread.debugLog(1, TAG, "ultraSetSpace: " + rc);
                            }
                        });
                    }
                });
            }
        });
    }

    public void process(ArrayList<Short> processBufferData) {
        if (audioService == null) {
            return;
        }
        audioService.ultraSignalAlignment(curId.getAndIncrement(), processBufferData, checkboxNeedFilter.isChecked(), new IOperateCallback() {
            @Override
            public void onResult(int rc, String msg) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        LogThread.debugLog(1, TAG, "ultraReset: " + rc);
                    }
                });
            }
        });
    }

    public void reSetUltra() {
        if (audioService == null) {
            return;
        }
        curId.set(0);
        checkboxFindMaxVal.setChecked(false);
        audioService.ultraReset(new IOperateCallback() {
            @Override
            public void onResult(int rc, String msg) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        LogThread.debugLog(1, TAG, "ultraReset: " + rc);
                    }
                });
            }
        });
    }

    public void start(IKernelCreateListener listener) {
        File file_root;
        file_root = new File(appPath);
        if (!file_root.exists()) {
            file_root.mkdirs();
        }

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

    public void readUltraPcm(String path, IOperateCallback result) {
        audioService.readUltraPcm(kernelPath + "/" + path, true, true, result);
    }

    private final IDependsAdapter getIDependsAdapter() {
        return new IDependsAdapter() {

        };
    }

    private final IKernelSessionListener getIKernelSessionListener(IKernelCreateListener listener) {
        return new IKernelSessionListener() {

            @Override
            public void onSessionCreate(int result, String sessionId) {

            }
        };
    }
}
