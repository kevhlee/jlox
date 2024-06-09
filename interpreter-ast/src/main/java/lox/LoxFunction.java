package lox;

import java.util.List;

/**
 * @author Kevin Lee
 */
class LoxFunction implements LoxCallable {

    public LoxFunction(Stmt.Function declaration, Environment closure, boolean initializer) {
        this.closure = closure;
        this.declaration = declaration;
        this.initializer = initializer;
    }

    @Override
    public int arity() {
        return declaration.parameters().size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var environment = new Environment(closure);
        for (int i = 0; i < declaration.parameters().size(); i++) {
            environment.define(declaration.parameters().get(i).lexeme(), arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body(), environment);
        } catch (ReturnValue returnValue) {
            if (initializer) {
                return closure.getAt(0, "this");
            }
            return returnValue.value;
        }

        if (initializer) {
            return closure.getAt(0, "this");
        }
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name().lexeme() + ">";
    }

    protected LoxFunction bind(LoxInstance instance) {
        var environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, initializer);
    }

    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean initializer;

}