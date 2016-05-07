package com.bkromhout.rrvl;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Implementation of a fast scroller for our RecyclerView.
 */
class FastScroller extends LinearLayout {
    private static final int BUBBLE_ANIMATION_DURATION = 100;
    private static final int HANDLE_ANIMATION_DURATION = 100;
    static final int DEFAULT_HANDLE_HIDE_DELAY = 2000;

    private View handle;
    private TextView bubble;
    private RecyclerView recyclerView;

    private ObjectAnimator currentBubbleShowAnimator = null;
    private ObjectAnimator currentBubbleHideAnimator = null;
    private ObjectAnimator currentHandleShowAnimator = null;
    private ObjectAnimator currentHandleHideAnimator = null;
    private int height;

    private boolean autoHideHandle = false;
    private int autoHideDelay = DEFAULT_HANDLE_HIDE_DELAY;
    private boolean useBubble = false;
    private BubbleTextProvider bubbleTextProvider = null;
    private FastScrollHandleStateListener handleStateListener = null;
    private boolean eatVisibilityUpdates = false;

    /**
     * RecyclerView.OnScrollListener to make sure that we update the visibility of our views when scrolling the recycler
     * view normally.
     */
    private final RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (handle.isSelected()) return;

            float proportion = computeScrollProportionRelativeToMax(recyclerView.computeVerticalScrollOffset(),
                    recyclerView.computeVerticalScrollRange(), recyclerView.computeVerticalScrollExtent());

