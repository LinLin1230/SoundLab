package com.example.lin.soundlab;

import android.os.Build;

import java.util.Locale;

public class Utils {
    public static String getPlatform() {
        return Build.HARDWARE;
    }

    public static boolean isMTK(){
        return getPlatform().toLowerCase(Locale.ROOT).contains("mt");
    }

    public static boolean isQCOM(){
        return getPlatform().toLowerCase(Locale.ROOT).contains("qcom");
    }
}
