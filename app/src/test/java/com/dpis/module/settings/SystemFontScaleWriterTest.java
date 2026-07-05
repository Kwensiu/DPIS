package com.dpis.module.settings;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SystemFontScaleWriterTest {
    @Test
    public void falseWriteReportsFailureWithoutSuccess() {
        RecordingHost host = new RecordingHost(false, false);

        SystemFontScaleWriter.write(host, 120);

        assertEquals(1, host.writeCalls);
        assertEquals(0, host.successCalls);
        assertEquals(1, host.failureCalls);
    }

    @Test
    public void throwingWriteReportsFailureWithoutSuccess() {
        RecordingHost host = new RecordingHost(true, true);

        SystemFontScaleWriter.write(host, 120);

        assertEquals(1, host.writeCalls);
        assertEquals(0, host.successCalls);
        assertEquals(1, host.failureCalls);
    }

    @Test
    public void successfulWriteReportsSuccess() {
        RecordingHost host = new RecordingHost(true, false);

        SystemFontScaleWriter.write(host, 120);

        assertEquals(1, host.writeCalls);
        assertEquals(1, host.successCalls);
        assertEquals(0, host.failureCalls);
        assertEquals(120, host.lastSuccessPercent);
    }

    private static final class RecordingHost implements SystemFontScaleWriter.Host {
        private final boolean writeResult;
        private final boolean throwOnWrite;
        int writeCalls;
        int successCalls;
        int failureCalls;
        int lastSuccessPercent;

        RecordingHost(boolean writeResult, boolean throwOnWrite) {
            this.writeResult = writeResult;
            this.throwOnWrite = throwOnWrite;
        }

        @Override
        public boolean writePercent(int percent) {
            writeCalls++;
            if (throwOnWrite) {
                throw new RuntimeException("write failed");
            }
            return writeResult;
        }

        @Override
        public void onWriteSucceeded(int percent) {
            successCalls++;
            lastSuccessPercent = percent;
        }

        @Override
        public void onWriteFailed() {
            failureCalls++;
        }
    }
}
