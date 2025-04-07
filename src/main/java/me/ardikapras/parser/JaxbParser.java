package me.ardikapras.parser;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import me.ardikapras.model.Merchandiser;
import me.ardikapras.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JaxbParser {
    private static final Logger log = LoggerFactory.getLogger(JaxbParser.class);
    public List<Product> parse(File file) {
        JAXBContext jaxbContext;
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)
        ) {
            jaxbContext = JAXBContext.newInstance(Merchandiser.class);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            final Merchandiser merchandiser = (Merchandiser) unmarshaller.unmarshal(file);
            return new ArrayList<>(merchandiser.getProducts());
        } catch (IOException e) {
            log.error("IO error while parsing file: {}", file.getName(), e);
            return Collections.emptyList();
        } catch (JAXBException e) {
            log.error("JAXB parsing failed for file: {}", file.getName(), e);
            return Collections.emptyList();
        }
    }
}
