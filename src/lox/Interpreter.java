package lox;

import java.util.Objects;

/**
 * Lox interpreter instance.
 *
 * @author Kevin Lee
 */
public class Interpreter implements Expr.Visitor<Object> {

    public void interpret(Expr expr) {
        try {
            var result = evaluate(expr);
            System.out.println(stringify(result));
        } catch (RuntimeError runtimeError) {
            Lox.runtimeError(runtimeError);
        }
    }

    //
    // Expr
    //

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

}