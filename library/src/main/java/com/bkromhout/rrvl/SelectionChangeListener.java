package com.bkromhout.rrvl;

/**
 * Implementers will be notified when items are selected or unselected.
 */
public interface SelectionChangeListener {
    /**
     * Called when the item selection set changes so that implementers may respond.
     */
    void itemSelectionChanged();
}
