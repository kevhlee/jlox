package lox;

import java.util.List;

/**
 * @author Kevin Lee
 */
public interface LoxCallable {

    int arity();

    Object call(Interpreter interpreter, List<Object> arguments);

}