package com.hfad.timetrack.models;

public class Task {
    private String id;
    private String name;
    private long startTime;
    private long timeSpent;
    private boolean active;
    private boolean completed;
    private String proofImageUrl;
    private long endTime;

    public Task() {}

    public Task(String name, long startTime, long timeSpent, boolean completed, String proofImageUrl) {
        this.name = name;
        this.startTime = startTime;
        this.timeSpent = timeSpent;
        this.completed = completed;
        this.proofImageUrl = proofImageUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getProofImageUrl() {
        return proofImageUrl;
    }

    public void setProofImageUrl(String proofImageUrl) {
        this.proofImageUrl = proofImageUrl;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(long timeSpent) {
        this.timeSpent = timeSpent;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}