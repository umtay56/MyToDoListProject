package com.example.todolisttt;

import java.io.Serializable;

public class TaskItem implements Serializable {
    private final String description;
    private final String detail;
    private boolean completed;

    public TaskItem(String description, String detail) {
        this.description = description;
        this.detail = detail;
        this.completed = false;
    }

    public String getDescription() {
        return description;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
