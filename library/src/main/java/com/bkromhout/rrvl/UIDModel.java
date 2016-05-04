package com.bkromhout.rrvl;

/**
 * Realm model classes should implement this so that a {@link RealmRecyclerViewAdapter} can correctly perform predictive
 * animations.
 */
public interface UIDModel {
    /**
     * Return a value which uniquely identifies this model object instance in relation to others of its type so that an
     * instance of {@link RealmRecyclerViewAdapter} can keep track of its position and correctly perform predictive
     * animations as needed.
     * <p/>
     * The returned value <i>should not</i> change based on other items. Said another way, the returned value should be
     * based upon data from the item it identifies, and should be the same each time this method is called on that
     * item.
     * <p/>
     * It is assumed that the returned value is not {@code null}, and that all instances of this model class will return
     * the same type of value.
     * <p/>
     * It is recommended that the returned value simply be the value of the model's primary key field (or any other
     * field which your code provides uniqueness guarantees for).
     * @return Value which uniquely identifies an instance of this model object.
     */
    Object getUID();
}
