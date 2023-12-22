// Generated; DO NOT EDIT
package lox;

/**
 * @author Kevin Lee
 */
public interface Stmt {
    void accept(Visitor visitor);

    interface Visitor {
        void visitBlock(Block stmt);
        void visitExpression(Expression stmt);
        void visitFunction(Function stmt);
        void visitIf(If stmt);
        void visitPrint(Print stmt);
        void visitReturn(Return stmt);
        void visitVar(Var stmt);
        void visitWhile(While stmt);
    }

    record Block(java.util.List<Stmt> statements) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitBlock(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Expression(Expr expression) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitExpression(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Function(Token name, java.util.List<Token> parameters, java.util.List<Stmt> body) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitFunction(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record If(Expr condition, Stmt thenBranch, Stmt elseBranch) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitIf(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Print(Expr expression) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitPrint(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Return(Token keyword, Expr value) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitReturn(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record Var(Token name, Expr initializer) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitVar(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    record While(Expr condition, Stmt body) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitWhile(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }
}