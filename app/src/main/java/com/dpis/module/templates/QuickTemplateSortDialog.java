package com.dpis.module.templates;

import com.dpis.module.R;
import com.dpis.module.ui.DialogWindowSizer;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QuickTemplateSortDialog {
    public interface Host {
        boolean saveOrder(List<String> orderedIds);

        void onOrderSaved();

        void showToast(int messageResId);
    }

    private QuickTemplateSortDialog() {
    }

    public static void show(Activity activity,
            List<QuickTemplateStore.QuickTemplate> templates,
            Host host) {
        if (activity == null || templates == null || templates.isEmpty()) {
            return;
        }
        View root = LayoutInflater.from(activity).inflate(
                R.layout.dialog_quick_template_sort, null, false);
        RecyclerView listView = root.findViewById(R.id.quick_template_sort_list);
        MaterialButton cancelButton = root.findViewById(R.id.quick_template_sort_cancel_button);
        MaterialButton saveButton = root.findViewById(R.id.quick_template_sort_save_button);
        SortAdapter adapter = new SortAdapter(templates);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new DragCallback(adapter));
        adapter.setItemTouchHelper(itemTouchHelper);
        listView.setLayoutManager(new LinearLayoutManager(activity));
        listView.setAdapter(adapter);
        listView.addItemDecoration(new RowSpacingDecoration(
                activity.getResources().getDimensionPixelSize(
                        R.dimen.dialog_template_sort_row_spacing)));
        itemTouchHelper.attachToRecyclerView(listView);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(root)
                .create();
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        saveButton.setOnClickListener(v -> {
            if (host == null || host.saveOrder(adapter.orderedIds())) {
                dialog.dismiss();
                if (host != null) {
                    host.onOrderSaved();
                }
                return;
            }
            host.showToast(R.string.quick_template_sort_failed);
        });
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        DialogWindowSizer.applyLargeWidth(dialog, activity);
    }

    private static final class SortAdapter extends RecyclerView.Adapter<SortHolder> {
        private final ArrayList<QuickTemplateStore.QuickTemplate> items = new ArrayList<>();
        private ItemTouchHelper itemTouchHelper;

        SortAdapter(List<QuickTemplateStore.QuickTemplate> templates) {
            items.addAll(templates);
        }

        void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
            this.itemTouchHelper = itemTouchHelper;
        }

        void move(int fromPosition, int toPosition) {
            if (fromPosition < 0
                    || toPosition < 0
                    || fromPosition >= items.size()
                    || toPosition >= items.size()
                    || fromPosition == toPosition) {
                return;
            }
            Collections.swap(items, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
        }

        List<String> orderedIds() {
            ArrayList<String> ids = new ArrayList<>(items.size());
            for (QuickTemplateStore.QuickTemplate item : items) {
                ids.add(item.id);
            }
            return ids;
        }

        @NonNull
        @Override
        public SortHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quick_template_sort, parent, false);
            return new SortHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SortHolder holder, int position) {
            QuickTemplateStore.QuickTemplate item = items.get(position);
            holder.name.setText(item.name);
            holder.dragHandle.setOnTouchListener((view, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && itemTouchHelper != null) {
                    itemTouchHelper.startDrag(holder);
                    return true;
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static final class SortHolder extends RecyclerView.ViewHolder {
        final View dragHandle;
        final MaterialTextView name;

        SortHolder(@NonNull View itemView) {
            super(itemView);
            dragHandle = itemView.findViewById(R.id.quick_template_sort_drag_handle);
            name = itemView.findViewById(R.id.quick_template_sort_name);
        }
    }

    private static final class DragCallback extends ItemTouchHelper.Callback {
        private final SortAdapter adapter;

        DragCallback(SortAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder source,
                @NonNull RecyclerView.ViewHolder target) {
            adapter.move(source.getBindingAdapterPosition(), target.getBindingAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }
    }

    private static final class RowSpacingDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        RowSpacingDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect,
                @NonNull View view,
                @NonNull RecyclerView parent,
                @NonNull RecyclerView.State state) {
            if (parent.getChildAdapterPosition(view) > 0) {
                outRect.top = spacing;
            }
        }
    }
}
