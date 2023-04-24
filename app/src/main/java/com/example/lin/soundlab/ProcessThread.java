package com.example.lin.soundlab;

import android.app.Activity;
import android.media.AudioFormat;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

public class ProcessThread implements Runnable {
    private static final String TAG = "SoundLabProcessThread";

    private int sleepInterval = 10;

    private static SonicQueue sonicQueue = new SonicQueue();


    private static int processFrequency = 10;
    private static int samplingRate = 48000;
    private static int processBufferDataSize = samplingRate/processFrequency;
    private static short[] processBufferData = new short[processBufferDataSize];;
    private static int channel;

    private Activity superActivity;
    private TextView textviewTotalVolume;
    private ProgressBar progressbarVolume1;
    private TextView textviewVolume1;
    private ProgressBar progressbarVolume2;
    private TextView textviewVolume2;

    private ProgressBar progressbarBufferUsage;
    private TextView textviewBufferUsage;

    private Button buttonPlayStart;
    private Button buttonPlayReset;
    private int volumeThreshold = 65;


    @Override
    public void run() {
        LogThread.debugLog(2, TAG, "ProcessThread.run()");
        while(true) {
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
        processBufferDataSize = samplingRate/processFrequency;
        channel = recordChannel;
        processBufferData = new short[processBufferDataSize];


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
        double bufferUsage = (double)sonicQueue.getLength()/sonicQueue.getCapacity();
        LogThread.debugLog(0, TAG, "Buffer usage: " + bufferUsage);
        setBufferUsage(bufferUsage);

        // MONO mode
        if (channel == AudioFormat.CHANNEL_IN_MONO) {
            long sum = 0;
            for (int i = 0; i < processBufferDataSize;i=i+1) {
                sum += Math.pow(processBufferData[i],2);
            }
            double mean = sum / (double) processBufferDataSize;
            double volume = 10 * Math.log10(mean);
            double volumeWithCorrection = volumeCorrection(volume);
            LogThread.debugLog(0, TAG, "volume: " + volumeWithCorrection);
            setTotalVolume(volumeWithCorrection);
            setVolume1(volumeWithCorrection);
            setVolume2(volumeWithCorrection);

            if (volume>volumeThreshold) {
                playStart();
            }
        }
        // STEREO mode
        if (channel == AudioFormat.CHANNEL_IN_STEREO) {
            long sum1 = 0;
            long sum2 = 0;
            for (int i = 0; i < processBufferDataSize;i=i+2) {
                sum1 += Math.pow(processBufferData[i],2);
                sum2 += Math.pow(processBufferData[i+1],2);
                LogThread.debugLog(0, TAG, "short1: " + processBufferData[i] + "  short2: " + processBufferData[i+1]);
            }

            double volume1 = 10 * Math.log10(((double) sum1 / processBufferDataSize)*2);
            double volume2 = 10 * Math.log10(((double) sum2 / processBufferDataSize)*2);
            double volume1WithCorrection = volumeCorrection(volume1);
            double volume2WithCorrection = volumeCorrection(volume2);
            LogThread.debugLog(0, TAG, "volume1: " + volume1WithCorrection + "  volume2: " + volume2WithCorrection);
            setVolume1(volume1WithCorrection);
            setVolume2(volume2WithCorrection);
            setTotalVolume((volume1WithCorrection+volume2WithCorrection)/2);

            if (volume1>volumeThreshold | volume2>volumeThreshold) {
                playStart();
            }
        }

    }

    public synchronized static boolean write(byte[] data) {
        boolean isWrite = sonicQueue.write(data);
        if (isWrite) {
            return true;
        }
        else {
            LogThread.debugLog(4, TAG, "Write fail. The queue may be full. Buffer usage: " + (double)sonicQueue.getLength()/sonicQueue.getCapacity());
            return false;
        }
    }

    public synchronized static boolean write(short[] data) {
        boolean isWrite = sonicQueue.write(data);
        if (isWrite) {
            return true;
        }
        else {
            LogThread.debugLog(4, TAG, "Write fail. The queue may be full. Buffer usage: " + (double)sonicQueue.getLength()/sonicQueue.getCapacity());
            return false;
        }
    }

    public synchronized static boolean read(byte[] data) {
        return sonicQueue.read(data);
    }

    public synchronized static boolean read(short[] data) {
        return sonicQueue.read(data);
    }




    private void setTotalVolume(double volume) {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textviewTotalVolume.setText(String.format(Locale.US,"%.2f",volume));
            }
        });
    }

    private void setVolume1(double volume) {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textviewVolume1.setText(String.format(Locale.US,"%.2f",volume));
                progressbarVolume1.setProgress((int)volume);
            }
        });
    }

    private void setVolume2(double volume) {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textviewVolume2.setText(String.format(Locale.US,"%.2f",volume));
                progressbarVolume2.setProgress((int)volume);
            }
        });
    }

    private double volumeCorrection(double volume) {
//        return 1.037*volume-7.191;
        return volume;
    }

    private void setBufferUsage(double bufferUsage) {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textviewBufferUsage.setText(String.format(Locale.US,"%.2f/%s",bufferUsage*100, "%"));
                progressbarBufferUsage.setProgress((int)(bufferUsage*100));
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

}
