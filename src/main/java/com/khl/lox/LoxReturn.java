package com.khl.lox;

/**
 * A Lox return value.
 *
 * @author Kevin Lee
 */
class LoxReturn extends RuntimeException {
    public LoxReturn(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    private final Object value;
}