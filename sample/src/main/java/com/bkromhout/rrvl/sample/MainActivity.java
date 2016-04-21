package com.bkromhout.rrvl.sample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
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
        showOptionsDialog();
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

    private void showOptionsDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.options_dialog, null);
        final CheckBox dragAndDrop = ButterKnife.findById(content, R.id.drag_and_drop);
        final CheckBox longClickTriggersDrag = ButterKnife.findById(content, R.id.long_click_triggers_drag);
        final CheckBox fastScroll = ButterKnife.findById(content, R.id.fast_scroll);
        final CheckBox autoHideHandle = ButterKnife.findById(content, R.id.auto_hide_handle);
        final EditText autoHideDelay = ButterKnife.findById(content, R.id.auto_hide_delay);
        final CheckBox useBubble = ButterKnife.findById(content, R.id.use_bubble);

        dragAndDrop.setChecked(recyclerView.getDragAndDrop());
        longClickTriggersDrag.setChecked(recyclerView.getLongClickTriggersDrag());
        fastScroll.setChecked(recyclerView.getFastScroll());
        autoHideHandle.setChecked(recyclerView.getAutoHideFastScrollHandle());
        autoHideDelay.setText(String.valueOf(recyclerView.getHandleAutoHideDelay()));
        useBubble.setChecked(recyclerView.getUseFastScrollBubble());

        new MaterialDialog.Builder(this)
                .title(R.string.options)
                .customView(content, true)
                .positiveText(R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        recyclerView.setDragAndDrop(dragAndDrop.isChecked());
                        recyclerView.setLongClickTriggersDrag(longClickTriggersDrag.isChecked());
                        recyclerView.setFastScroll(fastScroll.isChecked());
                        recyclerView.setAutoHideFastScrollHandle(autoHideHandle.isChecked());
                        try {
                            recyclerView.setHandleAutoHideDelay(Integer.valueOf(autoHideDelay.getText().toString()));
                        } catch (NumberFormatException e) {
                            recyclerView.setHandleAutoHideDelay(-1);
                        }
                        recyclerView.setUseFastScrollBubble(useBubble.isChecked());
                    }
                })
                .show();
    }
}
