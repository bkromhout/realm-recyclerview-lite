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
 * <p/>
 * See {@link com.bkromhout.rrvl.R.styleable#RealmRecyclerView RealmRecyclerView Attributes}
 * @attr ref com.bkromhout.rrvl.R.styleable#RealmRecyclerView_rrvlEmptyLayoutId
 * @attr ref com.bkromhout.rrvl.R.styleable#RealmRecyclerView_rrvlDragAndDrop
 * @attr ref com.bkromhout.rrvl.R.styleable#RealmRecyclerView_rrvlDragStartTrigger
 * @attr ref com.bkromhout.rrvl.R.styleable#RealmRecyclerView_rrvlFastScrollEnabled
 * @attr ref com.bkromhout.rrvl.R.styleable#RealmRecyclerView_rrvlAutoHideFastScrollHandle
 * @attr ref com.bkromhout.rrvl.R.styleable#RealmRecyclerView_rrvlHandleAutoHideDelay
 * @attr ref com.bkromhout.rrvl.R.styleable#RealmRecyclerView_rrvlUseFastScrollBubble
 */
public class RealmRecyclerView extends RelativeLayout implements RealmBasedRecyclerViewAdapter.StartDragListener {

    private enum DragTrigger {
        UserDefined, LongClick
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
    private boolean fastScrollEnabled;

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

        // Read attributes.
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RealmRecyclerView);
        emptyViewId = typedArray.getResourceId(R.styleable.RealmRecyclerView_rrvlEmptyLayoutId, 0);
        // Drag and drop attributes.
        dragAndDrop = typedArray.getBoolean(R.styleable.RealmRecyclerView_rrvlDragAndDrop, false);
        int dragStartTriggerVal = typedArray.getInt(R.styleable.RealmRecyclerView_rrvlDragStartTrigger, -1);
        dragTrigger = dragStartTriggerVal != -1 ? DragTrigger.values()[dragStartTriggerVal] : DragTrigger.UserDefined;
        // Fast scroll attributes.
        fastScrollEnabled = typedArray.getBoolean(R.styleable.RealmRecyclerView_rrvlFastScrollEnabled, false);
        boolean autoHideHandle = typedArray.getBoolean(R.styleable.RealmRecyclerView_rrvlAutoHideFastScrollHandle,
                false);
        int handleAutoHideDelay = typedArray.getInt(R.styleable.RealmRecyclerView_rrvlHandleAutoHideDelay,
                FastScroller.DEFAULT_HANDLE_HIDE_DELAY);
        boolean useFastScrollBubble = typedArray.getBoolean(R.styleable.RealmRecyclerView_rrvlUseFastScrollBubble,
                false);
        typedArray.recycle();

        // Get views.
        recyclerView = (RecyclerView) findViewById(R.id.rrv_recycler_view);
        fastScroller = (FastScroller) findViewById(R.id.rrv_fast_scroller);
        emptyContentContainer = (ViewStub) findViewById(R.id.rrv_empty_content_container);

        // Inflate empty view if present.
        if (emptyViewId != 0) {
            emptyContentContainer.setLayoutResource(emptyViewId);
            emptyContentContainer.inflate();
        }

        // Set LinearLayoutManager, override the onLayoutChildren() method.
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false) {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                if (!fastScrollEnabled) return;

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

        // Drag and drop.
        if (dragAndDrop) {
            realmSimpleItemTouchHelperCallback = new RealmSimpleItemTouchHelperCallback(
                    dragTrigger == DragTrigger.LongClick);
            touchHelper = new ItemTouchHelper(realmSimpleItemTouchHelperCallback);
            touchHelper.attachToRecyclerView(recyclerView);
        }

        // Fast scroll.
        fastScroller.setRecyclerView(recyclerView);
        fastScroller.setUseBubble(useFastScrollBubble);
        fastScroller.setAutoHideHandle(autoHideHandle);
        fastScroller.setAutoHideDelay(handleAutoHideDelay);
        // Only show system scrollbar if we don't have a fast scroller.
        recyclerView.setVerticalScrollBarEnabled(!fastScrollEnabled);
        if (fastScrollEnabled) fastScroller.setVisibility(VISIBLE);
    }

    private void updateEmptyContentContainerVisibility(RecyclerView.Adapter adapter) {
        if (emptyViewId == 0) return;
        emptyContentContainer.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public final void startDragging(RecyclerView.ViewHolder viewHolder) {
        if (touchHelper != null) touchHelper.startDrag(viewHolder);
    }

    /**
     * Set the adapter for this RealmRecyclerView.
     * @param adapter {@link RealmBasedRecyclerViewAdapter}.
     */
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

    /**
     * Enable/Disable the fast scroller. The system-drawn scrollbars will be enabled if the fast scroller isn't (and
     * vice versa).
     * @param enabled Whether to enable the fast scroller or not.
     */
    @SuppressWarnings("unused")
    public final void setFastScrollEnabled(boolean enabled) {
        this.fastScrollEnabled = enabled;
        recyclerView.setVerticalScrollBarEnabled(!enabled);
        fastScroller.setVisibility(enabled ? VISIBLE : GONE);
    }

    /**
     * Set whether the fast scroller handle will auto-hide or stay visible.
     * @param autoHide Whether to auto-hide the fast scroller handle or not.
     */
    @SuppressWarnings("unused")
    public final void setAutoHideFastScrollHandle(boolean autoHide) {
        fastScroller.setAutoHideHandle(autoHide);
    }

    /**
     * Set the delay (in milliseconds) before which the fast scroller handle will auto-hide. Default is {@link
     * FastScroller#DEFAULT_HANDLE_HIDE_DELAY}.
     * @param autoHideDelay Auto-hide delay. If < 0, will use the default.
     */
    @SuppressWarnings("unused")
    public final void setHandleAutoHideDelay(int autoHideDelay) {
        fastScroller.setAutoHideDelay(autoHideDelay);
    }

    /**
     * Set whether to use the fast scroller bubble or not.
     * <p/>
     * If set to true, you need to have a class implement {@link BubbleTextProvider#getFastScrollBubbleText(int)} and
     * pass it to this {@link RealmRecyclerView} using {@link #setBubbleTextProvider(BubbleTextProvider)} so that the
     * fast scroller will know what text to put into the bubble.
     * @param useBubble Whether to use the fast scroller bubble or not.
     */
    @SuppressWarnings("unused")
    public final void setUseFastScrollBubble(boolean useBubble) {
        fastScroller.setUseBubble(useBubble);
    }

    /**
     * Set the bubble text provider to use.
     * @param bubbleTextProvider Bubble text provider.
     */
    @SuppressWarnings("unused")
    public final void setBubbleTextProvider(BubbleTextProvider bubbleTextProvider) {
        fastScroller.setBubbleTextProvider(bubbleTextProvider);
    }

    /**
     * Get the actual RecyclerView which backs this {@link RealmRecyclerView}.
     * @return Internal RecyclerView.
     */
    @SuppressWarnings("unused")
    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    /**
     * Get the LinearLayoutManager attached to the RealmRecyclerView.
     * @return LinearLayoutManager.
     */
    @SuppressWarnings("unused")
    public LinearLayoutManager getLayoutManager() {
        return (LinearLayoutManager) recyclerView.getLayoutManager();
    }
}
