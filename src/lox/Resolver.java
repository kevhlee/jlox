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
    public void visitClass(Stmt.Class stmt) {
        var enclosingClassType = currentClassType;
        currentClassType = ClassType.CLASS;

        declare(stmt.name());
        define(stmt.name());

        var name = stmt.name().lexeme();

        if (stmt.superclass() != null) {
            if (name.equals(stmt.superclass().name().lexeme())) {
                Lox.syntaxError(stmt.superclass().name(), "A class can't inherit from itself.");
            }

            currentClassType = ClassType.SUBCLASS;
            resolve(stmt.superclass());
            beginScope();
            scopes.peek().put("super", true);
        }

        beginScope();
        scopes.peek().put("this", true);

        for (var method : stmt.methods()) {
            var functionType = FunctionType.METHOD;
            if (method.name().lexeme().equals("init")) {
                functionType = FunctionType.INITIALIZER;
            }
            resolveFunction(method, functionType);
        }

        endScope();

        if (stmt.superclass() != null) {
            endScope();
        }

        currentClassType = enclosingClassType;
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
            if (currentFunctionType == FunctionType.INITIALIZER) {
                Lox.syntaxError(stmt.keyword(), "Can't return a value from an initializer.");
            }

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
    public Void visitGet(Expr.Get expr) {
        resolve(expr.object());
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
    public Void visitSet(Expr.Set expr) {
        resolve(expr.value());
        resolve(expr.object());
        return null;
    }

    @Override
    public Void visitSuper(Expr.Super expr) {
        if (currentClassType == ClassType.NONE) {
            Lox.syntaxError(expr.keyword(), "Can't use 'super' outside of a class.");
        } else if (currentClassType != ClassType.SUBCLASS) {
            Lox.syntaxError(expr.keyword(), "Can't use 'super' in a class with no superclass.");
        }

        resolveLocal(expr, expr.keyword());
        return null;
    }

    @Override
    public Void visitThis(Expr.This expr) {
        if (currentClassType == ClassType.NONE) {
            Lox.syntaxError(expr.keyword(), "Can't use 'this' outside of a class.");
            return null;
        }

        resolveLocal(expr, expr.keyword());
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
    private ClassType currentClassType = ClassType.NONE;
    private FunctionType currentFunctionType = FunctionType.NONE;

    private enum ClassType {
        NONE,
        CLASS,
        SUBCLASS;
    }

    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD;
    }

}