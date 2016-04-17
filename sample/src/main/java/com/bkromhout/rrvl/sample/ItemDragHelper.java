package com.bkromhout.rrvl.sample;

import com.google.common.math.LongMath;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import java.util.ArrayList;

/**
 * Contains static methods which help with reordering items as they're dragged around.
 * <p/>
 * Note that the static methods were adapted from those in the RBookList class in one of my apps, "Minerva", which can
 * be found on GitHub <a href="https://github.com/bkromhout/Minerva">here</a>.
 */
public class ItemDragHelper {
    /**
     * Swaps the positions of the items whose unique IDs are {@code item1Id} and {@code item2Id}. Will do nothing if the
     * items are the same.
     * @param item1Id An item's unique ID.
     * @param item2Id Another item's unique ID.
     * @throws IllegalArgumentException if either item is null or items aren't from the same list.
     */
    public static void swapItemPositions(long item1Id, long item2Id) {
        try (Realm realm = Realm.getDefaultInstance()) {
            Item innerItem1 = realm.where(Item.class).equalTo("uniqueId", item1Id).findFirst();
            Item innerItem2 = realm.where(Item.class).equalTo("uniqueId", item2Id).findFirst();

            realm.beginTransaction();
            // Swap the positions.
            Long temp = innerItem1.position;
            innerItem1.position = innerItem2.position;
            innerItem2.position = temp;
            realm.commitTransaction();
        }
    }

    /**
     * Moves the {@link Item} whose unique ID is {@code itemToMoveId} to somewhere before the {@lin Item} whose unique
     * ID is {@code targetItemId}.
     * @param itemToMoveId Unique ID of item to move.
     * @param targetItemId Unique ID of item which item whose unique ID is {@code itemToMoveId} will be moved before.
     */
    public static void moveItemToBefore(long itemToMoveId, long targetItemId) {
        try (Realm realm = Realm.getDefaultInstance()) {
            Item itemToMove = realm.where(Item.class).equalTo("uniqueId", itemToMoveId).findFirst();
            Item targetItem = realm.where(Item.class).equalTo("uniqueId", targetItemId).findFirst();

            if (itemToMove == null || targetItem == null)
                throw new IllegalArgumentException("Neither item may be null.");
            if (itemToMove.uniqueId == targetItem.uniqueId) return;

            // Get the items which come before targetItem.
            RealmResults<Item> beforeTarget = realm.where(Item.class)
                                                   .lessThan("position", targetItem.position)
                                                   .findAllSorted("position", Sort.DESCENDING);

            // Move itemToMove to between beforeTarget.first()/null and targetItem.
            moveItemToBetween(realm, itemToMove, beforeTarget.isEmpty() ? null : beforeTarget.first(), targetItem);
        }
    }

    /**
     * Moves the {@link Item} whose unique ID is {@code itemToMoveId} to somewhere after the {@link Item} whose unique
     * ID is {@code targetItemId}.
     * @param itemToMoveId Unique ID of item to move.
     * @param targetItemId Unique ID of item which item whose unique ID is {@code itemToMoveId} will be moved after.
     */
    public static void moveItemToAfter(long itemToMoveId, long targetItemId) {
        try (Realm realm = Realm.getDefaultInstance()) {
            Item itemToMove = realm.where(Item.class).equalTo("uniqueId", itemToMoveId).findFirst();
            Item targetItem = realm.where(Item.class).equalTo("uniqueId", targetItemId).findFirst();

            if (itemToMove == null || targetItem == null)
                throw new IllegalArgumentException("Neither item may be null.");
            if (itemToMove.uniqueId == targetItem.uniqueId) return;

            // Get the items which come after targetItem.
            RealmResults<Item> afterTarget = realm.where(Item.class)
                                                  .greaterThan("position", targetItem.position)
                                                  .findAllSorted("position");

            // Move itemToMove to between targetItem and afterTarget.first()/null.
            moveItemToBetween(realm, itemToMove, targetItem, afterTarget.isEmpty() ? null : afterTarget.first());
        }
    }

