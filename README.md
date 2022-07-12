# SoundLab

A sound tool for Android smartphone.

![image](https://user-images.githubusercontent.com/20986755/178256091-c5b5152b-3851-4f2b-b8ac-d7c670e79c87.png)

## Requirements

SoundLab require recording and storage permission. SoundLab will make a folder named 'SoundLab' in external storage. Please copy files to be played to SoundLab\PlayList. Playing and recording only support .pcm file. PLease refer to https://en.wikipedia.org/wiki/Pulse-code_modulation for more information about pcm encoding. 

## Instructions from top to bottom.

## Play:

Played file selection.

Playing parameters slsection:
- ContentType: MUSIC/CALL
- ChannelMask: STEREO/MONO
- SampleRate: 48000/44100/16000/8000

Refer to `AudioTrack.Builder()` 

State display: Ready/Playing/Pause/Reset

Left time in seconds.

Action buttons: START/PAUSE/RESET


## Record:

Check the checkbox of record and use 'start' button to record sound.

Recording parameters slsection:
- AudioSource: MIC/UNPROCESSED/CAMCORDER
- ChannelMask: STEREO/MONO
- SampleRate: 48000/44100

Refer to `AudioRecord.Builder()` 

The default file name will be current time like '20220102040506.pcm'. 
File name prefix will be added before the default file name. For example, 'PrefixTest20220102040506.pcm'

Action buttons: START/STOP. Once click 'START', the button will be 'Stop'(and vice versa). 
Even the recording checkbox is bot checked, the recording will still work just without writing to file.

## Info display

### Sound Volume

Two recording channel volume display gives a volume in dB. However, it is not calibrated, the volume number only gives a reference. 
For stereo recording, two channels will be displayed separately. 
For mono chcannel recording, two channels will be the same. 
The bottom value number is the average volume of two channels.

### Log

Build in light weight log display for debug. 