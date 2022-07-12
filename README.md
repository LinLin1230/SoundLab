# SoundLab

A sound tool for Android smartphone. 
SoundLab supports playing and recording loseless pcm encoding files.

![image](https://user-images.githubusercontent.com/20986755/178256091-c5b5152b-3851-4f2b-b8ac-d7c670e79c87.png)

## Requirements

SoundLab requires recording and storage permissions. 

## Storage
SoundLab will make a folder named 'SoundLab'. 
Recorded files will be stored in 'SoundLab\'.
Please copy files to be played to SoundLab\PlayList. 

## PCM

Playing and recording only support .pcm file. PLease refer to https://en.wikipedia.org/wiki/Pulse-code_modulation for more information about pcm encoding. 

## Instructions from top to bottom.

## Play:

Played file selection.

Playing parameters selection:
- ContentType: MUSIC/CALL
- ChannelMask: STEREO/MONO
- SampleRate: 48000/44100/16000/8000

Refer to `AudioTrack.Builder()`. 

State display: Ready/Playing/Pause/Reset

Left time in seconds.

Action buttons: START/PAUSE/RESET


## Record:

Check the checkbox of 'Record' and use 'START' button to record sound.

Recording parameters selection:
- AudioSource: MIC/UNPROCESSED/CAMCORDER
- ChannelMask: STEREO/MONO
- SampleRate: 48000/44100

Refer to `AudioRecord.Builder()`. 

The default file name will be current time like '20220102040506.pcm'. 
File name prefix will be added before the default file name. For example, 'PrefixTest20220102040506.pcm'

Action buttons: START/STOP. 

Once click 'START', the button will be 'STOP'(and vice versa). 
Even the recording checkbox is not checked, the recording will still work just without writing to file.

## Info display

### Sound Volume

Two recording channel volume display gives a volume in dB. However, it is not calibrated, the volume number is for reference only. 
For stereo recording, two channels will be displayed separately. 
For mono chcannel recording, two channels will be the same. 
The bottom value number is the average volume of two channels.

### Log

Build in light weight log display for debug. 