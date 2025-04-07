package me.ardikapras.parser;

import me.ardikapras.parser.helper.MapPool;
import me.ardikapras.parser.helper.ProductHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class StAXParser {
    private static final Logger log = LoggerFactory.getLogger(StAXParser.class);
    private static final XMLInputFactory xmlInputFactory;
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_BATCH_SIZE = 5000;

    static {
        xmlInputFactory = XMLInputFactory.newInstance();
        // Combine properties setting for better readability
        Map<String, Object> properties = Map.of(
                XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false,
                XMLInputFactory.SUPPORT_DTD, false,
                XMLInputFactory.IS_COALESCING, true,
                XMLInputFactory.IS_NAMESPACE_AWARE, false
        );
        properties.forEach(xmlInputFactory::setProperty);
    }

    private final MapPool mapPool;

    public StAXParser() {
        this.mapPool = new MapPool(MAX_BATCH_SIZE * 2);
    }

    public List<Map<String, String>> parse(File input) {
        List<Map<String, String>> results = new ArrayList<>();
        parseWithCallback(input, results::addAll);
        return results;
    }

    public void parseWithCallback(File input, Consumer<List<Map<String, String>>> batchProcessor) {
        List<Map<String, String>> currentBatch = new ArrayList<>(MAX_BATCH_SIZE);

        try (var fis = new FileInputStream(input);
             var bis = new BufferedInputStream(fis, BUFFER_SIZE)) {

            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(bis);
            ProductHandler handler = createProductHandler(currentBatch, batchProcessor);

            while (reader.hasNext()) {
                processEvent(reader, handler);
            }

            processFinalBatch(currentBatch, batchProcessor);

        } catch (Exception e) {
            log.error("Failed to parse: {}", e.getMessage());
        } finally {
            mapPool.clear();
        }
    }

    private ProductHandler createProductHandler(List<Map<String, String>> batch,
                                                Consumer<List<Map<String, String>>> processor) {
        return new ProductHandler(mapPool, product -> {
            batch.add(product);
            if (batch.size() >= MAX_BATCH_SIZE) {
                processor.accept(new ArrayList<>(batch));
                batch.clear();
            }
        });
    }

    private void processFinalBatch(List<Map<String, String>> batch,
                                   Consumer<List<Map<String, String>>> processor) {
        if (!batch.isEmpty()) {
            processor.accept(new ArrayList<>(batch));
        }
    }

    private void processEvent(XMLStreamReader reader, ProductHandler handler) throws XMLStreamException {
        switch (reader.next()) {
            case XMLStreamConstants.START_ELEMENT -> handler.startElement(reader);
            case XMLStreamConstants.CHARACTERS -> handler.characters(reader);
            case XMLStreamConstants.END_ELEMENT -> handler.endElement(reader);
            default -> { /* do nothing */ }
        }
    }
}
