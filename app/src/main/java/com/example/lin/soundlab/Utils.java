package com.example.lin.soundlab;

import android.os.Build;

public class Utils {
    public static String getPlatform() {
        return Build.HARDWARE;
    }

    public static boolean isMTK(){
        return getPlatform().contains("mt");
    }

    public static boolean isQCOM(){
        return getPlatform().contains("QCOM");
    }
}
