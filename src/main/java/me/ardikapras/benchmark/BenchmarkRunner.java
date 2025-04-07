package me.ardikapras.benchmark;

import me.ardikapras.benchmark.profiler.MemoryProfiler;
import me.ardikapras.generator.DataGenerator;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class BenchmarkRunner {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkRunner.class);
    public static void main(String[] args) throws RunnerException {
        // Generate test data files if they don't exist
        generateTestFiles(new int[]{1, 10, 100});

        // Run JMH benchmarks
        Options opt = new OptionsBuilder()
                .include(ParserBenchmark.class.getSimpleName())
                .resultFormat(ResultFormatType.JSON)
                .result("benchmark-results.json")
                .addProfiler(MemoryProfiler.class)
                .build();

        new Runner(opt).run();
    }

    private static void generateTestFiles(int[] sizes) {
        DataGenerator generator = new DataGenerator();

        for (int size : sizes) {
            String fileName = String.format("%s_%d.xml.gz", "products", size);
            File file = new File(fileName);

            if (!file.exists()) {
                log.info("Generating test file: {}", fileName);
                // For the benchmark, we'll use size * 1000 for more realistic data
                generator.generateFile(file, size * 1000);
            } else {
                log.info("Test file already exists: {}", fileName);
            }
        }
    }
}
