package com.example.lin.soundlab;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class LogThread implements Runnable{
    private static final String TAG = "SoundLabLogThread";
    private static int displayLogLevel=0;
    private static TextView textviewDebugLog;
    private static int logsDisplayLength = 50;
    private static ArrayBlockingQueue<String> logQueue = new ArrayBlockingQueue<String>(logsDisplayLength);
    private static String[] logs = new String[logsDisplayLength];
    private static int logsPointer = 0;
    private static String logsDisplayed = "";
    private static int sleepInterval = 20;
    private static Activity superActivity;
    private static String logString = "";

    @Override
    public void run() {
        debugLog(2, TAG, "Log thread start.");
        while(true){
            processLog();
            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void processLog() {
        if(!(logQueue.isEmpty())) {
            // Take out one log.
            try {
                logString = logQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            logs[logsPointer] = logString;
            logsPointer = (logsPointer + 1) % logsDisplayLength;

            // Construct display text.
            StringBuilder logsDisplayedBuilder = new StringBuilder();
            for(int m=0;m<logsDisplayLength;m++){
                String curString = logs[(logsPointer+m) % logsDisplayLength];
                if(curString!=null) {
                    logsDisplayedBuilder.append(curString);
                }

            }
            logsDisplayed = logsDisplayedBuilder.toString();
            superActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textviewDebugLog.setText(logsDisplayed);
                }
            });

            // Move to the bottom of the text.
            superActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(textviewDebugLog.getLayout()!=null) {
                        int scrollAmount = textviewDebugLog.getLayout().getLineTop(textviewDebugLog.getLineCount())-textviewDebugLog.getHeight();
                        textviewDebugLog.scrollTo(0, Math.max(scrollAmount, 0));
                    }

                }
            });
        }
    }

    public static void setup(Activity activity, int displayLevel) {
        superActivity = activity;
        textviewDebugLog = superActivity.findViewById(R.id.textviewDebugLog);

        displayLogLevel = displayLevel;
    }

    public static void debugLog(int level, String label, String log) {
        switch(level) {
            // 0:verbose 1:debug 2:info 3:warning 4:error
            case 0:
                Log.v(label, log);
                break;
            case 1:
                Log.d(label, log);
                break;
            case 2:
                Log.i(label, log);
                break;
            case 3:
                Log.w(label, log);
                break;
            case 4:
                Log.e(label, log);
                break;
        }
        if(level>=displayLogLevel) {
            String logString = formatLog(label, log);

            try {
                logQueue.put(logString);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }



    }
    private static String formatLog(String label, String log) {
        return label + ": " + log + "\n";
    }
}
