package com.bkromhout.rrvl.sample;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import com.bkromhout.rrvl.RealmRecyclerViewAdapter;
import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity implements FastScrollHandleStateListener {
    @Bind(R.id.recycler)
    RealmRecyclerView recyclerView;
    @Bind(R.id.fab)
    FloatingActionButton fab;

    private Realm realm;
    private RealmRecyclerViewAdapter adapter;

    private boolean logHandleEvents = false;

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
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu != null) for (int i = 0; i < menu.size(); i++)
            if (menu.getItem(i).getIcon() != null) menu.getItem(i).getIcon().setColorFilter(
                    ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.close();
        realm.close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                showOptionsDialog();
                return true;
            case R.id.action_bulk_add:
                new MaterialDialog.Builder(this)
                        .title(R.string.action_bulk_add)
                        .input(R.string.prompt_bulk_add, 0, false, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                final int num = Integer.parseInt(input.toString());
                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        Util.addXItems(realm, num);
                                    }
                                });
                            }
                        })
                        .show();
                return true;
            case R.id.action_delete_all:
                new MaterialDialog.Builder(this)
                        .title(R.string.action_delete_all)
                        .positiveText("Yes")
                        .negativeText("No")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        Util.removeAllItems(realm);
                                    }
                                });
                            }
                        })
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.fab)
    void onFabClick() {
        new MaterialDialog.Builder(this)
                .title(R.string.action_add)
                .autoDismiss(false)
                .negativeText(R.string.cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .input("New Item Name", null, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        // If it's the same value, do nothing.
                        final String newName = input.toString().trim();

                        // Get Realm to check if name exists.
                        try (Realm innerRealm = Realm.getDefaultInstance()) {
                            // If the name exists, set the error text on the edit text. If it doesn't, add it
                            // and dismiss the dialog.
                            if (innerRealm.where(Item.class).equalTo("name", newName).findFirst() != null) {
                                //noinspection ConstantConditions
                                dialog.getInputEditText().setError("Name is already taken.");
                            } else {
                                innerRealm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        realm.copyToRealm(new Item(newName));
                                    }
                                });
                                dialog.dismiss();
                            }
                        }
                    }
                })
                .show();
    }

    @Override
    public void onHandleStateChanged(FastScrollerHandleState newState) {
        if (logHandleEvents) {
            // Only log if we want that.
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
        } else if (newState == FastScrollerHandleState.PRESSED)
            fab.hide();
    }

    private void showOptionsDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.options_dialog, null);
        final CheckBox dragAndDrop = ButterKnife.findById(content, R.id.drag_and_drop);
        final CheckBox longClickTriggersDrag = ButterKnife.findById(content, R.id.long_click_triggers_drag);
        final CheckBox fastScroll = ButterKnife.findById(content, R.id.fast_scroll);
        final CheckBox autoHideHandle = ButterKnife.findById(content, R.id.auto_hide_handle);
        final EditText autoHideDelay = ButterKnife.findById(content, R.id.auto_hide_delay);
        final CheckBox logHandleEvents = ButterKnife.findById(content, R.id.log_handle_events);
        final CheckBox useBubble = ButterKnife.findById(content, R.id.use_bubble);

        dragAndDrop.setChecked(recyclerView.getDragAndDrop());
        longClickTriggersDrag.setChecked(recyclerView.getLongClickTriggersDrag());
        fastScroll.setChecked(recyclerView.getFastScroll());
        autoHideHandle.setChecked(recyclerView.getAutoHideFastScrollHandle());
        autoHideDelay.setText(String.valueOf(recyclerView.getHandleAutoHideDelay()));
        logHandleEvents.setChecked(this.logHandleEvents);
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
                        MainActivity.this.logHandleEvents = logHandleEvents.isChecked();
                        recyclerView.setUseFastScrollBubble(useBubble.isChecked());
                    }
                })
                .show();
    }
}
