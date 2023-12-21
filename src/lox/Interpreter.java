package lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lox interpreter instance.
 *
 * @author Kevin Lee
 */
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor {

    public Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError runtimeError) {
            Lox.runtimeError(runtimeError);
        }
    }

    //
    // Stmt
    //

    @Override
    public void visitBlock(Stmt.Block stmt) {
        executeBlock(stmt.statements(), new Environment(currentEnvironment));
    }

    @Override
    public void visitExpression(Stmt.Expression stmt) {
        evaluate(stmt.expression());
    }

    @Override
    public void visitFunction(Stmt.Function stmt) {
        currentEnvironment.define(stmt.name().lexeme(), new LoxFunction(stmt, currentEnvironment));
    }

    @Override
    public void visitIf(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition()))) {
            execute(stmt.thenBranch());
        } else if (stmt.elseBranch() != null) {
            execute(stmt.elseBranch());
        }
    }

    @Override
    public void visitPrint(Stmt.Print stmt) {
        var value = evaluate(stmt.expression());
        System.out.println(stringify(value));
    }

    @Override
    public void visitReturn(Stmt.Return stmt) {
        throw new LoxReturn((stmt.value() != null) ? evaluate(stmt.value()) : null);
    }

    @Override
    public void visitVar(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer() != null) {
            value = evaluate(stmt.initializer());
        }
        currentEnvironment.define(stmt.name(), value);
    }

    @Override
    public void visitWhile(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition()))) {
            execute(stmt.body());
        }
    }

    //
    // Expr
    //

    @Override
    public Object visitAssign(Expr.Assign expr) {
        var value = evaluate(expr.value());
        currentEnvironment.assign(expr.name(), value);
        return value;
    }

    @Override
    public Object visitBinary(Expr.Binary expr) {
        var left = evaluate(expr.left());
        var right = evaluate(expr.right());

        switch (expr.operator().type()) {
            case PLUS:
                if (left instanceof Double leftNumber && right instanceof Double rightNumber) {
                    return leftNumber + rightNumber;
                } else if (left instanceof String leftString && right instanceof String rightString) {
                    return leftString + rightString;
                } else {
                    throw new RuntimeError(expr.operator(), "Operands must be two numbers or two strings.");
                }
            case BANG_EQUAL:
                return !Objects.equals(left, right);
            case EQUAL_EQUAL:
                return Objects.equals(left, right);
        }

        checkNumberOperands(expr.operator(), left, right);

        var leftNumber = (double) left;
        var rightNumber = (double) right;

        return switch (expr.operator().type()) {
            case MINUS -> leftNumber - rightNumber;
            case STAR -> leftNumber * rightNumber;
            case SLASH -> leftNumber / rightNumber;
            case GREATER -> leftNumber > rightNumber;
            case GREATER_EQUAL -> leftNumber >= rightNumber;
            case LESS -> leftNumber < rightNumber;
            case LESS_EQUAL -> leftNumber <= rightNumber;
            // Unreachable
            default -> null;
        };
    }

    @Override
    public Object visitCall(Expr.Call expr) {
        var callee = evaluate(expr.callee());

        var arguments = new ArrayList<>();
        for (var argument : expr.arguments()) {
            arguments.add(evaluate(argument));
        }

        if (callee instanceof LoxCallable callable) {
            if (arguments.size() != callable.arity()) {
                throw new RuntimeError(
                    expr.paren(),
                    String.format("Expected %d arguments but got %d.", callable.arity(), arguments.size()));
            }

            return callable.call(this, arguments);
        } else {
            throw new RuntimeError(expr.paren(), "Can only call functions and classes.");
        }
    }

    @Override
    public Object visitGrouping(Expr.Grouping expr) {
        return evaluate(expr.expression());
    }

    @Override
    public Object visitLiteral(Expr.Literal expr) {
        return expr.value();
    }

    @Override
    public Object visitLogical(Expr.Logical expr) {
        var left = evaluate(expr.left());

        if ((expr.operator().type() == TokenType.OR && isTruthy(left)) ||
            (expr.operator().type() == TokenType.AND && !isTruthy(left))) {

            return left;
        }

        return evaluate(expr.right());
    }

    @Override
    public Object visitUnary(Expr.Unary expr) {
        var right = evaluate(expr.right());

        return switch (expr.operator().type()) {
            case BANG -> !isTruthy(right);
            case MINUS -> {
                checkNumberOperand(expr.operator(), right);
                yield -(double) right;
            }
            // Unreachable
            default -> null;
        };
    }

    @Override
    public Object visitVariable(Expr.Variable expr) {
        return currentEnvironment.get(expr.name());
    }

    //
    // Interpreter
    //

    protected void executeBlock(List<Stmt> statements, Environment environment) {
        var previousEnvironment = currentEnvironment;
        try {
            currentEnvironment = environment;
            for (var statement : statements) {
                execute(statement);
            }
        } finally {
            currentEnvironment = previousEnvironment;
        }
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private boolean isTruthy(Object value) {
        if (value == null) {
            return false;
        }
        return !(value instanceof Boolean) || (boolean) value;
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        var text = object.toString();
        if (object instanceof Double && text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }
        return text;
    }

    private final Environment globals = new Environment(null);
    private Environment currentEnvironment = globals;

}