/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (@LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.ui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class SapoToolWindowBenchmarkTest {

    private static final NumberFormat STATIC_FORMAT = NumberFormat.getInstance(Locale.ROOT);
    private static final ThreadLocal<NumberFormat> THREAD_LOCAL_FORMAT = ThreadLocal.withInitial(() -> NumberFormat.getInstance(Locale.ROOT));
    private static final int ITERATIONS = 100000;
    private static final String SCORE = "7.5";

    @Test
    public void benchmarkNumberFormatPerformance() throws ParseException {
        // Warmup
        for (int i = 0; i < 1000; i++) {
            runUnoptimized();
            runStatic();
            runThreadLocal();
        }

        // Unoptimized run
        long startUnopt = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            runUnoptimized();
        }
        long durationUnopt = System.nanoTime() - startUnopt;

        // Static run
        long startStatic = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            runStatic();
        }
        long durationStatic = System.nanoTime() - startStatic;

        // ThreadLocal run
        long startTL = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            runThreadLocal();
        }
        long durationTL = System.nanoTime() - startTL;

        System.out.printf("Unoptimized (new instance): %.2f ms%n", durationUnopt / 1_000_000.0);
        System.out.printf("Static instance:           %.2f ms%n", durationStatic / 1_000_000.0);
        System.out.printf("ThreadLocal instance:      %.2f ms%n", durationTL / 1_000_000.0);

        double improvement = (double) durationUnopt / durationTL;
        System.out.printf("ThreadLocal Speedup: %.2fx%n", improvement);

        // Assert that ThreadLocal is significantly faster than unoptimized
        Assertions.assertTrue(durationTL < durationUnopt, "ThreadLocal version should be faster");
    }

    private void runUnoptimized() throws ParseException {
        NumberFormat.getInstance(Locale.ROOT).parse(SCORE).doubleValue();
    }

    private void runStatic() throws ParseException {
        STATIC_FORMAT.parse(SCORE).doubleValue();
    }

    private void runThreadLocal() throws ParseException {
        THREAD_LOCAL_FORMAT.get().parse(SCORE).doubleValue();
    }
}
