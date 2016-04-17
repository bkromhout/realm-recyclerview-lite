package com.bkromhout.rrvl;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.RelativeLayout;
import io.realm.RealmBasedRecyclerViewAdapter;

/**
 * A RecyclerView that supports Realm.
 */
public class RealmRecyclerView extends RelativeLayout implements RealmBasedRecyclerViewAdapter.StartDragListener {

    private enum DragTrigger {
        UserDefined, LongClick
    }

    private enum FastScrollMode {
        Off, Handle, HandleBubble
    }

    private RecyclerView recyclerView;
    private FastScroller fastScroller;
    private ViewStub emptyContentContainer;
    private RealmBasedRecyclerViewAdapter adapter;
    private ItemTouchHelper touchHelper;
    private RealmSimpleItemTouchHelperCallback realmSimpleItemTouchHelperCallback;

    // Attributes
    private int emptyViewId;
    private boolean dragAndDrop;
    private DragTrigger dragTrigger;
    private FastScrollMode fastScrollMode;

    public RealmRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public RealmRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RealmRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.realm_recycler_view, this);
        initAttrs(context, attrs);

        recyclerView = (RecyclerView) findViewById(R.id.rrv_recycler_view);
        fastScroller = (FastScroller) findViewById(R.id.rrv_fast_scroller);
        emptyContentContainer = (ViewStub) findViewById(R.id.rrv_empty_content_container);

        if (emptyViewId != 0) {
            emptyContentContainer.setLayoutResource(emptyViewId);
            emptyContentContainer.inflate();
        }

        // Only show system scrollbar if we don't have a fast scroller.
        recyclerView.setVerticalScrollBarEnabled(fastScrollMode == FastScrollMode.Off);

        // Set LinearLayoutManager, override the onLayoutChildren() method if we have a fast scroller.
        recyclerView.setLayoutManager(fastScrollMode == FastScrollMode.Off ? new LinearLayoutManager(getContext()) :
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false) {
                    @Override
                    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                        super.onLayoutChildren(recycler, state);
                        final int firstVisibleItemPosition = findFirstVisibleItemPosition();
                        if (firstVisibleItemPosition != 0) {
                            // Hide the fast scroller if not initialized, or no items are shown.
                            if (firstVisibleItemPosition == -1) fastScroller.setVisibility(View.GONE);
                            return;
                        }
                        final int lastVisibleItemPosition = findLastVisibleItemPosition();
                        int itemsShown = lastVisibleItemPosition - firstVisibleItemPosition + 1;
                        // Hide fast scroller if all items are visible in the viewport currently.
                        fastScroller.setVisibility(adapter != null && adapter.getItemCount() > itemsShown
                                ? View.VISIBLE : View.GONE);
                    }
                });
        recyclerView.setHasFixedSize(true);

        if (dragAndDrop) {
            realmSimpleItemTouchHelperCallback = new RealmSimpleItemTouchHelperCallback(
                    dragTrigger == DragTrigger.LongClick);
            touchHelper = new ItemTouchHelper(realmSimpleItemTouchHelperCallback);
            touchHelper.attachToRecyclerView(recyclerView);
        }

        if (fastScrollMode != FastScrollMode.Off) {
            fastScroller.setVisibility(VISIBLE);
            fastScroller.setRecyclerView(recyclerView);
            fastScroller.setViewsToUse(R.layout.fast_scroller, fastScrollMode == FastScrollMode.Handle ? -1
                    : R.id.fast_scroller_bubble, R.id.fast_scroller_handle);
        }
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RealmRecyclerView);

        emptyViewId = typedArray.getResourceId(R.styleable.RealmRecyclerView_rrvlEmptyLayoutId, 0);

        dragAndDrop = typedArray.getBoolean(R.styleable.RealmRecyclerView_rrvlDragAndDrop, false);

        int dragStartTriggerValue = typedArray.getInt(R.styleable.RealmRecyclerView_rrvlDragStartTrigger, -1);
        if (dragStartTriggerValue != -1) dragTrigger = DragTrigger.values()[dragStartTriggerValue];
        else dragTrigger = DragTrigger.UserDefined;

        int fastScrollModeValue = typedArray.getInt(R.styleable.RealmRecyclerView_rrvlFastScrollMode, -1);
        if (fastScrollModeValue != -1) fastScrollMode = FastScrollMode.values()[fastScrollModeValue];
        else fastScrollMode = FastScrollMode.Off;

        typedArray.recycle();
    }

    public void setAdapter(final RealmBasedRecyclerViewAdapter adapter) {
        this.adapter = adapter;
        recyclerView.setAdapter(adapter);

        if (dragAndDrop) realmSimpleItemTouchHelperCallback.setListener(adapter);
        if (dragAndDrop && dragTrigger == DragTrigger.UserDefined) adapter.setOnStartDragListener(this);

        if (adapter != null) {
            adapter.registerAdapterDataObserver(
                    new RecyclerView.AdapterDataObserver() {
                        @Override
                        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                            super.onItemRangeMoved(fromPosition, toPosition, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeRemoved(int positionStart, int itemCount) {
                            super.onItemRangeRemoved(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeInserted(int positionStart, int itemCount) {
                            super.onItemRangeInserted(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeChanged(int positionStart, int itemCount) {
                            super.onItemRangeChanged(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onChanged() {
                            super.onChanged();
                            update();
                        }

                        private void update() {
                            updateEmptyContentContainerVisibility(adapter);
                        }
                    }
            );
            updateEmptyContentContainerVisibility(adapter);
        }
    }

    private void updateEmptyContentContainerVisibility(RecyclerView.Adapter adapter) {
        if (emptyViewId == 0) return;
        emptyContentContainer.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public final void startDragging(RecyclerView.ViewHolder viewHolder) {
        if (touchHelper != null) touchHelper.startDrag(viewHolder);
    }

    /*
     * Expose the RecyclerView.
     */

    /**
     * Get the actual RecyclerView which backs this {@link RealmRecyclerView}.
     * @return Internal RecyclerView.
     */
    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    /*
     * Expose the LinearLayoutManager.
     */

    /**
     * Get the LinearLayoutManager attached to the RealmRecyclerView.
     * @return LinearLayoutManager.
     */
    public LinearLayoutManager getLayoutManger() {
        return (LinearLayoutManager) recyclerView.getLayoutManager();
    }
}
