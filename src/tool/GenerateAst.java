package tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A script for generating AST classes.
 *
 * @author Kevin Lee
 */
public class GenerateAst {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: generate-ast <output directory>");
            System.exit(64);
        }

        var path = Paths.get(args[0]);

        var expr = new TreeMap<String, List<String>>() {
            {
                put("Binary", List.of("Expr left", "Token operator", "Expr right"));
                put("Grouping", List.of("Expr expression"));
                put("Literal", List.of("Object value"));
                put("Unary", List.of("Token operator", "Expr right"));
            }
        };

        createAst(path, "Expr", expr, true);
    }

    private static void createAst(
            Path path, String baseName, Map<String, List<String>> definitions,
            boolean hasReturn)
        throws Exception {

        var formatter = new Formatter("    ");

        formatter.write("// Generated; DO NOT EDIT");
        formatter.writeNewline();
        formatter.write("package lox;");
        formatter.writeNewline();
        formatter.writeNewline();
        formatter.write("/**");
        formatter.writeNewline();
        formatter.write(" * @author Kevin Lee");
        formatter.writeNewline();
        formatter.write(" */");
        formatter.writeNewline();
        formatter.write("public interface ");
        formatter.write(baseName);
        formatter.write(" {");
        formatter.writeNewline();

        try (var indent = formatter.increaseIndent()) {
            if (hasReturn) {
                formatter.write("<R> R accept(Visitor<R> visitor);");
            } else {
                formatter.write("void accept(Visitor visitor);");
            }

            formatter.writeNewline();
            formatter.writeNewline();

            buildAstVisitor(formatter, baseName, definitions, hasReturn);

            for (var definition : definitions.entrySet()) {
                formatter.writeNewline();
                buildAstDefinition(formatter, baseName, definition, hasReturn);
            }
        }

        formatter.write("}");

        Files.writeString(path.resolve(baseName + ".java"), formatter.toString());

        System.out.printf("Generated '%s'\n", baseName);
    }

    private static void buildAstVisitor(
            Formatter formatter, String baseName, Map<String, List<String>> definitions,
            boolean hasReturn)
        throws Exception {

        formatter.write("interface Visitor");

        if (hasReturn) {
            formatter.write("<R>");
        }

        formatter.write(" {");
        formatter.writeNewline();

        try (var indent = formatter.increaseIndent()) {
            for (var definition : definitions.entrySet()) {
                if (hasReturn) {
                    formatter.write("R");
                } else {
                    formatter.write("void");
                }

                formatter.write(" visit");
                formatter.write(definition.getKey());
                formatter.write("(");
                formatter.write(definition.getKey());
                formatter.write(" ");
                formatter.write(baseName.toLowerCase());
                formatter.write(");");
                formatter.writeNewline();
            }
        }

        formatter.write("}");
        formatter.writeNewline();
    }

    private static void buildAstDefinition(
            Formatter formatter, String baseName, Map.Entry<String,
            List<String>> definition, boolean hasReturn)
        throws Exception {

        formatter.write("record ");
        formatter.write(definition.getKey());
        formatter.write("(");
        formatter.write(String.join(", ", definition.getValue()));
        formatter.write(")");
        formatter.write(" implements ");
        formatter.write(baseName);
        formatter.write(" {");
        formatter.writeNewline();

        try (var indent1 = formatter.increaseIndent()) {
            formatter.write("@Override");
            formatter.writeNewline();

            if (hasReturn) {
                formatter.write("public <R> R accept(Visitor<R> visitor) {");
            } else {
                formatter.write("public void accept(Visitor visitor) {");
            }

            formatter.writeNewline();

            try (var indent2 = formatter.increaseIndent()) {
                if (hasReturn) {
                    formatter.write("return ");
                }

                formatter.write("visitor.visit");
                formatter.write(definition.getKey());
                formatter.write("(this);");
            }

            formatter.writeNewline();
            formatter.write("}");
            formatter.writeNewline();
        }

        formatter.write("}");
        formatter.writeNewline();
    }

    private static class Formatter {

        public Formatter(String indent) {
            this.indent = indent;
        }

        public AutoCloseable increaseIndent() {
            indentLevel++;
            return () -> indentLevel--;
        }

        public void write(String s) {
            if (newline) {
                builder.append(indent.repeat(indentLevel));
                newline = false;
            }
            builder.append(s);
        }

        public void writeNewline() {
            builder.append(System.lineSeparator());
            newline = true;
        }

        @Override
        public String toString() {
            return builder.toString();
        }

        private final StringBuilder builder = new StringBuilder();
        private int indentLevel;
        private final String indent;
        private boolean newline;

    }

}