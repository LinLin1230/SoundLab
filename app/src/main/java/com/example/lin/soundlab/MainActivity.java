package com.example.lin.soundlab;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SoundLabMainActivity";
    private static final String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String appPath = storagePath+"/"+"SoundLab";
    private static final String playListPath = appPath+"/"+"PlayList";

    // Play settings.
    private static final String[] listPlayUsage = {"MUSIC","CALL"};
    private static final int[] valuePlayUsage = {0,1};
    private int curPlayUsage= valuePlayUsage[0];
    private static final String[] listPlayChannel = {"Mono","Stereo"};
    private static final int[] valuePlayChannel = {AudioFormat.CHANNEL_OUT_MONO, AudioFormat.CHANNEL_OUT_STEREO};
    private int curPlayChannel = valuePlayChannel[0];
    private static final String[] listPlaySamplingRate = {"48000","44100","16000","8000"};
    private static final int[] valuePlaySamplingRate = {48000,44100,16000,8000};
    private int curPlaySamplingRate = valuePlaySamplingRate[0];

    private final int initialPlayUsagePosition = 0;
    private int initialPlayChannelPosition = 1;
    private int initialPlaySamplingRatePosition = 0;

    private int curPlayItemPosition;
    private String curPlayItemPath;
    private String[] listFileNames;

    // Record settings
    private static final String[] listRecordAudioSource = {"MIC","UNPROCESSED","CAMCORDER","COMMUNICATION","RECOGNITION"};
    private static final int[] valueRecordAudioSource = {MediaRecorder.AudioSource.MIC,MediaRecorder.AudioSource.UNPROCESSED,MediaRecorder.AudioSource.CAMCORDER,MediaRecorder.AudioSource.VOICE_COMMUNICATION,MediaRecorder.AudioSource.VOICE_RECOGNITION};
    private int curRecordAudioSource = valueRecordAudioSource[0];
    private static final String[] listRecordChannel = {"Mono","Stereo"};
    private static final int[] valueRecordChannel = {AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO};
    private int curRecordChannel = valueRecordChannel[0];
    private static final String[] listRecordSamplingRate = {"48000","44100","16000","8000"};
    private static final int[] valueRecordSamplingRate = {48000,44100,16000,8000};
    private int curRecordSamplingRate = valueRecordSamplingRate[0];

    private int initialRecordAudioSourcePosition = 4;
    private int initialRecordChannelPosition = 0;
    private int initialSamplingRatePosition = 3;

    private boolean initialCheckboxIsChecked = false;

    private String recordFileNamePrefix = "";
    private String curRecordItemPath = "";
    private boolean isRecording = false;
    private boolean isRecordChecked = false;

    // Log settings
    private static final String[] listDisplayLogLevel = {"Verbose","Debug","Info","Warn","Error"};
    private static final int[] valueDisplayLogLevel = {0,1,2,3,4};
    private int curDisplayLogLevel = valueDisplayLogLevel[0];
    private int initialDisplayLogLevelPosition = 0;

    // Thread runnable.
    private PlayThread playThreadRunnable;
    private RecordThread recordThreadRunnable;
    private LogThread logThreadRunnable;
    private ProcessThread processThreadRunnable;




    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Log thread initialization.
        logThreadRunnable = new LogThread();
        setLogThread();

        // Log widgets
        Spinner spinnerDisplayLogLevel = findViewById(R.id.spinnerDisplayLogLevel);
        ArrayAdapter<String> adapterDisplayLogLevel = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listDisplayLogLevel);
        adapterDisplayLogLevel.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDisplayLogLevel.setAdapter(adapterDisplayLogLevel);
        spinnerDisplayLogLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                curDisplayLogLevel = valueDisplayLogLevel[position];
                setLogThread();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerDisplayLogLevel.setSelection(initialDisplayLogLevelPosition);

        TextView textviewDebugLog = findViewById(R.id.textviewDebugLog);
        textviewDebugLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        textviewDebugLog.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                if(event.getAction()==MotionEvent.ACTION_MOVE) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                if(event.getAction()==MotionEvent.ACTION_UP) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            }
        });

        CheckBox checkboxLog = findViewById(R.id.checkboxLog);
        checkboxLog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    findViewById(R.id.textviewDebugLog).setVisibility(View.VISIBLE);
                }
                else {
                    findViewById(R.id.textviewDebugLog).setVisibility(View.INVISIBLE);
                }
            }
        });
        checkboxLog.setChecked(initialCheckboxIsChecked);
        new Thread(logThreadRunnable).start();
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        // Process thread initialization
//        processThreadRunnable = new ProcessThread();
//        new Thread(processThreadRunnable).start();

        if(!(permissionCheck() && pathCheck())) {
//            finishAffinity();
            System.exit(0);
        }

        // Play thread.
        playThreadRunnable = new PlayThread();

        // Play widgets.
        TextView textviewPlayStatus = findViewById(R.id.textviewPlayStatus);
        Button buttonPlayStart = findViewById(R.id.buttonPlayStart);
        Button buttonPlayPause = findViewById(R.id.buttonPlayPause);
        Button buttonPlayReset = findViewById(R.id.buttonPlayReset);

        Spinner spinnerFileList = findViewById(R.id.spinnerFileList);
        Spinner spinnerPlayUsage = findViewById(R.id.spinnerPlayUsage);
        Spinner spinnerPlayChannel = findViewById(R.id.spinnerPlayChannel);
        Spinner spinnerPlaySamplingRate = findViewById(R.id.spinnerPlaySamplingRate);

        buttonPlayStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textviewPlayStatus.setText("Playing");
                new Thread(playThreadRunnable).start();
            }
        });

        buttonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textviewPlayStatus.setText("Pause");
                playThreadRunnable.pause();
            }
        });

        buttonPlayReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textviewPlayStatus.setText("Reset");
                playThreadRunnable.reset();
            }
        });

        listFileNames = getFileList(playListPath, "pcm");
        ArrayAdapter<String> adapterFileList = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listFileNames);
        adapterFileList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFileList.setAdapter(adapterFileList);
        spinnerFileList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if(position==0) {
                    LogThread.debugLog(2, TAG, "Refresh file list." );
                    listFileNames = getFileList(playListPath, "pcm");
                    ArrayAdapter<String> adapterFileList = new ArrayAdapter<String>(parent.getContext(), android.R.layout.simple_spinner_item, listFileNames);
                    adapterFileList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerFileList.setAdapter(adapterFileList);
                    curPlayItemPosition = position+1;
                    curPlayItemPath = playListPath + "/" + listFileNames[curPlayItemPosition];
                    LogThread.debugLog(1, TAG, "curPlayItemPath: " + curPlayItemPath);
                    spinnerFileList.setSelection(1);
                    setPlayThread();

                }
                else {
                    curPlayItemPosition = position;
                    curPlayItemPath = playListPath + "/" + listFileNames[curPlayItemPosition];
                    LogThread.debugLog(1, TAG, "curPlayItemPath: " + curPlayItemPath);
                    setPlayThread();
                }



            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<String> adapterPlayUsage = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listPlayUsage);
        adapterPlayUsage.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPlayUsage.setAdapter(adapterPlayUsage);
        spinnerPlayUsage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LogThread.debugLog(1, TAG, "curPlayUsage: " + listPlayUsage[position]);
                curPlayUsage = valuePlayUsage[position];
                setPlayThread();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerPlayUsage.setSelection(initialPlayUsagePosition);

        ArrayAdapter<String> adapterPlayChannel = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listPlayChannel);
        adapterPlayChannel.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPlayChannel.setAdapter(adapterPlayChannel);
        spinnerPlayChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LogThread.debugLog(1, TAG, "curPlayChannel: " + listPlayChannel[position]);
                curPlayChannel = valuePlayChannel[position];
                setPlayThread();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerPlayChannel.setSelection(initialPlayChannelPosition);

        ArrayAdapter<String> adapterPlaySamplingRate = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listPlaySamplingRate);
        adapterPlaySamplingRate.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPlaySamplingRate.setAdapter(adapterPlaySamplingRate);
        spinnerPlaySamplingRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                curPlaySamplingRate =  valuePlaySamplingRate[position];
                LogThread.debugLog(1,TAG, "curPlaySamplingRate: "+curPlaySamplingRate);
                setPlayThread();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerPlaySamplingRate.setSelection(initialPlaySamplingRatePosition);


        // Record thread.
        recordThreadRunnable = new RecordThread();

        // Record widgets
        CheckBox checkboxRecord = findViewById(R.id.checkboxRecord);
        Spinner spinnerRecordAudioSource = findViewById(R.id.spinnerRecordAudioSource);
        Spinner spinnerRecordChannel = findViewById(R.id.spinnerRecordChannel);
        Spinner spinnerRecordSamplingRate = findViewById(R.id.spinnerRecordSamplingRate);
        EditText textFileNamePrefix = findViewById(R.id.textFileNamePrefix);
        Button buttonRecord = findViewById(R.id.buttonRecord);

        checkboxRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LogThread.debugLog(1, TAG, "isRecordChecked: " + isChecked);
                isRecordChecked = isChecked;
                setRecordThread();
            }
        });

        ArrayAdapter<String> adapterRecordAudioSource = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listRecordAudioSource);
        adapterRecordAudioSource.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecordAudioSource.setAdapter(adapterRecordAudioSource);
        spinnerRecordAudioSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                curRecordAudioSource = valueRecordAudioSource[position];
                if(curRecordAudioSource == MediaRecorder.AudioSource.CAMCORDER) {
                    spinnerRecordChannel.setSelection(1);
                }
                LogThread.debugLog(1, TAG, "curRecordAudioSource: " + listRecordAudioSource[position]);
                setRecordThread();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerRecordAudioSource.setSelection(initialRecordAudioSourcePosition);

        ArrayAdapter<String> adapterRecordChannel = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listRecordChannel);
        adapterRecordChannel.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecordChannel.setAdapter(adapterRecordChannel);
        spinnerRecordChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                curRecordChannel = valueRecordChannel[position];
                LogThread.debugLog(1, TAG, "curRecordChannel: " + listRecordChannel[position]);
                setRecordThread();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerRecordChannel.setSelection(initialRecordChannelPosition);

        ArrayAdapter<String> adapterRecordSamplingRate = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listRecordSamplingRate);
        adapterRecordSamplingRate.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecordSamplingRate.setAdapter(adapterRecordSamplingRate);
        spinnerRecordSamplingRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                curRecordSamplingRate = valueRecordSamplingRate[position];
                LogThread.debugLog(1, TAG, "curRecordSamplingRate: " + listRecordSamplingRate[position]);
                setRecordThread();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerRecordSamplingRate.setSelection(initialSamplingRatePosition);

        textFileNamePrefix.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                recordFileNamePrefix = s.toString();
                LogThread.debugLog(1, TAG, "recordFileNamePrefix: " + s.toString());
            }
        });

        buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogThread.debugLog(2, TAG, "buttonRecord");

                if(isRecording) {
                    isRecording = !isRecording;
                    buttonRecord.setText("Start");
                    recordThreadRunnable.stop();
                }
                else {
                    SimpleDateFormat curDate = new SimpleDateFormat("yyyyMMddHHmmss");
                    curRecordItemPath = appPath + "/" + recordFileNamePrefix + curDate.format(new Date()) + ".pcm";
                    LogThread.debugLog(1, TAG, "curRecordItemPath: " + curRecordItemPath);
                    setRecordThread();
                    isRecording = !isRecording;
                    buttonRecord.setText("Stop");
                    new Thread(recordThreadRunnable).start();
                }
            }
        });

        // Process thread initialization
        processThreadRunnable = new ProcessThread();
        new Thread(processThreadRunnable).start();
    }




    // Permission related.
    private boolean permissionCheck() {
        boolean checkResult = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                LogThread.debugLog(2, TAG, "permissionCheckManageExternalStorage: OK");
            } else {
                checkResult  = false;
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
        if (permissionCheckAudio() && permissionCheckStorageRead() && permissionCheckStorageWrite()) {
            LogThread.debugLog(2, TAG, "All permission granted.");
        }
        else {
            permissionRequire(new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE});
            checkResult = false;
        }
        return checkResult;

    }
    private boolean permissionCheckAudio() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            LogThread.debugLog(2, TAG, "permissionCheckAudio: OK");
            return true;
        }
        else {
            return false;
        }
    }
    private boolean permissionCheckStorageRead() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            LogThread.debugLog(2, TAG, "permissionCheckStorageRead: OK");
            return true;
        }
        else {
            return false;
        }
    }
    private boolean permissionCheckStorageWrite() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            LogThread.debugLog(2, TAG, "permissionCheckStorageWrite: OK");
            return true;
        }
        else {
            return false;
        }
    }


    private void permissionRequire(String[] permissionString) {
        ActivityCompat.requestPermissions(this, permissionString, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LogThread.debugLog(2, TAG, "permissionCheckAudio: PERMISSION_GRANTED");
                }
                else {
                    LogThread.debugLog(2, TAG, "permissionCheckAudio: NOT_GRANTED");
                }
                if (grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    LogThread.debugLog(2, TAG, "permissionCheckReadStorage: PERMISSION_GRANTED");
                }
                else {
                    LogThread.debugLog(2, TAG, "permissionCheckReadStorage: NOT_GRANTED");
                }
                if (grantResults.length > 0 && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    LogThread.debugLog(2, TAG, "permissionCheckWriteStorage: PERMISSION_GRANTED");
                }
                else {
                    LogThread.debugLog(2, TAG, "permissionCheckWriteStorage: NOT_GRANTED");
                }
                break;
        }
    }

    // Check file paths.
    private boolean pathCheck(){
        File file;
        file= new File(appPath);
        if(!file.exists())
            file.mkdirs();
        file= new File(playListPath);
        if(!file.exists()) {
            file.mkdirs();
        }
        file= new File(playListPath+"/SoundLab.pcm");
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        LogThread.debugLog(1, TAG, "pathCheck: "+ file.exists());
        return file.exists();
    }

    // Get play list.
    private static String[] getFileList(String path, String type){
        File file =  new File(path);
//        File files[] = file.listFiles();
        String[] fileNameList = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(type);
            }
        });

        if(fileNameList==null) {
            fileNameList = new String[]{"Empty File List"};
        }
        Arrays.sort(fileNameList);
        String[] fileNameListFinal = new String[fileNameList.length+1];
        fileNameListFinal[0] = "Refresh file list.";
        for(int m=0;m<fileNameList.length;m++) {
            fileNameListFinal[m+1] = fileNameList[m];
        }

        return fileNameListFinal;
    }
    private static String[] getFileList(String path){
        File file =  new File(path);
        File[] files = file.listFiles();
        String[] fileNames = new String[files.length];
        for(int i=0;i<files.length;i++){
            fileNames[i] = files[i].getName();
        }
        return fileNames;
    }

    // set play thread
    private void setPlayThread() {
        playThreadRunnable.setup(this, curPlayItemPath, curPlayUsage, curPlayChannel, curPlaySamplingRate);
    }

    // set record thread
    private void setRecordThread() {

        recordThreadRunnable.setup(this,curRecordItemPath, curRecordAudioSource, curRecordChannel, curRecordSamplingRate, isRecordChecked);
        setProcessThread();
    }

    //set log thread
    private void setLogThread() {
        logThreadRunnable.setup(this, curDisplayLogLevel);
    }

    private void setProcessThread() {
        processThreadRunnable.setup(this, curRecordChannel, curRecordSamplingRate);
    }







}