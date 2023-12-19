package com.khl.lox;

/**
 * Lox lexical token type.
 *
 * @author Kevin Lee
 */
public enum TokenType {
    // Special

    EOF, ERROR,

    // Single character

    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, COMMA, DOT, MINUS, PLUS,
    SEMICOLON, SLASH, STAR,

    // Single or double characters

    BANG, BANG_EQUAL, EQUAL, EQUAL_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,

    // Literals

    IDENTIFIER, NUMBER, STRING,

    // Keywords

    AND, CLASS, ELSE, FALSE, FOR, FUN, IF, NIL, OR, PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,
}