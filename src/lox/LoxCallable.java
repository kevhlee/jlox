package lox;

import java.util.List;

/**
 * @author Kevin Lee
 */
interface LoxCallable {

    int arity();

    Object call(Interpreter interpreter, List<Object> arguments);

}