package com.bkromhout.rrvl.sample;

import android.app.Application;
import io.realm.Realm;
import io.realm.RealmConfiguration;

import java.util.concurrent.atomic.AtomicLong;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        Realm.setDefaultConfiguration(new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .initialData(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        Util.addXItems(realm, 50);
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
