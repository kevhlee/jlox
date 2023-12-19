package lox;

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

    public Expr parse() {
        try {
            return expression();
        } catch (ParseError parseError) {
            synchronize();
            return null;
        }
    }

    //
    // Expr
    //

    private Expr expression() {
        return equality();
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
        return primary();
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
        if (token.type() == TokenType.EOF) {
            Lox.syntaxError(token.line(), " at end", message);
        } else {
            Lox.syntaxError(token.line(), " at '" + token.lexeme() + "'", message);
        }
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