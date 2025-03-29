package lox;

import java.util.ArrayList;
import java.util.List;

/**
 * Lox parser.
 *
 * @author Kevin Lee
 */
public class Parser {
    private int current;
    private final List<Token> tokens;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() throws SyntaxError {
        var statements = new ArrayList<Stmt>();
        while (isParsing()) {
            statements.add(declaration());
        }
        return statements;
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

    private Token consume(TokenType type, String message) throws SyntaxError {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private SyntaxError error(Token token, String message) {
        if (token.type() == TokenType.EOF) {
            return new SyntaxError(token.line(), " at end", message);
        }
        else {
            return new SyntaxError(token.line(), " at '" + token.lexeme() + "'", message);
        }
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

    //
    // Stmt
    //

    private Stmt declaration() throws SyntaxError {
        if (match(TokenType.VAR)) {
            return varDeclaration();
        }

        return statement();
    }

    private Stmt.Var varDeclaration() throws SyntaxError {
        var name = consume(TokenType.IDENTIFIER, "Expect variable name");

        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() throws SyntaxError {
        if (match(TokenType.PRINT)) {
            return printStatement();
        }

        if (match(TokenType.LEFT_BRACE)) {
            return new Stmt.Block(block());
        }

        return expressionStatement();
    }

    private List<Stmt> block() throws SyntaxError {
        var statements = new ArrayList<Stmt>();
        while (isParsing() && !check(TokenType.RIGHT_BRACE)) {
            statements.add(statement());
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after block");
        return statements;
    }

    private Stmt.Print printStatement() throws SyntaxError {
        var value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value");
        return new Stmt.Print(value);
    }

    private Stmt.Expression expressionStatement() throws SyntaxError {
        var value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression");
        return new Stmt.Expression(value);
    }

    //
    // Expr
    //

    private Expr expression() throws SyntaxError {
        return assignment();
    }

    private Expr assignment() throws SyntaxError {
        var expr = equality();

        if (match(TokenType.EQUAL)) {
            var equal = previous();
            var value = assignment();

            if (expr instanceof Expr.Variable(Token name)) {
                return new Expr.Assign(name, value);
            }

            throw error(equal, "Invalid assignment target");
        }

        return expr;
    }

    private Expr equality() throws SyntaxError {
        var expr = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            expr = new Expr.Binary(expr, previous(), comparison());
        }
        return expr;
    }

    private Expr comparison() throws SyntaxError {
        var expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            expr = new Expr.Binary(expr, previous(), term());
        }
        return expr;
    }

    private Expr term() throws SyntaxError {
        var expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            expr = new Expr.Binary(expr, previous(), factor());
        }
        return expr;
    }

    private Expr factor() throws SyntaxError {
        var expr = unary();
        while (match(TokenType.STAR, TokenType.SLASH)) {
            expr = new Expr.Binary(expr, previous(), unary());
        }
        return expr;
    }

    private Expr unary() throws SyntaxError {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            return new Expr.Unary(previous(), unary());
        }
        return primary();
    }

    private Expr primary() throws SyntaxError {
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

        throw error(peek(), "Expect expression");
    }
}