            setBubbleAndHandlePosition(height * proportion, proportion);
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (!autoHideHandle) return;
            if (newState == RecyclerView.SCROLL_STATE_IDLE) hideHandle();
            else showHandle();
        }
    };

    public FastScroller(Context context) {
        this(context, null, 0);
    }

    public FastScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setClipChildren(false);
        LayoutInflater.from(context).inflate(R.layout.fast_scroller, this, true);
        bubble = (TextView) findViewById(R.id.fast_scroller_bubble);
        handle = findViewById(R.id.fast_scroller_handle);
    }

    void setRecyclerView(final RecyclerView recyclerView) {
        if (this.recyclerView != recyclerView) {
            if (this.recyclerView != null) this.recyclerView.removeOnScrollListener(onScrollListener);

            this.recyclerView = recyclerView;
            if (this.recyclerView == null) return;

            recyclerView.addOnScrollListener(onScrollListener);
        }

        if (recyclerView != null)
            recyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (handle.isSelected()) return true;

                    float proportion = computeScrollProportionRelativeToMax(recyclerView.computeVerticalScrollOffset(),
                            recyclerView.computeVerticalScrollRange(), recyclerView.computeVerticalScrollExtent());

                    setBubbleAndHandlePosition(height * proportion, proportion);
                    return true;
                }
            });
    }

    boolean getAutoHideHandle() {
        return autoHideHandle;
    }

    /**
     * Whether or not to automatically hide the handle if it hasn't been touched of the recycler view hasn't been
     * scrolled for a certain amount of time. False by default.
     * @param autoHideHandle Whether to automatically hide the handle (true), or to keep it visible (false).
     */
    void setAutoHideHandle(boolean autoHideHandle) {
        if (this.autoHideHandle && !autoHideHandle) {
            // Changing auto-hide from on to off.
            showHandle();
        } else if (!this.autoHideHandle && autoHideHandle) {
            // Changing auto-hide from off to on.
            hideHandle();
        }
        this.autoHideHandle = autoHideHandle;
    }

    int getAutoHideDelay() {
        return autoHideDelay;
    }

    /**
     * Set the delay (in ms) before the handle auto-hides. {@link #DEFAULT_HANDLE_HIDE_DELAY} is the default.
     * @param autoHideDelay Time in milliseconds to delay before auto-hiding the handle. If < 0, the default will be
     *                      used.
     */
    void setAutoHideDelay(int autoHideDelay) {
        if (autoHideDelay < 0) this.autoHideDelay = DEFAULT_HANDLE_HIDE_DELAY;
        else this.autoHideDelay = autoHideDelay;
    }

    boolean getUseBubble() {
        return useBubble;
    }

    /**
     * Whether or not to use the bubble. False by default.
     * @param useBubble Whether or not to use the bubble.
     */
    void setUseBubble(boolean useBubble) {
        if (this.useBubble && !useBubble) hideBubble();
        this.useBubble = useBubble;
    }

    /**
     * Set the {@link BubbleTextProvider} to use.
     * @param bubbleTextProvider Bubble text provider.
     */
    void setBubbleTextProvider(BubbleTextProvider bubbleTextProvider) {
        this.bubbleTextProvider = bubbleTextProvider;
    }

    /**
     * Set the {@link FastScrollHandleStateListener} to use.
     * @param handleStateListener Handle state listener.
     */
    void setHandleStateListener(FastScrollHandleStateListener handleStateListener) {
        this.handleStateListener = handleStateListener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        height = h;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (recyclerView != null) {
            recyclerView.removeOnScrollListener(onScrollListener);
            recyclerView = null;
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // If the handle isn't visible, or it is but the touch event isn't on the handle, ignore this.
                if (handle.getVisibility() != VISIBLE ||
                        event.getX() < handle.getX() - ViewCompat.getPaddingStart(handle)) return false;
                // If we're using the bubble, show it now.
                if (useBubble && bubble.getVisibility() == INVISIBLE) showBubble();
                // Select the handle.
                handle.setSelected(true);
                notifyHandleListener(FastScrollerHandleState.PRESSED);
            case MotionEvent.ACTION_MOVE:
                // If the handle isn't visible, ignore this.
                if (handle.getVisibility() != VISIBLE) return false;
                // If we have auto-hide turned on, make sure the handle is shown.
                if (autoHideHandle) showHandle();
                // Set the positions of the bubble (unless we aren't using it), the handle, and the recyclerview.
                float y = event.getY();
                setBubbleAndHandlePosition(y);
                setRecyclerViewPosition(y);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Un-select the handle.
                handle.setSelected(false);
                notifyHandleListener(FastScrollerHandleState.RELEASED);
                // Hide the bubble (if we're using it).
                hideBubble();
                // If we have auto-hide turned on, make sure we hide the handle (after a delay).
                if (autoHideHandle) hideHandle();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void setRecyclerViewPosition(float y) {
        if (recyclerView != null) {
            int itemCount = recyclerView.getAdapter().getItemCount();
            float rawTargetItemProportion;

            if (handle.getY() == 0) rawTargetItemProportion = 0f;
            else if (handle.getY() + handle.getHeight() > height) rawTargetItemProportion = 1f;
            else rawTargetItemProportion = y / ((float) height - (float) handle.getHeight());

            float targetItemProportion = rawTargetItemProportion * computeMaxScrollProportion(
                    recyclerView.computeVerticalScrollRange(), recyclerView.computeVerticalScrollExtent());

            int targetPos = (int) getValueInRange(0, itemCount - 1, targetItemProportion * (float) itemCount);
            ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(targetPos, 0);

            if (useBubble) {
                if (bubbleTextProvider == null)
                    throw new IllegalStateException("You haven't set a BubbleTextProvider.");
                bubble.setText(bubbleTextProvider.getFastScrollBubbleText(targetPos));
            }
        }
    }

    private void setBubbleAndHandlePosition(float y) {
        setBubbleAndHandlePosition(y, 0.5f);
    }

    private void setBubbleAndHandlePosition(float y, float proportion) {
        int handleHeight = handle.getHeight();

        handle.setY(getValueInRange(0, height - handleHeight, y - handleHeight * proportion));
        if (useBubble) {
            int bubbleHeight = bubble.getHeight();
            bubble.setY(getValueInRange(0, height - bubbleHeight - handleHeight / 2, y - bubbleHeight));
        }
    }

    /**
     * Returns {@code value} so long as it is within the range of {@code min} to {@code max}.
     * @param min   Minimum.
     * @param max   Maximum.
     * @param value Value to return if in range.
     * @return {@code value} if {@code min <= value <= max}, {@code min} if {@code value < min}, or {@code max} if
     * {@code value > max}.
     */
    private float getValueInRange(float min, float max, float value) {
        float minimum = Math.max(min, value);
        return Math.min(minimum, max);
    }

    /**
     * Computes the maximum scroll offset necessary to show the last item in the recycler view by doing {@code
     * scrollRange - scrollExtent}.
     * @param scrollRange  Scroll range.
     * @param scrollExtent Scroll extent.
     * @return Maximum scroll offset.
     */
    private int computeMaxScrollOffset(int scrollRange, int scrollExtent) {
        return scrollRange - scrollExtent;
    }

    /**
     * Computes the maximum scroll proportion by dividing {@link #computeMaxScrollOffset(int, int)} by {@code
     * scrollRange}.
     * @param scrollRange  Scroll range.
     * @param scrollExtent Scroll extent.
     * @return Maximum scroll proportion.
     */
    private float computeMaxScrollProportion(int scrollRange, int scrollExtent) {
        return (float) computeMaxScrollOffset(scrollRange, scrollExtent) / (float) scrollRange;
    }

    /**
     * Computes the scroll proportion relative to the maximum scroll offset by dividing {@code scrollOffset} by {@link
     * #computeMaxScrollOffset(int, int)}.
     * @param scrollOffset Scroll offset.
     * @param scrollRange  Scroll range.
     * @param scrollExtent Scroll extent.
     * @return Scroll proportion, relative to the maximum scroll offset.
     */
    private float computeScrollProportionRelativeToMax(int scrollOffset, int scrollRange, int scrollExtent) {
        return (float) scrollOffset / (float) computeMaxScrollOffset(scrollRange, scrollExtent);
    }

    /**
     * Show the fast scroller handle. First we check to see if {@link #currentHandleHideAnimator} is running, and if it
     * is we reverse that and cancel it. Then we check to see if {@link #autoHideHandle} is false, and if it is then we
     * just make sure that the handle is visible. Lastly, we check to see if the handle is visible, and if it still
     * isn't then we create and run {@link #currentHandleShowAnimator} (unless it's already running).
     */
    private void showHandle() {
        // If we're currently animating a hide, reverse and cancel that, then set the handle to visible.
        if (currentHandleHideAnimator != null) {
            // Don't allow the listener to be notified when the reversed hide animation ends.
            eatVisibilityUpdates = true;
            currentHandleHideAnimator.reverse();
            handle.setVisibility(VISIBLE);
            eatVisibilityUpdates = false;
        }

        // At this point, if autoHideHandle is false, just make sure that the handle is visible and we're done.
        if (!autoHideHandle) {
            if (handle.getVisibility() != VISIBLE) {
                handle.setVisibility(VISIBLE);
                notifyHandleListener(FastScrollerHandleState.VISIBLE);
            }
            return;
        }

        // Otherwise, if the handle isn't visible currently, animate it showing.
        if (handle.getVisibility() != VISIBLE && currentHandleShowAnimator == null) {
            handle.setVisibility(VISIBLE);
            currentHandleShowAnimator = ObjectAnimator.ofFloat(handle, "alpha", 0f, 1f)
                                                      .setDuration(HANDLE_ANIMATION_DURATION);
            currentHandleShowAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    notifyHandleListener(FastScrollerHandleState.VISIBLE);
                    currentHandleShowAnimator = null;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    notifyHandleListener(FastScrollerHandleState.VISIBLE);
                    currentHandleShowAnimator = null;
                }
            });
            currentHandleShowAnimator.start();
        }
    }

    /**
     * Hide the fast scroller handle. First we check to see if {@link #currentHandleShowAnimator} is running, and if
     * both it is running and {@link #autoHideHandle} is false, then we reverse and cancel the show animation. Then we
     * check to see if {@link #autoHideHandle} is false, and if it is then we just make sure that the handle isn't
     * visible. Lastly, we check to see if the handle is visible, and if it is then we create and run {@link
     * #currentHandleHideAnimator} (unless it's already running), which will animate its hiding after a delay.
     */
    private void hideHandle() {
        // If we're currently animating a show and auto-hide isn't turned on, reverse and cancel that animation, then
        // set the handle to be not visible.
        if (currentHandleShowAnimator != null && !autoHideHandle) {
            // Don't allow the listener to be notified when the reversed show animation ends.
            eatVisibilityUpdates = true;
            currentHandleShowAnimator.reverse();
            eatVisibilityUpdates = false;
            handle.setVisibility(INVISIBLE);
        }

        // At this point, if autoHideHandle is false, just make sure that the handle isn't visible and we're done.
        if (!autoHideHandle) {
            if (handle.getVisibility() == VISIBLE) {
                handle.setVisibility(INVISIBLE);
                notifyHandleListener(FastScrollerHandleState.HIDDEN);
            }
            return;
        }

        // Otherwise, if the handle is visible currently, animate it hiding after a delay.
        if (handle.getVisibility() == VISIBLE && currentHandleHideAnimator == null) {
            currentHandleHideAnimator = ObjectAnimator.ofFloat(handle, "alpha", 1f, 0f)
                                                      .setDuration(HANDLE_ANIMATION_DURATION);
            // Make sure the delay also includes the remaining show animation time if it's running.
            currentHandleHideAnimator.setStartDelay(autoHideDelay + (currentHandleShowAnimator == null ? 0
                    : (HANDLE_ANIMATION_DURATION - currentHandleShowAnimator.getCurrentPlayTime())));
            currentHandleHideAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    handle.setVisibility(INVISIBLE);
                    notifyHandleListener(FastScrollerHandleState.HIDDEN);
                    currentHandleHideAnimator = null;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    handle.setVisibility(INVISIBLE);
                    notifyHandleListener(FastScrollerHandleState.HIDDEN);
                    currentHandleHideAnimator = null;
                }
            });
            currentHandleHideAnimator.start();
        }
    }

    /**
     * Show the fast scroller bubble, unless {@link #useBubble} is false. If {@link #currentBubbleHideAnimator} is
     * running, just reverse that animation and ensure the bubble is shown. Otherwise, as long as the bubble isn't shown
     * already, run the show animation.
     */
    private void showBubble() {
        // If we're not using the bubble, ignore this.
        if (!useBubble) return;

        // If we're currently animating a hide, just reverse that and ensure the bubble is visible.
        if (currentBubbleHideAnimator != null) {
            currentHandleHideAnimator.reverse();
            bubble.setVisibility(VISIBLE);
        }

        // If the bubble ins't visible at this point, animate a show.
        if (bubble.getVisibility() != VISIBLE && currentBubbleShowAnimator == null) {
            bubble.setVisibility(VISIBLE);
            currentBubbleShowAnimator = ObjectAnimator.ofFloat(bubble, "alpha", 0f, 1f)
                                                      .setDuration(BUBBLE_ANIMATION_DURATION);
            currentBubbleShowAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    currentBubbleShowAnimator = null;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    currentBubbleShowAnimator = null;
                }
            });
            currentBubbleShowAnimator.start();
        }
    }

    /**
     * Hide the fast scroller bubble. First check to see if {@link #currentBubbleShowAnimator} is running, and if so
     * then just reverse that animation and hide the bubble. Next check to see if {@link #useBubble} is false, and if so
     * then just hide the bubble. Lastly, if the bubble is still visible, just run the hide animation.
     */
    private void hideBubble() {
        // If we're currently animating a show, just reverse the animation and set the bubble to be not visible.
        if (currentBubbleShowAnimator != null) {
            currentBubbleShowAnimator.reverse();
            bubble.setVisibility(INVISIBLE);
        }

        // At this point, if we aren't using the bubble, just make sure it isn't visible and we're done.
        if (!useBubble) {
            bubble.setVisibility(INVISIBLE);
            return;
        }

        // Otherwise, if the bubble is visible currently, animate it hiding.
        if (bubble.getVisibility() == VISIBLE && currentBubbleHideAnimator == null) {
            currentBubbleHideAnimator = ObjectAnimator.ofFloat(bubble, "alpha", 1f, 0f)
                                                      .setDuration(BUBBLE_ANIMATION_DURATION);
            currentBubbleHideAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    bubble.setVisibility(INVISIBLE);
                    currentBubbleHideAnimator = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    bubble.setVisibility(INVISIBLE);
                    currentBubbleHideAnimator = null;
                }
            });
            currentBubbleHideAnimator.start();
        }
    }

    /**
     * Convenience method to notify the handle listener if certain conditions are met.
     * @param state State to notify the handle listener of.
     */
    private void notifyHandleListener(FastScrollerHandleState state) {
        // The event will be sent if we have a listener, and if it's either not a visibility event or we aren't eating
        // the visibility events (and thus we don't care what it is).
        if (handleStateListener != null && (!eatVisibilityUpdates || state == FastScrollerHandleState.PRESSED ||
                state == FastScrollerHandleState.RELEASED)) handleStateListener.onHandleStateChanged(state);
    }
}