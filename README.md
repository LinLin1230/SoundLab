SoundLab

A sound tool for Android smartphone.

![image](https://user-images.githubusercontent.com/20986755/178256091-c5b5152b-3851-4f2b-b8ac-d7c670e79c87.png)

Require recording and storage permission.
SoundLab will make a folder named 'SoundLab' in external storage.
Please copy files to be played to SoundLab\PlayList.
Playing and recording only in .pcm file.
PLease refer to https://en.wikipedia.org/wiki/Pulse-code_modulation for more information. 

Play:

ContentType: MUSIC/CALL

ChannelMask: STEREO/MONO

SampleRate: 48000/44100/16000/8000

Refer to AudioTrack.Builder() 

Record:

AudioSource: MIC/UNPROCESSED/CAMCORDER

ChannelMask: STEREO/MONO

SampleRate: 48000/44100

Check the checkbox of record and use 'start' button to record sound.

Refer to AudioRecord.Builder() 
