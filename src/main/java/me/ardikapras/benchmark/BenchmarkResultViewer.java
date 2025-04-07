package me.ardikapras.benchmark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class BenchmarkResultViewer {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkResultViewer.class);

    public static final String SECONDARY_METRICS = "secondaryMetrics";

    public static void main(String[] args) {
        try {
            String benchmarkResults = Files.readString(Paths.get("benchmark-results.json"));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(benchmarkResults);

            Map<String, Map<String, Double>> parsedResults = parseResults(rootNode);
            printResults(parsedResults);
        } catch (IOException e) {
            log.error("Failed to read or parse benchmark results", e);
        }
    }

    private static Map<String, Map<String, Double>> parseResults(JsonNode rootNode) {
        Map<String, Map<String, Double>> results = new HashMap<>();

        for (JsonNode benchmark : rootNode) {
            String benchmarkName = benchmark.get("benchmark").asText();
            // Extract parser type and data size from benchmark name
            String[] parts = benchmarkName.split("\\.");
            String parserType = parts[parts.length - 1].replace("Parsing", "");
            String paramsNode = benchmark.get("params").get("dataSize").asText();
            String resultsKey = parserType + "_" + paramsNode;

            // Get primary score (execution time)
            double score = benchmark.get("primaryMetric").get("score").asDouble();

            // Get memory usage from secondary metrics if available
            Double memoryUsage = null;
            if (benchmark.has(SECONDARY_METRICS) &&
                    benchmark.get(SECONDARY_METRICS).has("heap.used.MB")) {
                memoryUsage = benchmark.get(SECONDARY_METRICS)
                        .get("heap.used.MB")
                        .get("score").asDouble();
            }

            Map<String, Double> metrics = new HashMap<>();
            metrics.put("time_ms", score);
            if (memoryUsage != null) {
                metrics.put("memory_mb", memoryUsage);
            }

            results.put(resultsKey, metrics);
        }

        log.debug("Parsed {} benchmark results", results.size());
        return results;
    }

    private static void printResults(Map<String, Map<String, Double>> results) {
        log.info("Displaying benchmark results summary");

        StringBuilder table = new StringBuilder();
        table.append("Benchmark Results Summary:\n");
        table.append("==========================\n\n");

        // Format as a table
        table.append(String.format("%-15s %-10s %-15s %-15s%n", "Parser", "Data Size", "Time (ms)", "Memory (MB)"));
        table.append("-------------------------------------------------------------\n");

        for (Map.Entry<String, Map<String, Double>> entry : results.entrySet()) {
            String[] parts = entry.getKey().split("_");
            String parser = parts[0];
            String dataSize = parts[1];

            Map<String, Double> metrics = entry.getValue();
            Double time = metrics.get("time_ms");
            Double memory = metrics.get("memory_mb");

            table.append(String.format("%-15s %-10s %-15.2f %-15s%n",
                    parser,
                    dataSize,
                    time,
                    memory != null ? memory.toString() : "N/A"));
        }

        table.append("\nNote: Lower values are better for both time and memory usage.");

        // Use logger to output the table
        log.info("\n{}", table);
    }
}
