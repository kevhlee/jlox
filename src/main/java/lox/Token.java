package lox;

/**
 * Lox lexical token.
 *
 * @author Kevin Lee
 */
public record Token(TokenType type, String lexeme, Object literal, int line) {
}