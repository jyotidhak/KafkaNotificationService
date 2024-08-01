package com.notification.dto;

import java.util.List;

public class StepsConfig {

    private String category;
    private List<String> steps;

    public String getCategory() {
        return category;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setRole(String role) {
        this.category = category;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }
}
