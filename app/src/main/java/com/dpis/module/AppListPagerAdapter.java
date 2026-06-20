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
    private boolean swipeRefreshEnabled = true;

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

    void setSwipeRefreshEnabled(boolean enabled) {
        swipeRefreshEnabled = enabled;
        for (PageHolder holder : activeHolders.values()) {
            holder.setSwipeRefreshEnabled(enabled);
        }
    }

    @Override
    public PageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_list_page, parent, false);
        return new PageHolder(view, onAppClickListener, onRefreshListener,
                onPageListScrollListener, onIconResolveRequestListener,
                systemScopeSelectedSupplier);
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
                Boolean.TRUE.equals(refreshingStates.get(page)));
        holder.setSwipeRefreshEnabled(swipeRefreshEnabled);
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
        private final AppListPageController controller;

        PageHolder(View itemView,
                OnAppClickListener onAppClickListener,
                OnRefreshListener onRefreshListener,
                OnPageListScrollListener onPageListScrollListener,
                OnIconResolveRequestListener onIconResolveRequestListener,
                BooleanSupplier systemScopeSelectedSupplier) {
            super(itemView);
            controller = new AppListPageController(
                    itemView.findViewById(R.id.page_swipe_refresh),
                    itemView.findViewById(R.id.page_list),
                    onAppClickListener,
                    onRefreshListener,
                    onPageListScrollListener,
                    onIconResolveRequestListener,
                    systemScopeSelectedSupplier);
        }

        void bind(AppListPage page,
                List<AppListItem> items,
                Parcelable scrollState,
                boolean refreshing) {
            controller.bind(page, items, scrollState, refreshing);
        }

        void submitItems(List<AppListItem> items) {
            controller.submitItems(items);
        }

        AppListPage getBoundPage() {
            return controller.getBoundPage();
        }

        void setRefreshing(boolean refreshing) {
            controller.setRefreshing(refreshing);
        }

        void setSwipeRefreshEnabled(boolean enabled) {
            controller.setSwipeRefreshEnabled(enabled);
        }

        void refreshStatuses() {
            controller.refreshStatuses();
        }

        Parcelable captureScrollState() {
            return controller.captureScrollState();
        }

        void restoreScrollState(Parcelable state) {
            controller.restoreScrollState(state);
        }
    }

    static final class AppListPageController {
        private final SwipeRefreshLayout swipeRefreshLayout;
        private final RecyclerView recyclerView;
        private final PageListAdapter adapter;
        private AppListPage boundPage;

        AppListPageController(SwipeRefreshLayout swipeRefreshLayout,
                RecyclerView recyclerView,
                OnAppClickListener onAppClickListener,
                OnRefreshListener onRefreshListener,
                OnPageListScrollListener onPageListScrollListener,
                OnIconResolveRequestListener onIconResolveRequestListener,
                BooleanSupplier systemScopeSelectedSupplier) {
            this.swipeRefreshLayout = swipeRefreshLayout;
            this.recyclerView = recyclerView;
            recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
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
            boolean pageChanged = boundPage != page;
            boundPage = page;
            adapter.submit(items, this::refreshStatuses);
            setRefreshing(refreshing);
            if (pageChanged && scrollState != null) {
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

        void setSwipeRefreshEnabled(boolean enabled) {
            swipeRefreshLayout.setEnabled(enabled);
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
            setHasStableIds(true);
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
            RowHolder holder = new RowHolder(view);
            holder.itemView.setOnClickListener(v -> {
                int position = holder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }
                onAppClickListener.onAppClicked(getItem(position));
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(RowHolder holder, int position) {
            AppListItem item = getItem(position);

            holder.label.setText(item.label);
            holder.packageName.setText(item.packageName);
            bindIcon(holder, item);
            String compactStatusText = AppStatusFormatter.formatCompact(
                    holder.status.getResources(), statusInput(item));
            boolean warnViewport = item.scopeKnown && AppStatusFormatter.shouldWarnViewportEmulation(
                    item.viewportTargetSpec, item.viewportMode,
                    systemScopeSelectedSupplier.getAsBoolean(),
                    item.dpisEnabled);
            boolean warnFont = item.scopeKnown && AppStatusFormatter.shouldWarnFontEmulation(
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
        }

        @Override
        public long getItemId(int position) {
            return stablePackageId(getItem(position).packageName);
        }

        private static long stablePackageId(String packageName) {
            long hash = 1125899906842597L;
            String value = packageName != null ? packageName : "";
            for (int i = 0; i < value.length(); i++) {
                hash = 31L * hash + value.charAt(i);
            }
            return hash;
        }

        private static AppStatusFormatter.StatusInput statusInput(AppListItem item) {
            return new AppStatusFormatter.StatusInput(
                    item.inScope,
                    item.scopeKnown,
                    item.installed,
                    item.viewportTargetSpec,
                    item.viewportMode,
                    item.fontScalePercent,
                    item.fontMode,
                    item.typefaceId,
                    item.dpisEnabled,
                    item.hasAppSpecificConfig(),
                    item.wechatDpi);
        }

        @Override
        public void onBindViewHolder(@NonNull RowHolder holder,
                int position,
                @NonNull List<Object> payloads) {
            if (!payloads.isEmpty()) {
                AppListItem item = getItem(position);
                String compactStatusText = AppStatusFormatter.formatCompact(
                        holder.status.getResources(), statusInput(item));
                boolean warnViewport = item.scopeKnown && AppStatusFormatter.shouldWarnViewportEmulation(
                        item.viewportTargetSpec, item.viewportMode,
                        systemScopeSelectedSupplier.getAsBoolean(),
                        item.dpisEnabled);
                boolean warnFont = item.scopeKnown && AppStatusFormatter.shouldWarnFontEmulation(
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

        private static final DiffUtil.ItemCallback<AppListItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<AppListItem>() {
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
                        && oldItem.scopeKnown == newItem.scopeKnown
                        && Objects.equals(oldItem.viewportWidthDp, newItem.viewportWidthDp)
                        && Objects.equals(oldItem.viewportScalePermille, newItem.viewportScalePermille)
                        && Objects.equals(oldItem.viewportTargetSpec, newItem.viewportTargetSpec)
                        && oldItem.viewportMode.equals(newItem.viewportMode)
                        && Objects.equals(oldItem.fontScalePercent, newItem.fontScalePercent)
                        && oldItem.fontMode.equals(newItem.fontMode)
                        && Objects.equals(oldItem.typefaceId, newItem.typefaceId)
                        && oldItem.appSpecificConfigActive == newItem.appSpecificConfigActive
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
