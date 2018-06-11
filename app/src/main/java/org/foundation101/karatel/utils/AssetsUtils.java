package org.foundation101.karatel.utils;

import org.foundation101.karatel.KaratelApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AssetsUtils {
    public static String readAssets(String fileName) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        InputStream inputStream = KaratelApplication.getInstance().getAssets().open(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();

        return stringBuilder.toString();
    }
}
