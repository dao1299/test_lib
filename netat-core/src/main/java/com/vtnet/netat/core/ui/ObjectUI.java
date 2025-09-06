package com.vtnet.netat.core.ui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectUI {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String uuid;
    private String name;
    private String type;
    private String description;
    private List<Locator> locators;

    // --- Constructors, Getters and Setters (Giữ nguyên) ---
    public ObjectUI() {
        this.locators = Collections.emptyList();
    }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
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
     */
    public List<Locator> getActiveLocators() {
        if (locators == null || locators.isEmpty()) {
            return Collections.emptyList();
        }
        return locators.stream()
                .filter(Locator::isActive)
                .collect(Collectors.toList());
    }

    /**
     * --- PHƯƠNG THỨC MỚI ---
     * Lấy ra locator được đánh dấu là mặc định.
     * Trả về một Optional để xử lý trường hợp không có locator nào là mặc định.
     */
    public Optional<Locator> getDefaultLocator() {
        if (locators == null || locators.isEmpty()) {
            return Optional.empty();
        }
        return locators.stream()
                .filter(Locator::isDefault)
                .findFirst();
    }


    // --- toString() and toJson() (Giữ nguyên) ---
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