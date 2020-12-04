package com.msisuzney.glidehelper;

//import com.msisuzney.glidehelper.BuildConfig;

public class LogSwitch {
    public static boolean LOGGING = false;

    public static void open() {
        LOGGING = true;
    }

    public static void close() {
        LOGGING = false;
    }
}
