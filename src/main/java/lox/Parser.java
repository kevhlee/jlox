package lox;

import java.util.ArrayList;
import java.util.Arrays;
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
        if (match(TokenType.FUN)) {
            return function("function");
        }

        if (match(TokenType.VAR)) {
            return varDeclaration();
        }

        return statement();
    }

    private Stmt.Function function(String kind) throws SyntaxError {
        var name = consume(TokenType.IDENTIFIER, "Expect %s name".formatted(kind));
        consume(TokenType.LEFT_PAREN, "Expect '(' after %s name".formatted(kind));

        var parameters = new ArrayList<Token>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    throw error(peek(), "Can't have more than 255 parameters");
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name"));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters");

        consume(TokenType.LEFT_BRACE, "Expect '{' before %s body".formatted(kind));
        return new Stmt.Function(name, parameters, block());
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

    private List<Stmt> block() throws SyntaxError {
        var statements = new ArrayList<Stmt>();
        while (isParsing() && !check(TokenType.RIGHT_BRACE)) {
            statements.add(declaration());
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after block");
        return statements;
    }

    private Stmt.If ifStatement() throws SyntaxError {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'");
        var condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'if' condition");
        var thenBranch = statement();

        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt forStatement() throws SyntaxError {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'");

        Stmt initializer = null;
        if (match(TokenType.VAR)) {
            initializer = varDeclaration();
        }
        else if (!check(TokenType.SEMICOLON)) {
            initializer = expressionStatement();
        }

        Expr condition;
        if (check(TokenType.SEMICOLON)) {
            condition = new Expr.Literal(true);
        }
        else {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition");

        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'for' clauses");

        var body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt.While whileStatement() throws SyntaxError {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'");
        var condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'while' condition");
        return new Stmt.While(condition, statement());
    }

    private Stmt.Print printStatement() throws SyntaxError {
        var value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value");
        return new Stmt.Print(value);
    }

    private Stmt.Return returnStatement() throws SyntaxError {
        var keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return value");
        return new Stmt.Return(keyword, value);
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
        var expr = or();

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

    private Expr or() throws SyntaxError {
        var expr = and();
        while (match(TokenType.OR)) {
            expr = new Expr.Logical(expr, previous(), and());
        }
        return expr;
    }

    private Expr and() throws SyntaxError {
        var expr = equality();
        while (match(TokenType.AND)) {
            expr = new Expr.Logical(expr, previous(), equality());
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
        return call();
    }

    private Expr call() throws SyntaxError {
        var expr = primary();
        while (match(TokenType.LEFT_PAREN)) {
            expr = finishCall(expr);
        }
        return expr;
    }

    private Expr finishCall(Expr callee) throws SyntaxError {
        var arguments = new ArrayList<Expr>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    throw error(peek(), "Can't have more than 255 arguments");
                }
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }
        return new Expr.Call(callee, consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments"), arguments);
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