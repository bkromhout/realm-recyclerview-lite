package com.bkromhout.rrvl.sample;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import io.realm.Realm;
import io.realm.RealmConfiguration;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.setDefaultConfiguration(new RealmConfiguration.Builder(this).build());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        try (Realm realm = Realm.getDefaultInstance()) {
            // Populate Realm if we haven't yet.
            if (!prefs.getBoolean("hasPopulatedRealm", false)) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm tRealm) {
                        createDefaultRealmData(tRealm);
                    }
                });
                prefs.edit().putBoolean("hasPopulatedRealm", true).apply();
            }
            // Ensure that nextPos and nextUniqueId are correct.
            Item.nextPos = realm.where(Item.class).max("position").longValue() + Item.GAP;
            Item.nextUniqueId = new AtomicLong(realm.where(Item.class).max("uniqueId").longValue() + 1);
        }
    }

    /**
     * Fill in Realm with some default data.
     */
    private void createDefaultRealmData(Realm realm) {
        ArrayList<Item> items = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) items.add(new Item("Item " + String.valueOf(i)));
        realm.copyToRealm(items);
    }
}
