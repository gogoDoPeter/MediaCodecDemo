package com.infinite.mediacodecdemo.util;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {
    private static final String TAG="FileUtils";

    public static void dumpData(byte[] data, int width, int height, String fileName) {
        if (data == null || width <= 0 || height <= 0) {
            Log.d(TAG, "input param error");
            return;
        }
        int dataSize = data.length;
        Log.d(TAG,"dump file="+fileName+" dataSize="+dataSize);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileName));
            bos.write(data, 0, dataSize);
            bos.flush();
            bos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
