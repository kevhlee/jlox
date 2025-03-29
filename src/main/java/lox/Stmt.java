// Generated; DO NOT EDIT
package lox;

/**
 * @author Kevin Lee
 */
public interface Stmt {
    void accept(Visitor visitor);

    interface Visitor {
        void visitBlockStmt(Block stmt);
        void visitExpressionStmt(Expression stmt);
        void visitPrintStmt(Print stmt);
        void visitVarStmt(Var stmt);
    }

    record Block(java.util.List<Stmt> statements) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitBlockStmt(this);
        }
    }

    record Expression(Expr expression) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitExpressionStmt(this);
        }
    }

    record Print(Expr value) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitPrintStmt(this);
        }
    }

    record Var(Token name, Expr initializer) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitVarStmt(this);
        }
    }
}