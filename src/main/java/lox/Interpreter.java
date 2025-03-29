package lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lox interpreter instance.
 *
 * @author Kevin Lee
 */
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor {
    private Environment currentEnvironment = new Environment(null);

    public void interpret(List<Stmt> statements) {
        for (var statement : statements) {
            execute(statement);
        }
    }

    protected void executeBlock(List<Stmt> statements, Environment environment) {
        var previousEnvironment = currentEnvironment;

        try {
            currentEnvironment = environment;

            for (var statement : statements) {
                execute(statement);
            }
        }
        finally {
            currentEnvironment = previousEnvironment;
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
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

    protected static class Environment {
        private final Environment enclosing;
        private final Map<String, Object> values = new HashMap<>();

        public Environment(Environment enclosing) {
            this.enclosing = enclosing;
        }

        public void define(String name, Object value) {
            values.put(name, value);
        }

        public void assign(Token name, Object value) throws RuntimeError {
            if (values.containsKey(name.lexeme())) {
                values.put(name.lexeme(), value);
                return;
            }

            if (enclosing != null) {
                enclosing.assign(name, value);
                return;
            }

            throw new RuntimeError(name, "Undefined variable '%s'".formatted(name.lexeme()));
        }

        public Object get(Token name) throws RuntimeError {
            var value = values.get(name.lexeme());
            if (value != null) {
                return value;
            }

            if (enclosing != null) {
                return enclosing.get(name);
            }

            throw new RuntimeError(name, "Undefined variable '%s'".formatted(name.lexeme()));
        }
    }

    //
    // Stmt
    //

    @Override
    public void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements(), new Environment(currentEnvironment));
    }

    @Override
    public void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression());
    }

    @Override
    public void visitPrintStmt(Stmt.Print stmt) {
        System.out.println(stringify(evaluate(stmt.value())));
    }

    @Override
    public void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer() != null) {
            value = evaluate(stmt.initializer());
        }
        currentEnvironment.define(stmt.name().lexeme(), value);
    }

    //
    // Expr
    //

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value());
        currentEnvironment.assign(expr.name(), value);
        return value;
    }

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

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return currentEnvironment.get(expr.name());
    }
}