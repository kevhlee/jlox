package com.khl.lox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A parser that converts source code into a Lox AST.
 *
 * @author Kevin Lee
 */
public class Parser {
    /**
     * A class for recording syntax errors detected by the parser.
     */
    public static class Error extends Throwable {
        private final Token token;
        private final String message;

        public Error(Token token, String message) {
            this.token = token;
            this.message = message;
        }

        public String message() {
            return message;
        }

        public Token token() {
            return token;
        }
    }

    /**
     * A class for containing parser results.
     */
    public record Result(List<Stmt> statements, List<Error> errors) {
        // This is a data class
    }

    /**
     * Converts source code into ASTs.
     *
     * @param source the source code
     * @return A {@link com.khl.lox.Parser.Result} object which contains the ASTs and any syntax errors detected
     * during parsing.
     */
    public static Result parse(String source) {
        return new Parser(source).parse();
    }

    private int current;
    private final List<Token> tokens = new ArrayList<>();
    private final List<Error> errors = new ArrayList<>();

    private Parser(String source) {
        for (var token : Scanner.scanTokens(source)) {
            if (token.type() == TokenType.ERROR) {
                errors.add(new Error(token, token.lexeme()));
            } else {
                tokens.add(token);
            }
        }
    }

    private Result parse() {
        var statements = new ArrayList<Stmt>();

        while (isParsing()) {
            try {
                statements.add(declaration());
            } catch (Error error) {
                errors.add(error);
                synchronize();
            }
        }

        return new Result(Collections.unmodifiableList(statements), Collections.unmodifiableList(errors));
    }

    private Token advance() {
        if (isParsing()) {
            current++;
        }
        return previous();
    }

    private boolean check(TokenType type) {
        return isParsing() && peek().type() == type;
    }

    private Token consume(TokenType type, String message) throws Error {
        if (check(type)) {
            return advance();
        }
        throw new Error(peek(), message);
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

    //
    // Stmt
    //

    private Stmt declaration() throws Error {
        if (match(TokenType.VAR)) {
            return varDeclaration();
        }

        return statement();
    }

    private Stmt.Var varDeclaration() throws Error {
        var name = consume(TokenType.IDENTIFIER, "Expect variable name");

        Expr initializer;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        } else {
            initializer = new Expr.Literal(null);
        }

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() throws Error {
        if (match(TokenType.PRINT)) {
            return printStatement();
        }

        if (match(TokenType.LEFT_BRACE)) {
            return new Stmt.Block(block());
        }

        return expressionStatement();
    }

    private List<Stmt> block() throws Error {
        var body = new ArrayList<Stmt>();

        while (isParsing() && !check(TokenType.RIGHT_BRACE)) {
            body.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block");
        return Collections.unmodifiableList(body);
    }

    private Stmt.Print printStatement() throws Error {
        var value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value");
        return new Stmt.Print(value);
    }

    private Stmt.Expression expressionStatement() throws Error {
        var expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression");
        return new Stmt.Expression(expr);
    }

    //
    // Expr
    //

    private Expr expression() throws Error {
        return assignment();
    }

    private Expr assignment() throws Error {
        var expr = equality();

        if (match(TokenType.EQUAL)) {
            var equal = previous();

            if (expr instanceof Expr.Variable(Token name)) {
                return new Expr.Assign(name, expression());
            }

            throw new Error(equal, "Invalid assignment target");
        }

        return expr;
    }

    private Expr equality() throws Error {
        var expr = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            expr = new Expr.Binary(expr, previous(), comparison());
        }
        return expr;
    }

    private Expr comparison() throws Error {
        var expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            expr = new Expr.Binary(expr, previous(), term());
        }
        return expr;
    }

    private Expr term() throws Error {
        var expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            expr = new Expr.Binary(expr, previous(), factor());
        }
        return expr;
    }

    private Expr factor() throws Error {
        var expr = unary();
        while (match(TokenType.STAR, TokenType.SLASH)) {
            expr = new Expr.Binary(expr, previous(), unary());
        }
        return expr;
    }

    private Expr unary() throws Error {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            return new Expr.Unary(previous(), unary());
        }
        return primary();
    }

    private Expr primary() throws Error {
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

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            var expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }

        throw new Error(peek(), "Expect expression");
    }

}