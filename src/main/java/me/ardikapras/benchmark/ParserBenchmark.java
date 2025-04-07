package me.ardikapras.benchmark;

import me.ardikapras.model.Product;
import me.ardikapras.parser.JaxbParser;
import me.ardikapras.parser.StAXParser;
import me.ardikapras.utils.FileUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3)
@Measurement(iterations = 3)
public class ParserBenchmark {
    private static final Logger log = LoggerFactory.getLogger(ParserBenchmark.class);

    @Param({"1", "10", "100"})
    private int dataSize;

    private File gzipFile;
    private File unzippedFile;
    private JaxbParser jaxbParser;
    private StAXParser staxParser;

    @Setup
    public void setup() throws IOException {
        // Initialize parsers
        jaxbParser = new JaxbParser();
        staxParser = new StAXParser();

        // Locate gzipped test file
        String fileName = String.format("products_%d.xml.gz", dataSize);
        gzipFile = new File(fileName);

        if (!gzipFile.exists()) {
            throw new RuntimeException("Test file " + fileName + " not found. Please generate it first.");
        }

        // Extract file before benchmarks
        log.info("Extracting file: {}", gzipFile.getName());
        unzippedFile = FileUtils.unzipToTemp(gzipFile);
        log.info("Extracted to: {}", unzippedFile.getAbsolutePath());
    }

    @TearDown
    public void tearDown() {
        // Clean up the temporary unzipped file
        if (unzippedFile != null && unzippedFile.exists()) {
            log.info("Deleting temporary file: {}", unzippedFile.getAbsolutePath());
            FileUtils.deleteQuietly(unzippedFile);
        }
    }

    @Benchmark
    public void jaxbParsing(Blackhole blackhole) {
        List<Product> products = jaxbParser.parse(unzippedFile);
        blackhole.consume(products);
    }

    @Benchmark
    public void staxParsing(Blackhole blackhole) {
        AtomicInteger count = new AtomicInteger(0);
        staxParser.parseWithCallback(unzippedFile, batch -> {
            count.addAndGet(batch.size());
            blackhole.consume(batch);
        });
        blackhole.consume(count.get());
    }
}
