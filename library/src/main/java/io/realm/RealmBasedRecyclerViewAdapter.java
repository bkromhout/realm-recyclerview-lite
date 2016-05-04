/*
 * Originally based on io.realm.RealmBaseAdapter
 * =============================================
 * Copyright 2014 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.realm;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import com.bkromhout.rrvl.RealmRecyclerView;
import com.bkromhout.rrvl.RealmSimpleItemTouchHelperCallback;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import io.realm.internal.RealmObjectProxy;
import io.realm.internal.Row;
import io.realm.internal.Table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * The base {@code RecyclerView.Adapter} that includes custom functionality to be used with {@link RealmRecyclerView}.
 */
public abstract class RealmBasedRecyclerViewAdapter<T extends RealmObject, VH extends RecyclerView.ViewHolder> extends
        RecyclerView.Adapter<VH> implements RealmSimpleItemTouchHelperCallback.Listener {
    private static final String SEL_POSITIONS_KEY = "rrvl-state-key-selected-positions";

    /**
     * Implemented by {@link RealmRecyclerView} so that we can call it to have it start a drag event.
     */
    public interface StartDragListener {
        void startDragging(RecyclerView.ViewHolder viewHolder);
    }

    private static final List<Long> EMPTY_LIST = new ArrayList<>(0);

    private StartDragListener startDragListener;
    private RealmChangeListener<RealmResults<T>> changeListener;

    private boolean animateResults;
    private boolean gotAnimationInfo = false;
    private long animatePrimaryKeyIndex;
    private RealmFieldType animatePrimaryKeyType;
    private String animateExtraField;
    private long animateExtraFieldIndex = -1;
    private RealmFieldType animateExtraFieldType;

    protected LayoutInflater inflater;
    protected RealmResults<T> realmResults;
    protected List ids;
    protected HashSet<Integer> selectedPositions;
    protected int lastSelectedPos = -1;

    public RealmBasedRecyclerViewAdapter(Context context, RealmResults<T> realmResults,
                                         boolean automaticUpdate, boolean animateResults, String animateExtraField) {
        if (context == null) throw new IllegalArgumentException("Context cannot be null");
        this.changeListener = (!automaticUpdate) ? null : getRealmChangeListener();
        this.animateResults = animateResults;
        this.animateExtraField = animateExtraField;
        this.inflater = LayoutInflater.from(context);

        selectedPositions = new HashSet<>();

        // If automatic updates aren't enabled, then animateResults should be false as well.
        this.animateResults = (automaticUpdate && animateResults);

        updateRealmResults(realmResults);
    }

    private void updateAnimationInfo() {
        if (!animateResults || realmResults == null || gotAnimationInfo) return;
        Table modelTable = realmResults.getTable().getTable();

        // Ensure we have a primary key.
        if (!modelTable.hasPrimaryKey())
            throw new IllegalStateException("Animating the results requires a primary key.");
        // Get the primary key's column index and type.
        animatePrimaryKeyIndex = modelTable.getPrimaryKey();
        animatePrimaryKeyType = modelTable.getColumnType(animatePrimaryKeyIndex);
        // Ensure that the primary key column isn't nullable.
        if (modelTable.isColumnNullable(animatePrimaryKeyIndex)) throw new IllegalStateException("The primary key " +
                "field must not be nullable (it must have the @Required annotation).");

        // If not null, ensure that the extra field is valid.
        if (animateExtraField != null) {
            // Get extra field's column index.
            animateExtraFieldIndex = modelTable.getColumnIndex(animateExtraField);
            // If we couldn't find it, it doesn't exist.
            if (animateExtraFieldIndex == Table.NO_MATCH)
                throw new IllegalArgumentException("animateExtraField must be a valid field name");
            // Ensure that the column isn't nullable.
            if (modelTable.isColumnNullable(animateExtraFieldIndex)) throw new IllegalStateException(
                    "animateExtraField must not be nullable (it must have the @Required annotation).");

            // Try to get extra field's type.
            animateExtraFieldType = modelTable.getColumnType(animateExtraFieldIndex);
            // Make sure it's a short/int/long or string.
            if (animateExtraFieldType != RealmFieldType.INTEGER && animateExtraFieldType != RealmFieldType.STRING)
                throw new IllegalArgumentException("animateExtraField must be short, int, long, or string type.");
        }

        gotAnimationInfo = true;
    }

    private List getIdsOfRealmResults() {
        if (!animateResults || realmResults == null || realmResults.size() == 0) return EMPTY_LIST;

        realmResults.syncIfNeeded();
        // Get/Update IDs.
        List ids = new ArrayList(realmResults.size());
        for (int i = 0; i < realmResults.size(); i++) //noinspection unchecked
            ids.add(getRealmRowId(i));

        return ids;
    }

    private Object getRealmRowId(int realmIndex) {
        Row row = ((RealmObjectProxy) realmResults.get(realmIndex)).realmGet$proxyState().getRow$realm();
        Object rowId;

        if (animatePrimaryKeyType == RealmFieldType.INTEGER)
            rowId = row.getLong(animatePrimaryKeyIndex);
        else if (animatePrimaryKeyType == RealmFieldType.STRING)
            rowId = row.getString(animatePrimaryKeyIndex);
        else throw new IllegalStateException("Unknown animatedIdType");

        if (animateExtraFieldIndex != -1) {
            String rowIdStr = rowId instanceof String ? (String) rowId : String.valueOf(rowId);

            if (animateExtraFieldType == RealmFieldType.INTEGER)
                return rowIdStr + String.valueOf(row.getLong(animateExtraFieldIndex));
            else if (animateExtraFieldType == RealmFieldType.STRING)
                return rowIdStr + row.getString(animateExtraFieldIndex);
            else throw new IllegalStateException("Unknown animateExtraFieldType");
        }

        return rowId;
    }

    private RealmChangeListener<RealmResults<T>> getRealmChangeListener() {
        return new RealmChangeListener<RealmResults<T>>() {
            @Override
            public void onChange(RealmResults<T> realmResults) {
                clearSelections();

                if (animateResults && ids != null && !ids.isEmpty()) {
                    List newIds = getIdsOfRealmResults();

                    // If the list is now empty, just notify the recyclerView of the change.
                    if (newIds.isEmpty()) {
                        ids = newIds;
                        notifyDataSetChanged();
                        return;
                    }

                    Patch patch = DiffUtils.diff(ids, newIds);
                    //noinspection unchecked
                    List<Delta> deltas = patch.getDeltas();
                    ids = newIds;

                    // If the notification was for a different object/table (we'll have no deltas), don't do anything.
                    if (!deltas.isEmpty()) {
                        /* See https://github.com/bkromhout/realm-recyclerview-lite/issues/4#issuecomment-210951358 for
                        an explanation as to why this remains here. */
                        // Try to be smarter here and detect cases where an item has simply moved.
                        /*if (deltas.size() == 2 && areDeltasFromMove(deltas.get(0), deltas.get(1))) {
                            if (deltas.get(0).getType() == Delta.TYPE.DELETE) {
                                notifyItemMoved(deltas.get(0).getOriginal().getPosition(),
                                        deltas.get(1).getRevised().getPosition());
                            } else {
                                notifyItemMoved(deltas.get(1).getOriginal().getPosition(),
                                        deltas.get(0).getRevised().getPosition());
                            }
                        } else {*/

                        if (!(deltas.size() == 2 && areDeltasFromMove(deltas.get(0), deltas.get(1)))) {
                            // Loop through deltas backwards and send notifications for them.
                            for (int i = deltas.size() - 1; i >= 0; i--) {
                                Delta d = deltas.get(i);
                                if (d.getType() == Delta.TYPE.INSERT) {
                                    notifyItemRangeInserted(d.getOriginal().getPosition(), d.getRevised().size());
                                } else if (d.getType() == Delta.TYPE.DELETE) {
                                    notifyItemRangeRemoved(d.getOriginal().getPosition(), d.getOriginal().size());
                                } else {
                                    notifyItemRangeChanged(d.getRevised().getPosition(), d.getRevised().size());
                                }
                            }
                        }
                    }
                } else {
                    notifyDataSetChanged();
                    ids = getIdsOfRealmResults();
                }
            }
        };
    }

    /**
     * Check {@code delta1} and {@code delta2} to determine if, together, they represent a situation where an item has
     * simply moved to somewhere else in the list.
     * @param delta1 A delta.
     * @param delta2 Another delta.
     * @return True if the deltas represent an item having moved, otherwise false.
     */
    private boolean areDeltasFromMove(Delta delta1, Delta delta2) {
        // Check delta types, make sure we have one insert and one delete.
        if (!((delta1.getType() == Delta.TYPE.INSERT && delta2.getType() == Delta.TYPE.DELETE)
                || (delta1.getType() == Delta.TYPE.DELETE && delta2.getType() == Delta.TYPE.INSERT))) return false;
        // Figure out which is which.
        Delta insert = delta1.getType() == Delta.TYPE.INSERT ? delta1 : delta2;
        Delta delete = delta2.getType() == Delta.TYPE.DELETE ? delta2 : delta1;
        // Make sure they only affect one "line".
        if (delete.getOriginal().size() != 1 || insert.getRevised().size() != 1) return false;
        // And make sure that that "line" has the same content.
        return delete.getOriginal().getLines().get(0).equals(insert.getRevised().getLines().get(0));
    }

    /**
     * Start dragging the given {@code viewHolder}. Will do nothing if drag and drop isn't enabled.
     * @param viewHolder ViewHolder to start dragging.
     */
    @SuppressWarnings("unused")
    protected final void startDragging(RecyclerView.ViewHolder viewHolder) {
        if (startDragListener != null) startDragListener.startDragging(viewHolder);
    }

    @Override
    public int getItemCount() {
        return realmResults != null ? realmResults.size() : 0;
    }

    @SuppressWarnings("unused")
    public Object getLastItem() {
        return realmResults.get(realmResults.size() - 1);
    }

    public final void setOnStartDragListener(StartDragListener startDragListener) {
        this.startDragListener = startDragListener;
    }

    /**
     * Ensure this is called whenever {@link Realm#close()} is called to ensure that the {@link #realmResults} are
     * invalidated and the change listener removed.
     */
    public void close() {
        updateRealmResults(null);
    }

    /**
     * Update the RealmResults associated with the Adapter. Useful when the query has been changed. If the query does
     * not change you might consider using the automaticUpdate feature.
     * @param queryResults the new RealmResults coming from the new query.
     */
    public void updateRealmResults(RealmResults<T> queryResults) {
        if (changeListener != null && realmResults != null) realmResults.removeChangeListener(changeListener);

        realmResults = queryResults;
        if (realmResults != null && changeListener != null) realmResults.addChangeListener(changeListener);

        selectedPositions.clear();
        lastSelectedPos = -1;
        updateAnimationInfo();
        ids = getIdsOfRealmResults();
        notifyDataSetChanged();
    }

    /**
     * Check whether the item at {@code position} is selected.
     * @param position The position to check.
     * @return True if the item is selected, otherwise false.
     */
    @SuppressWarnings("unused")
    public final boolean isSelected(int position) {
        return selectedPositions.contains(position);
    }

    /**
     * Set the selected state of the item at {@code position}.
     * <p/>
     * This method will call notifyItemChanged(position) when it completes; it is up to extending class to check if the
     * position is selected when onBindViewHolder gets called again and react accordingly.
     * @param selected Whether or not the item is selected.
     * @param position Position of the item to set.
     */
    @SuppressWarnings("unused")
    public final void setSelected(boolean selected, int position) {
        if (position < 0 || position >= realmResults.size()) return;

        // Don't trigger a redraw if we've already selected the item.
        if (selected == selectedPositions.contains(position)) return;

        if (selected) {
            selectedPositions.add(position);
            lastSelectedPos = position;
        } else {
            selectedPositions.remove(position);
            lastSelectedPos = -1;
        }

        notifyItemChanged(position);
    }

    /**
     * Toggles the selection state of the item at {@code position}.
     * @param position Position of the item to toggle.
     */
    @SuppressWarnings("unused")
    public final void toggleSelected(int position) {
        if (position < 0 || position >= realmResults.size()) return;

        if (!selectedPositions.remove(position)) {
            selectedPositions.add(position);
            lastSelectedPos = position;
        } else {
            lastSelectedPos = -1;
        }

        notifyItemChanged(position);
    }

    /**
     * Get the number of selected items.
     * @return Number of selected items.
     */
    @SuppressWarnings("unused")
    public final int getSelectedItemCount() {
        return selectedPositions.size();
    }

    /**
     * Get the RealmObjects whose items are currently selected.
     * @return List of realm objects, or null if called when the load more view, section headers, or the footer view are
     * added/enabled.
     */
    @SuppressWarnings("unused")
    public final List<T> getSelectedRealmObjects() {
        ArrayList<T> realmObjects = new ArrayList<>();
        // If everything is selected, be quick.
        if (realmResults.size() == selectedPositions.size()) realmObjects.addAll(realmResults);
        else for (Integer i : selectedPositions) realmObjects.add(realmResults.get(i));
        return realmObjects;
    }

    /**
     * Extends the current selection from the last selected item to the given {@code position}. If {@code position} is
     * already selected, de-selects it. If nothing is selected or the last item tapped was de-selected, just selects
     * {@code position}.
     * @param position The position to extend the selection to.
     */
    @SuppressWarnings("unused")
    public final void extendSelectionTo(int position) {
        if (position < 0 || position >= realmResults.size()) return;

        if (selectedPositions.contains(position)) {
            // If this is already selected, de-select it.
            selectedPositions.remove(position);
            notifyItemChanged(position);
            lastSelectedPos = -1;
            return;
        } else if (lastSelectedPos == -1) {
            // If we don't have a previously selected position, just select this one.
            selectedPositions.add(position);
            notifyItemChanged(position);
            return;
        }

        if (lastSelectedPos < position) {
            // Ex: lastSelectedPos = 1, pos = 3. Need to select 2, 3.
            for (int i = lastSelectedPos + 1; i <= position; i++) selectedPositions.add(i);

            notifyItemRangeChanged(lastSelectedPos + 1, position - lastSelectedPos);
            lastSelectedPos = -1;
        } else {
            // lastSelectedPos = 3, pos = 1. Need to select 1, 2.
            for (int i = position; i < lastSelectedPos; i++) selectedPositions.add(i);

            notifyItemRangeChanged(position, lastSelectedPos - position);
            lastSelectedPos = -1;
        }
    }

    /**
     * Select all of the items in the list.
     */
    @SuppressWarnings("unused")
    public final void selectAll() {
        // Add all positions.
        for (int i = 0; i < realmResults.size(); i++) selectedPositions.add(i);
        notifyDataSetChanged();
    }

    /**
     * Clears any selections that may exist.
     */
    public final void clearSelections() {
        // We definitely don't want to do any redrawing if we don't have anything selected!
        if (selectedPositions.isEmpty()) return;
        // If there's only one item selected, we can be efficient and just redraw one view.
        int oneItemPos = selectedPositions.size() == 1 ? (int) selectedPositions.toArray()[0] : -1;

        selectedPositions.clear();
        lastSelectedPos = -1;

        if (oneItemPos != -1) notifyItemChanged(oneItemPos);
        else notifyDataSetChanged();
    }

    /**
     * Calls {@code notifyItemChanged()} on each of the currently selected positions.
     */
    public void notifySelectedItemsChanged() {
        for (Integer i : selectedPositions) notifyItemChanged(i);
    }

    /**
     * Called when an item has been moved whilst dragging. There are two things that overriding classes must
     * consider:<br/>-This is called EVERY time an item "moves", not just when it is "dropped".<br/>-An item
     * <i>technically</i> "moves" each time it is dragged over another item (as in, when the two items should appear to
     * swap); however, if the item is being dragged fast enough Android tends to batch together what would otherwise be
     * multiple calls to this method (if the drag occurred slower) into a single call, meaning that item may have moved
     * multiple spaces.
     * @param dragging The ViewHolder item being dragged.
     * @param target   The ViewHolder item under the item being dragged.
     */
    @Override
    public boolean onMove(RecyclerView.ViewHolder dragging, RecyclerView.ViewHolder target) {
        // Left for the user to implement.
        return false;
    }

    /**
     * Save state of this adapter instance in the given Bundle. An example of such data would be the list of selected
     * indices.
     * @param out Bundle to save state to.
     */
    @SuppressWarnings("unused")
    public final void saveInstanceState(Bundle out) {
        if (out != null) {
            out.putIntegerArrayList(SEL_POSITIONS_KEY, new ArrayList<>(selectedPositions));
        }
    }

    /**
     * Restore state from the given Bundle.
     * @param in Bundle to try and restore state from.
     * @see #saveInstanceState(Bundle)
     */
    @SuppressWarnings("unused")
    public final void restoreInstanceState(Bundle in) {
        if (in != null) {
            ArrayList<Integer> temp = in.getIntegerArrayList(SEL_POSITIONS_KEY);
            if (temp == null) selectedPositions = new HashSet<>();
            else {
                selectedPositions = new HashSet<>(temp);
                notifySelectedItemsChanged();
            }
        }
    }
}

