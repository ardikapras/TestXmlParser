package me.ardikapras.parser.helper;

import javax.xml.stream.XMLStreamReader;
import java.util.Map;

public class ProductBuilder {
    private final Map<String, String> product;
    private static final String[] REQUIRED_ATTRS = {"id", "name", "sku", "part_number"};
    private static final String[] PRODUCT_KEYS = {
            "product_id", "product_name", "product_sku", "product_part_number"
    };

    public ProductBuilder(Map<String, String> baseMap) {
        this.product = baseMap;
    }

    public void setBasicInfo(String merchantId, String merchantName) {
        product.put("merchant_id", merchantId);
        product.put("merchant_name", merchantName);
        product.put("asp_id", "1");
    }

    public void processAttributes(XMLStreamReader reader) {
        for (int i = 0; i < REQUIRED_ATTRS.length; i++) {
            String value = reader.getAttributeValue(null, REQUIRED_ATTRS[i]);
            if (value != null && !value.isEmpty()) {
                product.put(PRODUCT_KEYS[i], value);
            }
        }
    }

    public void setValue(String key, String value) {
        product.put(key, value);
    }

    public Map<String, String> build() {
        return product;
    }
}
