package com.example.fintrac.core;

/**
 * Wraps a one-time value (e.g. a toast message) emitted through LiveData, so it's shown
 * once and not again after a screen rotation re-delivers the latest value.
 */
public final class Event<T> {

    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    /** Returns the content the first time, then null. */
    public T consume() {
        if (handled) return null;
        handled = true;
        return content;
    }
}
