package lox;

/**
 * An error that occurs during the Lox interpreter runtime.
 *
 * @author Kevin Lee
 */
public class RuntimeError extends RuntimeException {
    private final Token token;

    public RuntimeError(Token token, String message) {
        super(message);

        this.token = token;
    }

    public int getLine() {
        return token.line();
    }
}