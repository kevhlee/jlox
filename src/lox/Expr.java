// Generated; DO NOT EDIT
package lox;

/**
 * @author Kevin Lee
 */
public interface Expr {
    <R> R accept(Visitor<R> visitor);

    interface Visitor<R> {
        R visitAssign(Assign expr);
        R visitBinary(Binary expr);
        R visitCall(Call expr);
        R visitGet(Get expr);
        R visitGrouping(Grouping expr);
        R visitLiteral(Literal expr);
        R visitLogical(Logical expr);
        R visitSet(Set expr);
        R visitThis(This expr);
        R visitUnary(Unary expr);
        R visitVariable(Variable expr);
    }

    record Assign(Token name, Expr value) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitAssign(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Binary(Expr left, Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinary(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Call(Expr callee, Token paren, java.util.List<Expr> arguments) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitCall(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Get(Expr object, Token name) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGet(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Grouping(Expr expression) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGrouping(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Literal(Object value) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteral(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Logical(Expr left, Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogical(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Set(Expr object, Token name, Expr value) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitSet(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record This(Token keyword) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitThis(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Unary(Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnary(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Variable(Token name) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariable(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }
}