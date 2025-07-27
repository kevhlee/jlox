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

    private static final int MAX_ARGS = 255;
    private static final int MAX_PARAMETERS = 255;

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
            var declaration = declaration();
            if (declaration != null) {
                statements.add(declaration);
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

    private Stmt declaration() {
        try {
            if (match(TokenType.FUN)) {
                return funDeclaration("function");
            }

            if (match(TokenType.VAR)) {
                return varDeclaration();
            }

            return statement();
        } catch (Error error) {
            errors.add(error);
            synchronize();
            return null;
        }
    }

    private Stmt.Function funDeclaration(String kind) throws Error {
        var name = consume(TokenType.IDENTIFIER, "Expect %s name".formatted(kind));
        consume(TokenType.LEFT_PAREN, "Expect '(' after %s name".formatted(kind));

        var parameters = new ArrayList<Token>();

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= MAX_PARAMETERS) {
                    throw new Error(peek(), "Can't have more than %d parameters".formatted(MAX_PARAMETERS));
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name"));
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters");
        consume(TokenType.LEFT_BRACE, "Expect '{' before %s body".formatted(kind));
        return new Stmt.Function(name, Collections.unmodifiableList(parameters), block());
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
        if (match(TokenType.IF)) {
            return ifStatement();
        }

        if (match(TokenType.FOR)) {
            return forStatement();
        }

        if (match(TokenType.WHILE)) {
            return whileStatement();
        }

        if (match(TokenType.PRINT)) {
            return printStatement();
        }

        if (match(TokenType.RETURN)) {
            return returnStatement();
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

    private Stmt.If ifStatement() throws Error {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'");
        var condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition");
        var thenBranch = statement();

        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt forStatement() throws Error {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'");

        // Look for initializer

        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        // Look for condition

        Expr condition;
        if (check(TokenType.SEMICOLON)) {
            condition = new Expr.Literal(true);
        } else {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition");

        // Look for increment

        Expr increment;
        if (check(TokenType.RIGHT_PAREN)) {
            increment = null;
        } else {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ';' after for clauses");

        // Construct the 'for' statement

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(List.of(body, new Stmt.Expression(increment)));
        }

        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(List.of(initializer, body));
        }

        return body;
    }

    private Stmt.While whileStatement() throws Error {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'");
        var condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition");
        return new Stmt.While(condition, statement());
    }

    private Stmt.Print printStatement() throws Error {
        var value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value");
        return new Stmt.Print(value);
    }

    private Stmt.Return returnStatement() throws Error {
        var keyword = previous();

        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }

        consume(TokenType.SEMICOLON, "Expect after return value");
        return new Stmt.Return(keyword, value);
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
        return or();
    }

    private Expr or() throws Error {
        var expr = and();
        while (match(TokenType.OR)) {
            expr = new Expr.Logical(expr, previous(), and());
        }
        return expr;
    }

    private Expr and() throws Error {
        var expr = assignment();
        while (match(TokenType.AND)) {
            expr = new Expr.Logical(expr, previous(), assignment());
        }
        return expr;
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
        return call();
    }

    private Expr call() throws Error {
        var expr = primary();

        while (match(TokenType.LEFT_PAREN)) {
            var arguments = new ArrayList<Expr>();

            if (!check(TokenType.RIGHT_PAREN)) {
                do {
                    if (arguments.size() >= MAX_ARGS) {
                        throw new Error(peek(), "Can't have more than %d arguments".formatted(MAX_ARGS));
                    }
                    arguments.add(expression());
                } while (match(TokenType.COMMA));
            }

            expr = new Expr.Call(
                    expr, consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments"),
                    Collections.unmodifiableList(arguments));
        }

        return expr;
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