package com.dpis.module;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class QuickTemplateTargetAdapter
        extends RecyclerView.Adapter<QuickTemplateTargetAdapter.TargetHolder> {
    interface SelectionListener {
        void onSelectionChanged(String packageName, boolean selected);
    }

    private final ArrayList<QuickTemplateTargetSelectionActivity.TargetAppItem> items =
            new ArrayList<>();
    private final Set<String> selectedPackages;
    private final SelectionListener selectionListener;

    QuickTemplateTargetAdapter(Set<String> selectedPackages, SelectionListener selectionListener) {
        this.selectedPackages = selectedPackages != null
                ? selectedPackages
                : new LinkedHashSet<>();
        this.selectionListener = selectionListener;
    }

    void submit(List<QuickTemplateTargetSelectionActivity.TargetAppItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TargetHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_template_target_app, parent, false);
        return new TargetHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TargetHolder holder, int position) {
        QuickTemplateTargetSelectionActivity.TargetAppItem item = items.get(position);
        holder.label.setText(item.label);
        holder.packageName.setText(item.packageName);
        holder.configuredBadge.setVisibility(item.configured ? View.VISIBLE : View.GONE);
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(selectedPackages.contains(item.packageName));
        holder.root.setOnClickListener(v -> {
            boolean selected = !selectedPackages.contains(item.packageName);
            holder.checkbox.setChecked(selected);
            if (selectionListener != null) {
                selectionListener.onSelectionChanged(item.packageName, selected);
            }
        });
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (selectionListener != null) {
                selectionListener.onSelectionChanged(item.packageName, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class TargetHolder extends RecyclerView.ViewHolder {
        final View root;
        final MaterialTextView label;
        final MaterialTextView packageName;
        final MaterialTextView configuredBadge;
        final MaterialCheckBox checkbox;

        TargetHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView;
            label = itemView.findViewById(R.id.quick_template_target_label);
            packageName = itemView.findViewById(R.id.quick_template_target_package);
            configuredBadge = itemView.findViewById(R.id.quick_template_target_configured_badge);
            checkbox = itemView.findViewById(R.id.quick_template_target_checkbox);
        }
    }
}
