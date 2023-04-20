package com.example.lin.soundlab;

import android.media.AudioFormat;

public class ProcessThread implements Runnable {
    private static final String TAG = "SoundLabProcessThread";

    private int sleepInterval = 10;

    private static SonicQueue sonicQueue = new SonicQueue();
    private static int audioBufferDataSize = 48000*2;
//    private static byte[] audioBufferData = new byte[audioBufferDataSize];

    private static int samplingRate = 48000;
    private static int processBufferDataSize = samplingRate;
    private static int recordChannel;








    @Override
    public void run() {
        LogThread.debugLog(2, TAG, "ProcessThread.run()");
//        while(true) {
//            process();
//            try {
//                Thread.sleep(sleepInterval);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

    }

    private void process() {
        if (sonicQueue.getLength() < processBufferDataSize) {
            return;
        }
        if (recordChannel == AudioFormat.CHANNEL_IN_MONO) {

        }
        if (recordChannel == AudioFormat.CHANNEL_IN_STEREO) {

        }

    }

    public static boolean write(byte[] data) {
        return sonicQueue.write(data);
    }

    public static boolean write(short[] data) {
        return sonicQueue.write(data);
    }

    public static void setBufferReadSize(int bufferSize) {
        audioBufferDataSize = audioBufferDataSize;
    }

    public static void setSamplingRate(int rate) {
        samplingRate = rate;
    }
    public static void setRecordChannel(int channel) {
        recordChannel = channel;
    }


}
