package org.foundation101.karatel.utils;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.Set;

/**
 * Created by Dima on 29.09.2017.
 */

public class DescriptionFormatter {
    @NonNull
    public static String format(Map<String, String> data) {
        StringBuilder result = new StringBuilder("<br/>");
        Set<Map.Entry<String, String>> entries = data.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            result  .append("<b>")
                    .append(entry.getKey())
                    .append(":</b> ")
                    .append(entry.getValue())
                    .append("<br/>");
        }
        return result.toString();
    }
}
