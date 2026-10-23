package com.example.fintrac;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

public class ChallengeNavigationHelper {

    private static final String TAG = "ChallengeNavigation";

    public static void navigateToChallenge(FragmentActivity activity, String challengeId, String challengeName) {
        if (activity == null || challengeId == null || challengeId.isEmpty()) return;

        // Check if we're already on the Challenge fragment
        Fragment currentFragment = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (currentFragment instanceof Challenge) {
            // We're already on Challenge fragment, just send the ID
            Bundle result = new Bundle();
            result.putString("open_challenge_id", challengeId);
            if (challengeName != null) {
                result.putString("open_challenge_name", challengeName);
            }
            currentFragment.getParentFragmentManager().setFragmentResult("open_challenge_request", result);
        } else {
            // Navigate to Challenge fragment with the ID
            Challenge challengeFragment = new Challenge();
            Bundle args = new Bundle();
            args.putString("open_challenge_id", challengeId);
            if (challengeName != null) {
                args.putString("open_challenge_name", challengeName);
            }
            challengeFragment.setArguments(args);

            // Switch the bottom navigation to Challenges first (the host is dashboard)
            if (activity instanceof TabNavigator) {
                ((TabNavigator) activity).selectBottomNavItem(R.id.navigation_challenges);
            }
            FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, challengeFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
}