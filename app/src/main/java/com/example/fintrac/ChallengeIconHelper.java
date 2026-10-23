package com.example.fintrac;

public class ChallengeIconHelper {

    public static int getIcon(String name) {
        if (name == null) return R.drawable.hat;
        String lower = name.toLowerCase();

        // Specific matches first to avoid false positives
        if (lower.contains("café") || lower.contains("cafe") || lower.contains("coffee") || lower.contains("no-caf")) return R.drawable.coffee;
        if (lower.contains("shopping") || lower.contains("detox")) return R.drawable.ic_shopping_cart;
        if (lower.contains("wi-fi") || lower.contains("wifi") || lower.contains("wi fi")) return R.drawable.ic_wifi;
        if (lower.contains("homebody") || lower.contains("home") || lower.contains("weekend")) return R.drawable.ic_home;
        if (lower.contains("water") || lower.contains("drink")) return R.drawable.ic_water;
        if (lower.contains("baon") || lower.contains("lunch") || lower.contains("packed")) return R.drawable.ic_lunchbox;
        if (lower.contains("sweets") || lower.contains("dessert") || lower.contains("sugar")) return R.drawable.ic_sweets;
        if (lower.contains("fast food") || lower.contains("fastfood") || lower.contains("burger") || lower.contains("eat out")) return R.drawable.burger;
        if (lower.contains("walkathon") || lower.contains("walk") || lower.contains("trike") || lower.contains("transport")) return R.drawable.airplane;
        if (lower.contains("p50") || lower.contains("pesos") || lower.contains("peso") || lower.contains("save") || lower.contains("baon warrior")) return R.drawable.savings;

        return R.drawable.hat;
    }

    public static int getColor(String name) {
        if (name == null) return R.color.challenge_orange;
        String lower = name.toLowerCase();

        if (lower.contains("café") || lower.contains("cafe") || lower.contains("coffee") || lower.contains("no-caf")) return R.color.challenge_orange;
        if (lower.contains("shopping") || lower.contains("detox")) return R.color.royal_purple;
        if (lower.contains("wi-fi") || lower.contains("wifi") || lower.contains("wi fi")) return R.color.sunset_orange;
        if (lower.contains("homebody") || lower.contains("home") || lower.contains("weekend")) return R.color.forest_green;
        if (lower.contains("water") || lower.contains("drink")) return R.color.ocean_blue;
        if (lower.contains("baon") || lower.contains("lunch") || lower.contains("packed")) return R.color.challenge_red;
        if (lower.contains("sweets") || lower.contains("dessert") || lower.contains("sugar")) return R.color.challenge_red;
        if (lower.contains("fast food") || lower.contains("fastfood") || lower.contains("burger") || lower.contains("eat out")) return R.color.challenge_red;
        if (lower.contains("walkathon") || lower.contains("walk") || lower.contains("trike") || lower.contains("transport")) return R.color.challenge_green;
        if (lower.contains("p50") || lower.contains("pesos") || lower.contains("peso") || lower.contains("save")) return R.color.challenge_blue;

        return R.color.challenge_orange;
    }
}