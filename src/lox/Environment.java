package lox;

import java.util.HashMap;
import java.util.Map;

/**
 * A Lox interpreter instance's environment containing its state.
 *
 * @author Kevin Lee
 */
public class Environment {

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    public void assign(Token token, Object value) {
        if (!values.containsKey(token.lexeme())) {
            if (enclosing != null) {
                enclosing.assign(token, value);
            }
            throw new RuntimeError(token, "Undefined variable '" + token.lexeme() + "'.");
        }

        values.put(token.lexeme(), value);
    }

    public void define(Token token, Object value) {
        define(token.lexeme(), value);
    }

    public void define(String name, Object value) {
        values.put(name, value);
    }

    public Object get(Token token) {
        if (!values.containsKey(token.lexeme())) {
            if (enclosing != null) {
                return enclosing.get(token);
            }
            throw new RuntimeError(token, "Undefined variable '" + token.lexeme() + "'.");
        }

        return values.get(token.lexeme());
    }

    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

}