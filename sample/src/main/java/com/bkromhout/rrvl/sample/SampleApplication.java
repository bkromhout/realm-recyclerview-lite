package com.bkromhout.rrvl.sample;

import android.app.Application;
import io.realm.Realm;
import io.realm.RealmConfiguration;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.setDefaultConfiguration(new RealmConfiguration.Builder(this)
                .deleteRealmIfMigrationNeeded()
                .initialData(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        ArrayList<Item> items = new ArrayList<>(100);
                        for (int i = 0; i < 100; i++)
                            items.add(new Item(String.valueOf(i / 10) + " Item " + String.valueOf(i)));
                        realm.copyToRealm(items);
                    }
                })
                .build());

        try (Realm realm = Realm.getDefaultInstance()) {
            // Ensure that nextPos and nextUniqueId are correct.
            Item.nextPos = realm.where(Item.class).max("position").longValue() + Item.GAP;
            Item.nextUniqueId = new AtomicLong(realm.where(Item.class).max("uniqueId").longValue() + 1);
        }
    }
}
