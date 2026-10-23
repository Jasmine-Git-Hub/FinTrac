package com.example.fintrac;

/** Implemented by the activity that hosts the bottom navigation (dashboard). */
public interface TabNavigator {
    /** Switches to a bottom-navigation tab, e.g. {@code R.id.navigation_bills}. */
    void selectBottomNavItem(int menuItemId);
}
