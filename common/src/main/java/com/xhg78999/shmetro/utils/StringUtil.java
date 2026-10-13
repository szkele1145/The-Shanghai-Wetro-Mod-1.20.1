package com.xhg78999.shmetro.utils;

public class StringUtil {
    public static String getCjkMain(String name) {
        try {
            return name.split("\\|\\|")[0].split("\\|")[0];
        }
        catch (Exception e) {
            return name;
        }
    }

    public static String getEngMain(String name) {
        try {
            return name.split("\\|\\|")[0].split("\\|")[1];
        }
        catch (Exception e) {
            return "";
        }
    }

    public static String getAnnotation(String name) {
        try {
            return name.split("\\|\\|")[1];
        }
        catch (Exception e) {
            return "";
        }
    }
}

