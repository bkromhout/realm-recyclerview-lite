package com.bkromhout.rrvl;

/**
 * Implementers will be notified when the fast scroller's handle changes state.
 * @see FastScrollerHandleState
 */
public interface FastScrollHandleStateListener {
    /**
     * The fast scroller's handle has changed states.
     * @param newState The state which the handle has just entered.
     */
    void onHandleStateChanged(FastScrollerHandleState newState);
}
