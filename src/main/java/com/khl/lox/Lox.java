package com.khl.lox;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The entry point of the Lox interpreter.
 *
 * @author Kevin Lee
 */
public class Lox {
    private static final Interpreter interpreter = new Interpreter(System.out);
    private static boolean hadCompileError;
    private static boolean hadRuntimeError;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.err.println("Usage: jlox [script]");
            System.exit(64);
        }

        if (args.length == 0) {
            runREPL();
        } else {
            runFile(args[0]);
        }
    }

    private static void runFile(String filename) throws IOException {
        runInterpreter(Files.readString(Paths.get(filename), StandardCharsets.UTF_8));

        if (hadCompileError) {
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    private static void runREPL() throws IOException {
        try (var terminal = TerminalBuilder.builder().build()) {
            var lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            while (true) {
                try {
                    runInterpreter(lineReader.readLine(">>> "));

                    hadCompileError = false;
                    hadRuntimeError = false;
                } catch (EndOfFileException endOfFileException) {
                    break;
                } catch (UserInterruptException userInterruptException) {
                    System.exit(1);
                }
            }
        }
    }

    private static void runInterpreter(String source) {
        var result = Parser.parse(source);

        for (var error : result.errors()) {
            var token = error.token();

            switch (token.type()) {
                case EOF -> reportCompileError(token.line(), " at end", error.message());
                case ERROR -> reportCompileError(token.line(), "", error.message());
                default -> reportCompileError(token.line(), String.format(" at '%s'", token.lexeme()), error.message());
            }
        }

        if (hadCompileError) {
            return;
        }

        try {
            interpreter.interpret(result.statements());
        } catch (RuntimeError runtimeError) {
            reportRuntimeError(runtimeError.getLine(), runtimeError.getMessage());
        }
    }

    private static void reportCompileError(int line, String where, String message) {
        hadCompileError = true;
        System.err.printf("[line %d] Error%s: %s.\n", line, where, message);
    }

    private static void reportRuntimeError(int line, String message) {
        hadRuntimeError = true;
        System.err.printf("%s.%n[line %d]%n", message, line);
    }
}