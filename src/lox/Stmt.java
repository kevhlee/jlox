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
        void visitPrint(Print stmt);
        void visitVar(Var stmt);
    }

    record Block(java.util.List<Stmt> statements) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitBlock(this);
        }
    }

    record Expression(Expr expression) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitExpression(this);
        }
    }

    record Print(Expr expression) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitPrint(this);
        }
    }

    record Var(Token name, Expr initializer) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitVar(this);
        }
    }
}