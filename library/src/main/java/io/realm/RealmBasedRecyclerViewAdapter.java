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
import com.bkromhout.realmrecyclerview.RealmRecyclerView;
import com.bkromhout.realmrecyclerview.RealmSimpleItemTouchHelperCallback;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import io.realm.internal.TableOrView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * The base {@link RecyclerView.Adapter} that includes custom functionality to be used with {@link RealmRecyclerView}.
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

    protected LayoutInflater inflater;
    protected RealmResults<T> realmResults;
    protected List ids;
    protected HashSet<Integer> selectedPositions;
    protected int lastSelectedPos = -1;
    protected StartDragListener startDragListener;

    private RealmChangeListener listener;
    private boolean animateResults;

    private long animatePrimaryColumnIndex;
    private RealmFieldType animatePrimaryIdType;
    private long animateExtraColumnIndex;
    private RealmFieldType animateExtraIdType;


    public RealmBasedRecyclerViewAdapter(Context context, RealmResults<T> realmResults, boolean automaticUpdate,
                                         boolean animateResults, String animateExtraColumnName) {
        if (context == null) throw new IllegalArgumentException("Context cannot be null");

        this.animateResults = animateResults;
        this.inflater = LayoutInflater.from(context);
        this.listener = (!automaticUpdate) ? null : getRealmChangeListener();

        selectedPositions = new HashSet<>();

        // If automatic updates aren't enabled, then animateResults should be false as well.
        this.animateResults = (automaticUpdate && animateResults);
        if (animateResults) {
            animatePrimaryColumnIndex = realmResults.getTable().getTable().getPrimaryKey();
            if (animatePrimaryColumnIndex == TableOrView.NO_MATCH) throw new IllegalStateException(
                    "Animating the results requires a primaryKey.");

            animatePrimaryIdType = realmResults.getTable().getColumnType(animatePrimaryColumnIndex);
            if (animatePrimaryIdType != RealmFieldType.INTEGER && animatePrimaryIdType != RealmFieldType.STRING)
                throw new IllegalStateException("Animating requires a primary key of type Integer/Long or String");


            if (animateExtraColumnName != null) {
                animateExtraColumnIndex = realmResults.getTable().getTable().getColumnIndex(animateExtraColumnName);
                if (animateExtraColumnIndex == TableOrView.NO_MATCH) throw new IllegalStateException(
                        "Animating the results requires a valid animateColumnName.");

                animateExtraIdType = realmResults.getTable().getColumnType(animateExtraColumnIndex);
                if (animateExtraIdType != RealmFieldType.INTEGER && animateExtraIdType != RealmFieldType.STRING)
                    throw new IllegalStateException(
                            "Animating requires a animateColumnName of type Int/Long or String");
            } else {
                animateExtraColumnIndex = -1;
            }
        }

        updateRealmResults(realmResults);
    }

    public Object getLastItem() {
        return realmResults.get(realmResults.size() - 1);
    }

    @Override
    public int getItemCount() {
        return realmResults != null ? realmResults.size() : 0;
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
        if (listener != null && realmResults != null) realmResults.realm.removeChangeListener(listener);

        realmResults = queryResults;
        if (realmResults != null) realmResults.realm.addChangeListener(listener);

        selectedPositions.clear();
        lastSelectedPos = -1;
        ids = getIdsOfRealmResults();
        notifyDataSetChanged();
    }

    private List getIdsOfRealmResults() {
        if (!animateResults || realmResults == null || realmResults.size() == 0) return EMPTY_LIST;

        List ids = new ArrayList(realmResults.size());
        for (int i = 0; i < realmResults.size(); i++) ids.add(getRealmRowId(i));

        return ids;
    }

    private Object getRealmRowId(int realmIndex) {
        Object rowPrimaryId;
        if (animatePrimaryIdType == RealmFieldType.INTEGER) {
            rowPrimaryId = realmResults.get(realmIndex).row.getLong(animatePrimaryColumnIndex);
        } else if (animatePrimaryIdType == RealmFieldType.STRING) {
            rowPrimaryId = realmResults.get(realmIndex).row.getString(animatePrimaryColumnIndex);
        } else {
            throw new IllegalStateException("Unknown animatedIdType");
        }

        if (animateExtraColumnIndex != -1) {
            String rowPrimaryIdStr = (rowPrimaryId instanceof String) ? (String) rowPrimaryId : String.valueOf(
                    rowPrimaryId);
            if (animateExtraIdType == RealmFieldType.INTEGER) {
                return rowPrimaryIdStr + String.valueOf(
                        realmResults.get(realmIndex).row.getLong(animateExtraColumnIndex));
            } else if (animateExtraIdType == RealmFieldType.STRING) {
                return rowPrimaryIdStr + realmResults.get(realmIndex).row.getString(animateExtraColumnIndex);
            } else {
                throw new IllegalStateException("Unknown animateExtraIdType");
            }
        } else {
            return rowPrimaryId;
        }
    }

    private RealmChangeListener getRealmChangeListener() {
        return new RealmChangeListener() {
            @Override
            public void onChange() {
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
                    List<Delta> deltas = patch.getDeltas();
                    ids = newIds;

                    // If the notification was for a different object/table (we'll have no deltas), don't do anything.
                    if (!deltas.isEmpty()) {
                        // Try to be smarter here and detect cases where an item has simply moved.
                        if (deltas.size() == 2 && areDeltasFromMove(deltas.get(0), deltas.get(1))) {
                            if (deltas.get(0).getType() == Delta.TYPE.DELETE) {
                                notifyItemMoved(deltas.get(0).getOriginal().getPosition(),
                                        deltas.get(1).getRevised().getPosition());
                            } else {
                                notifyItemMoved(deltas.get(1).getOriginal().getPosition(),
                                        deltas.get(0).getRevised().getPosition());
                            }
                        } else {
                            for (Delta delta : deltas) {
                                if (delta.getType() == Delta.TYPE.INSERT) {
                                    notifyItemRangeInserted(delta.getRevised().getPosition(),
                                            delta.getRevised().size());
                                } else if (delta.getType() == Delta.TYPE.DELETE) {
                                    notifyItemRangeRemoved(delta.getOriginal().getPosition(),
                                            delta.getOriginal().size());
                                } else {
                                    notifyItemRangeChanged(delta.getRevised().getPosition(), delta.getRevised().size());
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
     * Check whether the item at {@code position} is selected.
     * @param position The position to check.
     * @return True if the item is selected, otherwise false.
     */
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
    public final int getSelectedItemCount() {
        return selectedPositions.size();
    }

    /**
     * Get the RealmObjects whose items are currently selected.
     * @return List of realm objects, or null if called when the load more view, section headers, or the footer view are
     * added/enabled.
     */
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
     * consider:<br/>-This is called EVERY time an item "moves", not just when it is "dropped".<br/>-An item technically
     * "moves" each time it is dragged over another item (as in, when the two items should appear to swap); however, if
     * a drag happens very fast this tends to not get called until the dragged item has already moved past more than one
     * target item.
     * <p/>
     * Put together, this means that the following three cases should be considered for best performance:<br/>1: The
     * dragged item moves past one item (the items swap) -> Swap the values of whatever field is used to maintain
     * order.<br/>2: The dragged item has moved up past several items -> Recalculate the order field's value for the
     * dragging item.<br/>3: The dragged item has moved down past several items -> Recalculate the order field's value
     * for the dragging item.
     * <p/>
     * If these three cases are handled well (specifically, the latter two do not cause the whole list's order field
     * values to be recalculated), then dragging items should be nearly (if not completely) lag free.
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
    public final void saveInstanceState(Bundle out) {
        if (out == null) return;
        out.putIntegerArrayList(SEL_POSITIONS_KEY, new ArrayList<>(selectedPositions));
    }

    /**
     * Restore state from the given Bundle.
     * @param in Bundle to try and restore state from.
     * @see #saveInstanceState(Bundle)
     */
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

