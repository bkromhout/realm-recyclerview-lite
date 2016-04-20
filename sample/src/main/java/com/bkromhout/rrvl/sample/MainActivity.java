package com.bkromhout.rrvl.sample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bkromhout.rrvl.FastScrollHandleStateListener;
import com.bkromhout.rrvl.FastScrollerHandleState;
import com.bkromhout.rrvl.RealmRecyclerView;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity implements FastScrollHandleStateListener {
    @Bind(R.id.recycler)
    RealmRecyclerView recyclerView;
    @Bind(R.id.fab)
    FloatingActionButton fab;

    private Realm realm;
    private RealmBasedRecyclerViewAdapter adapter;
    private boolean isBubbleOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        realm = Realm.getDefaultInstance();
        RealmResults<Item> items = realm.where(Item.class).findAllSorted("position");
        adapter = new ItemAdapter(this, items);
        recyclerView.setAdapter(adapter);
        recyclerView.setBubbleTextProvider((ItemAdapter) adapter);
        recyclerView.setFastScrollHandleStateListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.close();
        realm.close();
    }

    @OnClick(R.id.fab)
    void onFabClick() {
        isBubbleOn = !isBubbleOn;
        recyclerView.setUseFastScrollBubble(isBubbleOn);
        Toast.makeText(this, "Fast Scroll Bubble turned " + (isBubbleOn ? "on." : "off."), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onHandleStateChanged(FastScrollerHandleState newState) {
        switch (newState) {
            case VISIBLE:
                Log.d("MainActivity", "Handle visible.");
                break;
            case HIDDEN:
                Log.d("MainActivity", "Handle hidden.");
                break;
            case PRESSED:
                // Hide the FloatingActionButton.
                fab.hide();
                Log.d("MainActivity", "Handle pressed.");
                break;
            case RELEASED:
                Log.d("MainActivity", "Handle released.");
                break;
        }
    }
}
