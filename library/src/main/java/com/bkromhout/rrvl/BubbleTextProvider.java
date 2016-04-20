package com.bkromhout.rrvl;

/**
 * Implementing classes will be called upon to provide text to put in the bubble of the fast scroller. Good candidates
 * for such classes are the concrete adapter classes or any other class which has access to a copy of the same {@code
 * RealmResults} that the adapter is using.
 */
public interface BubbleTextProvider {
    /**
     * Get the text which should be shown in the fast scroller's bubble for the item at {@code position}.
     * <p/>
     * This method returns null by default. It only needs to be implemented if {@link RealmRecyclerView} had the XML
     * attribute {@code rrvlFastScrollMode} set to {@code "Handle_Bubble"}.
     * @param position Position of the item to return text for.
     * @return Text to show in the fast scroller's bubble.
     */
    String getFastScrollBubbleText(int position);
}
