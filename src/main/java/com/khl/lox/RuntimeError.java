package com.khl.lox;

/**
 * An error that occurs during the Lox interpreter runtime.
 *
 * @author Kevin Lee
 */
public class RuntimeError extends RuntimeException {
    public RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }

    public int getLine() {
        return token.line();
    }

    private final Token token;
}