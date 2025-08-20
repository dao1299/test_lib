package com.vtnet.netat.tools.gendoc;

import java.util.List;

public class KeywordInfo {
    private String name;
    private String description;
    private String category;
    private List<ParameterInfo> parameters;
    private String example;

    // Getters and Setters...
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<ParameterInfo> getParameters() { return parameters; }
    public void setParameters(List<ParameterInfo> parameters) { this.parameters = parameters; }
    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }
}
