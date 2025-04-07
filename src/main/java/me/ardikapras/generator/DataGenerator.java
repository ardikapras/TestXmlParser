package me.ardikapras.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

public class DataGenerator {
    private static final Logger log = LoggerFactory.getLogger(DataGenerator.class);
    private static final String[] BRANDS = {"Nike", "Adidas", "Puma", "Under Armour", "New Balance"};
    private static final String[] CATEGORIES = {"Shoes", "Shirts", "Pants", "Accessories", "Equipment"};
    private static final String[] COLORS = {"Red", "Blue", "Black", "White", "Green"};
    private static final String[] SIZES = {"S", "M", "L", "XL", "XXL"};

    private final Random random;
    private final XMLOutputFactory xmlFactory;

    public DataGenerator() {
        this.random = new Random();
        this.xmlFactory = XMLOutputFactory.newInstance();
        // Disable external entities for security
        this.xmlFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    }

    public void generateFile(File outputFile, int productCount) {
        log.info("Starting generation of {} products to file: {}", productCount, outputFile.getName());

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos, 8192);
             OutputStreamWriter writer = new OutputStreamWriter(gzos, StandardCharsets.UTF_8)) {

            XMLStreamWriter xml = xmlFactory.createXMLStreamWriter(writer);

            // Start document
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("merchandiser");

            // Write header
            writeHeader(xml);

            // Write products
            for (int i = 0; i < productCount; i++) {
                writeProduct(xml, i);

                if (i > 0 && i % 10000 == 0) {
                    log.info("Generated {} products", i);
                }
            }

            // End document
            xml.writeEndElement(); // merchandiser
            xml.writeEndDocument();
            xml.flush();
            xml.close();

            log.info("Completed generating {} products", productCount);
            log.info("Output file size: {} MB", outputFile.length() / (1024.0 * 1024.0));

        } catch (Exception e) {
            log.error("Failed to generate file: {}", outputFile.getName(), e);
        }
    }

    private void writeHeader(XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("header");
        writeElement(xml, "merchantId", "TEST_MERCHANT_001");
        writeElement(xml, "merchantName", "Test Merchant");
        xml.writeEndElement();
    }

    private void writeProduct(XMLStreamWriter xml, int index) throws XMLStreamException {
        xml.writeStartElement("product");

        // Write attributes
        xml.writeAttribute("id", String.format("PROD_%06d", index));
        xml.writeAttribute("sku", String.format("SKU_%06d", index));

        // Write elements
        String brand = BRANDS[random.nextInt(BRANDS.length)];
        String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
        String color = COLORS[random.nextInt(COLORS.length)];
        String size = SIZES[random.nextInt(SIZES.length)];
        double price = 20.0 + random.nextDouble() * 180.0;

        writeElement(xml, "name", String.format("%s %s - %s", brand, category, color));
        writeElement(xml, "brand", brand);
        writeElement(xml, "category", category);
        writeElement(xml, "color", color);
        writeElement(xml, "size", size);
        writeElement(xml, "price", String.format("%.2f", price));
        writeElement(xml, "sale_price", String.format("%.2f", price * 0.8));
        writeElement(xml, "product_url", "https://example.com/product/" + index);
        writeElement(xml, "image_url", "https://example.com/images/" + index + ".jpg");
        writeElement(xml, "description", "Sample product description for " + index);

        xml.writeEndElement();
    }

    private void writeElement(XMLStreamWriter xml, String name, String value) throws XMLStreamException {
        xml.writeStartElement(name);
        xml.writeCharacters(value);
        xml.writeEndElement();
    }
}
