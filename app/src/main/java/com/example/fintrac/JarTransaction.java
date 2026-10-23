package com.example.fintrac;

import com.google.firebase.Timestamp;
import java.io.Serializable;

public class JarTransaction implements Serializable {
    private String id;
    private String jarId;
    private String jarName;
    private String userId;
    private String userName;
    private double amount;
    private String type;
    private String note;
    private Timestamp timestamp;
    private boolean isJarsActivity;

    public JarTransaction() {}

    public JarTransaction(String jarId, String userId, String userName, double amount,
                          String type, String note) {
        this.jarId = jarId;
        this.userId = userId;
        this.userName = userName;
        this.amount = amount;
        this.type = type;
        this.note = note;
        this.timestamp = Timestamp.now();
        this.isJarsActivity = true;
    }

    public JarTransaction(String id, String jarId, String jarName, String userId,
                          String userName, double amount, String type, String note,
                          Timestamp timestamp) {
        this.id = id;
        this.jarId = jarId;
        this.jarName = jarName;
        this.userId = userId;
        this.userName = userName;
        this.amount = amount;
        this.type = type;
        this.note = note;
        this.timestamp = timestamp;
        this.isJarsActivity = true;
    }

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getJarId() { return jarId != null ? jarId : ""; }
    public void setJarId(String jarId) { this.jarId = jarId; }

    public String getJarName() { return jarName != null ? jarName : ""; }
    public void setJarName(String jarName) { this.jarName = jarName; }

    public String getUserId() { return userId != null ? userId : ""; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName != null ? userName : ""; }
    public void setUserName(String userName) { this.userName = userName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type != null ? type : ""; }
    public void setType(String type) { this.type = type; }

    public String getNote() { return note != null ? note : ""; }
    public void setNote(String note) { this.note = note; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public boolean isJarsActivity() { return isJarsActivity; }
    public void setJarsActivity(boolean jarsActivity) { isJarsActivity = jarsActivity; }
}