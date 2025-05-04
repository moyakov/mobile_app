package com.hfad.timetrack.chats;

import java.util.Map;

public class Chat {
    private String id;
    private String lastMessage;
    private long timestamp;
    private Map<String, Boolean> participants;

    public Chat() {}

    public String getId() { return id; }
    public String getLastMessage() { return lastMessage; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Boolean> getParticipants() { return participants; }

    public void setId(String id) { this.id = id; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setParticipants(Map<String, Boolean> participants) { this.participants = participants; }
}