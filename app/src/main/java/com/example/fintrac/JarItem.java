package com.example.fintrac;

import java.io.Serializable;

public class JarItem implements Serializable {
    private final String id;
    private final String name;
    private final String type;
    private double savedAmount;
    private final double goalAmount;
    private final String daysLeft;
    private final String weekly;
    private final String iconName;
    private final int jarColor;

    // 👉 ITO YUNG HINAHANAP NG JARSFRAGMENT MO
    private boolean isLocked;

    public JarItem(String id, String name, String type, double savedAmount, double goalAmount,
                   String daysLeft, String weekly, String iconName, int jarColor) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.savedAmount = savedAmount;
        this.goalAmount = goalAmount;
        this.daysLeft = daysLeft;
        this.weekly = weekly;
        this.iconName = iconName;
        this.jarColor = jarColor;
        this.isLocked = false; // Default na hindi naka-lock
    }

    // 👉 ITO YUNG MGA METHODS NA PANG-RESOLVE SA ERROR MO
    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
    }

    // Setters allowing updates
    public void setSavedAmount(double amount) {
        this.savedAmount = amount;
    }

    public void addSavedAmount(double amount) {
        this.savedAmount += amount;
    }

    // Accurate Progress Calculation
    public int getProgressPercentage() {
        if (goalAmount <= 0) return 0;
        double percentage = (savedAmount / goalAmount) * 100;
        return (int) Math.min(percentage, 100);
    }

    // Owner's uid; only the owner may delete the jar
    private String createdBy = "";

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy != null ? createdBy : ""; }

    /** Field-by-field comparison for list diffing. */
    public boolean sameContentAs(JarItem other) {
        return other != null
                && java.util.Objects.equals(id, other.id)
                && java.util.Objects.equals(name, other.name)
                && java.util.Objects.equals(type, other.type)
                && savedAmount == other.savedAmount
                && goalAmount == other.goalAmount
                && java.util.Objects.equals(daysLeft, other.daysLeft)
                && java.util.Objects.equals(iconName, other.iconName)
                && jarColor == other.jarColor
                && isLocked == other.isLocked;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public double getSavedAmount() { return savedAmount; }
    public double getGoalAmount() { return goalAmount; }
    public String getDaysLeft() { return daysLeft; }
    public String getWeekly() { return weekly; }
    public String getIconName() { return iconName; }
    public int getJarColor() { return jarColor; }
}