package com.bkromhout.rrvl.sample;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkromhout.rrvl.BubbleTextProvider;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;

/**
 * Simple item adapter. Supports drag and drop and the fast scroller's bubble text.
 */
public class ItemAdapter extends RealmBasedRecyclerViewAdapter<Item, ItemAdapter.ItemVH> implements BubbleTextProvider {

    public ItemAdapter(Context context, RealmResults<Item> realmResults) {
        super(context, realmResults, true, true, null);
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return realmResults.get(position).uniqueId;
    }

    @Override
    public ItemVH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemVH(inflater.inflate(R.layout.item_card, parent, false));
    }

    @Override
    public void onBindViewHolder(final ItemVH holder, int position) {
        Item item = realmResults.get(position);
        holder.name.setText(item.name);
        // We set the unique ID as the tag on a view so that we will be able to get it in the onMove() method.
        holder.content.setTag(item.uniqueId);
        // Grabbing the drag handle should trigger a drag.
        holder.dragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN)
                    startDragging(holder);
                return false;
            }
        });
    }

    @Override
    public boolean onMove(RecyclerView.ViewHolder dragging, RecyclerView.ViewHolder target) {
        // Get positions of items in adapter.
        int draggingPos = dragging.getAdapterPosition();
        int targetPos = target.getAdapterPosition();

        // Get the unique IDs of the items from the tag that we set in onBindViewHolder().
        long draggingId = (long) ((ItemVH) dragging).content.getTag();
        long targetId = (long) ((ItemVH) target).content.getTag();

        // Determine if we can swap the items or if we need to actually move the item being dragged.
        if (Math.abs(draggingPos - targetPos) == 1)
            // Swapped.
            ItemDragHelper.swapItemPositions(draggingId, targetId);
        else if (draggingPos > targetPos)
            // Moved up multiple.
            ItemDragHelper.moveItemToBefore(draggingId, targetId);
        else
            // Moved down multiple.
            ItemDragHelper.moveItemToAfter(draggingId, targetId);

        return true;
    }

    @Override
    public String getFastScrollBubbleText(int position) {
        return String.valueOf(realmResults.get(position).name.charAt(0));
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        @Bind(R.id.content)
        RelativeLayout content;
        @Bind(R.id.drag_handle)
        ImageView dragHandle;
        @Bind(R.id.name)
        TextView name;

        public ItemVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
