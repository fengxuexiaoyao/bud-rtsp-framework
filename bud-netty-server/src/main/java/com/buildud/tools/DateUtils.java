package com.buildud.tools;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by fengxuexiaoyao on 2021/3/16.
 */
public class DateUtils {

    public static SimpleDateFormat milliFormat = new SimpleDateFormat("HHmmssSSS");

    public static String getMillisecond(){
        return milliFormat.format(new Date());
    }

    public static Integer getMillisecondToInt(){
        String time = getMillisecond();
        return Integer.valueOf(time);
    }
}
