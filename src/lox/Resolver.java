package lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author Kevin Lee
 */
class Resolver implements Expr.Visitor<Void>, Stmt.Visitor {

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    public void resolve(List<Stmt> statements) {
        for (var statement : statements) {
            resolve(statement);
        }
    }

    //
    // Stmt
    //

    @Override
    public void visitBlock(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements());
        endScope();
    }

    @Override
    public void visitExpression(Stmt.Expression stmt) {
        resolve(stmt.expression());
    }

    @Override
    public void visitFunction(Stmt.Function stmt) {
        declare(stmt.name());
        define(stmt.name());

        resolveFunction(stmt, FunctionType.FUNCTION);
    }

    @Override
    public void visitIf(Stmt.If stmt) {
        resolve(stmt.condition());
        resolve(stmt.thenBranch());
        if (stmt.elseBranch() != null) {
            resolve(stmt.elseBranch());
        }
    }

    @Override
    public void visitPrint(Stmt.Print stmt) {
        resolve(stmt.expression());
    }

    @Override
    public void visitReturn(Stmt.Return stmt) {
        if (currentFunctionType == FunctionType.NONE) {
            Lox.syntaxError(stmt.keyword(), "Can't return from top-level code.");
        }

        if (stmt.value() != null) {
            resolve(stmt.value());
        }
    }

    @Override
    public void visitVar(Stmt.Var stmt) {
        declare(stmt.name());
        if (stmt.initializer() != null) {
            resolve(stmt.initializer());
        }
        define(stmt.name());
    }

    @Override
    public void visitWhile(Stmt.While stmt) {
        resolve(stmt.condition());
        resolve(stmt.body());
    }

    //
    // Expr
    //

    @Override
    public Void visitAssign(Expr.Assign expr) {
        resolve(expr.value());
        resolveLocal(expr, expr.name());
        return null;
    }

    @Override
    public Void visitBinary(Expr.Binary expr) {
        resolve(expr.left());
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visitCall(Expr.Call expr) {
        resolve(expr.callee());
        for (var argument : expr.arguments()) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGrouping(Expr.Grouping expr) {
        resolve(expr.expression());
        return null;
    }

    @Override
    public Void visitLiteral(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogical(Expr.Logical expr) {
        resolve(expr.left());
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visitUnary(Expr.Unary expr) {
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visitVariable(Expr.Variable expr) {
        var name = expr.name();

        if (!scopes.isEmpty() && scopes.peek().get(name.lexeme()) == Boolean.FALSE) {
            Lox.syntaxError(name, "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, name);
        return null;
    }

    //
    // Resolver
    //

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void resolveFunction(Stmt.Function function, FunctionType functionType) {
        var enclosingFunctionType = currentFunctionType;
        currentFunctionType = functionType;

        beginScope();
        for (var parameter : function.parameters()) {
            declare(parameter);
            define(parameter);
        }
        resolve(function.body());
        endScope();
        currentFunctionType = enclosingFunctionType;
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) {
            return;
        }

        var scope = scopes.peek();
        if (scope.containsKey(name.lexeme())) {
            Lox.syntaxError(name, "Already a variable with this name in this scope.");
        }

        scope.put(name.lexeme(), false);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) {
            return;
        }
        scopes.peek().put(name.lexeme(), true);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme())) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunctionType = FunctionType.NONE;

    private enum FunctionType {
        NONE,
        FUNCTION,
    }

}