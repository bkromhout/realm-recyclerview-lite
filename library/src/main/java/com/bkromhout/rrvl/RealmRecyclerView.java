package com.bkromhout.rrvl;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;

/**
 * A RecyclerView that supports Realm.
 */
public class RealmRecyclerView extends FrameLayout {
    // Views.
    private RecyclerView recyclerView;
    private FastScroller fastScroller;
    private ViewStub emptyContentContainer;

    // Attributes.
    private int emptyViewId;
    private boolean swipe;
    private boolean dragAndDrop;
    private boolean fastScrollEnabled;

    private RealmRecyclerViewAdapter adapter;
    private ItemTouchHelper touchHelper;
    private RealmSimpleItemTouchHelperCallback touchHelperCallback;

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

        // Get views.
        recyclerView = (RecyclerView) findViewById(R.id.rrv_recycler_view);
        fastScroller = (FastScroller) findViewById(R.id.rrv_fast_scroller);
        emptyContentContainer = (ViewStub) findViewById(R.id.rrv_empty_content_container);

        // Read attributes and set things up.
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RealmRecyclerView);
        emptyViewId = ta.getResourceId(R.styleable.RealmRecyclerView_emptyLayoutId, 0);
        // Touch helper.
        initTouchHelper(ta);
        // Fast scroll.
        initFastScroller(ta);
        // RecyclerView padding.
        initRVPadding(ta);
        ta.recycle();

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
    }

    private void initTouchHelper(TypedArray ta) {
        swipe = ta.getBoolean(R.styleable.RealmRecyclerView_swipe, false);
        dragAndDrop = ta.getBoolean(R.styleable.RealmRecyclerView_dragAndDrop, false);
        touchHelperCallback = new RealmSimpleItemTouchHelperCallback(swipe, dragAndDrop,
                ta.getBoolean(R.styleable.RealmRecyclerView_longClickTriggersDrag, false));
        touchHelper = new ItemTouchHelper(touchHelperCallback);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    private void initFastScroller(TypedArray ta) {
        setFastScroll(ta.getBoolean(R.styleable.RealmRecyclerView_fastScroll, false));
        fastScroller.setAutoHideHandle(ta.getBoolean(R.styleable.RealmRecyclerView_autoHideFastScrollHandle, false));
        fastScroller.setAutoHideDelay(ta.getInt(R.styleable.RealmRecyclerView_handleAutoHideDelay,
                FastScroller.DEFAULT_HANDLE_HIDE_DELAY));
        fastScroller.setUseBubble(ta.getBoolean(R.styleable.RealmRecyclerView_useFastScrollBubble, false));
        fastScroller.setRecyclerView(recyclerView);
    }

    private void initRVPadding(TypedArray ta) {
        int padding = ta.getDimensionPixelSize(R.styleable.RealmRecyclerView_rvPadding, 0);
        int paddingStart = ta.getDimensionPixelSize(R.styleable.RealmRecyclerView_rvPaddingStart, -1);
        int paddingTop = ta.getDimensionPixelSize(R.styleable.RealmRecyclerView_rvPaddingTop, -1);
        int paddingEnd = ta.getDimensionPixelSize(R.styleable.RealmRecyclerView_rvPaddingEnd, -1);
        int paddingBottom = ta.getDimensionPixelSize(R.styleable.RealmRecyclerView_rvPaddingBottom, -1);
        // Specific padding values are more important than the overall padding value.
        setRVPadding(paddingStart > -1 ? paddingStart : padding,
                paddingTop > -1 ? paddingTop : padding,
                paddingEnd > -1 ? paddingEnd : padding,
                paddingBottom > -1 ? paddingBottom : padding);
    }

    private void updateEmptyContentContainerVisibility(RecyclerView.Adapter adapter) {
        if (emptyViewId == 0) return;
        emptyContentContainer.setVisibility(adapter != null && adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    final void startDragging(RecyclerView.ViewHolder viewHolder) {
        if (dragAndDrop && touchHelper != null) touchHelper.startDrag(viewHolder);
    }

    /**
     * Set the adapter for this RealmRecyclerView.
     * @param adapter {@link RealmRecyclerViewAdapter}.
     */
    public final void setAdapter(final RealmRecyclerViewAdapter adapter) {
        this.adapter = adapter;
        recyclerView.setAdapter(adapter);

        touchHelperCallback.setListener(adapter);

        if (adapter != null) {
            adapter.setRealmRecyclerView(this);
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
        }
        updateEmptyContentContainerVisibility(adapter);
    }

    /**
     * Get whether swipe is enabled.
     * @return Whether swipe is enabled or not.
     */
    public final boolean getSwipe() {
        return swipe;
    }

    /**
     * Enable/Disable swipe.
     * @param enabled Whether to allow swipe.
     */
    public final void setSwipe(boolean enabled) {
        this.swipe = enabled;
        touchHelperCallback.setSwipe(enabled);
    }

    /**
     * Get whether drag and drop is enabled.
     * @return Whether drag and drop is enabled or not.
     */
    public final boolean getDragAndDrop() {
        return dragAndDrop;
    }

    /**
     * Enable/Disable drag and drop.
     * @param enabled Whether to allow drag and drop.
     */
    public final void setDragAndDrop(boolean enabled) {
        this.dragAndDrop = enabled;
        touchHelperCallback.setDragAndDrop(enabled);
    }

    /**
     * Get whether long click triggers drags.
     * @return Whether long click triggers drags or not.
     */
    public final boolean getLongClickTriggersDrag() {
        return touchHelperCallback.getLongClickTriggersDrag();
    }

    /**
     * Whether to use long click to trigger the drag or not.
     * @param longClickTriggersDrag Whether to allow long clicks to start drags.
     */
    public final void setLongClickTriggersDrag(boolean longClickTriggersDrag) {
        touchHelperCallback.setLongClickTriggersDrag(longClickTriggersDrag);
    }

    /**
     * Get whether fast scrolling is enabled.
     * @return Whether fast scrolling is enabled or not.
     */
    public final boolean getFastScroll() {
        return fastScrollEnabled;
    }

    /**
     * Enable/Disable the fast scroller. The system-drawn scrollbars will be enabled if the fast scroller isn't (and
     * vice versa).
     * @param enabled Whether to enable the fast scroller or not.
     */
    @SuppressWarnings("unused")
    public final void setFastScroll(boolean enabled) {
        this.fastScrollEnabled = enabled;
        recyclerView.setVerticalScrollBarEnabled(!enabled);
        fastScroller.setVisibility(enabled ? VISIBLE : GONE);
    }

    /**
     * Get whether the fast scroller's handle is set to auto-hide.
     * @return Whether the fast scroller's handle is set to auto-hide or not.
     */
    public final boolean getAutoHideFastScrollHandle() {
        return fastScroller.getAutoHideHandle();
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
     * Get the delay (in milliseconds) before which the fast scroller handle with auto-hide.
     * @return Auto-hide delay.
     */
    public final int getHandleAutoHideDelay() {
        return fastScroller.getAutoHideDelay();
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
     * Get whether the fast scroller's bubble is being used.
     * @return Whether the fast scroller's bubble is being used or not.
     */
    public final boolean getUseFastScrollBubble() {
        return fastScroller.getUseBubble();
    }

    /**
     * Set whether to use the fast scroller bubble or not.
     * <p>
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
     * Set the fast scroll handle state listener to use.
     * @param handleStateListener Fast scroll handle state listener.
     */
    @SuppressWarnings("unused")
    public final void setFastScrollHandleStateListener(FastScrollHandleStateListener handleStateListener) {
        fastScroller.setHandleStateListener(handleStateListener);
    }

    /**
     * Set the padding on the actual {@code RecyclerView} which backs this {@link RealmRecyclerView}.
     * @param padding Padding in pixels.
     */
    @SuppressWarnings("unused")
    public final void setRVPadding(int padding) {
        setRVPadding(padding, padding, padding, padding);
    }

    /**
     * Set the padding on the actual {@code RecyclerView} which backs this {@link RealmRecyclerView}.
     * <p>
     * If running on a device whose API level is < 17, {@code start} and {@code end} are used as the values for left and
     * right, respectively.
     * @param start  The start (or left) padding in pixels.
     * @param top    The top padding in pixels.
     * @param end    The end (or right) padding in pixels.
     * @param bottom The bottom padding in pixels.
     */
    public final void setRVPadding(int start, int top, int end, int bottom) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            recyclerView.setPaddingRelative(start, top, end, bottom);
        else recyclerView.setPadding(start, top, end, bottom);
    }

    /**
     * Get the actual {@code RecyclerView} which backs this {@link RealmRecyclerView}.
     * @return Internal {@code RecyclerView}.
     */
    @SuppressWarnings("unused")
    public final RecyclerView getRecyclerView() {
        return recyclerView;
    }

    /**
     * Get the {@code LinearLayoutManager} attached to the {@link RealmRecyclerView}.
     * @return {@code LinearLayoutManager}.
     */
    @SuppressWarnings("unused")
    public final LinearLayoutManager getLayoutManager() {
        return (LinearLayoutManager) recyclerView.getLayoutManager();
    }
}
