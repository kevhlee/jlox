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
            for (var statement : statements) {
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
        executeBlock(stmt.statements(), new Environment(environment));
    }

    @Override
    public void visitClass(Stmt.Class stmt) {
        environment.define(stmt.name().lexeme(), null);

        var methods = new HashMap<String, LoxFunction>();
        for (var method : stmt.methods()) {
            var methodName = method.name().lexeme();
            methods.put(methodName, new LoxFunction(method, environment, methodName.equals("init")));
        }

        var clazz = new LoxClass(stmt.name().lexeme(), methods);
        environment.assign(stmt.name(), clazz);
    }

    @Override
    public void visitExpression(Stmt.Expression stmt) {
        evaluate(stmt.expression());
    }

    @Override
    public void visitFunction(Stmt.Function stmt) {
        environment.define(stmt.name().lexeme(), new LoxFunction(stmt, environment, false));
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
        throw new ReturnValue((stmt.value() != null) ? evaluate(stmt.value()) : null);
    }

    @Override
    public void visitVar(Stmt.Var stmt) {
        var value = (stmt.initializer() != null) ? evaluate(stmt.initializer()) : null;
        environment.define(stmt.name().lexeme(), value);
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

        var distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name(), value);
        } else {
            globals.assign(expr.name(), value);
        }

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

        if (!(callee instanceof LoxCallable callable)) {
            throw new RuntimeError(expr.paren(),
                "Can only call functions and classes.");
        }

        var arguments = expr.arguments().stream().map(this::evaluate).toList();
        if (arguments.size() != callable.arity()) {
            throw new RuntimeError(
                expr.paren(),
                String.format("Expected %d arguments but got %d.", callable.arity(), arguments.size()));
        }

        return callable.call(this, arguments);
    }

    @Override
    public Object visitGet(Expr.Get expr) {
        var object = evaluate(expr.object());
        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(expr.name());
        }
        throw new RuntimeError(expr.name(), "Only instances have properties.");
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
    public Object visitSet(Expr.Set expr) {
        var object = evaluate(expr.object());

        if (!(object instanceof LoxInstance instance)) {
            throw new RuntimeError(expr.name(), "Only instances have fields.");
        }

        var value = evaluate(expr.value());
        instance.set(expr.name(), value);
        return value;
    }

    @Override
    public Object visitThis(Expr.This expr) {
        return lookUpVariable(expr.keyword(), expr);
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
        return lookUpVariable(expr.name(), expr);
    }

    //
    // Interpreter
    //

    protected void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    protected void executeBlock(List<Stmt> statements, Environment environment) {
        var previousEnvironment = this.environment;
        try {
            this.environment = environment;

            for (var statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previousEnvironment;
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

    private Object lookUpVariable(Token name, Expr expr) {
        var distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme());
        } else {
            return globals.get(name);
        }
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

    private final Map<Expr, Integer> locals = new HashMap<>();
    private final Environment globals = new Environment();
    private Environment environment = globals;

}