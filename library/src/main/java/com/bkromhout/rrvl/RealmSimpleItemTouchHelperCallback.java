package com.bkromhout.rrvl;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * Implementation of {@link ItemTouchHelper.Callback} for supporting drag and drop. Adapted from <a
 * href="https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf">this article</a>.
 */
public class RealmSimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private final boolean isDragTriggerLongClick;
    private Listener listener;

    public RealmSimpleItemTouchHelperCallback(boolean isDragTriggerLongClick) {
        this.isDragTriggerLongClick = isDragTriggerLongClick;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return isDragTriggerLongClick;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder dragging, RecyclerView.ViewHolder target) {
        return dragging.getItemViewType() == target.getItemViewType() && listener.onMove(dragging, target);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        // Never called.
    }

    public interface Listener {
        boolean onMove(RecyclerView.ViewHolder dragging, RecyclerView.ViewHolder target);
    }
}
