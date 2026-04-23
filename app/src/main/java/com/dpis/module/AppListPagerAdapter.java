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
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

final class AppListPagerAdapter extends RecyclerView.Adapter<AppListPagerAdapter.PageHolder> {
    interface OnAppClickListener {
        void onAppClicked(AppListItem item);
    }

    interface OnRefreshListener {
        void onRefresh(AppListPage page);
    }

    interface OnPageListScrollListener {
        void onPageListScrolled(AppListPage page, int dy);
    }

    interface OnIconResolveRequestListener {
        void onIconResolveRequested(String packageName);
    }

    private final EnumMap<AppListPage, List<AppListItem>> pages = new EnumMap<>(AppListPage.class);
    private final EnumMap<AppListPage, Parcelable> pageScrollStates = new EnumMap<>(AppListPage.class);
    private final EnumMap<AppListPage, Boolean> refreshingStates = new EnumMap<>(AppListPage.class);
    private final EnumMap<AppListPage, PageHolder> activeHolders = new EnumMap<>(AppListPage.class);
    private final OnAppClickListener onAppClickListener;
    private final OnRefreshListener onRefreshListener;
    private final OnPageListScrollListener onPageListScrollListener;
    private final OnIconResolveRequestListener onIconResolveRequestListener;
    private final BooleanSupplier systemScopeSelectedSupplier;

    AppListPagerAdapter(OnAppClickListener onAppClickListener,
                        OnRefreshListener onRefreshListener,
                        OnPageListScrollListener onPageListScrollListener,
                        OnIconResolveRequestListener onIconResolveRequestListener,
                        BooleanSupplier systemScopeSelectedSupplier) {
        this.onAppClickListener = onAppClickListener;
        this.onRefreshListener = onRefreshListener;
        this.onPageListScrollListener = onPageListScrollListener;
        this.onIconResolveRequestListener = onIconResolveRequestListener;
        this.systemScopeSelectedSupplier = systemScopeSelectedSupplier;
        for (AppListPage page : AppListPage.values()) {
            pages.put(page, new ArrayList<>());
            refreshingStates.put(page, false);
        }
    }

