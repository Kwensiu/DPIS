package com.dpis.module;

import android.os.Parcelable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

final class AppListPagerAdapter extends RecyclerView.Adapter<AppListPagerAdapter.PageHolder> {
    interface OnAppClickListener {
        void onAppClicked(AppListItem item);
    }

    interface OnRefreshListener {
        void onRefresh(AppListPage page);
    }

    private final EnumMap<AppListPage, List<AppListItem>> pages = new EnumMap<>(AppListPage.class);
    private final EnumMap<AppListPage, Parcelable> pageScrollStates = new EnumMap<>(AppListPage.class);
    private final EnumMap<AppListPage, Boolean> refreshingStates = new EnumMap<>(AppListPage.class);
    private final EnumMap<AppListPage, PageHolder> activeHolders = new EnumMap<>(AppListPage.class);
    private final OnAppClickListener onAppClickListener;
    private final OnRefreshListener onRefreshListener;

    AppListPagerAdapter(OnAppClickListener onAppClickListener, OnRefreshListener onRefreshListener) {
        this.onAppClickListener = onAppClickListener;
        this.onRefreshListener = onRefreshListener;
        for (AppListPage page : AppListPage.values()) {
            pages.put(page, new ArrayList<>());
            refreshingStates.put(page, false);
        }
    }

    void submitPage(AppListPage page, List<AppListItem> items) {
        pages.put(page, new ArrayList<>(items));
        notifyItemChanged(page.position());
    }

    SparseArray<Parcelable> capturePageScrollStates() {
        for (AppListPage page : AppListPage.values()) {
            PageHolder holder = activeHolders.get(page);
            if (holder != null) {
                pageScrollStates.put(page, holder.captureScrollState());
            }
        }
        SparseArray<Parcelable> states = new SparseArray<>();
        for (AppListPage page : AppListPage.values()) {
            Parcelable pageState = pageScrollStates.get(page);
            if (pageState != null) {
                states.put(page.position(), pageState);
            }
        }
        return states;
    }

    void restorePageScrollStates(SparseArray<Parcelable> states) {
        pageScrollStates.clear();
        if (states == null) {
            return;
        }
        for (int i = 0; i < states.size(); i++) {
            int position = states.keyAt(i);
            AppListPage page = AppListPage.fromPosition(position);
            pageScrollStates.put(page, states.valueAt(i));
        }
    }

    void setRefreshing(AppListPage page, boolean refreshing) {
        refreshingStates.put(page, refreshing);
        PageHolder holder = activeHolders.get(page);
        if (holder != null) {
            holder.setRefreshing(refreshing);
        }
    }

    @Override
    public PageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_list_page, parent, false);
        return new PageHolder(view, onAppClickListener, onRefreshListener);
    }

    @Override
    public void onBindViewHolder(PageHolder holder, int position) {
        AppListPage page = AppListPage.fromPosition(position);
        AppListPage previousPage = holder.getBoundPage();
        if (previousPage != null && previousPage != page) {
            pageScrollStates.put(previousPage, holder.captureScrollState());
            activeHolders.remove(previousPage);
        }
        activeHolders.put(page, holder);
        holder.bind(
                page,
                pages.get(page),
                pageScrollStates.get(page),
                Boolean.TRUE.equals(refreshingStates.get(page))
        );
        pageScrollStates.remove(page);
    }

    @Override
    public void onViewRecycled(@NonNull PageHolder holder) {
        AppListPage page = holder.getBoundPage();
        if (page != null) {
            pageScrollStates.put(page, holder.captureScrollState());
            activeHolders.remove(page);
        }
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return AppListPage.values().length;
    }

    static final class PageHolder extends RecyclerView.ViewHolder {
        private final SwipeRefreshLayout swipeRefreshLayout;
        private final RecyclerView recyclerView;
        private final PageListAdapter adapter;
        private AppListPage boundPage;

        PageHolder(View itemView,
                   OnAppClickListener onAppClickListener,
                   OnRefreshListener onRefreshListener) {
            super(itemView);
            swipeRefreshLayout = itemView.findViewById(R.id.page_swipe_refresh);
            recyclerView = itemView.findViewById(R.id.page_list);
            recyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            adapter = new PageListAdapter(onAppClickListener);
            recyclerView.setAdapter(adapter);
            swipeRefreshLayout.setOnRefreshListener(() -> {
                if (boundPage != null) {
                    onRefreshListener.onRefresh(boundPage);
                }
            });
        }

        void bind(AppListPage page,
                  List<AppListItem> items,
                  Parcelable scrollState,
                  boolean refreshing) {
            boundPage = page;
            adapter.submit(items);
            setRefreshing(refreshing);
            if (scrollState != null) {
                restoreScrollState(scrollState);
            }
        }

        AppListPage getBoundPage() {
            return boundPage;
        }

        void setRefreshing(boolean refreshing) {
            swipeRefreshLayout.setRefreshing(refreshing);
        }

        Parcelable captureScrollState() {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager == null) {
                return null;
            }
            return layoutManager.onSaveInstanceState();
        }

        void restoreScrollState(Parcelable state) {
            recyclerView.post(() -> {
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    layoutManager.onRestoreInstanceState(state);
                }
            });
        }
    }

    private static final class PageListAdapter extends ListAdapter<AppListItem, RowHolder> {
        private final OnAppClickListener onAppClickListener;

        private PageListAdapter(OnAppClickListener onAppClickListener) {
            super(DIFF_CALLBACK);
            this.onAppClickListener = onAppClickListener;
        }

        private void submit(List<AppListItem> newItems) {
            submitList(new ArrayList<>(newItems));
        }

        @NonNull
        @Override
        public RowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_app_entry, parent, false);
            return new RowHolder(view);
        }

        @Override
        public void onBindViewHolder(RowHolder holder, int position) {
            AppListItem item = getItem(position);

            holder.label.setText(item.label);
            holder.packageName.setText(item.packageName);
            holder.icon.setImageDrawable(item.icon);
            holder.status.setText(AppStatusFormatter.format(
                    item.inScope, item.viewportWidthDp, item.fontScalePercent, item.fontMode));
            holder.headerClickTarget.setOnClickListener(v -> onAppClickListener.onAppClicked(item));
        }

        private static final DiffUtil.ItemCallback<AppListItem> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<AppListItem>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull AppListItem oldItem,
                                                   @NonNull AppListItem newItem) {
                        return oldItem.packageName.equals(newItem.packageName);
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull AppListItem oldItem,
                                                      @NonNull AppListItem newItem) {
                        return oldItem.label.equals(newItem.label)
                                && oldItem.inScope == newItem.inScope
                                && Objects.equals(oldItem.viewportWidthDp, newItem.viewportWidthDp)
                                && Objects.equals(oldItem.fontScalePercent, newItem.fontScalePercent)
                                && oldItem.fontMode.equals(newItem.fontMode)
                                && oldItem.systemApp == newItem.systemApp;
                    }
                };
    }

    private static final class RowHolder extends RecyclerView.ViewHolder {
        final View headerClickTarget;
        final ImageView icon;
        final MaterialTextView label;
        final MaterialTextView packageName;
        final MaterialTextView status;
        private RowHolder(View root) {
            super(root);
            headerClickTarget = root.findViewById(R.id.header_click_target);
            icon = root.findViewById(R.id.app_icon);
            label = root.findViewById(R.id.app_label);
            packageName = root.findViewById(R.id.app_package);
            status = root.findViewById(R.id.app_status);
        }
    }
}
