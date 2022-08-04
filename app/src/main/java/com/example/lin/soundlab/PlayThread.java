package com.example.lin.soundlab;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class PlayThread implements Runnable {
    private static final String TAG = "SoundLabPlayThread";
    private int audioBufferDataSize;
    private String path;
    private int usage;
    private int channel;
    private int samplingRate;
    private int encoding;
    private AudioTrack audioTrack;
    private AudioAttributes audioAttributes;
    private AudioFormat audioFormat;
    private byte[] audioBufferData;
    private boolean isPlaying;
    private int timeLeft;
    private DataInputStream dataIS;

    // info display
    private Activity superActivity;
    private TextView textviewPlayStatus;
    private TextView textviewTimeLeft;
    // settings
    private Spinner spinnerFileList;
    private Spinner spinnerPlayUsage;
    private Spinner spinnerPlayChannel;
    private Spinner spinnerPlaySamplingRate;

    @Override
    public void run() {
        LogThread.debugLog(2, TAG, "PlayThread.run()");
        start();
    }

    public void setup(Activity activity, String playItemPath, int playUsage, int playChannel, int playSamplingRate) {
        LogThread.debugLog(2, TAG, "PlayThread.setup()");
        LogThread.debugLog(1, TAG, "Current Setup: path:" + playItemPath + " usage:" + playUsage + " channel:" + playChannel + "sampling rate:" + playSamplingRate);
        superActivity = activity;
        // info display
        textviewPlayStatus = superActivity.findViewById(R.id.textviewPlayStatus);
        textviewTimeLeft = superActivity.findViewById(R.id.textviewTimeLeft);

        // settings
        spinnerFileList = superActivity.findViewById(R.id.spinnerFileList);
        spinnerPlayUsage = superActivity.findViewById(R.id.spinnerPlayUsage);
        spinnerPlayChannel = superActivity.findViewById(R.id.spinnerPlayChannel);
        spinnerPlaySamplingRate = superActivity.findViewById(R.id.spinnerPlaySamplingRate);


        path = playItemPath;
        usage = playUsage;
        channel = playChannel;
        samplingRate = playSamplingRate;
        encoding = AudioFormat.ENCODING_PCM_16BIT;

        audioBufferDataSize = 4 * AudioTrack.getMinBufferSize(samplingRate, channel, encoding);
        LogThread.debugLog(1, TAG, "audioBufferSize:" + audioBufferDataSize);
        if (usage == 0) {
            audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        }
        if (usage == 1) {
            int specific_usage = Utils.isMTK() ? AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING : AudioAttributes.USAGE_VOICE_COMMUNICATION;
            audioAttributes = new AudioAttributes.Builder().setUsage(specific_usage).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        }

        audioFormat = new AudioFormat.Builder().setEncoding(encoding).setSampleRate(samplingRate).setChannelMask(channel).build();
        audioTrack = new AudioTrack.Builder().setAudioAttributes(audioAttributes).setAudioFormat(audioFormat).setBufferSizeInBytes(audioBufferDataSize).build();
        audioBufferData = new byte[audioBufferDataSize / 2];

        try {
            dataIS = new DataInputStream(new FileInputStream(path));
            timeLeft = getTimeLeft();

        } catch (IOException e) {
            e.printStackTrace();
        }
        setTextviewTimeLeft("" + timeLeft);
        setTextviewPlayStatus("Ready");
    }

    public void start() {
        LogThread.debugLog(2, TAG, "PlayThread.play()");
        if (isPlaying == true) {
            LogThread.debugLog(1, TAG, "Playing...");
        } else {
            isPlaying = true;
            lockPlaySettings();

            audioTrack.play();
            try {
                while (isPlaying && dataIS.available() > 0) {
                    int dataISReadCount = dataIS.read(audioBufferData, 0, audioBufferData.length);
                    int audioTrackWriteCount = audioTrack.write(audioBufferData, 0, audioBufferData.length);
                    timeLeft = getTimeLeft();

                    setTextviewTimeLeft("" + timeLeft);
                    LogThread.debugLog(0, TAG, "time left: " + timeLeft + " R/W: " + dataISReadCount + "/" + audioTrackWriteCount);
                }
                if (dataIS.available() == 0) {
                    setTextviewPlayStatus("End");
                    unlockPlaySettings();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void pause() {
        LogThread.debugLog(2, TAG, "PlayThread.pause()");
        isPlaying = false;
        audioTrack.pause();
        setTextviewPlayStatus("Pause");
    }

    public void reset() {
        LogThread.debugLog(2, TAG, "PlayThread.reset()");
        isPlaying = false;
        audioTrack.stop();
        setup(superActivity, path, usage, channel, samplingRate);
        unlockPlaySettings();
    }

    public void end() {
        LogThread.debugLog(2, TAG, "PlayThread.end()");
    }

    private int getTimeLeft() {
        int channelRatio = 1;
        switch (channel) {
            case AudioFormat.CHANNEL_OUT_MONO:
                channelRatio = 1;
                break;
            case AudioFormat.CHANNEL_OUT_STEREO:
                channelRatio = 2;
                break;
        }

        int timeLeft = 0;
        try {
            timeLeft = dataIS.available() / samplingRate / 2 / channelRatio;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return timeLeft;
    }

    private void setTextviewPlayStatus(String text) {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textviewPlayStatus.setText(text);
            }
        });
//        textviewPlayStatus.post(new Runnable() {
//            @Override
//            public void run() {
//                textviewPlayStatus.setText(text);
//            }
//        });
    }

    private void setTextviewTimeLeft(String text) {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textviewTimeLeft.setText(text);
            }
        });
//        textviewTimeLeft.post(new Runnable() {
//            @Override
//            public void run() {
//                textviewTimeLeft.setText(text);
//            }
//        });
    }

    private void lockPlaySettings() {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinnerFileList.setEnabled(false);
                spinnerPlayUsage.setEnabled(false);
                spinnerPlayChannel.setEnabled(false);
                spinnerPlaySamplingRate.setEnabled(false);
            }
        });
    }

    private void unlockPlaySettings() {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinnerFileList.setEnabled(true);
                spinnerPlayUsage.setEnabled(true);
                spinnerPlayChannel.setEnabled(true);
                spinnerPlaySamplingRate.setEnabled(true);
            }
        });
    }


}
