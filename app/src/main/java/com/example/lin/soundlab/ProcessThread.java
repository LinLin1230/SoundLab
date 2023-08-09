package com.example.lin.soundlab;

import android.app.Activity;
import android.media.AudioFormat;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wireless.kernel.KernelService;
import com.wireless.kernel.KernelSetter;
import com.wireless.kernel.api.IKernelCreateListener;
import com.wireless.kernel.nativeinterface.IKernelAudioListener;
import com.wireless.kernel.nativeinterface.IKernelAudioService;
import com.wireless.kernel.nativeinterface.IOperateCallback;
import com.wireless.kernel.nativeinterface.UltraResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import android.os.Handler;

import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignExstrom;
import biz.source_code.dsp.filter.IirFilterCoefficients;

public class ProcessThread implements Runnable {
    private static final String TAG = "SoundLabProcessThread";

    private int sleepInterval = 10;

    private static SonicQueue sonicQueue = new SonicQueue();


    private static int processFrequency = 10;
    private static int samplingRate = 48000;
    private static int processBufferDataSize = samplingRate / processFrequency;
    private static Short[] processBufferData = new Short[processBufferDataSize];
    private static double[] processBufferDataDouble = new double[processBufferDataSize];
    private static double[] processBufferDataDoubleL = new double[processBufferDataSize];
    private static double[] processBufferDataDoubleR = new double[processBufferDataSize];

    private static int channel;

    private Activity superActivity;
    private TextView textviewTotalVolume;
    private ProgressBar progressbarVolume1;
    private TextView textviewVolume1;
    private ProgressBar progressbarVolume2;
    private TextView textviewVolume2;

    private ProgressBar progressbarBufferUsage;
    private TextView textviewBufferUsage;

    private KernelService kernelService = null;
    private IKernelAudioService audioService = null;

    private AtomicLong curId = new AtomicLong(0);

    private EditText textUltraThre;

    private static Handler mHandler = null;


    private Button buttonPlayStart;
    private Button buttonPlayReset;
    private TextView textviewPlayStatus;
    private int volumeThreshold = 20; // 40


    @Override
    public void run() {
        LogThread.debugLog(2, TAG, "ProcessThread.run()");
        while (true) {
            process();
            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void setup(Activity activity, int recordChannel, int recordSamplingRate) {
        superActivity = activity;

        samplingRate = recordSamplingRate;
        processBufferDataSize = samplingRate / processFrequency;
        channel = recordChannel;
        processBufferData = new Short[processBufferDataSize];

        // info display
        textviewTotalVolume = superActivity.findViewById(R.id.textviewTotalVolume);
        progressbarVolume1 = superActivity.findViewById(R.id.progressbarVolume1);
        textviewVolume1 = superActivity.findViewById(R.id.textviewVolume1);
        progressbarVolume2 = superActivity.findViewById(R.id.progressbarVolume2);
        textviewVolume2 = superActivity.findViewById(R.id.textviewVolume2);

        // buffer usage info
        progressbarBufferUsage = superActivity.findViewById(R.id.progressbarBufferUsage);
        textviewBufferUsage = superActivity.findViewById(R.id.textviewBufferUsage);

        // button control
        buttonPlayStart = superActivity.findViewById(R.id.buttonPlayStart);
        buttonPlayReset = superActivity.findViewById(R.id.buttonPlayReset);
        textviewPlayStatus = superActivity.findViewById(R.id.textviewPlayStatus);

        Button buttonSet = superActivity.findViewById(R.id.buttonSet);
        textUltraThre = superActivity.findViewById(R.id.textUltraThre);
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
            }
        });