    void submitPage(AppListPage page, List<AppListItem> items) {
        List<AppListItem> snapshot = new ArrayList<>(items);
        pages.put(page, snapshot);
        PageHolder holder = activeHolders.get(page);
        if (holder != null) {
            holder.submitItems(snapshot);
        }
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

    void refreshVisibleStatuses() {
        for (PageHolder holder : activeHolders.values()) {
            holder.refreshStatuses();
        }
    }

    @Override
    public PageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_list_page, parent, false);
        return new PageHolder(view, onAppClickListener, onRefreshListener,
                onPageListScrollListener, onIconResolveRequestListener, systemScopeSelectedSupplier);
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
                   OnRefreshListener onRefreshListener,
                   OnPageListScrollListener onPageListScrollListener,
                   OnIconResolveRequestListener onIconResolveRequestListener,
                   BooleanSupplier systemScopeSelectedSupplier) {
            super(itemView);
            swipeRefreshLayout = itemView.findViewById(R.id.page_swipe_refresh);
            recyclerView = itemView.findViewById(R.id.page_list);
            recyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            adapter = new PageListAdapter(
                    onAppClickListener,
                    onIconResolveRequestListener,
                    systemScopeSelectedSupplier);
            recyclerView.setAdapter(adapter);
            RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
            if (itemAnimator instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
            }
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (boundPage == null || dy == 0) {
                        return;
                    }
                    onPageListScrollListener.onPageListScrolled(boundPage, dy);
                }
            });
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
            adapter.submit(items, this::refreshStatuses);
            setRefreshing(refreshing);
            if (scrollState != null) {
                restoreScrollState(scrollState);
            }
        }

        void submitItems(List<AppListItem> items) {
            adapter.submit(items);
        }

        AppListPage getBoundPage() {
            return boundPage;
        }

        void setRefreshing(boolean refreshing) {
            swipeRefreshLayout.setRefreshing(refreshing);
        }

        void refreshStatuses() {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (!(layoutManager instanceof LinearLayoutManager)) {
                adapter.refreshVisibleRows(0, adapter.getItemCount() - 1);
                return;
            }
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            int firstVisible = linearLayoutManager.findFirstVisibleItemPosition();
            int lastVisible = linearLayoutManager.findLastVisibleItemPosition();
            adapter.refreshVisibleRows(firstVisible, lastVisible);
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
        private final OnIconResolveRequestListener onIconResolveRequestListener;
        private final BooleanSupplier systemScopeSelectedSupplier;
        private static final Object PAYLOAD_SYSTEM_SCOPE_CHANGED = new Object();

        private PageListAdapter(OnAppClickListener onAppClickListener,
                                OnIconResolveRequestListener onIconResolveRequestListener,
                                BooleanSupplier systemScopeSelectedSupplier) {
            super(DIFF_CALLBACK);
            this.onAppClickListener = onAppClickListener;
            this.onIconResolveRequestListener = onIconResolveRequestListener;
            this.systemScopeSelectedSupplier = systemScopeSelectedSupplier;
        }

        private void submit(List<AppListItem> newItems) {
            submitList(newItems);
        }

        private void submit(List<AppListItem> newItems, Runnable onCommitted) {
            submitList(newItems, onCommitted);
        }

        private void refreshVisibleRows(int firstPosition, int lastPosition) {
            if (getItemCount() <= 0 || firstPosition < 0 || lastPosition < firstPosition) {
                return;
            }
            int start = Math.max(0, firstPosition);
            int end = Math.min(getItemCount() - 1, lastPosition);
            if (end < start) {
                return;
            }
            notifyItemRangeChanged(start, end - start + 1, PAYLOAD_SYSTEM_SCOPE_CHANGED);
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
            bindIcon(holder, item);
            String statusText = AppStatusFormatter.format(
                    item.inScope, item.viewportWidthDp, item.viewportMode,
                    item.fontScalePercent, item.fontMode, item.dpisEnabled);
            String compactStatusText = AppStatusFormatter.toCompactDisplay(statusText);
            boolean warnViewport = AppStatusFormatter.shouldWarnViewportEmulation(
                    item.viewportWidthDp, item.viewportMode,
                    systemScopeSelectedSupplier.getAsBoolean(),
                    item.dpisEnabled);
            boolean warnFont = AppStatusFormatter.shouldWarnFontEmulation(
                    item.fontScalePercent, item.fontMode,
                    systemScopeSelectedSupplier.getAsBoolean(),
                    item.dpisEnabled);
            if (warnViewport || warnFont) {
                int warnColor = MaterialColors.getColor(holder.status,
                        androidx.appcompat.R.attr.colorError);
                holder.status.setText(AppStatusFormatter.applyConfigSegmentsWarnStyle(
                        compactStatusText, warnColor, warnViewport, warnFont));
            } else {
                holder.status.setText(compactStatusText);
            }
            holder.headerClickTarget.setOnClickListener(v -> onAppClickListener.onAppClicked(item));
        }

        @Override
        public void onBindViewHolder(@NonNull RowHolder holder,
                                     int position,
                                     @NonNull List<Object> payloads) {
            if (!payloads.isEmpty()) {
                AppListItem item = getItem(position);
                String statusText = AppStatusFormatter.format(
                        item.inScope, item.viewportWidthDp, item.viewportMode,
                        item.fontScalePercent, item.fontMode, item.dpisEnabled);
                String compactStatusText = AppStatusFormatter.toCompactDisplay(statusText);
                boolean warnViewport = AppStatusFormatter.shouldWarnViewportEmulation(
                        item.viewportWidthDp, item.viewportMode,
                        systemScopeSelectedSupplier.getAsBoolean(),
                        item.dpisEnabled);
                boolean warnFont = AppStatusFormatter.shouldWarnFontEmulation(
                        item.fontScalePercent, item.fontMode,
                        systemScopeSelectedSupplier.getAsBoolean(),
                        item.dpisEnabled);
                if (warnViewport || warnFont) {
                    int warnColor = MaterialColors.getColor(holder.status,
                            androidx.appcompat.R.attr.colorError);
                    holder.status.setText(AppStatusFormatter.applyConfigSegmentsWarnStyle(
                            compactStatusText, warnColor, warnViewport, warnFont));
                } else {
                    holder.status.setText(compactStatusText);
                }
                return;
            }
            super.onBindViewHolder(holder, position, payloads);
        }

        private void bindIcon(RowHolder holder, AppListItem item) {
            if (item.icon != null) {
                holder.icon.setImageDrawable(item.icon);
                holder.iconSkeleton.setVisibility(View.GONE);
                return;
            }
            holder.icon.setImageDrawable(null);
            holder.iconSkeleton.setVisibility(View.VISIBLE);
            onIconResolveRequestListener.onIconResolveRequested(item.packageName);
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
                                && oldItem.viewportMode.equals(newItem.viewportMode)
                                && Objects.equals(oldItem.fontScalePercent, newItem.fontScalePercent)
                                && oldItem.fontMode.equals(newItem.fontMode)
                                && oldItem.dpisEnabled == newItem.dpisEnabled
                                && oldItem.systemApp == newItem.systemApp
                                && (oldItem.icon != null) == (newItem.icon != null);
                    }
                };
    }

    private static final class RowHolder extends RecyclerView.ViewHolder {
        final View headerClickTarget;
        final ImageView icon;
        final View iconSkeleton;
        final MaterialTextView label;
        final MaterialTextView packageName;
        final MaterialTextView status;
        private RowHolder(View root) {
            super(root);
            headerClickTarget = root.findViewById(R.id.header_click_target);
            icon = root.findViewById(R.id.app_icon);
            iconSkeleton = root.findViewById(R.id.app_icon_skeleton);
            label = root.findViewById(R.id.app_label);
            packageName = root.findViewById(R.id.app_package);
            status = root.findViewById(R.id.app_status);
        }
    }
}
