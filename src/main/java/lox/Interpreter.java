package lox;

import java.util.Objects;

/**
 * Lox interpreter instance.
 *
 * @author Kevin Lee
 */
public class Interpreter implements Expr.Visitor<Object> {
    public void interpret(Expr expr) {
        var result = evaluate(expr);
        System.out.println(stringify(result));
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

        var text = String.valueOf(object);
        if (object instanceof Double && text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }
        return text;
    }

    //
    // Expr
    //

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        var left = evaluate(expr.left());
        var right = evaluate(expr.right());

        switch (expr.operator().type()) {
            case PLUS:
                if (left instanceof Double lhs && right instanceof Double rhs) {
                    return lhs + rhs;
                }
                if (left instanceof String lhs && right instanceof String rhs) {
                    return lhs + rhs;
                }
                throw new RuntimeError(expr.operator(), "Operands must be two numbers or two strings");
            case BANG_EQUAL:
                return !Objects.equals(left, right);
            case EQUAL_EQUAL:
                return Objects.equals(left, right);
        }

        if (left instanceof Double lhs && right instanceof Double rhs) {
            return switch (expr.operator().type()) {
                case MINUS -> lhs - rhs;
                case STAR -> lhs * rhs;
                case SLASH -> lhs / rhs;
                case GREATER -> lhs > rhs;
                case GREATER_EQUAL -> lhs >= rhs;
                case LESS -> lhs < rhs;
                case LESS_EQUAL -> lhs <= rhs;
                // Unreachable
                default -> null;
            };
        }

        throw new RuntimeError(expr.operator(), "Operands must be numbers");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression());
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value();
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        var right = evaluate(expr.right());

        return switch (expr.operator().type()) {
            case BANG -> !isTruthy(right);
            case MINUS -> {
                if (right instanceof Double rhs) {
                    yield -rhs;
                }
                throw new RuntimeError(expr.operator(), "Operand must be a number");
            }
            // Unreachable
            default -> null;
        };
    }
}