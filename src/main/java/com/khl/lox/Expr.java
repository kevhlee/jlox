package com.khl.lox;

import java.util.List;

/**
 * A Lox expression AST node.
 *
 * @author Kevin Lee
 */
public interface Expr {
    <R> R accept(Visitor<R> visitor);

    interface Visitor<R> {
        R visitAssign(Assign expr);

        R visitBinary(Binary expr);

        R visitCall(Call expr);

        R visitGrouping(Grouping expr);

        R visitLiteral(Literal expr);

        R visitLogical(Logical expr);

        R visitUnary(Unary expr);

        R visitVariable(Variable expr);
    }

    //
    // Classes
    //

    record Assign(Token name, Expr value) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitAssign(this);
        }
    }

    record Binary(Expr left, Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinary(this);
        }
    }

    record Call(Expr callee, Token paren, List<Expr> arguments) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitCall(this);
        }
    }

    record Grouping(Expr expression) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGrouping(this);
        }
    }

    record Literal(Object value) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteral(this);
        }
    }

    record Logical(Expr left, Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogical(this);
        }
    }

    record Unary(Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnary(this);
        }
    }

    record Variable(Token name) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariable(this);
        }
    }
}