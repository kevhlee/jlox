package com.khl.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A scanner that converts source code into lexical tokens.
 *
 * @author Kevin Lee
 */
public class Scanner {
    /**
     * Converts source code into lexical tokens.
     *
     * <p>
     * If a syntax error is detected during scanning, then it will be recorded as a lexical token of type
     * {@link com.khl.lox.TokenType#ERROR} where the lexeme contains the error message.
     * </p>
     *
     * @param source the source code
     * @return A list of lexical tokens
     */
    public static List<Token> scanTokens(String source) {
        return new Scanner(source).scanTokens();
    }

    //
    // Internal
    //

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>(16) {
        {
            put("and", TokenType.AND);
            put("class", TokenType.CLASS);
            put("else", TokenType.ELSE);
            put("false", TokenType.FALSE);
            put("for", TokenType.FOR);
            put("fun", TokenType.FUN);
            put("if", TokenType.IF);
            put("nil", TokenType.NIL);
            put("or", TokenType.OR);
            put("print", TokenType.PRINT);
            put("return", TokenType.RETURN);
            put("super", TokenType.SUPER);
            put("this", TokenType.THIS);
            put("true", TokenType.TRUE);
            put("var", TokenType.VAR);
            put("while", TokenType.WHILE);
        }
    };

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start;
    private int current;
    private int line = 1;

    private Scanner(String source) {
        this.source = source;
    }

    private List<Token> scanTokens() {
        while (isScanning()) {
            start = current;
            scan();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addError(String message) {
        tokens.add(new Token(TokenType.ERROR, message, null, line));
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        tokens.add(new Token(type, getLexeme(), literal, line));
    }

    private String getLexeme() {
        return source.substring(start, current);
    }

    private boolean isAlpha(char ch) {
        return ch == '_' || (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }

    private boolean isAlphaDigit(char ch) {
        return isAlpha(ch) || isDigit(ch);
    }

    private boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private boolean isScanning() {
        return current < source.length();
    }

    private boolean match(char expected) {
        if (!isScanning() || peek() != expected) {
            return false;
        }

        advance();
        return true;
    }

    private char peek() {
        if (isScanning()) {
            return source.charAt(current);
        }
        return 0;
    }

    private char peekNext() {
        if (current < source.length() - 1) {
            return source.charAt(current + 1);
        }
        return 0;
    }

    private boolean skipWhitespace() {
        var skipped = false;

        while (isScanning()) {
            switch (peek()) {
                case '\n':
                    line++;
                case ' ', '\r', '\t':
                    skipped = true;
                    advance();
                    break;
                default:
                    return skipped;
            }
        }

        return skipped;
    }

    private void scan() {
        if (skipWhitespace()) {
            return;
        }

        var ch = advance();

        switch (ch) {
            case '(' -> addToken(TokenType.LEFT_PAREN);
            case ')' -> addToken(TokenType.RIGHT_PAREN);
            case '{' -> addToken(TokenType.LEFT_BRACE);
            case '}' -> addToken(TokenType.RIGHT_BRACE);
            case ',' -> addToken(TokenType.COMMA);
            case '.' -> addToken(TokenType.DOT);
            case '-' -> addToken(TokenType.MINUS);
            case '+' -> addToken(TokenType.PLUS);
            case ';' -> addToken(TokenType.SEMICOLON);
            case '/' -> {
                if (match('/')) {
                    while (isScanning() && peek() != '\n') {
                        advance();
                    }
                } else {
                    addToken(TokenType.SLASH);
                }
            }
            case '*' -> addToken(TokenType.STAR);
            case '!' -> addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
            case '=' -> addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
            case '>' -> addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
            case '<' -> addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
            case '"' -> scanString();
            default -> {
                if (isDigit(ch)) {
                    scanNumber();
                } else if (isAlpha(ch)) {
                    scanIdentifier();
                } else {
                    addError("Unexpected character");
                }
            }
        }
    }

    private void scanNumber() {
        while (isDigit(peek())) {
            advance();
        }

        if (peek() == '.' && isDigit(peekNext())) {
            do {
                advance();
            } while (isDigit(peek()));
        }

        try {
            addToken(TokenType.NUMBER, Double.parseDouble(getLexeme()));
        } catch (NumberFormatException numberFormatException) {
            addError("Invalid number");
        }
    }

    private void scanString() {
        while (isScanning() && peek() != '"') {
            if (peek() == '\n') {
                line++;
            }
            advance();
        }

        if (!isScanning()) {
            addError("Unterminated string");
            return;
        }

        advance();
        addToken(TokenType.STRING, source.substring(start + 1, current - 1));
    }

    private void scanIdentifier() {
        while (isAlphaDigit(peek())) {
            advance();
        }
        addToken(KEYWORDS.getOrDefault(getLexeme(), TokenType.IDENTIFIER));
    }

}