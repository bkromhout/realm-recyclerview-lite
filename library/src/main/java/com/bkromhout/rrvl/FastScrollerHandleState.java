package com.bkromhout.rrvl;

/**
 * Represents the states that the fast scroller's handle moves through.
 * @see FastScrollHandleStateListener
 */
public enum FastScrollerHandleState {
    /**
     * Indicates that the handle is now hidden.
     * <p/>
     * A handle cannot be {@link #PRESSED} until it is first {@link #VISIBLE}.
     */
    HIDDEN,
    /**
     * Indicates that the handle is now visible.
     */
    VISIBLE,
    /**
     * Indicates that the handle has just been pressed. It should be assumed that the handle is still pressed until it
     * is {@link #RELEASED}.
     * <p/>
     * A pressed handle cannot be {@link #HIDDEN} until it has first been {@link #RELEASED}.
     */
    PRESSED,
    /**
     * Indicates that the handle has just exited the {@link #PRESSED} state.
     * <p/>
     * The handle should still be considered {@link #VISIBLE} (though the listener, if present, will not be explicitly
     * notified as such) until it has moved to the {@link #HIDDEN} state.
     */
    RELEASED
}
