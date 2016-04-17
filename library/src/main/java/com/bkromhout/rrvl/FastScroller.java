package com.bkromhout.rrvl;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
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
public class FastScroller extends LinearLayout {
    private static final int BUBBLE_ANIMATION_DURATION = 100;
    private static final int HANDLE_ANIMATION_DURATION = 100;
    private static final int HANDLE_HIDE_DELAY = 2000;
    private static final int TRACK_SNAP_RANGE = 5;

    private TextView bubble;
    private boolean useBubble = false;
    private View handle;
    private boolean autoHideHandle = false;
    private RecyclerView recyclerView;
    private int height;
    private boolean isInitialized = false;
    private ObjectAnimator currentBubbleAnimator = null;
    private ObjectAnimator currentHandleShowAnimator = null;
    private ObjectAnimator currentHandleHideAnimator = null;

    private final RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(final RecyclerView recyclerView, final int dx, final int dy) {
            if ((useBubble && bubble == null) || handle.isSelected()) return;

            final int verticalScrollOffset = recyclerView.computeVerticalScrollOffset();
            final int verticalScrollRange = recyclerView.computeVerticalScrollRange();
            float proportion = (float) verticalScrollOffset / ((float) verticalScrollRange - height);

            setBubbleAndHandlePosition(height * proportion);
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (!autoHideHandle) return;
            if (newState == RecyclerView.SCROLL_STATE_IDLE) hideHandle();
            else showHandle();
        }
    };

    public interface BubbleTextGetter {
        String getFastScrollBubbleText(int position);
    }

    public FastScroller(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public FastScroller(final Context context) {
        super(context);
        init(context);
    }

    public FastScroller(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    protected void init(Context context) {
        if (isInitialized) return;
        isInitialized = true;
        setOrientation(HORIZONTAL);
        setClipChildren(false);
    }

    /**
     * Layout customization.
     * @param layoutResId Main layout of Fast Scroller.
     * @param bubbleResId Drawable resource for Bubble containing the Text. Pass -1 to not use the bubble.
     * @param handleResId Drawable resource for the Handle.
     */
    public void setViewsToUse(@LayoutRes int layoutResId, @IdRes int bubbleResId, @IdRes int handleResId) {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(layoutResId, this, true);

        useBubble = bubbleResId != -1;
        if (useBubble) bubble = (TextView) findViewById(bubbleResId);
        if (bubble != null) bubble.setVisibility(INVISIBLE);

        handle = findViewById(handleResId);
    }

    /**
     * Whether or not to automatically hide the handle if it hasn't been touched of the recycler view hasn't been
     * scrolled for a certain amount of time. False by default.
     * @param autoHideHandle Whether to automatically hide the handle (true), or to keep it visible (false).
     */
    public void setAutoHideHandle(boolean autoHideHandle) {
        if (this.autoHideHandle && !autoHideHandle) {
            // Changing auto-hide from on to off.
            showHandle();
        } else if (!this.autoHideHandle && autoHideHandle) {
            // Changing auto-hide from off to on.
            hideHandle();
        }
        this.autoHideHandle = autoHideHandle;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        height = h;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // If the handle isn't visible, or it is but the touch event isn't on the handle, ignore this.
                if (handle.getVisibility() != VISIBLE ||
                        event.getX() < handle.getX() - ViewCompat.getPaddingStart(handle)) return false;
                // Cancel any existing bubble animation.
                if (currentBubbleAnimator != null) currentBubbleAnimator.cancel();
                // If we're using the bubble, show it now.
                if (useBubble && bubble != null && bubble.getVisibility() == INVISIBLE) showBubble();
                // Select the handle.
                handle.setSelected(true);
            case MotionEvent.ACTION_MOVE:
                // If the handle isn't visible, ignore this.
                if (handle.getVisibility() != VISIBLE) return false;
                // If we have auto-hide turned on, make sure the handle is shown.
                if (autoHideHandle) showHandle();
                // Set the positions of the bubble (unless we aren't using it), the handle, and the recyclerview.
                final float y = event.getY();
                setBubbleAndHandlePosition(y);
                setRecyclerViewPosition(y);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Un-select the handle.
                handle.setSelected(false);
                // Hide the bubble (if we're using it).
                hideBubble();
                // If we have auto-hide turned on, make sure we hide the handle (after a delay).
                if (autoHideHandle) hideHandle();
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void setRecyclerView(final RecyclerView recyclerView) {
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
                    if ((useBubble && bubble == null) || handle.isSelected()) return true;

                    final int verticalScrollOffset = recyclerView.computeVerticalScrollOffset();
                    final int verticalScrollRange = recyclerView.computeVerticalScrollRange();
                    float proportion = (float) verticalScrollOffset / ((float) verticalScrollRange - height);

                    setBubbleAndHandlePosition(height * proportion);
                    return true;
                }
            });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (recyclerView != null) {
            recyclerView.removeOnScrollListener(onScrollListener);
            recyclerView = null;
        }
    }

    private void setRecyclerViewPosition(float y) {
        if (recyclerView != null) {
            final int itemCount = recyclerView.getAdapter().getItemCount();
            float proportion;

            if (handle.getY() == 0) proportion = 0f;
            else if (handle.getY() + handle.getHeight() >= height - TRACK_SNAP_RANGE) proportion = 1f;
            else proportion = y / (float) height;

            final int targetPos = getValueInRange(0, itemCount - 1, (int) (proportion * (float) itemCount));
            ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(targetPos, 0);

            if (useBubble && bubble != null) {
                String bubbleText = ((BubbleTextGetter) recyclerView.getAdapter()).getFastScrollBubbleText(targetPos);
                bubble.setText(bubbleText);
            }
        }
    }

    private int getValueInRange(int min, int max, int value) {
        int minimum = Math.max(min, value);
        return Math.min(minimum, max);
    }

    private void setBubbleAndHandlePosition(float y) {
        final int handleHeight = handle.getHeight();

        handle.setY(getValueInRange(0, height - handleHeight, (int) (y - handleHeight / 2)));
        if (useBubble && bubble != null) {
            int bubbleHeight = bubble.getHeight();
            bubble.setY(getValueInRange(0, height - bubbleHeight - handleHeight / 2, (int) (y - bubbleHeight)));
        }
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
            currentHandleHideAnimator.reverse();
            handle.setVisibility(VISIBLE);
        }
        // At this point, if autoHideHandle is false, just make sure that the handle is visible and we're done.
        if (!autoHideHandle) {
            handle.setVisibility(VISIBLE);
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
                    currentHandleShowAnimator = null;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
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
            currentHandleShowAnimator.reverse();
            handle.setVisibility(INVISIBLE);
        }
        // At this point, if autoHideHandle is false, just make sure that the handle isn't visible and we're done.
        if (!autoHideHandle) {
            handle.setVisibility(INVISIBLE);
            return;
        }
        // Otherwise, if the handle is visible currently, animate it hiding after a delay.
        if (handle.getVisibility() == VISIBLE && currentHandleHideAnimator == null) {
            currentHandleHideAnimator = ObjectAnimator.ofFloat(handle, "alpha", 1f, 0f)
                                                      .setDuration(HANDLE_ANIMATION_DURATION);
            // Make sure the delay also includes the remaining show animation time if it's running.
            currentHandleHideAnimator.setStartDelay(HANDLE_HIDE_DELAY + (currentHandleShowAnimator == null ? 0
                    : (HANDLE_ANIMATION_DURATION - currentHandleShowAnimator.getCurrentPlayTime())));
            currentHandleHideAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    handle.setVisibility(INVISIBLE);
                    currentHandleHideAnimator = null;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    handle.setVisibility(INVISIBLE);
                    currentHandleHideAnimator = null;
                }
            });
            currentHandleHideAnimator.start();
        }
    }

    private void showBubble() {
        if (!useBubble || bubble == null) return;
        bubble.setVisibility(VISIBLE);

        if (currentBubbleAnimator != null) currentBubbleAnimator.cancel();
        currentBubbleAnimator = ObjectAnimator.ofFloat(bubble, "alpha", 0f, 1f)
                                              .setDuration(BUBBLE_ANIMATION_DURATION);
        currentBubbleAnimator.start();
    }

    private void hideBubble() {
        if (useBubble && bubble == null) return;

        if (currentBubbleAnimator != null) currentBubbleAnimator.cancel();
        currentBubbleAnimator = ObjectAnimator.ofFloat(bubble, "alpha", 1f, 0f)
                                              .setDuration(BUBBLE_ANIMATION_DURATION);
        currentBubbleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                bubble.setVisibility(INVISIBLE);
                currentBubbleAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                bubble.setVisibility(INVISIBLE);
                currentBubbleAnimator = null;
            }
        });
        currentBubbleAnimator.start();
    }
}