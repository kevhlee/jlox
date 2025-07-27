package com.khl.lox;

import java.util.List;

/**
 * A Lox function object.
 *
 * @author Kevin Lee
 */
record LoxFunction(Interpreter.Environment closure, Stmt.Function declaration) implements LoxCallable {
    @Override
    public int arity() {
        return declaration.parameters().size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var environment = new Interpreter.Environment(closure);

        for (var i = 0; i < declaration.parameters().size(); i++) {
            environment.define(declaration.parameters().get(i).lexeme(), arguments.get(i));
        }

        try {
            interpreter.executeBlock(environment, declaration.body());
            return null;
        } catch (LoxReturn loxReturn) {
            return loxReturn.getValue();
        }
    }

    @Override
    public String toString() {
        return "<fn %s>".formatted(declaration.name().lexeme());
    }
}