        if (kernelService == null) {
            mHandler = new Handler(Looper.getMainLooper());
            kernelService = new KernelService();
            kernelService.start(new IKernelCreateListener() {

                @Override
                public void onKernelInitComplete() {
                    audioService = kernelService.audioService;
                    audioService.addListener(new IKernelAudioListener() {
                        @Override
                        public void onUltraSignalUpdate(UltraResult result) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    LogThread.debugLog(1, TAG, "onUltraSignalUpdate: " + result.toString());
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
        }
    }

    public void reSetUltra() {
        if (audioService == null) {
            return;
        }
        curId.set(0);
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

    private void process() {
        if (sonicQueue.getLength() < processBufferDataSize) {
            return;
        }
        // try to read data from buffer
        boolean isReadSuccess = read(processBufferData);
        if (!isReadSuccess) {
            return;
        }

        // update buffer usage
        double bufferUsage = (double) sonicQueue.getLength() / sonicQueue.getCapacity();
        LogThread.debugLog(0, TAG, "Buffer usage: " + bufferUsage);
        setBufferUsage(bufferUsage);

        double[] processBufferDataDouble = new double[processBufferDataSize];
        // C++
        if (audioService != null) {
            ArrayList<Short> tempBuffers = new ArrayList<Short>(Arrays.asList(processBufferData));
            audioService.ultraSignalAlignment(curId.getAndIncrement(), tempBuffers, true, new IOperateCallback() {
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

//        // MONO mode
//        if (channel == AudioFormat.CHANNEL_IN_MONO) {
//
//            // highpass filter 15000 Hz at 48000 Hz sampling rate
//            for (int i = 0; i < processBufferDataSize; i++) {
//                processBufferDataDouble[i] = (double) processBufferData[i];
//            }
//            double[] processBufferDataDoubleAfterFilter = IIRFilter(processBufferDataDouble, FilterPassType.highpass, 5, 15 / 48.0, 15 / 48.0);
//
//
//            long sum = 0;
//            for (int i = 0; i < processBufferDataSize; i++) {
//                sum += Math.pow(processBufferDataDoubleAfterFilter[i], 2);
//            }
//            double mean = sum / (double) processBufferDataSize;
//            double volume = 10 * Math.log10(mean);
//            double volumeWithCorrection = volumeCorrection(volume);
//            LogThread.debugLog(0, TAG, "volume: " + volumeWithCorrection);
//            setTotalVolume(volumeWithCorrection);
//            setVolume1(volumeWithCorrection);
//            setVolume2(volumeWithCorrection);
//
//            tryPlayReset();
//
//            if (volume > volumeThreshold) {
//                replyZC();
//            }
//        }
//        // STEREO mode
//        if (channel == AudioFormat.CHANNEL_IN_STEREO) {
//
//            // highpass filter 15000 Hz at 48000 Hz sampling rate
//            for (int i = 0; i < processBufferDataSize / 2; i++) {
//                processBufferDataDoubleL[i] = (double) processBufferData[2 * i];
//                processBufferDataDoubleR[i] = (double) processBufferData[2 * i + 1];
//            }
//            double[] processBufferDataDoubleAfterFilterL = IIRFilter(processBufferDataDoubleL, FilterPassType.highpass, 5, 15 / 48.0, 15 / 48.0);
//            double[] processBufferDataDoubleAfterFilterR = IIRFilter(processBufferDataDoubleR, FilterPassType.highpass, 5, 15 / 48.0, 15 / 48.0);
//
//            long sum1 = 0;
//            long sum2 = 0;
//            for (int i = 0; i < processBufferDataSize / 2; i++) {
//                sum1 += Math.pow(processBufferDataDoubleAfterFilterL[i], 2);
//                sum2 += Math.pow(processBufferDataDoubleAfterFilterR[i], 2);
////                LogThread.debugLog(0, TAG, "short1: " + processBufferData[i] + "  short2: " + processBufferData[i+1]);
//            }
//
//            double volume1 = 10 * Math.log10(((double) sum1 / processBufferDataSize) * 2);
//            double volume2 = 10 * Math.log10(((double) sum2 / processBufferDataSize) * 2);
//            double volume1WithCorrection = volumeCorrection(volume1);
//            double volume2WithCorrection = volumeCorrection(volume2);
//            LogThread.debugLog(0, TAG, "volume1: " + volume1WithCorrection + "  volume2: " + volume2WithCorrection);
//            setVolume1(volume1WithCorrection);
//            setVolume2(volume2WithCorrection);
//            setTotalVolume((volume1WithCorrection + volume2WithCorrection) / 2);
//
//
//            tryPlayReset();
//            if (volume1 > volumeThreshold | volume2 > volumeThreshold) {
//                replyZC();
//            }
//        }

    }

    public synchronized static boolean write(byte[] data) {
        boolean isWrite = sonicQueue.write(data);
        if (isWrite) {
            return true;
        } else {
            LogThread.debugLog(4, TAG, "Write fail. The queue may be full. Buffer usage: " + (double) sonicQueue.getLength() / sonicQueue.getCapacity());
            return false;
        }
    }

    public synchronized static boolean write(short[] data) {
        boolean isWrite = sonicQueue.write(data);
        if (isWrite) {
            return true;
        } else {
            LogThread.debugLog(4, TAG, "Write fail. The queue may be full. Buffer usage: " + (double) sonicQueue.getLength() / sonicQueue.getCapacity());
            return false;
        }
    }

    public synchronized static boolean read(byte[] data) {
        return sonicQueue.read(data);
    }

    public synchronized static boolean read(Short[] data) {
        return sonicQueue.read(data);
    }


    private void setTotalVolume(double volume) {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textviewTotalVolume.setText(String.format(Locale.US, "%.2f", volume));
            }
        });
    }

    private void setVolume1(double volume) {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textviewVolume1.setText(String.format(Locale.US, "%.2f", volume));
                progressbarVolume1.setProgress((int) volume);
            }
        });
    }

    private void setVolume2(double volume) {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textviewVolume2.setText(String.format(Locale.US, "%.2f", volume));
                progressbarVolume2.setProgress((int) volume);
            }
        });
    }

    private double volumeCorrection(double volume) {
//        return 1.037*volume-7.191;
        return volume;
    }

    private void setBufferUsage(double bufferUsage) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                textviewBufferUsage.setText(String.format(Locale.US, "%.2f/%s", bufferUsage * 100, "%"));
                progressbarBufferUsage.setProgress((int) (bufferUsage * 100));
            }
        });
    }

    private void replyZC() {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String curPlayStatus = (String) textviewPlayStatus.getText();
                if (curPlayStatus.equals("Ready")) {
                    buttonPlayStart.performClick();
                }
                if (curPlayStatus.equals("End")) {
                    buttonPlayReset.performClick();
                    buttonPlayStart.performClick();
                }
            }
        });
    }

    private void tryPlayReset() {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String curPlayStatus = (String) textviewPlayStatus.getText();
                if (curPlayStatus.equals("End")) {
                    buttonPlayReset.performClick();
                }
            }
        });
    }

    private void playStart() {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buttonPlayStart.performClick();
            }
        });
    }

    private void playReset() {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buttonPlayReset.performClick();
            }
        });
    }

    private double[] IIRFilter(double[] inData, FilterPassType filterType, int filterOrder, double fcf1, double fcf2) {
        IirFilterCoefficients coefficients = IirFilterDesignExstrom.design(filterType, filterOrder, fcf1, fcf2);
        double[] a = coefficients.a;
        double[] b = coefficients.b;
        double[] in = new double[b.length];
        double[] out = new double[a.length - 1];
        double[] outData = new double[inData.length];

        for (int i = 0; i < inData.length; i++) {
            System.arraycopy(in, 0, in, 1, in.length - 1);
            in[0] = inData[i];

            double y = 0;
            for (int j = 0; j < b.length; j++) {
                y += in[j] * b[j];
            }
            for (int j = 0; j < a.length - 1; j++) {
                y -= out[j] * a[j + 1];
            }

            System.arraycopy(out, 0, out, 1, out.length - 1);
            out[0] = y;

            outData[i] = y;

        }
        return outData;
    }

}
