package me.ardikapras.benchmark.profiler;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ScalarResult;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MemoryProfiler implements InternalProfiler {

    private long beforeHeapUsed;
    private long beforeHeapCommitted;
    private long beforeNonHeapUsed;
    private long gcCountBefore;
    private long gcTimeBefore;

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        // Force GC before measurement
        System.gc();
        System.gc();

        // Record memory usage before benchmark
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        beforeHeapUsed = heapUsage.getUsed();
        beforeHeapCommitted = heapUsage.getCommitted();
        beforeNonHeapUsed = nonHeapUsage.getUsed();

        // Record GC stats
        gcCountBefore = getGcCount();
        gcTimeBefore = getGcTime();
    }

    @Override
    public Collection<? extends Result<?>> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult result) {
        // Force GC again to get stable measurements
        System.gc();
        System.gc();

        // Record memory usage after benchmark
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        long afterHeapUsed = heapUsage.getUsed();
        long afterHeapCommitted = heapUsage.getCommitted();
        long afterNonHeapUsed = nonHeapUsage.getUsed();

        // Calculate differences
        long heapUsedDiff = (afterHeapUsed - beforeHeapUsed) / (1024 * 1024); // MB
        long heapCommittedDiff = (afterHeapCommitted - beforeHeapCommitted) / (1024 * 1024); // MB
        long nonHeapUsedDiff = (afterNonHeapUsed - beforeNonHeapUsed) / (1024 * 1024); // MB

        // GC stats
        long gcCountAfter = getGcCount();
        long gcTimeAfter = getGcTime();
        long gcCountDiff = gcCountAfter - gcCountBefore;
        long gcTimeDiff = gcTimeAfter - gcTimeBefore;

        // Create results
        List<Result<?>> results = new ArrayList<>();
        results.add(new ScalarResult("heap.used.MB", heapUsedDiff, "MB", AggregationPolicy.AVG));
        results.add(new ScalarResult("heap.committed.MB", heapCommittedDiff, "MB", AggregationPolicy.AVG));
        results.add(new ScalarResult("nonheap.used.MB", nonHeapUsedDiff, "MB", AggregationPolicy.AVG));
        results.add(new ScalarResult("gc.count", gcCountDiff, "counts", AggregationPolicy.SUM));
        results.add(new ScalarResult("gc.time.ms", gcTimeDiff, "ms", AggregationPolicy.SUM));

        return results;
    }

    private long getGcCount() {
        long count = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long c = bean.getCollectionCount();
            if (c > -1) {
                count += c;
            }
        }
        return count;
    }

    private long getGcTime() {
        long time = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long t = bean.getCollectionTime();
            if (t > -1) {
                time += t;
            }
        }
        return time;
    }

    @Override
    public String getDescription() {
        return "Memory usage and GC profiler";
    }
}
