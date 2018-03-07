package com.example.sucharita.cameraapp;

import android.os.Environment;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by sucharithanalla on 08-01-2018.
 */

public class PathUtils {

    static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM-dd-yyyy");
    static Calendar cal = Calendar.getInstance();
    static String date = simpleDateFormat.format(cal.getTime());

    public static String getDateString(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM-dd-yyyy HH:mm");
        Calendar cal = Calendar.getInstance();
        String date = simpleDateFormat.format(cal.getTime());
        return date;
    }

    public static String folderPath = Environment.getExternalStorageDirectory() + "/Memory Mirror/";
    public static String photoSavePath = folderPath+"/Pictures/";
    public static String photoName = "img_";
    public static String videoSavePath = folderPath+"/Videos/";
    public static String videoName = "vid_";
}
