package me.ardikapras.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Product {
    @XmlAttribute
    private String id;

    @XmlAttribute
    private String sku;

    @XmlElement
    private String name;

    @XmlElement
    private String brand;

    @XmlElement
    private String category;

    @XmlElement
    private String color;

    @XmlElement
    private String size;

    @XmlElement(name = "price")
    private String price;

    @XmlElement(name = "sale_price")
    private String salePrice;

    @XmlElement(name = "product_url")
    private String productUrl;

    @XmlElement(name = "image_url")
    private String imageUrl;

    @XmlElement
    private String description;
}
