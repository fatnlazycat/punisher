package org.foundation101.thepunisher;

/**
 * Created by Dima on 27.05.2016.
 */
public interface ItemTouchHelperAdapter {
    void onItemDismiss(int position);
    boolean onItemMove(int fromPosition, int toPosition);
}
