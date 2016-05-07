package com.bkromhout.rrvl;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * Implementation of {@code ItemTouchHelper.Callback} for supporting drag and drop. Adapted from <a
 * href="https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf">this article</a>.
 */
class RealmSimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {
    interface Listener {
        boolean onMove(RecyclerView.ViewHolder dragging, RecyclerView.ViewHolder target);

        void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState);

        void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder);

        boolean onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive);

        boolean onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX,
                                float dY, int actionState, boolean isCurrentlyActive);
    }

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

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        listener.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        listener.clearView(recyclerView, viewHolder);
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        // Either let listener handle it or call super.
        if (!listener.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive))
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX,
                                float dY, int actionState, boolean isCurrentlyActive) {
        // Either let listener handle it or call super.
        if (!listener.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive))
            super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
