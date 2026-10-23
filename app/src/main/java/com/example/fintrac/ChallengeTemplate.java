package com.example.fintrac;

public class ChallengeTemplate {
    private final String name;
    private final String description;
    private final int durationDays;
    private final double targetAmount;

    public ChallengeTemplate(String name, String description, int durationDays, double targetAmount) {
        this.name = name;
        this.description = description;
        this.durationDays = durationDays;
        this.targetAmount = targetAmount;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getDurationDays() { return durationDays; }
    public double getTargetAmount() { return targetAmount; }
}