package com.khl.lox;

import java.util.List;

/**
 * A Lox object that can be called (e.g. classes, functions).
 *
 * @author Kevin Lee
 */
public interface LoxCallable {
    /**
     * Returns the expected number of arguments.
     *
     * @return the expected number of arguments
     */
    int arity();

    /**
     * Executes the callable given a Lox interpreter instance and input arguments.
     *
     * @param interpreter the Lox interpreter instance
     * @param arguments   the input arguments
     * @return the result of the call
     */
    Object call(Interpreter interpreter, List<Object> arguments);
}