package com.example.lin.soundlab;

public class SonicQueue {
    private static String TAG = "SonicQueue";
    private int lengthInSecond = 60;
    private int lengthInFrame = lengthInSecond*48000;
    private short[] queue;
    private int pointerHead = 0;
    private int pointerTail = 0;

    public SonicQueue() {
        queue = new short[lengthInFrame];
    }

    public SonicQueue(int length) {
        lengthInFrame = length;
        queue = new short[lengthInFrame];
    }

    // write byte[]
    public boolean write(byte[] data) {
        if (getLength()+(data.length/2) >= getCapacity()) {
            return false;
        }
        else {
            for(int i=0;i<data.length;i+=2) {
                queue[pointerTail] = (short)(data[i] & 0x00ff | data[i+1] << 8);
                pointerTail = (pointerTail + 1) % lengthInFrame;
            }
            return true;
        }
    }

    // write short[]
    public boolean write(short[] data) {
        if (getLength()+(data.length) >= getCapacity()) {
            return false;
        }
        else {
            for(int i=0;i<data.length;i++) {
                queue[pointerTail] = data[i];
                pointerTail = (pointerTail + 1) % lengthInFrame;
            }
            return true;
        }

    }

    // read byte[]
    public boolean read(byte[] data) {
        int len = data.length/2;
        if(getLength()<len)
            return false;
        for(int i=0;i<len;i++) {
            data[2*i] = (byte)(queue[pointerHead] >> 0);
            data[2*i+1] = (byte)(queue[pointerHead] >> 8);
            pointerHead = (pointerHead + 1) % lengthInFrame;
        }
        return true;
    }

    // read short[]
    public boolean read(short[] data) {
        int len = data.length;
        if(getLength()<len)
            return false;
        for(int i=0;i<len;i++) {
            data[i] = queue[pointerHead];
            pointerHead = (pointerHead + 1) % lengthInFrame;
        }
        return true;
    }

    // read but not buffer read
    public boolean storageRead(short[] data) {
        int tempPointer = pointerTail - data.length;
        while(tempPointer<0)
            tempPointer += lengthInFrame;
        for(int i=0;i<data.length;i++) {
            data[i] = queue[tempPointer];
            tempPointer = (tempPointer + 1) % lengthInFrame;
        }
        return true;
    }

    // get current data length in short
    public int getLength() {
        int len  =(pointerTail-pointerHead);
        len = len<0?len+lengthInFrame:len;
        return len;
    }

    // get current data capacity in short
    public int getCapacity() {
        return lengthInFrame;
    }


}
