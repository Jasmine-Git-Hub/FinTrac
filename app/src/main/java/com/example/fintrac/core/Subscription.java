package com.example.fintrac.core;

/** A live data feed (e.g. a Firestore snapshot listener). Cancel it when the screen goes away. */
public interface Subscription {
    void cancel();
}
