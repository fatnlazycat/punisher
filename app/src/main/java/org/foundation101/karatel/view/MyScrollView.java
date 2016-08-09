package org.foundation101.karatel.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ScrollView;

import org.foundation101.karatel.Globals;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dima on 07.07.2016.
 */
public class MyScrollView extends ScrollView{

    /*
     *peace of code that fights the problem when keyboard gets focused and this throws exception
     */

    /*@Override
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
    }*/


    public MyScrollView(Context context) {
        super(context);
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //this is used to enable internal scrolling of views within this ScrollView
    List<View> mInterceptScrollViews = new ArrayList<>();

    public void addInterceptScrollView(View view) {
        mInterceptScrollViews.add(view);
    }

    public void removeInterceptScrollView(View view) {
        mInterceptScrollViews.remove(view);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        // check if we have any views that should use their own scrolling
        if (mInterceptScrollViews.size() > 0) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            Rect bounds = new Rect();

            for (View view : mInterceptScrollViews) {
                view.getHitRect(bounds);
                if (bounds.contains(x, y)) {
                    //were touching a view that should intercept scrolling
                    return false;
                }
            }
        }

        return super.onInterceptTouchEvent(event);
    }
}

