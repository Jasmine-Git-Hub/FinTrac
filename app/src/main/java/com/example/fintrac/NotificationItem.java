package com.example.fintrac;

import com.google.firebase.firestore.PropertyName; // DAGDAG ITO
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NotificationItem {
    private String id;
    private String type; // "JAR_INVITE", "BILL_DUE", "JAR_GOAL_REACHED", etc.
    private String title;
    private String message;
    private String jarId;
    private String jarName;
    private String inviterId;
    private String inviterName;
    private Map<String, Object> data;
    private boolean isRead;
    private Date timestamp;
    private String actionType; // "ACCEPT/DECLINE" for invites, "VIEW" for others

    // Empty constructor for Firestore
    public NotificationItem() {}

    // Constructor for jar invites
    public NotificationItem(String type, String title, String message, String jarId,
                            String jarName, String inviterId, String inviterName) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.jarId = jarId;
        this.jarName = jarName;
        this.inviterId = inviterId;
        this.inviterName = inviterName;
        this.isRead = false;
        this.timestamp = new Date();
        this.actionType = "ACCEPT/DECLINE";
        this.data = new HashMap<>();
    }

    // Full constructor with additional data
    public NotificationItem(String type, String title, String message, String jarId,
                            String jarName, String inviterId, String inviterName,
                            double goalAmount, Date dueDate, int participantCount) {
        this(type, title, message, jarId, jarName, inviterId, inviterName);
        this.data.put("goalAmount", goalAmount);
        this.data.put("dueDate", dueDate);
        this.data.put("participantCount", participantCount);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getJarId() { return jarId; }
    public void setJarId(String jarId) { this.jarId = jarId; }

    public String getJarName() { return jarName; }
    public void setJarName(String jarName) { this.jarName = jarName; }

    public String getInviterId() { return inviterId; }
    public void setInviterId(String inviterId) { this.inviterId = inviterId; }

    public String getInviterName() { return inviterName; }
    public void setInviterName(String inviterName) { this.inviterName = inviterName; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    // DITO NATIN NILAGAY ANG PROPERTY NAME PARA HINDI MAG-BUG SA DATABASE
    @PropertyName("isRead")
    public boolean isRead() { return isRead; }

    @PropertyName("isRead")
    public void setRead(boolean read) { isRead = read; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    // Helper methods to get data from map
    public Double getGoalAmount() {
        if (data != null && data.containsKey("goalAmount")) {
            Object value = data.get("goalAmount");
            if (value instanceof Double) return (Double) value;
            if (value instanceof Long) return ((Long) value).doubleValue();
            if (value instanceof Integer) return ((Integer) value).doubleValue();
        }
        return 0.0;
    }

    public Date getDueDate() {
        if (data != null && data.containsKey("dueDate")) {
            Object value = data.get("dueDate");
            if (value instanceof Date) return (Date) value;
        }
        return null;
    }

    public Integer getParticipantCount() {
        if (data != null && data.containsKey("participantCount")) {
            Object value = data.get("participantCount");
            if (value instanceof Integer) return (Integer) value;
            if (value instanceof Long) return ((Long) value).intValue();
        }
        return 0;
    }
}