package com.textcraft.util;

/**
 * OOP: Generics - a value or an error message, used to hand results from worker threads to the UI.
 */
public final class Result<T> {
    private final T value;
    private final String error;

    private Result(T value, String error) {
        this.value = value;
        this.error = error;
    }

    public static <T> Result<T> ok(T value) {
        return new Result<>(value, null);
    }

    public static <T> Result<T> fail(String error) {
        return new Result<>(null, error);
    }

    public boolean isOk() {
        return error == null;
    }

    public T get() {
        return value;
    }

    public String error() {
        return error;
    }
}
