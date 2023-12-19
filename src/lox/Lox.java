package lox;

import jline.console.ConsoleReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The entry point of the Lox interpreter.
 *
 * @author Kevin Lee
 */
public class Lox {

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runREPL();
        }
    }


    protected static void runtimeError(RuntimeError runtimeError) {
        System.err.printf("%s\n[line %d]\n", runtimeError.getMessage(), runtimeError.token.line());
        hadRuntimeError = true;
    }

    protected static void syntaxError(int line, String message) {
        syntaxError(line, "", message);
    }

    protected static void syntaxError(int line, String where, String message) {
        System.err.printf("[line %d] Error%s: %s\n", line, where, message);
        hadSyntaxError = true;
    }

    private static void runFile(String filename) throws IOException {
        runInterpreter(Files.readString(Paths.get(filename)));

        if (hadSyntaxError) {
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    private static void runREPL() throws IOException {
        System.out.println("| Welcome to Lox!");
        System.out.println("| Type ':exit' to quit REPL.\n");

        try (var consoleReader = new ConsoleReader()) {
            while (true) {
                var line = consoleReader.readLine("lox> ");
                if (line == null || line.equals(":exit")) {
                    break;
                }

                runInterpreter(line);
                hadSyntaxError = false;
                System.out.println();
            }
        }
    }

    private static void runInterpreter(String source) {
        var lexer = new Lexer(source);
        var tokens = lexer.scanTokens();

        var parser = new Parser(tokens);
        var statements = parser.parse();

        if (hadSyntaxError) {
            return;
        }

        interpreter.interpret(statements);
    }

    private static final Interpreter interpreter = new Interpreter();
    private static boolean hadSyntaxError;
    private static boolean hadRuntimeError;

}