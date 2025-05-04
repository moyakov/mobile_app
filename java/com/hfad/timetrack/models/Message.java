package com.hfad.timetrack.models;

import java.util.Date;

public class Message {
    private String senderId;
    private String text;
    private long timestamp;
    private String senderImageUrl;

    public Message() {

    }

    public Message(String senderId, String text, long timestamp, String senderImageUrl) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.senderImageUrl = senderImageUrl;
    }


    public String getSenderImageUrl() {
        return senderImageUrl;
    }

    public void setSenderImageUrl(String senderImageUrl) {
        this.senderImageUrl = senderImageUrl;
    }


    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Дополнительный метод для удобного получения времени
    public Date getDate() {
        return new Date(timestamp);
    }
}
