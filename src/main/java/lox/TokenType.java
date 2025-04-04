package lox;

/**
 * Lox lexical token type.
 *
 * @author Kevin Lee
 */
public enum TokenType {
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, COMMA, DOT, MINUS, PLUS,
    SEMICOLON, SLASH, STAR,

    BANG, BANG_EQUAL, EQUAL, EQUAL_EQUAL, GREATER, GREATER_EQUAL, LESS,
    LESS_EQUAL,

    IDENTIFIER, NUMBER, STRING,

    AND, CLASS, ELSE, FALSE, FOR, FUN, IF, NIL, OR, PRINT, RETURN, SUPER, THIS,
    TRUE, VAR, WHILE,

    EOF
}