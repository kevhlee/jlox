package lox;

import java.util.List;

/**
 * @author Kevin Lee
 */
class LoxFunction implements LoxCallable {

    public LoxFunction(Stmt.Function declaration, Environment closure) {
        this.closure = closure;
        this.declaration = declaration;
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
            return null;
        } catch (ReturnValue returnValue) {
            return returnValue.value;
        }
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name().lexeme() + ">";
    }

    private final Stmt.Function declaration;
    private final Environment closure;

}