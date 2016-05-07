package com.bkromhout.rrvl;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import io.realm.RealmChangeListener;
import io.realm.RealmModel;
import io.realm.RealmResults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * The base {@code RecyclerView.Adapter} that includes custom functionality to be used with {@link RealmRecyclerView}.
 */
public abstract class RealmRecyclerViewAdapter<T extends RealmModel & UIDModel, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements RealmSimpleItemTouchHelperCallback.Listener {

    private static final String SEL_POSITIONS_KEY = "rrvl-state-key-selected-positions";
    private static final List<Long> EMPTY_LIST = new ArrayList<>(0);

    private RealmRecyclerView rrv = null;
    private RealmChangeListener<RealmResults<T>> changeListener;

    protected LayoutInflater inflater;
    protected RealmResults<T> realmResults;
    protected List ids;
    protected HashSet<Integer> selectedPositions;
    protected int lastSelectedPos = -1;

    public RealmRecyclerViewAdapter(Context context, RealmResults<T> realmResults) {
        if (context == null) throw new IllegalArgumentException("Context cannot be null");
        this.changeListener = getRealmChangeListener();
        this.inflater = LayoutInflater.from(context);

        selectedPositions = new HashSet<>();
        updateRealmResults(realmResults);
    }

    private List getIdsOfRealmResults() {
        if (realmResults == null || realmResults.size() == 0) return EMPTY_LIST;

        // Get/Update IDs.
        List ids = new ArrayList(realmResults.size());
        for (int i = 0; i < realmResults.size(); i++)
            //noinspection unchecked
            ids.add(realmResults.get(i).getUID());

        return ids;
    }

    private RealmChangeListener<RealmResults<T>> getRealmChangeListener() {
        return new RealmChangeListener<RealmResults<T>>() {
            @Override
            public void onChange(RealmResults<T> newResults) {
                clearSelections();

                if (ids != null && !ids.isEmpty()) {
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

    final void setRealmRecyclerView(RealmRecyclerView rrv) {
        this.rrv = rrv;
    }

    /**
     * Start dragging the given {@code viewHolder}. Will do nothing if drag and drop isn't enabled or if the adapter
     * isn't attached to a {@link RealmRecyclerView}.
     * @param viewHolder ViewHolder to start dragging.
     */
    @SuppressWarnings("unused")
    protected final void startDragging(RecyclerView.ViewHolder viewHolder) {
        if (rrv != null) rrv.startDragging(viewHolder);
    }

    @Override
    public int getItemCount() {
        return realmResults != null ? realmResults.size() : 0;
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
        ids = getIdsOfRealmResults();
        notifyDataSetChanged();
    }

    /**
     * Ensure this is called whenever {@code Realm.close()} is called to ensure that the {@link #realmResults} are
     * invalidated and the change listener removed.
     */
    @SuppressWarnings("unused")
    public final void close() {
        updateRealmResults(null);
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
     * <p>
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
     * @return True if the viewHolder has been moved to the adapter position of target.
     */
    @Override
    public boolean onMove(RecyclerView.ViewHolder dragging, RecyclerView.ViewHolder target) {
        // Left for the user to implement.
        return false;
    }

    /**
     * Called when the ViewHolder swiped or dragged by the ItemTouchHelper is changed.
     * <p>
     * This is called after the framework's default behavior takes place; the default implementation does nothing.
     * @param viewHolder  The new ViewHolder that is being swiped or dragged. Might be null if it is cleared.
     * @param actionState One of {@code ItemTouchHelper.ACTION_STATE_IDLE}, {@code ItemTouchHelper.ACTION_STATE_SWIPE}
     *                    or {@code ItemTouchHelper.ACTION_STATE_DRAG}.
     */
    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        // Left for the user to implement.
    }

    /**
     * Called by the ItemTouchHelper when the user interaction with an element is over and it also completed its
     * animation.
     * <p>
     * This is a good place to clear all changes on the View that was done in {@link
     * #onSelectedChanged(RecyclerView.ViewHolder, int)}, {@link #onChildDraw(Canvas, RecyclerView,
     * RecyclerView.ViewHolder, float, float, int, boolean)} or {@link #onChildDrawOver(Canvas, RecyclerView,
     * RecyclerView.ViewHolder, float, float, int, boolean)}.
     * <p>
     * This is called after the framework's default behavior takes place; the default implementation does nothing.
     * @param recyclerView The RecyclerView which is controlled by the ItemTouchHelper.
     * @param viewHolder   The View that was interacted by the user.
     */
    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        // Left for the user to implement.
    }

    /**
     * Called by ItemTouchHelper on RecyclerView's onDraw callback.
     * <p>
     * If you would like to customize how your View's respond to user interactions, override this.
     * <p>
     * By default, this implementation does nothing and returns {@code false} to indicate we should use the framework's
     * default behavior.
     * @param c                 The canvas which RecyclerView is drawing its children
     * @param recyclerView      The RecyclerView to which ItemTouchHelper is attached to
     * @param viewHolder        The ViewHolder which is being interacted by the User or it was interacted and simply
     *                          animating to its original position
     * @param dX                The amount of horizontal displacement caused by user's action
     * @param dY                The amount of vertical displacement caused by user's action
     * @param actionState       The type of interaction on the View. Is either {@code ItemTouchHelper.ACTION_STATE_DRAG}
     *                          or {@code ItemTouchHelper.ACTION_STATE_SWIPE}.
     * @param isCurrentlyActive True if this view is currently being controlled by the user or false it is simply
     *                          animating back to its original state.
     * @return False if the framework's default behavior should be taken upon the completion of this method, true if the
     * framework's behavior should be skipped.
     * @see android.support.v7.widget.helper.ItemTouchHelper.Callback#onChildDraw(Canvas, RecyclerView,
     * RecyclerView.ViewHolder, float, float, int, boolean)
     */
    @Override
    public boolean onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX,
                               float dY, int actionState, boolean isCurrentlyActive) {
        // Use default framework behavior.
        return false;
    }

    /**
     * Called by ItemTouchHelper on RecyclerView's onDraw callback.
     * <p>
     * If you would like to customize how your View's respond to user interactions, override this.
     * <p>
     * By default, this implementation does nothing and returns {@code false} to indicate we should use the framework's
     * default behavior.
     * @param c                 The canvas which RecyclerView is drawing its children
     * @param recyclerView      The RecyclerView to which ItemTouchHelper is attached to
     * @param viewHolder        The ViewHolder which is being interacted by the User or it was interacted and simply
     *                          animating to its original position
     * @param dX                The amount of horizontal displacement caused by user's action
     * @param dY                The amount of vertical displacement caused by user's action
     * @param actionState       The type of interaction on the View. Is either {@code ItemTouchHelper.ACTION_STATE_DRAG}
     *                          or {@code ItemTouchHelper.ACTION_STATE_SWIPE}.
     * @param isCurrentlyActive True if this view is currently being controlled by the user or false it is simply
     *                          animating back to its original state.
     * @return False if the framework's default behavior should be taken upon the completion of this method, true if the
     * framework's behavior should be skipped.
     * @see android.support.v7.widget.helper.ItemTouchHelper.Callback#onChildDrawOver(Canvas, RecyclerView,
     * RecyclerView.ViewHolder, float, float, int, boolean)
     */
    @Override
    public boolean onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX,
                                   float dY, int actionState, boolean isCurrentlyActive) {
        // Use default framework behavior.
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
