// Generated; DO NOT EDIT
package lox;

/**
 * @author Kevin Lee
 */
public interface Expr {
    <R> R accept(Visitor<R> visitor);

    interface Visitor<R> {
        R visitBinary(Binary expr);
        R visitGrouping(Grouping expr);
        R visitLiteral(Literal expr);
        R visitUnary(Unary expr);
    }

    record Binary(Expr left, Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinary(this);
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

    record Unary(Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnary(this);
        }
    }
}