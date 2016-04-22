package com.bkromhout.rrvl;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * Implementation of {@code ItemTouchHelper.Callback} for supporting drag and drop. Adapted from <a
 * href="https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf">this article</a>.
 */
public class RealmSimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private boolean dragAndDrop;
    private boolean longClickTriggersDrag;
    private Listener listener;

    RealmSimpleItemTouchHelperCallback(boolean dragAndDrop, boolean longClickTriggersDrag) {
        this.dragAndDrop = dragAndDrop;
        this.longClickTriggersDrag = longClickTriggersDrag;
    }

    void setDragAndDrop(boolean enabled) {
        this.dragAndDrop = enabled;
    }

    boolean getLongClickTriggersDrag() {
        return longClickTriggersDrag;
    }

    void setLongClickTriggersDrag(boolean longClickTriggersDrag) {
        this.longClickTriggersDrag = longClickTriggersDrag;
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return dragAndDrop && longClickTriggersDrag;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(dragAndDrop ? (ItemTouchHelper.UP | ItemTouchHelper.DOWN) : 0, 0);
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
