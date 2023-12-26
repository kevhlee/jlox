package lox;

import java.util.ArrayList;
import java.util.List;

/**
 * Lox parser.
 *
 * @author Kevin Lee
 */
public class Parser {

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() {
        var statements = new ArrayList<Stmt>();
        while (isParsing()) {
            statements.add(declaration());
        }
        return statements;
    }

    //
    // Stmt
    //

    private Stmt declaration() {
        try {
            if (match(TokenType.CLASS)) {
                return classDeclaration();
            }
            if (match(TokenType.FUN)) {
                return funDeclaration("function");
            }
            if (match(TokenType.VAR)) {
                return varDeclaration();
            }
            return statement();
        } catch (ParseError parseError) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        var name = consume(TokenType.IDENTIFIER, "Expect class name.");
        consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");

        var methods = new ArrayList<Stmt.Function>();
        while (!check(TokenType.RIGHT_BRACE) && isParsing()) {
            methods.add(funDeclaration("method"));
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");
        return new Stmt.Class(name, methods);
    }

    private Stmt.Function funDeclaration(String kind) {
        var name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");

        var parameters = new ArrayList<Token>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    throw error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");

        return new Stmt.Function(name, parameters, blockStatements());
    }

    private Stmt varDeclaration() {
        var name = consume(TokenType.IDENTIFIER, "Expect variable name.");
        var initializer = match(TokenType.EQUAL) ? expression() : null;
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(TokenType.FOR)) {
            return forStatement();
        }
        if (match(TokenType.WHILE)) {
            return whileStatement();
        }
        if (match(TokenType.IF)) {
            return ifStatement();
        }
        if (match(TokenType.RETURN)) {
            return returnStatement();
        }
        if (match(TokenType.PRINT)) {
            return printStatement();
        }
        if (match(TokenType.LEFT_BRACE)) {
            return new Stmt.Block(blockStatements());
        }
        return expressionStatement();
    }

    private List<Stmt> blockStatements() {
        var statements = new ArrayList<Stmt>();
        while (!check(TokenType.RIGHT_BRACE) && isParsing()) {
            statements.add(declaration());
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        var condition = !check(TokenType.SEMICOLON) ? expression() : new Expr.Literal(true);
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

        var increment = !check(TokenType.RIGHT_PAREN) ? expression() : null;
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");

        var body = statement();

        if (increment != null) {
            body = new Stmt.Block(List.of(body, new Stmt.Expression(increment)));
        }

        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(List.of(initializer, body));
        }

        return body;
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        var condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");
        return new Stmt.While(condition, statement());
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        var condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

        var thenBranch = statement();
        var elseBranch = match(TokenType.ELSE) ? statement() : null;

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt returnStatement() {
        var keyword = previous();
        var value = !check(TokenType.SEMICOLON) ? expression() : null;
        consume(TokenType.SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt printStatement() {
        var expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(expr);
    }

    private Stmt expressionStatement() {
        var expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    //
    // Expr
    //

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        var expr = or();

        if (match(TokenType.EQUAL)) {
            var equals = previous();
            var value = assignment();

            if (expr instanceof Expr.Variable variable) {
                return new Expr.Assign(variable.name(), value);
            } else if (expr instanceof Expr.Get get) {
                return new Expr.Set(get.object(), get.name(), value);
            }
            throw error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        var expr = and();
        while (match(TokenType.OR)) {
            expr = new Expr.Logical(expr, previous(), and());
        }
        return expr;
    }

    private Expr and() {
        var expr = equality();
        while (match(TokenType.AND)) {
            expr = new Expr.Logical(expr, previous(), equality());
        }
        return expr;
    }

    private Expr equality() {
        var expr = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            expr = new Expr.Binary(expr, previous(), comparison());
        }
        return expr;
    }

    private Expr comparison() {
        var expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            expr = new Expr.Binary(expr, previous(), term());
        }
        return expr;
    }

    private Expr term() {
        var expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            expr = new Expr.Binary(expr, previous(), factor());
        }
        return expr;
    }

    private Expr factor() {
        var expr = unary();
        while (match(TokenType.STAR, TokenType.SLASH)) {
            expr = new Expr.Binary(expr, previous(), unary());
        }
        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            return new Expr.Unary(previous(), unary());
        }
        return call();
    }

    private Expr call() {
        var expr = primary();

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(TokenType.DOT)) {
                expr = new Expr.Get(expr, consume(TokenType.IDENTIFIER, "Expect property name after '.'."));
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        var arguments = new ArrayList<Expr>();

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    throw error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }

        var paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(TokenType.NIL)) {
            return new Expr.Literal(null);
        }
        if (match(TokenType.TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(TokenType.FALSE)) {
            return new Expr.Literal(false);
        }

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal());
        }

        if (match(TokenType.THIS)) {
            return new Expr.This(previous());
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            var expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    //
    // Parser
    //

    private Token advance() {
        if (isParsing()) {
            current++;
        }
        return previous();
    }

    private boolean check(TokenType type) {
        return isParsing() && peek().type() == type;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.syntaxError(token, message);
        return new ParseError();
    }

    private boolean isParsing() {
        return peek().type() != TokenType.EOF;
    }

    private boolean match(TokenType... types) {
        for (var type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private void synchronize() {
        advance();

        while (isParsing()) {
            if (previous().type() == TokenType.SEMICOLON) {
                return;
            }

            switch (peek().type()) {
                case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> {
                    return;
                }
                default -> advance();
            }
        }
    }

    private int current;
    private final List<Token> tokens;

    private static class ParseError extends RuntimeException {

        public ParseError() {
            super();
        }

    }

}