package com.vtnet.netat.tools.gendoc;

import java.util.List;

class KeywordInfo {
    private String name;
    private String description;
    private String category;
    private String example;
    private String returnValue;
    private List<String> prerequisites;
    private List<String> exceptions;
    private String platform;
    private String systemImpact;
    private String stability;
    private List<String> tags;
    private String className;
    private String methodName;
    private String returnType;
    private List<ParameterInfo> parameters;

    // Getters và Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }

    public String getReturnValue() { return returnValue; }
    public void setReturnValue(String returnValue) { this.returnValue = returnValue; }

    public List<String> getPrerequisites() { return prerequisites; }
    public void setPrerequisites(List<String> prerequisites) { this.prerequisites = prerequisites; }

    public List<String> getExceptions() { return exceptions; }
    public void setExceptions(List<String> exceptions) { this.exceptions = exceptions; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getSystemImpact() { return systemImpact; }
    public void setSystemImpact(String systemImpact) { this.systemImpact = systemImpact; }

    public String getStability() { return stability; }
    public void setStability(String stability) { this.stability = stability; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }

    public List<ParameterInfo> getParameters() { return parameters; }
    public void setParameters(List<ParameterInfo> parameters) { this.parameters = parameters; }
}
