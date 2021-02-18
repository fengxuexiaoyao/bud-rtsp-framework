package com.buildud.tools;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class FileUtils {

    private static  File file = new File("D:\\20210204.txt");

    public static void writeLineFile(byte[] content){
        try {
            FileOutputStream out = new FileOutputStream(file);
            writeLineFile(out,content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeLineFile(String filename, byte[] content){
        try {
            FileOutputStream out = new FileOutputStream(filename);
            writeLineFile(out,content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeLineFile(FileOutputStream out, byte[] content){
        try {
            OutputStreamWriter outWriter = new OutputStreamWriter(out, "UTF-8");
            BufferedWriter bufWrite = new BufferedWriter(outWriter);
            bufWrite.newLine();
            for (int i = 0; i < content.length; i++) {
                bufWrite.write(content[i]);
            }
            bufWrite.flush();
            bufWrite.write("\r\n");
            bufWrite.close();
            outWriter.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
