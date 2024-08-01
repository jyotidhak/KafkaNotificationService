package com.notification.dto;

import java.util.List;

public class UserCategoryDto {

    private String category;
    private List<String> rulesKey;

    public String getCategory() {
        return category;
    }

    public List<String> getRulesKey() {
        return rulesKey;
    }

    public void setRole(String role) {
        this.category = category;
    }

    public void setRulesKey(List<String> rulesKey) {
        this.rulesKey = rulesKey;
    }
}