    /**
     * Moves {@code itemToMove} to between {@code item1} and {@code item2} in this list. If {@code item1} and {@code
     * item2} aren't consecutive items, behavior is undefined.
     * <p/>
     * If {@code itemToMove} is the same as either {@code item1} or {@code item2} then this does nothing.<br/>If {@code
     * item1} is {@code null}, then {@code itemToMove} will be put after {@code item1} with the standard position
     * gap.<br/>If {@code item2} is null, then {@code itemToMove} will be put before {@code item2} with the standard
     * position gap.
     * <p/>
     * Please note that passing {@code null} for one of the items assumes that the non-null item is either the first (if
     * it's {@code item2}), or the last (if it's {@code item1}) item in this list. If this isn't the case, you'll likely
     * end up with multiple items in the same position!
     * <p/>
     * If there's no space between {@code item1} and {@code item2}, the whole list will have its items re-spaced before
     * moving the item.
     * <p/>
     * The current spacing gap is {@link Item#GAP}.
     * @param itemToMove The item which is being moved.
     * @param item1      The item which will now precede {@code itemToMove}.
     * @param item2      The item which will now follow {@code itemToMove}.
     */
    public static void moveItemToBetween(Realm realm, final Item itemToMove, Item item1, Item item2) {
        if (itemToMove == null || (item1 == null && item2 == null))
            throw new IllegalArgumentException("itemToMove, or both of item1 and item2 are null.");

        // Check if itemToMove is the same as either item1 or item2.
        if ((item1 != null && itemToMove.equals(item1)) || (item2 != null && itemToMove.equals(item2))) return;

        // Try to find the new position for the item, and make sure we didn't get a null back.
        Long newPos = findMiddlePos(realm, item1, item2);
        if (newPos == null) {
            // If newPos is null, we need to re-sort the items before moving itemToMove.
            resetPositions();
            newPos = findMiddlePos(realm, item1, item2);
            if (newPos == null)
                throw new IllegalArgumentException("Couldn't find space between item1 and item2 after re-spacing");
        }

        // Get Realm, update itemToMove, then close Realm.
        final Long finalNewPos = newPos;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm tRealm) {
                itemToMove.position = finalNewPos;
            }
        });
    }

    /**
     * Find the position number that is between the two given items. If there are no positions between the items, {@code
     * null} is returned. If {@code item1} and {@code item2} aren't consecutive items, this will potentially result in
     * the returned position already being taken.
     * <p/>
     * {@code null} can be passed for ONE of {@code item1} or {@code item2}:<br/>If {@code item1} is null, the number
     * returned will be {@code item1.getPosition() + gap}<br/>If {@code item2} is null, the number returned will be
     * {@code item2.getPosition() - gap}.<br/>(The current spacing gap is {@link Item#GAP}.)
     * <p/>
     * Please note that passing {@code null} for one of the items assumes that the non-null item is either the first (if
     * it's {@code item2}), or the last (if it's {@code item1}) item in this list. If this isn't the case, the returned
     * position might already be taken!
     * @param item1 The earlier item (which the returned position will follow).
     * @param item2 The later item (which the returned position will precede).
     * @return The position number between the two items, or {@code null} if there's no space between the items.
     */
    public static Long findMiddlePos(Realm realm, Item item1, Item item2) {
        // Handle nulls which should throw IllegalArgumentException.
        if (item1 == null && item2 == null) throw new IllegalArgumentException("Both items are null.");

        // Handle acceptable nulls.
        if (item1 == null) return LongMath.checkedSubtract(item2.position, Item.GAP);
        if (item2 == null) return LongMath.checkedAdd(item1.position, Item.GAP);

        // Get positions, make sure that item2 doesn't precede item1 and isn't in the same position as item1.
        Long p1 = item1.position, p2 = item2.position;
        if (p2 <= p1) throw new IllegalArgumentException("item2 was before or at the same position as item1.");

        // Calculate middle.
        Long pos = LongMath.mean(p1, p2);

        // Make sure there isn't an item in the calculated position. If there is, return null.
        return realm.where(Item.class).equalTo("position", pos).findFirst() == null ? pos : null;
    }

    /**
     * Reset the positions of the given list's items so that they are spaced evenly using the standard position gap
     * ({@link Item#GAP}).
     */
    public static void resetPositions() {
        try (Realm realm = Realm.getDefaultInstance()) {
            // Get the list's items in their current position-based order, but put them into an ArrayList instead of
            // using the RealmResults. By doing this we prevent the possibility of bugs which could be caused by items
            // being rearranged in the RealmResults as we update their positions.
            final ArrayList<Item> orderedItems = new ArrayList<>(realm.where(Item.class).findAllSorted("position"));
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm tRealm) {
                    // Start nextPos back at 0.
                    Item.nextPos = 0L;
                    // Loop through the items and set their new position values, then increment nextPos.
                    for (Item item : orderedItems) {
                        item.position = Item.nextPos;
                        Item.nextPos += Item.GAP;
                    }
                }
            });
        }
    }
}
