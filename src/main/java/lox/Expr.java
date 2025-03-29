// Generated; DO NOT EDIT
package lox;

/**
 * @author Kevin Lee
 */
public interface Expr {
    <R> R accept(Visitor<R> visitor);

    interface Visitor<R> {
        R visitAssignExpr(Assign expr);
        R visitBinaryExpr(Binary expr);
        R visitCallExpr(Call expr);
        R visitGroupingExpr(Grouping expr);
        R visitLiteralExpr(Literal expr);
        R visitLogicalExpr(Logical expr);
        R visitUnaryExpr(Unary expr);
        R visitVariableExpr(Variable expr);
    }

    record Assign(Token name, Expr value) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitAssignExpr(this);
        }
    }

    record Binary(Expr left, Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }

    record Call(Expr callee, Token paren, java.util.List<Expr> arguments) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitCallExpr(this);
        }
    }

    record Grouping(Expr expression) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }

    record Literal(Object value) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    record Logical(Expr left, Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }
    }

    record Unary(Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }

    record Variable(Token name) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }
    }
}