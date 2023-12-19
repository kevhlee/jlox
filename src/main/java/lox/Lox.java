package lox;

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
    private static final Interpreter interpreter = new Interpreter();
    private static boolean hadSyntaxError;
    private static boolean hadRuntimeError;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        }
        else if (args.length == 1) {
            runFile(args[0]);
        }
        else {
            runREPL();
        }
    }

    private static void reportRuntimeError(RuntimeError runtimeError) {
        hadRuntimeError = true;
        System.err.printf("%s.\n[line %d]\n", runtimeError.getMessage(), runtimeError.getLine());
    }

    private static void reportSyntaxError(SyntaxError syntaxError) {
        hadSyntaxError = true;
        System.err.printf("[line %d] Error%s: %s.\n", syntaxError.getLine(), syntaxError.getWhere(), syntaxError.getMessage());
    }

    private static void runFile(String filename) throws IOException {
        runInterpreter(Files.readString(Paths.get(filename), StandardCharsets.UTF_8));

        if (hadSyntaxError) {
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
                    hadSyntaxError = false;
                    hadRuntimeError = false;
                }
                catch (EndOfFileException endOfFileException) {
                    break;
                }
                catch (UserInterruptException userInterruptException) {
                    System.exit(1);
                }
            }
        }
    }

    private static void runInterpreter(String source) {
        try {
            var tokens = new Scanner(source).scanTokens();
            var expression = new Parser(tokens).parse();

            interpreter.interpret(expression);
        }
        catch (SyntaxError syntaxError) {
            reportSyntaxError(syntaxError);
        }
        catch (RuntimeError runtimeError) {
            reportRuntimeError(runtimeError);
        }
    }
}