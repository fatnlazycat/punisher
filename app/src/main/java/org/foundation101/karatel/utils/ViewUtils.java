package org.foundation101.karatel.utils;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Dima on 04.06.2017.
 */

public class ViewUtils {
    public static void applyToAllChildren(ViewGroup parent, ApplicableToView samInterface) {
        for (int i = 0; i < parent.getChildCount(); i++){
            View v = parent.getChildAt(i);
            samInterface.methodToApply(v);
            if (v instanceof ViewGroup) applyToAllChildren((ViewGroup)v, samInterface);
        }
    }

    public static View findFirstAmongChildren(ViewGroup parent, ApplicableToView samInterface) {
        View result = null;
        for (int i = 0; i < parent.getChildCount(); i++){
            View v = parent.getChildAt(i);
            if (samInterface.methodToApply(v)) return v;
            if (v instanceof ViewGroup) result = findFirstAmongChildren((ViewGroup)v, samInterface);
            if (result != null) return result;
        }
        return null;
    }
}
