package com.vtnet.netat.web.elements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectUI {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String udid;
    private String name;
    private String type;
    private String description;
    private List<Locator> locators;

    // --- Constructors ---
    public ObjectUI() {
        this.locators = Collections.emptyList();
    }

    // --- Getters and Setters ---
    public String getUdid() { return udid; }
    public void setUdid(String udid) { this.udid = udid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Locator> getLocators() { return locators; }
    public void setLocators(List<Locator> locators) { this.locators = locators; }

    // --- Convenience Methods ---

    /**
     * Cung cấp một danh sách các locator đang hoạt động (`active=true`).
     * Không còn sắp xếp vì đã loại bỏ trường 'priority'.
     */
    public List<Locator> getActiveLocators() {
        if (locators == null || locators.isEmpty()) {
            return Collections.emptyList();
        }
        return locators.stream()
                .filter(Locator::isActive) // Chỉ lấy các locator đang hoạt động
                .collect(Collectors.toList());
    }

    // --- toString() and toJson() ---
    @Override
    public String toString() {
        return "ObjectUI{" + "name='" + name + '\'' + ", type='" + type + '\'' +
                ", locators=" + (locators != null ? locators.size() : 0) + " locators" + '}';
    }

    public String toJson() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Error converting ObjectUI to JSON", e);
        }
    }
}