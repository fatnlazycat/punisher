package org.foundation101.karatel.utils;

import android.content.Context;

import org.foundation101.karatel.KaratelApplication;

/**
 * Created by Dima on 02.01.2018.
 */

public class LayoutUtils {
    public static int dpToPx(int dp){
        return dpToPx(KaratelApplication.getInstance(), dp);
    }

    public static int dpToPx(Context context, int dp){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }
}
