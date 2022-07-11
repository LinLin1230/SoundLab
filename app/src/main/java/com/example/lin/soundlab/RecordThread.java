package com.example.lin.soundlab;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MicrophoneInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class RecordThread implements Runnable {
    private static final String TAG = "SoundLabRecordThread";
    private int audioBufferDataSize;
    private String path;
    private int audioSource;
    private int channel;
    private int samplingRate;
    private boolean recordChecked;
    private int encoding;

    private AudioRecord audioRecord;
    private AudioFormat audioFormat;
    private byte[] audioBufferData;
    private boolean isRecording;
    private DataOutputStream dataOS;

    private Activity superActivity;
    private TextView textviewTotalVolume;
    private ProgressBar progressbarVolume1;
    private TextView textviewVolume1;
    private ProgressBar progressbarVolume2;
    private TextView textviewVolume2;

    private CheckBox checkboxRecord;
    private Spinner spinnerRecordAudioSource;
    private Spinner spinnerRecordChannel;
    private Spinner spinnerRecordSamplingRate;
    private EditText textFileNamePrefix;


    @Override
    public void run() {
        LogThread.debugLog(2, TAG, "RecordThread.run()");
        start();
    }

    public void setup(Activity activity, String recordItemPath, int recordAudioSource, int recordChannel, int recordSamplingRate, boolean isRecordChecked) {
        LogThread.debugLog(2, TAG, "RecordThread.setup()");
        LogThread.debugLog(1, TAG, "Current Setup: path:"+recordItemPath+" audio source:"+recordAudioSource+" channel:"+recordChannel+" sampling rate:"+recordSamplingRate+" isRecordChecked:"+isRecordChecked);
        superActivity = activity;

        // info display
        textviewTotalVolume = superActivity.findViewById(R.id.textviewTotalVolume);
        progressbarVolume1 = superActivity.findViewById(R.id.progressbarVolume1);
        textviewVolume1 = superActivity.findViewById(R.id.textviewVolume1);
        progressbarVolume2 = superActivity.findViewById(R.id.progressbarVolume2);
        textviewVolume2 = superActivity.findViewById(R.id.textviewVolume2);

        // setting
        checkboxRecord = superActivity.findViewById(R.id.checkboxRecord);
        spinnerRecordAudioSource = superActivity.findViewById(R.id.spinnerRecordAudioSource);
        spinnerRecordChannel = superActivity.findViewById(R.id.spinnerRecordChannel);
        spinnerRecordSamplingRate = superActivity.findViewById(R.id.spinnerRecordSamplingRate);
        textFileNamePrefix = superActivity.findViewById(R.id.textFileNamePrefix);

        path = recordItemPath;
        audioSource = recordAudioSource;
        channel = recordChannel;
        samplingRate = recordSamplingRate;
        recordChecked = isRecordChecked;
        encoding = AudioFormat.ENCODING_PCM_16BIT;

        audioBufferDataSize = 4*AudioRecord.getMinBufferSize(samplingRate, channel, encoding);
//        LogThread.debugLog(2, TAG, "audioBufferDataSize: "+audioBufferDataSize);
        audioFormat = new AudioFormat.Builder().setEncoding(encoding).setSampleRate(samplingRate).setChannelMask(channel).build();
        audioRecord = new AudioRecord.Builder().setAudioSource(audioSource).setAudioFormat(audioFormat).setBufferSizeInBytes(audioBufferDataSize).build();
        audioBufferData = new byte[audioBufferDataSize/2];


    }

//    @RequiresApi(api = Build.VERSION_CODES.P)
    public void start() {
        LogThread.debugLog(2, TAG, "RecordThread.start()");
        isRecording = true;
        lockRecordSettings();

        // If record checkbox is checked, create data output stream, to a file.
        if(recordChecked) {
            try {
                File recordFile = new File(path);
                dataOS = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(recordFile)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }


        audioRecord.startRecording();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            LogThread.debugLog(1, TAG, "audioRecord.getAudioFormat(): " + audioRecord.getAudioFormat());
            try {
                List<MicrophoneInfo> Mics = null;
                Mics = audioRecord.getActiveMicrophones();

                for (int m = 0; m < Mics.size(); m++) {
                    LogThread.debugLog(1, TAG, "getAddress: " + Mics.get(m).getAddress());
                    LogThread.debugLog(1, TAG, "getLocation: " + Mics.get(m).getLocation());
                    LogThread.debugLog(1, TAG, "getFrequencyResponse: " + Mics.get(m).getFrequencyResponse());
                    LogThread.debugLog(1, TAG, "getDescription: " + Mics.get(m).getDescription());
                    LogThread.debugLog(1, TAG, "getDirectionality: " + Mics.get(m).getDirectionality());
                    LogThread.debugLog(1, TAG, "getChannelMapping: " + Mics.get(m).getChannelMapping());
                    LogThread.debugLog(1, TAG, "getSql: " + Mics.get(m).getMaxSpl() + Mics.get(m).getMinSpl());
                    LogThread.debugLog(1, TAG, "getOrientation: " + Mics.get(m).getOrientation());
                    LogThread.debugLog(1, TAG, "getPosition: " + Mics.get(m).getPosition());
                    LogThread.debugLog(1, TAG, "getSensitivity: " + Mics.get(m).getSensitivity());
                    LogThread.debugLog(1, TAG, "getType: " + Mics.get(m).getType());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        while(isRecording) {

            int bufferReadResult = audioRecord.read(audioBufferData, 0, audioBufferData.length);
            LogThread.debugLog(0, TAG, "bufferReadResult: " + bufferReadResult);
            if(bufferReadResult != 0) {
                // write file
                if(recordChecked) {
                    try {
                        dataOS.write(audioBufferData,0,bufferReadResult);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //volume
                int volumeDataLength = bufferReadResult/10;
                if(volumeDataLength>10) {
                    if(channel == AudioFormat.CHANNEL_IN_MONO) {
                        long sum = 0;
                        for (int i = 0; i < volumeDataLength;i=i+2) {
                            sum += (audioBufferData[i] + audioBufferData[i+1]* 256)*(audioBufferData[i]  + audioBufferData[i+1]* 256);
                        }
                        double mean = sum / (double) volumeDataLength*2;
                        double volume = 10 * Math.log10(mean);
                        double volumeWithCorrection = volumeCorrection(volume);
                        LogThread.debugLog(0, TAG, "volume: " + volumeWithCorrection);
                        setTextviewTotalVolume(volumeWithCorrection);
                        setTextviewVolume1(volumeWithCorrection);
                        setTextviewVolume2(volumeWithCorrection);
                    }
                    if(channel == AudioFormat.CHANNEL_IN_STEREO) {
                        long sum1 = 0;
                        long sum2 = 0;
                        for (int i = 0; i < volumeDataLength;i=i+4) {
                            sum1 += Math.pow(audioBufferData[i] + audioBufferData[i+1]*256,2);
                            sum2 += Math.pow(audioBufferData[i+2] + audioBufferData[i+3]*256,2);
                        }

                        double volume1 = 10 * Math.log10(sum1 / (double) volumeDataLength*4);
                        double volume2 = 10 * Math.log10(sum2 / (double) volumeDataLength*4);
                        double volume1WithCorrection = volumeCorrection(volume1);
                        double volume2WithCorrection = volumeCorrection(volume2);
                        LogThread.debugLog(0, TAG, "volume1: " + volume1WithCorrection + "  volume2: " + volume2WithCorrection);
                        setTextviewVolume1(volume1WithCorrection);
                        setTextviewVolume2(volume2WithCorrection);
                        setTextviewTotalVolume((volume1WithCorrection+volume2)/2);
                    }
                }

            }
        }
        audioRecord.stop();
        audioRecord.release();
        if(recordChecked) {
            try {
                dataOS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        unlockRecordSettings();
    }

    public void stop() {
        LogThread.debugLog(2, TAG, "RecordThread.stop()");
        isRecording = false;
    }

    private void setTextviewTotalVolume(double volume) {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textviewTotalVolume.setText(String.format(Locale.US,"%.2f",volume));
            }
        });
//        textviewTotalVolume.post(new Runnable() {
//            @Override
//            public void run() {
//                textviewTotalVolume.setText(String.format(Locale.US,"%.2f",volume));
//            }
//        });

    }

    private void setTextviewVolume1(double volume) {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textviewVolume1.setText(String.format(Locale.US,"%.2f",volume));
                progressbarVolume1.setProgress((int)volume);
            }
        });
    }

    private void setTextviewVolume2(double volume) {
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

    private void lockRecordSettings() {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                checkboxRecord.setEnabled(false);
                spinnerRecordAudioSource.setEnabled(false);
                spinnerRecordChannel.setEnabled(false);
                spinnerRecordSamplingRate.setEnabled(false);
                textFileNamePrefix.setEnabled(false);
            }
        });
    }

    private void unlockRecordSettings() {
        superActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                checkboxRecord.setEnabled(true);
                spinnerRecordAudioSource.setEnabled(true);
                spinnerRecordChannel.setEnabled(true);
                spinnerRecordSamplingRate.setEnabled(true);
                textFileNamePrefix.setEnabled(true);
            }
        });
    }

}
