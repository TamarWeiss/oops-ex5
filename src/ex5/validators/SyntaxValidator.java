package ex5.validators;

import ex5.IllegalSjavaFileException;

import java.util.regex.Pattern;

/** Validates syntax rules specific to s-Java */
public class SyntaxValidator {
    // Patterns for syntax validation
    private static final String VALID_COMMENT = "^\\s*//.*$";
    private static final String INVALID_COMMENT = "/\\*|\\*/|^\\s+//";
    private static final String REQUIRED_WHITESPACE =
            "(?:void|final|int|double|String|boolean|char)(?!\\s+)\\w+";
    private static final String MISSING_REQUIRED_SPACE = "(void|final|int|double|String|boolean|char)(\\w+)";

    /**
     * Validates general line syntax
     *
     * @param line The line to validate
     * @throws IllegalSjavaFileException if syntax is invalid
     */
    public void validateLineSyntax(String line) throws IllegalSjavaFileException {
        // Check for illegal comment styles
        if (line.contains("/*") || line.contains("*/")) {
            throw new IllegalSjavaFileException("Multi-line comments are not allowed", -1);
        }

        // Check for invalid comment placement
        if (Pattern.matches(INVALID_COMMENT, line)) {
            throw new IllegalSjavaFileException("Invalid comment format", -1);
        }

        // Check required whitespace after keywords
        if (Pattern.matches(REQUIRED_WHITESPACE, line)) {
            throw new IllegalSjavaFileException("Missing required whitespace after keyword", -1);
        }
    }

    public void validateRequiredSpaces(String line) throws IllegalSjavaFileException {
        if (line.matches(".*" + MISSING_REQUIRED_SPACE + ".*")) {
            throw new IllegalSjavaFileException("Missing required space between type and identifier", -1);
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
            throw new IllegalSjavaFileException("Invalid comment syntax", -1);
        }
    }

    /**
     * Validates line ending based on a line type
     *
     * @param line       The line to validate
     * @param isBlockEnd Whether this is a block ending line
     * @throws IllegalSjavaFileException if line ending is invalid
     */
    public void validateLineEnding(String line, boolean isBlockEnd) throws IllegalSjavaFileException {
        line = line.trim();

        if (isBlockEnd) {
            if (!line.equals("}")) {
                throw new IllegalSjavaFileException("Block end must be on its own line", -1);
            }
            return;
        }

        if (!line.endsWith(";") && !line.endsWith("{") && !line.endsWith("}")) {
            throw new IllegalSjavaFileException("Invalid line ending", -1);
        }

        // Check for closing brace placement
        if (line.endsWith("}") && !line.equals("}")) {
            throw new IllegalSjavaFileException("Closing brace must be on its own line", -1);
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
            throw new IllegalSjavaFileException("Operators are not allowed in s-Java", -1);
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
            throw new IllegalSjavaFileException("Arrays are not supported in s-Java", -1);
        }
    }
}