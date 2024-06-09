package lox;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kevin Lee
 */
class LoxInstance {

    public LoxInstance(LoxClass clazz) {
        this.clazz = clazz;
    }

    public Object get(Token name) {
        if (fields.containsKey(name.lexeme())) {
            return fields.get(name.lexeme());
        }

        var method = clazz.findMethod(name.lexeme());
        if (method != null) {
            return method.bind(this);
        }

        throw new RuntimeError(name, "Undefined property '" + name.lexeme() + "'.");
    }

    public void set(Token name, Object value) {
        fields.put(name.lexeme(), value);
    }

    @Override
    public String toString() {
        return clazz.name() + " instance";
    }

    private final LoxClass clazz;
    private final Map<String, Object> fields = new HashMap<>();

}