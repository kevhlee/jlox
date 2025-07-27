package com.khl.lox;

import java.io.PrintStream;
import java.util.ArrayList;
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
    public Interpreter(PrintStream stdout) {
        this.stdout = stdout;

        // Define some native functions

        this.environment.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    /**
     * Executes Lox AST statements.
     *
     * @param statements AST statement nodes
     * @throws RuntimeError An error that occurs during interpreter runtime
     */
    public void interpret(List<Stmt> statements) throws RuntimeError {
        for (var statement : statements) {
            execute(statement);
        }
    }

    protected void executeBlock(Environment newEnvironment, List<Stmt> body) {
        var previous = environment;

        try {
            environment = newEnvironment;

            for (var stmt : body) {
                execute(stmt);
            }
        } finally {
            environment = previous;
        }
    }

    private Environment environment = new Environment(null);
    private final PrintStream stdout;

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private static boolean isTruthy(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return value != null;
    }

    private static String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        var text = String.valueOf(object);
        if (object instanceof Double && text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }
        return text;
    }

    /**
     * A class that represents a lexical scope and all values contained within it.
     */
    protected static class Environment {
        protected Environment(Environment parent) {
            this.parent = parent;
        }

        protected void define(String name, Object value) {
            values.put(name, value);
        }

        protected void assign(Token name, Object value) {
            if (values.containsKey(name.lexeme())) {
                values.put(name.lexeme(), value);
                return;
            }

            if (parent != null) {
                parent.assign(name, value);
                return;
            }

            throw new RuntimeError(name, String.format("Undefined variable '%s'", name.lexeme()));
        }

        protected Object get(Token name) {
            if (values.containsKey(name.lexeme())) {
                return values.get(name.lexeme());
            }

            if (parent != null) {
                return parent.get(name);
            }

            throw new RuntimeError(name, String.format("Undefined variable '%s'", name.lexeme()));
        }

        private final Environment parent;
        private final Map<String, Object> values = new HashMap<>();
    }

    //
    // Stmt
    //

    @Override
    public void visitBlock(Stmt.Block stmt) {
        executeBlock(new Environment(environment), stmt.body());
    }

    @Override
    public void visitExpression(Stmt.Expression stmt) {
        evaluate(stmt.expression());
    }

    @Override
    public void visitFunction(Stmt.Function stmt) {
        environment.define(stmt.name().lexeme(), new LoxFunction(environment, stmt));
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
        stdout.println(stringify(evaluate(stmt.value())));
    }

    @Override
    public void visitReturn(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value() != null) {
            value = evaluate(stmt.value());
        }
        throw new LoxReturn(value);
    }

    @Override
    public void visitVar(Stmt.Var stmt) {
        environment.define(stmt.name().lexeme(), evaluate(stmt.initializer()));
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
        environment.assign(expr.name(), value);
        return value;
    }

    @Override
    public Object visitBinary(Expr.Binary expr) {
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
    public Object visitCall(Expr.Call expr) {
        var callee = evaluate(expr.callee());

        if (callee instanceof LoxCallable callable) {
            var arguments = new ArrayList<>();
            for (var argument : expr.arguments()) {
                arguments.add(evaluate(argument));
            }

            if (arguments.size() != callable.arity()) {
                throw new RuntimeError(
                        expr.paren(), "Expected %d arguments but got %d".formatted(callable.arity(), arguments.size()));
            }

            return callable.call(this, arguments);
        }

        throw new RuntimeError(expr.paren(), "Can only call functions and classes");
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

        if (expr.operator().type() == TokenType.OR) {
            if (isTruthy(left)) {
                return left;
            }
        } else {
            if (!isTruthy(left)) {
                return left;
            }
        }

        return evaluate(expr.right());
    }

    @Override
    public Object visitUnary(Expr.Unary expr) {
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
    public Object visitVariable(Expr.Variable expr) {
        return environment.get(expr.name());
    }
}