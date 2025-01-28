package ex5.validators;

import ex5.IllegalSjavaFileException;
import ex5.parser.Types;

import java.util.regex.Pattern;

/** Validates syntax rules specific to s-Java */
public class SyntaxValidator {
    // Patterns for syntax validation
    private static final String VALID_COMMENT = "^\\s*//.*$";
    private static final String INVALID_COMMENT = "/\\*|\\*/|^\\s+//";
    private static final String REQUIRED_WHITESPACE = "(?:void|final|" + Types.LEGAL_TYPES + ")(?!\\s+)\\w+";
    private static final String MISSING_REQUIRED_SPACE = "(void|final|" + Types.LEGAL_TYPES + ")(\\w+)";
    private static final String INVALID_METHOD_DECLARATION = "\\s*(" + Types.LEGAL_TYPES + ")"
                                                             + "\\s+\\w+\\s*\\(.*";

    /**
     * Validates general line syntax
     *
     * @param line The line to validate
     * @throws IllegalSjavaFileException if syntax is invalid
     */
    public void validateLineSyntax(String line) throws IllegalSjavaFileException {
        // Check for illegal comment styles
        if (line.contains("/*") || line.contains("*/")) {
            throw new IllegalSjavaFileException("Multi-line comments are not allowed");
        }

        // Now check for inline comments in non-comment lines
        if (line.contains("//")) {
            throw new IllegalSjavaFileException("Inline comments are not allowed in s-Java");
        }

        // Check for invalid comment placement
        if (Pattern.matches(INVALID_COMMENT, line)) {
            throw new IllegalSjavaFileException("Invalid comment format");
        }

        // Check required whitespace after keywords
        if (Pattern.matches(REQUIRED_WHITESPACE, line)) {
            throw new IllegalSjavaFileException("Missing required whitespace after keyword");
        }

        if (line.matches(INVALID_METHOD_DECLARATION)) {
            throw new IllegalSjavaFileException(
                    "Invalid declaration: appears to be a method declaration with non-void return type. " +
                    "Only void methods are supported in s-Java"
            );
        }

    }

    public void validateRequiredSpaces(String line) throws IllegalSjavaFileException {
        if (line.matches(".*" + MISSING_REQUIRED_SPACE + ".*")) {
            throw new IllegalSjavaFileException("Missing required space between type and identifier");
        }
    }

    /**
     * Validates comment line syntax
     *
     * @param line The line to validate
     * @throws IllegalSjavaFileException if comment syntax is invalid
     */
    public void validateCommentSyntax(String line) throws IllegalSjavaFileException {
        if (!Pattern.matches(VALID_COMMENT, line)) {
            throw new IllegalSjavaFileException("Invalid comment syntax");
        }
    }

    /**
     * Validates operator absence (not allowed in s-Java)
     *
     * @param line The line to validate
     * @throws IllegalSjavaFileException if operators are found
     */
    public void validateNoOperators(String line) throws IllegalSjavaFileException {
        // Check for arithmetic and string operators
        if (line.matches(".*[+\\-*/%].*") && !line.matches(".*['\"].*")) {
            throw new IllegalSjavaFileException("Operators are not allowed in s-Java");
        }
    }

    /**
     * Validates array absence (not allowed in s-Java)
     *
     * @param line The line to validate
     * @throws IllegalSjavaFileException if array syntax is found
     */
    public void validateNoArrays(String line) throws IllegalSjavaFileException {
        if (line.contains("[") || line.contains("]")) {
            throw new IllegalSjavaFileException("Arrays are not supported in s-Java");
        }
    }
}