package ex5.parser;

import ex5.IllegalSjavaFileException;

import java.util.regex.Pattern;

/** Parser for handling variable declarations and assignments in s-Java */
public class VariableParser extends BaseParser {
    private static final String DECLARATION_PATTERN = "^\\s*(final\\s+)?(" + Types.LEGAL_TYPES + ")\\s+"
            + "(" + IDENTIFIER + "(?:\\s*=\\s*[^,;]+)?" + "(?:\\s*,\\s*" + IDENTIFIER +
            "(?:\\s*=\\s*[^,;]+)?)*)" + "\\s*;\\s*$";

    /**
     * Validates a variable declaration line according to s-Java rules
     *
     * @param line The line to validate
     * @throws IllegalSjavaFileException if the declaration is invalid
     */
    public void validateDeclaration(String line) throws IllegalSjavaFileException {
        if (!Pattern.matches(DECLARATION_PATTERN, line)) {
            throw new IllegalSjavaFileException("Invalid variable declaration format");
        }

        // Verify no double underscores at the start
        if (line.matches(".*\\b__\\w+.*")) {
            throw new IllegalSjavaFileException("Variable names cannot start with double underscore");
        }

        // Extract and validate each variable name
        String[] parts = line.split("[=,;]");
        for (String part : parts) {
            part = part.trim();
            if (part.matches(IDENTIFIER)) {
                validateIdentifier(part);
            }
        }
    }

    /**
     * Checks if a value is valid for a given type
     *
     * @param type  The variable type
     * @param value The value to check
     * @return true if the value is valid for the type
     */
    public boolean isValidValue(Types type, String value) {
        return switch (type) {
            case INT -> value.matches("^[+-]?\\d+$");
            case DOUBLE -> value.matches("^[+-]?\\d*\\.?\\d+$");
            case BOOLEAN -> value.matches("^(true|false|[+-]?\\d*\\.?\\d+)$");
            case CHAR -> value.matches("^'[^'\\\\]'$");
            case STRING -> value.matches("^\"[^\"\\\\]*\"$");
        };
    }
}