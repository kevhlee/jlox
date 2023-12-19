package lox;

import java.util.List;
import java.util.Objects;

/**
 * Lox interpreter instance.
 *
 * @author Kevin Lee
 */
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor {

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
        var previousEnvironment = currentEnvironment;

        try {
            currentEnvironment = new Environment(currentEnvironment);

            for (var statement : stmt.statements()) {
                execute(statement);
            }
        } finally {
            currentEnvironment = previousEnvironment;
        }
    }

    @Override
    public void visitExpression(Stmt.Expression stmt) {
        evaluate(stmt.expression());
    }

    @Override
    public void visitPrint(Stmt.Print stmt) {
        var value = evaluate(stmt.expression());
        System.out.println(stringify(value));
    }

    @Override
    public void visitVar(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer() != null) {
            value = evaluate(stmt.initializer());
        }
        currentEnvironment.define(stmt.name(), value);
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
    public Object visitGrouping(Expr.Grouping expr) {
        return evaluate(expr.expression());
    }

    @Override
    public Object visitLiteral(Expr.Literal expr) {
        return expr.value();
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

    private Environment currentEnvironment = new Environment(null);

}