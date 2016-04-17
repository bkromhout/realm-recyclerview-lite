package com.bkromhout.rrvl.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkromhout.rrvl.RealmRecyclerView;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity {
    @Bind(R.id.recycler)
    RealmRecyclerView recyclerView;

    private Realm realm;
    private RealmBasedRecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        realm = Realm.getDefaultInstance();
        RealmResults<Item> items = realm.where(Item.class).findAllSorted("position");
        adapter = new ItemAdapter(this, items);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.close();
        realm.close();
    }
}
