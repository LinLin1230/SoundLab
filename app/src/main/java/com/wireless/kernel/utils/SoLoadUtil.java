package com.wireless.kernel.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.example.lin.soundlab.LogThread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

public class SoLoadUtil {
    private static final String TAG = "SoLoadUtil";
    private static final String SP_NAME = "so_load_sp";
    public static final String TXLIB = "/txlib/";
    public static final String MODULE = "/module/";
    private static final int COPY_RETRY_MAX_COUNT = 5;
    private static Set<String> needRetryLibSet = new HashSet();

    public SoLoadUtil() {
    }

    public static boolean loadNativeLibrary(Context context, String libName) {
        boolean result = false;
        System.loadLibrary(libName);
        try {
            System.loadLibrary(libName);
            result = true;
        } catch (Throwable var4) {
            LogThread.debugLog(4, TAG, "loadNativeLibrary " + libName + " failed," + var4.getMessage());
        }

        if (!result) {
            result = loadSoFromAssets(context, libName, 2);
        } else {
            LogThread.debugLog(2, TAG, "loadNativeLibrary " + libName + " success");
        }

        return result;
    }

    public static boolean loadSoFromAssets(Context context, String libName, int version) {
        boolean rs = false;
        int copyRetryCount = 0;
        libName = "lib" + libName + ".so";
        String destDir = context.getFilesDir().getParent() + "/txlib/";
        String destPath = destDir + libName;
        SharedPreferences sp = context.getSharedPreferences("so_load_sp", 0);
        int oldVer = sp.getInt(libName, -1);
        if (oldVer == version) {
            try {
                System.load(destPath);
                rs = true;
            } catch (Throwable var45) {
                LogThread.debugLog(4, TAG, "loadSoFromAssets error: " + var45.getMessage());
            }
        }

        if (!rs) {
            InputStream is = null;
            FileOutputStream os = null;

            try {
                is = context.getAssets().open("lib/" + getDefaultPlatformString() + "/" + libName);
                File dir = new File(destDir);
                if (dir.exists() || dir.mkdir()) {
                    File destFile = new File(destPath);

                    while (true) {
                        boolean copyRs = false;
                        os = new FileOutputStream(destFile);

                        try {
                            copy(is, os);
                            copyRs = true;
                        } catch (Exception var41) {
                        } finally {
                            try {
                                os.close();
                            } catch (IOException var40) {
                                var40.printStackTrace();
                            }

                        }

                        if (copyRs || !needRetryLibSet.contains(libName) || copyRetryCount >= 5) {
                            sp.edit().putInt(libName, version).apply();
                            System.load(destPath);
                            rs = true;
                            break;
                        }

                        ++copyRetryCount;
                    }
                }
            } catch (Throwable var43) {
                StringBuilder ssb = new StringBuilder();
                ssb.append(libName).append(" is exist: ")
                        .append((new File(destPath)).exists())
                        .append(" , and load exception:")
                        .append(var43.getMessage());
                LogThread.debugLog(1, TAG, ssb.toString());
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException var39) {
                        var39.printStackTrace();
                    }
                }

            }
        }

        LogThread.debugLog(1, TAG, libName + ".so|version=" + version + "|load success = " + rs);

        return rs;
    }

    public static long copy(InputStream is, OutputStream os) throws IOException {
        long size = 0L;

        int bytesRead;
        for (byte[] buffer = new byte[8192];
             (bytesRead = is.read(buffer, 0, buffer.length)) != -1;
             size += (long) bytesRead) {
            os.write(buffer, 0, bytesRead);
        }

        return size;
    }

    public static String getModuleFilePath(Context context, String moduleAssertsDir, String fileName, int version) {
        InputStream is = null;
        OutputStream os = null;
        String moduleDestDir = "";
        String destDir = context.getFilesDir().getParent() + "/module/";
        String destPath = destDir + fileName;
        SharedPreferences sp = context.getSharedPreferences("so_load_sp", 0);
        int oldVer = sp.getInt(fileName, 0);
        if (oldVer == version) {
            return destDir;
        } else {
            try {
                is = context.getAssets().open(moduleAssertsDir + fileName);
                File dir = new File(destDir);
                if (dir.exists() || dir.mkdir()) {
                    File destFile = new File(destPath);
                    os = new FileOutputStream(destFile);

                    try {
                        copy(is, os);
                        moduleDestDir = destDir;
                        sp.edit().putInt(fileName, version).apply();
                        LogThread.debugLog(1, TAG, "fileName[" + fileName + "], moduleDestDir[" + destDir + "]");
                    } catch (Exception var43) {
                    } finally {
                        try {
                            is.close();
                            os.close();
                        } catch (IOException var42) {
                            var42.printStackTrace();
                        }

                    }
                }
            } catch (IOException var45) {
                var45.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException var41) {
                        var41.printStackTrace();
                    }
                }

                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException var40) {
                        var40.printStackTrace();
                    }
                }

            }

            return moduleDestDir;
        }
    }

    private static String getDefaultPlatformString() {
        String platform = Build.CPU_ABI;
        return platform != null && platform.contains("64") ? "arm64-v8a" : "armeabi";
    }
}
