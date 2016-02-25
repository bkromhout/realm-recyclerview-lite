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
import java.util.Set;

/**
 * The base {@link RecyclerView.Adapter} that includes custom functionality to be used with {@link RealmRecyclerView}.
 */
public abstract class RealmBasedRecyclerViewAdapter<T extends RealmObject, VH extends RecyclerView.ViewHolder> extends
        RecyclerView.Adapter<VH> implements RealmSimpleItemTouchHelperCallback.Listener {

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
    protected Set<Integer> selectedPositions;
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

        selectedPositions = new HashSet<>(100);

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

        // TODO update selected positions
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
                clearSelections(); // TODO make better?

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
                        if (deltas.size() == 2 && areDeltasFromDrag(deltas.get(0), deltas.get(1))) {
//                            notifyItemMoved(deltas.get(0).getOriginal().getPosition(),
//                                    deltas.get(1).getRevised().getPosition());
                        } else {
                            for (Delta delta : deltas) {
                                if (delta.getType() == Delta.TYPE.INSERT) {
                                    notifyItemRangeInserted(delta.getRevised().getPosition(),
                                            delta.getRevised().size());
                                } else if (delta.getType() == Delta.TYPE.DELETE) {
                                    notifyItemRangeRemoved(delta.getOriginal().getPosition(),
                                            delta.getOriginal().size());
                                } else {
                                    // TODO try to do just a notify item changed.
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

    private boolean areDeltasFromDrag(Delta delta1, Delta delta2) {
        // TODO What if they're the other way around?
        // Check delta types.
        if (delta1.getType() != Delta.TYPE.DELETE || delta2.getType() != Delta.TYPE.INSERT) return false;
        // Check delta sizes.
        if (delta1.getOriginal().size() != 1 || delta2.getRevised().size() != 1) return false;
        // Check delta places.
        int expectedInsertPos = delta1.getOriginal().getPosition() + 1;
        if (delta2.getRevised().getPosition() != expectedInsertPos) return false;
        // Lastly, make sure the delta lines are the same.
        return delta1.getOriginal().getLines().get(0).equals(delta2.getRevised().getLines().get(0));
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
     * <p>
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
    public final int getNumSelected() {
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
     * Called when an item has been moved whilst dragging. Note that this is called EVERY time an item moves, not just
     * when it is "dropped".
     * <p>
     * Only supported with type linearLayout and thus the realmResults can be accessed directly. If it is extended to
     * LinearLayoutWithHeaders, rowWrappers will have to be used.
     * @param dragging The ViewHolder item being dragged.
     * @param target   The ViewHolder item under the item being dragged.
     */
    @Override
    public boolean onMove(RecyclerView.ViewHolder dragging, RecyclerView.ViewHolder target) {
        // Left for the user to implement.
        return true;
    }
}

