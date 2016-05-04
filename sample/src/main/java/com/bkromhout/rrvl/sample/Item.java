package com.bkromhout.rrvl.sample;

import com.bkromhout.rrvl.UIDModel;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Item model.
 */
public class Item extends RealmObject implements UIDModel {
    /**
     * By default, have a 100 space gap between items. This helps us to not have to update every item's position when
     * moving one item.
     */
    public static final long GAP = 100L;
    @Ignore
    public static long nextPos = 0L;
    @Ignore
    public static AtomicLong nextUniqueId = new AtomicLong(0L);

    public String name;
    @Index
    public long position;
    @PrimaryKey
    public long uniqueId;

    public Item() {
    }

    public Item(String name) {
        this.name = name;
        this.position = nextPos;
        nextPos += GAP;
        this.uniqueId = nextUniqueId.getAndIncrement();
    }

    @Override
    public Object getUID() {
        // Use our uniqueId field as the UID.
        return uniqueId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Item item = (Item) o;

        return uniqueId == item.uniqueId;

    }

    @Override
    public int hashCode() {
        return (int) (uniqueId ^ (uniqueId >>> 32));
    }
}
