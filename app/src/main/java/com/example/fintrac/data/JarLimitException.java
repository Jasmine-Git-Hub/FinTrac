package com.example.fintrac.data;

import com.example.fintrac.core.Money;

/**
 * A deposit was refused because it would pass the jar's goal or the depositor's fair share.
 * Carries the amount still allowed (base PHP) so the UI can show it in the user's currency.
 */
public class JarLimitException extends RuntimeException {

    public enum Kind {
        /** The jar is already full. */
        GOAL_ALREADY_REACHED,
        /** Only {@link #remaining} more fits before the goal. */
        OVER_GOAL,
        /** The depositor already paid their full share of a Shared jar. */
        SHARE_ALREADY_PAID,
        /** The depositor may add at most {@link #remaining} more to a Shared jar. */
        OVER_SHARE
    }

    public final Kind kind;
    public final Money remaining;

    public JarLimitException(Kind kind, Money remaining) {
        super(kind.name());
        this.kind = kind;
        this.remaining = remaining;
    }
}
