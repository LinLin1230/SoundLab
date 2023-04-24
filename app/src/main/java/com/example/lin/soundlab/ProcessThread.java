package com.example.lin.soundlab;

import android.app.Activity;
import android.media.AudioFormat;
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

    private ProgressBar progressbarBufferCapacity;
    private TextView textviewBufferCapacity;


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

        progressbarBufferCapacity = superActivity.findViewById(R.id.progressbarBufferCapacity);
        textviewBufferCapacity = superActivity.findViewById(R.id.textviewBufferCapacity);
    }

    private void process() {
        if (sonicQueue.getLength() < processBufferDataSize) {
            return;
        }
        double bufferCapacity = (double)sonicQueue.getLength()/sonicQueue.getCapacity();
        LogThread.debugLog(0, TAG, "Buffer usage: " + bufferCapacity);
        setBufferCapacity(bufferCapacity);
        read(processBufferData);

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
        }
        if (channel == AudioFormat.CHANNEL_IN_STEREO) {
            long sum1 = 0;
            long sum2 = 0;
            for (int i = 0; i < processBufferDataSize;i=i+2) {
                sum1 += Math.pow(processBufferData[i],2);
                sum2 += Math.pow(processBufferData[i+1],2);
            }

            double volume1 = 10 * Math.log10(sum1 / (double) processBufferDataSize*2);
            double volume2 = 10 * Math.log10(sum2 / (double) processBufferDataSize*2);
            double volume1WithCorrection = volumeCorrection(volume1);
            double volume2WithCorrection = volumeCorrection(volume2);
            LogThread.debugLog(0, TAG, "volume1: " + volume1WithCorrection + "  volume2: " + volume2WithCorrection);
            setVolume1(volume1WithCorrection);
            setVolume2(volume2WithCorrection);
            setTotalVolume((volume1WithCorrection+volume2)/2);
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

    private void setBufferCapacity(double bufferCapacity) {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textviewBufferCapacity.setText(String.format(Locale.US,"%.2f%",bufferCapacity));
                progressbarBufferCapacity.setProgress((int)bufferCapacity);
            }
        });
    }

}
