package lox;

/**
 * @author Kevin Lee
 */
public class SyntaxError extends Exception {
    private final int line;
    private final String where;

    public SyntaxError(int line, String message) {
        this(line, "", message);
    }

    public SyntaxError(int line, String where, String message) {
        super(message);

        this.line = line;
        this.where = where;
    }

    public int getLine() {
        return line;
    }

    public String getWhere() {
        return where;
    }
}