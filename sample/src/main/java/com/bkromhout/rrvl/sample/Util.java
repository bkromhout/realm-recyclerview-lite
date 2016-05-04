package com.bkromhout.rrvl.sample;

import io.realm.Realm;

/**
 * Simple utility class.
 */
public class Util {
    public static void removeAllItems(Realm realm) {
        realm.delete(Item.class);
    }

    public static void addXItems(Realm realm, int numToAdd) {
        if (numToAdd < 1) return;
        for (int i = 0; i < numToAdd; i++) {
            // Generate a name.
            String name = String.valueOf(i / 10) + " Item " + String.valueOf(i);
            // Make sure this name isn't already taken (for purely vain purposes).
            if (realm.where(Item.class).equalTo("name", name).findFirst() != null) {
                // If we already have such an item, increment numToAdd and skip this iteration without adding an item.
                numToAdd++;
                continue;
            }
            // Create a new item and add it.
            realm.copyToRealm(new Item(name));
        }
    }
}
