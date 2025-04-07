package me.ardikapras.parser.helper;

import javax.xml.stream.XMLStreamReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

public class ProductHandler {
    private final ProductCallback callback;
    private Map<String, String> currentProduct;
    private final StringBuilder currentValue;
    private final Deque<String> elementStack;
    private final Object[] searchPath = new Object[5];
    private final MapPool mapPool;

    // Store merchant info
    private String merchantId;
    private String merchantName;

    // Store temporary currency info for current section
    private String priceCurrency;
    private String discountCurrency;
    private String attributeClassId;

    private static final String TAG_PRODUCT = "product";
    private static final String TAG_PRODUCT_URL = "product_url";
    private static final String TAG_HEADER = "header";
    private static final String TAG_MERCHANT_ID = "merchantId";
    private static final String TAG_MERCHANT_NAME = "merchantName";
    private static final String TAG_PRODUCT_IMAGE = "image_url";
    private static final String TAG_PRODUCT_PRICE = "price";
    private static final String TAG_PRODUCT_DISCOUNT = "discount";
    private static final String TAG_PRODUCT_ATTRIBUTE_CLASS = "attribute_class";

    // Product standard elements
    private static final String TAG_PRODUCT_PRIMARY = "primary";
    private static final String TAG_PRODUCT_SECONDARY = "secondary";
    private static final String TAG_PRODUCT_BRAND = "brand";
    private static final String TAG_PRODUCT_AVAILABILITY = "availability";
    private static final String TAG_PRODUCT_KEYWORDS = "keywords";
    private static final String TAG_PRODUCT_ATTRIBUTE_MISC = "attribute_misc";
    private static final String TAG_PRODUCT_ATTRIBUTE_PRODUCT_TYPE = "attribute_product_type";
    private static final String TAG_PRODUCT_ATTRIBUTE_SIZE = "attribute_size";
    private static final String TAG_PRODUCT_ATTRIBUTE_COLOR = "attribute_color";
    private static final String TAG_PRODUCT_ATTRIBUTE_GENDER = "attribute_gender";
    private static final String TAG_PRODUCT_ATTRIBUTE_MATERIAL = "attribute_material";
    private static final String TAG_PRODUCT_SHORT_DESC = "short_desc";
    private static final String TAG_PRODUCT_LONG_DESC = "long_desc";
    private static final String TAG_PRODUCT_DISCOUNT_TYPE = "discount_type";
    private static final String TAG_PRODUCT_SALE_PRICE = "sale_price";
    private static final String TAG_PRODUCT_RETAIL_PRICE = "retail_price";

    public ProductHandler(MapPool mapPool, ProductCallback callback) {
        this.mapPool = mapPool;
        this.callback = callback;
        this.currentValue = new StringBuilder(200);
        this.elementStack = new ArrayDeque<>();
    }

    private String getCurrentElement() {
        return elementStack.peek();
    }

    private boolean isInPath(String... elements) {
        int searchPathSize = 0;
        // Early exit if we don't have enough elements
        if (elements.length > elementStack.size()) {
            return false;
        }

        // Convert stack to array for faster access
        searchPathSize = 0;
        for (String elem : elementStack) {
            searchPath[searchPathSize++] = elem;
            if (searchPathSize >= searchPath.length) break;
        }

        // Check from end of stack (most recent elements)
        int stackIdx = searchPathSize - 1;
        for (int i = elements.length - 1; i >= 0; i--) {
            boolean found = false;
            String target = elements[i];

            // Search backwards in stack until we find the element
            while (stackIdx >= 0) {
                if (target.equals(searchPath[stackIdx])) {
                    found = true;
                    break;
                }
                stackIdx--;
            }

            if (!found) {
                return false;
            }
            stackIdx--; // Move to next position to search
        }
        return true;
    }

    public void startElement(XMLStreamReader reader) {
        String qName = reader.getLocalName();
        elementStack.push(qName);

        if (qName.equals(TAG_PRODUCT) && !isInPath(TAG_PRODUCT, TAG_PRODUCT_URL)) {
            initializeNewProduct(reader);
        } else {
            processElementAttributes(qName, reader);
        }

        currentValue.setLength(0);
    }

    private void initializeNewProduct(XMLStreamReader reader) {
        currentProduct = mapPool.borrowMap();
        currentProduct.put("merchant_id", merchantId);
        currentProduct.put("merchant_name", merchantName);
        currentProduct.put("asp_id", "1"); // Assuming this was Source.LINKSHARE.ordinal()

        String[] requiredAttrs = {
                "id",
                "name",
                "sku",
                "part_number"
        };

        String[] productKeys = {
                "product_id",
                "product_name",
                "product_sku",
                "product_part_number"
        };

        for (int i = 0; i < requiredAttrs.length; i++) {
            String value = reader.getAttributeValue(null, requiredAttrs[i]);
            if (value != null && !value.isEmpty()) {
                currentProduct.put(productKeys[i], value);
            }
        }
    }

