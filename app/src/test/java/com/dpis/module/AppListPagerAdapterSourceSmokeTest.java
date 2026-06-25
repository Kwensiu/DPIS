package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class AppListPagerAdapterSourceSmokeTest {
    @Test
    public void pageListAdapter_usesListAdapterAndDiffUtil() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppListPagerAdapter.java");

        assertTrue(source.contains("interface OnPageListScrollListener"));
        assertTrue(source.contains("interface OnIconResolveRequestListener"));
        assertTrue(source.contains("extends ListAdapter<AppListItem, RowHolder>"));
        assertTrue(source.contains("DiffUtil.ItemCallback<AppListItem>"));
        assertTrue(source.contains("submitList(newItems);"));
        assertTrue(source.contains("submitList(newItems, onCommitted);"));
        assertTrue(!source.contains("notifyDataSetChanged()"));
        assertTrue(source.contains("capturePageScrollStates()"));
        assertTrue(source.contains("restorePageScrollStates("));
        assertTrue(source.contains("setRefreshing(AppListPage page, boolean refreshing)"));
        assertTrue(source.contains("recyclerView.addOnScrollListener"));
        assertTrue(source.contains("onPageListScrollListener.onPageListScrolled("));
    }

    @Test
    public void submitPage_updatesActiveHolderWithoutPageLevelRebind() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppListPagerAdapter.java");

        assertTrue(source.contains("PageHolder holder = activeHolders.get(page);"));
        assertTrue(source.contains("holder.submitItems(snapshot);"));
        assertTrue(!source.contains("notifyItemChanged(page.position())"));
    }

    @Test
    public void statusRefresh_limitsUpdateToVisibleRowsAndDisablesChangeAnimations() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppListPagerAdapter.java");

        assertTrue(source.contains("setSupportsChangeAnimations(false)"));
        assertTrue(source.contains("adapter.submit(items, this::refreshStatuses);"));
        assertTrue(source.contains("findFirstVisibleItemPosition()"));
        assertTrue(source.contains("findLastVisibleItemPosition()"));
        assertTrue(source.contains("notifyItemRangeChanged(start, end - start + 1, PAYLOAD_SYSTEM_SCOPE_CHANGED);"));
    }

    @Test
    public void missingIcon_usesSkeletonAndRequestsAsyncResolve() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppListPagerAdapter.java");

        assertTrue(source.contains("holder.iconSkeleton.setVisibility(View.VISIBLE);"));
        assertTrue(source.contains("holder.iconSkeleton.setVisibility(View.GONE);"));
        assertTrue(source.contains("onIconResolveRequestListener.onIconResolveRequested(item.packageName);"));
        assertTrue(source.contains("(oldItem.icon != null) == (newItem.icon != null)"));
    }

    @Test
    public void diffCallback_comparesTypefaceId() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppListPagerAdapter.java");

        assertTrue(source.contains("Objects.equals(oldItem.typefaceId, newItem.typefaceId)"));
    }

    @Test
    public void diffCallback_comparesInactiveViewportScaleDraft() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppListPagerAdapter.java");

        assertTrue(source.contains(
                "Objects.equals(oldItem.viewportScaleMilliPercent, newItem.viewportScaleMilliPercent)"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
