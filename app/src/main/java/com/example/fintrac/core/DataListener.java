package com.example.fintrac.core;

/** Receives updates from a live data feed. Called on the main thread. */
public interface DataListener<T> {
    void onData(T data);
    void onError(Exception e);
}
