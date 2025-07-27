package com.khl.lox;

import java.util.List;

/**
 * A Lox statement AST node.
 *
 * @author Kevin Lee
 */
public interface Stmt {
    void accept(Stmt.Visitor visitor);

    interface Visitor {
        void visitBlock(Block stmt);

        void visitExpression(Expression stmt);

        void visitIf(If stmt);

        void visitPrint(Print stmt);

        void visitVar(Var stmt);

        void visitWhile(While stmt);
    }

    //
    // Classes
    //

    record Block(List<Stmt> body) implements Stmt {
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

    record If(Expr condition, Stmt thenBranch, Stmt elseBranch) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitIf(this);
        }
    }

    record Print(Expr value) implements Stmt {
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

    record While(Expr condition, Stmt body) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visitWhile(this);
        }
    }
}