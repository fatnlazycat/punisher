package org.foundation101.karatel.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ScrollView;

import org.foundation101.karatel.Globals;

/**
 * Created by Dima on 07.07.2016.
 */
public class MyScrollView extends ScrollView{

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (!isFocusDescendantOfThis()){
            View viewToFocus = getFirstFocusableInHierarchy(this);
            if (viewToFocus != null) viewToFocus.requestFocus();
        }
        try {
            super.onSizeChanged(w, h, oldw, oldh);
        } catch (IllegalArgumentException e) {
            Globals.showError(getContext(), e.getMessage(), e);
        }
    }

    boolean isFocusDescendantOfThis(){
        View currentFocused = findFocus();
        ViewParent parent = null;
        if (currentFocused != null) {
            parent = currentFocused.getParent();
        }
        while ((parent != null) && (parent instanceof View)){
            currentFocused = (View) parent;
            parent = currentFocused.getParent();
            if (parent == this) return true;
        }
        return false;
    }

    View getFirstFocusableInHierarchy(View oneView){
        boolean hasMore;
        if (oneView.isFocusableInTouchMode()) return oneView;
        if (oneView instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) oneView).getChildCount(); i++) {
                hasMore =  (i+1 != ((ViewGroup) oneView).getChildCount());
                View result = getFirstFocusableInHierarchy(((ViewGroup) oneView).getChildAt(i));
                if (result != null || !hasMore)
                    return result;
            }
        }
        return null;
    }

    public MyScrollView(Context context) {
        super(context);
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

}
