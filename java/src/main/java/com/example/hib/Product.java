package com.example.hib;

import jakarta.persistence.*;

/**
 * Entity with PRIVATE fields (no getters/setters).
 * Bytecode enhancement injects the $$_hibernate_* tracking machinery and
 * a lazy attribute. This is the classic case that proves enhancer works.
 */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // Lazy basic attribute, only loadable with bytecode enhancement
    @Basic(fetch = FetchType.LAZY)
    private String description;

    public Product() {
    }

    public Product(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', description='" + description + "'}";
    }
}