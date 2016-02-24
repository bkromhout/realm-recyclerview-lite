package co.moonmonkeylabs.realmrecyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;
import io.realm.RealmBasedRecyclerViewAdapter;

/**
 * A RecyclerView that supports Realm
 */
public class RealmRecyclerView extends FrameLayout implements RealmBasedRecyclerViewAdapter.StartDragListener {

    private enum DragTrigger {
        UserDefined, LongClick
    }

    private RecyclerView recyclerView;
    private ViewStub emptyContentContainer;
    private RealmBasedRecyclerViewAdapter adapter;
    private ItemTouchHelper touchHelper;
    private RealmSimpleItemTouchHelperCallback realmSimpleItemTouchHelperCallback;

    // Attributes
    private int emptyViewId;
    private boolean dragAndDrop;
    private DragTrigger dragTrigger;

    public RealmRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public RealmRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RealmRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.realm_recycler_view, this);
        initAttrs(context, attrs);

        recyclerView = (RecyclerView) findViewById(R.id.rrv_recycler_view);
        emptyContentContainer = (ViewStub) findViewById(R.id.rrv_empty_content_container);


        if (emptyViewId != 0) {
            emptyContentContainer.setLayoutResource(emptyViewId);
            emptyContentContainer.inflate();
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        recyclerView.setHasFixedSize(true);

        recyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                    }
                }
        );

        if (dragAndDrop) {
            realmSimpleItemTouchHelperCallback =
                    new RealmSimpleItemTouchHelperCallback(dragAndDrop, dragTrigger == DragTrigger.LongClick);
            touchHelper = new ItemTouchHelper(realmSimpleItemTouchHelperCallback);
            touchHelper.attachToRecyclerView(recyclerView);
        }
    }

    public int findFirstVisibleItemPosition() {
        return ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();

    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RealmRecyclerView);

        emptyViewId = typedArray.getResourceId(R.styleable.RealmRecyclerView_rrvEmptyLayoutId, 0);

        dragAndDrop = typedArray.getBoolean(R.styleable.RealmRecyclerView_rrvDragAndDrop, false);

        int dragStartTriggerValue = typedArray.getInt(R.styleable.RealmRecyclerView_rrvDragStartTrigger, -1);
        if (dragStartTriggerValue != -1) dragTrigger = DragTrigger.values()[dragStartTriggerValue];
        else dragTrigger = DragTrigger.UserDefined;

        typedArray.recycle();
    }

    public void setAdapter(final RealmBasedRecyclerViewAdapter adapter) {
        this.adapter = adapter;
        recyclerView.setAdapter(adapter);

        if (dragAndDrop) realmSimpleItemTouchHelperCallback.setListener(adapter);
        if (dragAndDrop && dragTrigger == DragTrigger.UserDefined) adapter.setOnStartDragListener(this);

        if (adapter != null) {
            adapter.registerAdapterDataObserver(
                    new RecyclerView.AdapterDataObserver() {
                        @Override
                        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                            super.onItemRangeMoved(fromPosition, toPosition, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeRemoved(int positionStart, int itemCount) {
                            super.onItemRangeRemoved(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeInserted(int positionStart, int itemCount) {
                            super.onItemRangeInserted(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeChanged(int positionStart, int itemCount) {
                            super.onItemRangeChanged(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onChanged() {
                            super.onChanged();
                            update();
                        }

                        private void update() {
                            updateEmptyContentContainerVisibility(adapter);
                        }
                    }
            );
            updateEmptyContentContainerVisibility(adapter);
        }
    }

    private void updateEmptyContentContainerVisibility(RecyclerView.Adapter adapter) {
        if (emptyViewId == 0) return;
        emptyContentContainer.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public final void startDragging(RecyclerView.ViewHolder viewHolder) {
        if (touchHelper != null) touchHelper.startDrag(viewHolder);
    }

    /*
     * Expose public RecyclerView methods to the RealmRecyclerView.
     */

    /**
     * @see RecyclerView#setItemViewCacheSize(int)
     */
    public void setItemViewCacheSize(int size) {
        recyclerView.setItemViewCacheSize(size);
    }

    /**
     * @see RecyclerView#scrollToPosition(int)
     */
    public void scrollToPosition(int position) {
        recyclerView.scrollToPosition(position);
    }

    /**
     * @see RecyclerView#smoothScrollToPosition(int)
     */
    public void smoothScrollToPosition(int position) {
        recyclerView.smoothScrollToPosition(position);
    }
}
