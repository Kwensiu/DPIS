package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppLoadCoordinatorTest {
    @Test
    public void firstRequest_startsLoadImmediately() {
        AppLoadCoordinator coordinator = new AppLoadCoordinator();

        int requestId = coordinator.onLoadRequested();

        assertEquals(1, requestId);
    }

    @Test
    public void olderResult_isDroppedWhenNewerRequestQueued() {
        AppLoadCoordinator coordinator = new AppLoadCoordinator();
        int first = coordinator.onLoadRequested();
        int second = coordinator.onLoadRequested();

        assertEquals(AppLoadCoordinator.NO_REQUEST, second);

        AppLoadCoordinator.LoadCompletion firstCompletion = coordinator.onLoadFinished(first);
        assertFalse(firstCompletion.shouldApplyResult);
        assertEquals(2, firstCompletion.nextRequestId);

        AppLoadCoordinator.LoadCompletion secondCompletion =
                coordinator.onLoadFinished(firstCompletion.nextRequestId);
        assertTrue(secondCompletion.shouldApplyResult);
        assertEquals(AppLoadCoordinator.NO_REQUEST, secondCompletion.nextRequestId);
    }

    @Test
    public void repeatedRequestsDuringLoad_coalesceIntoSingleFollowUp() {
        AppLoadCoordinator coordinator = new AppLoadCoordinator();
        int first = coordinator.onLoadRequested();

        coordinator.onLoadRequested();
        coordinator.onLoadRequested();
        coordinator.onLoadRequested();

        AppLoadCoordinator.LoadCompletion firstCompletion = coordinator.onLoadFinished(first);
        assertFalse(firstCompletion.shouldApplyResult);
        assertEquals(4, firstCompletion.nextRequestId);

        AppLoadCoordinator.LoadCompletion secondCompletion =
                coordinator.onLoadFinished(firstCompletion.nextRequestId);
        assertTrue(secondCompletion.shouldApplyResult);
        assertEquals(AppLoadCoordinator.NO_REQUEST, secondCompletion.nextRequestId);
    }
}
