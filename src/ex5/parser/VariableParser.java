package ex5.parser;

import ex5.IllegalSjavaFileException;

import java.util.regex.Pattern;

/** Parser for handling variable declarations and assignments in s-Java */
public class VariableParser extends BaseParser {
    // Regex for variable name according to s-Java specs
    private static final String VARIABLE_NAME = "(?:[a-zA-Z]\\w*|_[a-zA-Z0-9]\\w*)";  // Added grouping ()
//    private static final String DECLARATION_PATTERN =
//        "^\\s*(final\\s+)?(" + String.join("|", LEGAL_TYPES) + ")\\s+" +
//                "(" + VARIABLE_NAME + "(?:\\s*=\\s*[+-]?[^,;]+)?\\s*" +
//                "(?:,\\s*" + VARIABLE_NAME + "(?:\\s*=\\s*[+-]?[^,;]+)?\\s*)*)" +
//                "\\s*;\\s*$";
    // Updated declaration pattern to allow for multiple declarations on the same line
    private static final String DECLARATION_PATTERN =
            "^\\s*(final\\s+)?(" + String.join("|", LEGAL_TYPES) + ")\\s+" +
                    "(" + VARIABLE_NAME + "(?:\\s*=\\s*[^,;]+)?" +
                    "(?:\\s*,\\s*" + VARIABLE_NAME + "(?:\\s*=\\s*[^,;]+)?)*)" +
                    "\\s*;\\s*$";

    /**
     * Validates a variable declaration line according to s-Java rules
     *
     * @param line The line to validate
     * @throws IllegalSjavaFileException if the declaration is invalid
     */
    public void validateDeclaration(String line) throws IllegalSjavaFileException {
        if (!Pattern.matches(DECLARATION_PATTERN, line)) {
//            System.out.println("Pattern: " + DECLARATION_PATTERN);
            throw new IllegalSjavaFileException("Invalid variable declaration format", -1);
        }

        // Verify no double underscores at the start
        if (line.matches(".*\\b__\\w+.*")) {
            throw new IllegalSjavaFileException("Variable names cannot start with double underscore", -1);
        }

        // Extract and validate each variable name
        String[] parts = line.split("[=,;]");
        for (String part : parts) {
            part = part.trim();
            if (part.matches(VARIABLE_NAME)) {
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