    private void processElementAttributes(String qName, XMLStreamReader reader) {
        switch (qName) {
            case TAG_PRODUCT_PRICE -> priceCurrency = reader.getAttributeValue(null, "currency");
            case TAG_PRODUCT_DISCOUNT -> discountCurrency = reader.getAttributeValue(null, "currency");
            case TAG_PRODUCT_ATTRIBUTE_CLASS -> attributeClassId = reader.getAttributeValue(null, "id");
            default -> { /* do nothing */}
        }
    }

    public void characters(XMLStreamReader reader) {
        if (shouldCaptureCharacters()) {
            currentValue.append(reader.getText());
        }
    }

    private boolean shouldCaptureCharacters() {
        return currentProduct != null || isInPath(TAG_MERCHANT_ID, TAG_HEADER) || isInPath(TAG_MERCHANT_NAME, TAG_HEADER);
    }

    public void endElement(XMLStreamReader reader) {
        String qName = reader.getLocalName();
        String value = currentValue.toString().trim();

        if (!value.isEmpty()) {
            processElementValue(value);
        }

        if (isMainProductEnd(qName)) {
            handleProductEnd();
        }

        elementStack.pop();
    }

    private void processElementValue(String value) {
        if (isInPath(TAG_MERCHANT_ID, TAG_HEADER)) {
            merchantId = value;
            return;
        }
        if (isInPath(TAG_MERCHANT_NAME, TAG_HEADER)) {
            merchantName = value;
            return;
        }

        if (currentProduct == null) {
            return;
        }

        if (isInPath(TAG_PRODUCT, TAG_PRODUCT_URL)) {
            currentProduct.put(TAG_PRODUCT_URL, value);
        } else if (isInPath(TAG_PRODUCT_IMAGE, TAG_PRODUCT_URL)) {
            currentProduct.put("product_image", value);
        } else if (isInPath(TAG_PRODUCT_PRICE)) {
            currentProduct.put("price_currency", priceCurrency);
        } else if (isInPath(TAG_PRODUCT_DISCOUNT)) {
            currentProduct.put("discount_currency", discountCurrency);
        } else if (isInPath(TAG_PRODUCT_ATTRIBUTE_CLASS)) {
            currentProduct.put("attribute_class_id", attributeClassId);
        }
        processStandardElement(value);
    }

    private void handleProductEnd() {
        if (currentProduct != null) {
            callback.onProduct(currentProduct);
            mapPool.returnMap(currentProduct);
            currentProduct = null;
        }
    }

    private void processStandardElement(String value) {
        switch (getCurrentElement()) {
            case TAG_PRODUCT_PRIMARY -> currentProduct.put("product_primary", value);
            case TAG_PRODUCT_SECONDARY -> currentProduct.put("product_secondary", value);
            case TAG_PRODUCT_BRAND -> currentProduct.put("product_brand", value);
            case TAG_PRODUCT_AVAILABILITY -> currentProduct.put("product_availability", value);
            case TAG_PRODUCT_KEYWORDS -> currentProduct.put("product_keywords", value);
            case TAG_PRODUCT_ATTRIBUTE_MISC -> currentProduct.put("product_attribute_misc", value);
            case TAG_PRODUCT_ATTRIBUTE_PRODUCT_TYPE -> currentProduct.put("product_attribute_type", value);
            case TAG_PRODUCT_ATTRIBUTE_SIZE -> currentProduct.put("product_attribute_size", value);
            case TAG_PRODUCT_ATTRIBUTE_COLOR -> currentProduct.put("product_attribute_color", value);
            case TAG_PRODUCT_ATTRIBUTE_GENDER -> currentProduct.put("product_attribute_gender", value);
            case TAG_PRODUCT_ATTRIBUTE_MATERIAL -> currentProduct.put("product_attribute_material", value);
            case TAG_PRODUCT_SHORT_DESC -> currentProduct.put("product_short_desc", value);
            case TAG_PRODUCT_LONG_DESC -> currentProduct.put("product_long_desc", value);
            case TAG_PRODUCT_DISCOUNT_TYPE -> currentProduct.put("product_discount_type", value);
            case TAG_PRODUCT_SALE_PRICE -> currentProduct.put("product_sale_price", value);
            case TAG_PRODUCT_RETAIL_PRICE -> currentProduct.put("product_retail_price", value);
            default -> { /* do nothing */}
        }
    }

    public boolean isMainProductEnd(String localName) {
        return localName.equals(TAG_PRODUCT) && !isInPath(TAG_PRODUCT, TAG_PRODUCT_URL);
    }
}
