package com.sabortradicion.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "menu_items")
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 16)
    private String emoji;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false)
    private boolean available;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    protected MenuItem() {
    }

    public MenuItem(String name, String category, BigDecimal price, String emoji, String description,
                    boolean available, String imageUrl) {
        update(name, category, price, emoji, description, available, imageUrl);
    }

    public void update(String name, String category, BigDecimal price, String emoji, String description,
                       boolean available, String imageUrl) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.emoji = emoji;
        this.description = description;
        this.available = available;
        this.imageUrl = imageUrl;
    }

    public void changeAvailability(boolean available) {
        this.available = available;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public String getEmoji() { return emoji; }
    public String getDescription() { return description; }
    public boolean isAvailable() { return available; }
    public String getImageUrl() { return imageUrl; }
}
