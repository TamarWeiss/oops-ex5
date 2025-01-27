package ex5.validators;

import ex5.IllegalSjavaFileException;
import ex5.parser.LineType;
import ex5.parser.Types;

import java.util.regex.Pattern;

/** Validates syntax rules specific to s-Java */
public class SyntaxValidator {
    // Patterns for syntax validation
    private static final String VALID_COMMENT = "^\\s*//.*$";
    private static final String INVALID_COMMENT = "/\\*|\\*/|^\\s+//";
    private static final String REQUIRED_WHITESPACE = "(?:void|final|" + Types.LEGAL_TYPES + ")(?!\\s+)\\w+";
    private static final String MISSING_REQUIRED_SPACE = "(void|final|" + Types.LEGAL_TYPES + ")(\\w+)";

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
     * Validates that a line ends properly, according to s-Java rules
     *
     * @param line The line to validate
     * @param type the line's type
     * @throws IllegalSjavaFileException if the line ending is invalid
     */
    public void validateLineEnding(String line, LineType type) throws IllegalSjavaFileException {
        line = line.trim();

        switch (type) {
            case METHOD_DECLARATION:
            case BLOCK_START:
                if (!line.endsWith("{")) {
                    throw new IllegalSjavaFileException("Invalid block start line ending");
                }
                break;
            case BLOCK_END:
                if (!line.equals("}")) {
                    throw new IllegalSjavaFileException("Invalid block end line");
                }
                break;
            case METHOD_CALL:
            case VARIABLE_DECLARATION:
            case VARIABLE_ASSIGNMENT:
            case RETURN_STATEMENT:
                if (!line.trim().endsWith(";")) {
                    throw new IllegalSjavaFileException("Missing semicolon at line end");
                }
                break;
            case COMMENT:// No validation needed for comments
            case EMPTY:
                break; // No validation needed for these types
            default:
                throw new IllegalSjavaFileException("Invalid line format");
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