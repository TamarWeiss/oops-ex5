package ex5.parser;

import ex5.IllegalSjavaFileException;
import java.util.regex.Pattern;

/**
 * Parser for identifying and categorizing different types of lines in s-Java code
 */
public class LineParser extends BaseParser {
    // Line type patterns
    private static final String METHOD_PATTERN =
            "^\\s*void\\s+" + IDENTIFIER + "\\s*\\([^)]*\\)\\s*\\{\\s*$";
    private static final String VARIABLE_PATTERN =
            "^\\s*(final\\s+)?(" + String.join("|", LEGAL_TYPES) + ")\\s+.*";
    private static final String COMMENT_PATTERN = "^\\s*//.*";
    private static final String EMPTY_LINE_PATTERN = "^\\s*$";
    private static final String RETURN_PATTERN = "^\\s*return\\s*;\\s*$";
    private static final String BLOCK_START_PATTERN = "^\\s*(if|while)\\s*\\([^)]+\\)\\s*\\{\\s*$";
    private static final String BLOCK_END_PATTERN = "^\\s*}\\s*$";

    /**
     * Represents different types of lines in s-Java
     */
    public enum LineType {
        METHOD_DECLARATION,
        VARIABLE_DECLARATION,
        COMMENT,
        EMPTY,
        RETURN_STATEMENT,
        BLOCK_START,
        BLOCK_END,
        INVALID
    }

    /**
     * Determines the type of a given line
     * @param line The line to analyze
     * @return The type of the line
     */
    public LineType getLineType(String line) {
        if (Pattern.matches(EMPTY_LINE_PATTERN, line)) {
            return LineType.EMPTY;
        }
        if (Pattern.matches(COMMENT_PATTERN, line)) {
            return LineType.COMMENT;
        }
        if (Pattern.matches(METHOD_PATTERN, line)) {
            return LineType.METHOD_DECLARATION;
        }
        if (Pattern.matches(VARIABLE_PATTERN, line)) {
            return LineType.VARIABLE_DECLARATION;
        }
        if (Pattern.matches(RETURN_PATTERN, line)) {
            return LineType.RETURN_STATEMENT;
        }
        if (Pattern.matches(BLOCK_START_PATTERN, line)) {
            return LineType.BLOCK_START;
        }
        if (Pattern.matches(BLOCK_END_PATTERN, line)) {
            return LineType.BLOCK_END;
        }
        return LineType.INVALID;
    }

    /**
     * Validates that a line ends properly according to s-Java rules
     * @param line The line to validate
     * @throws IllegalSjavaFileException if the line ending is invalid
     */
    public void validateLineEnding(String line, LineType type) throws IllegalSjavaFileException {
        switch (type) {
            case METHOD_DECLARATION:
            case BLOCK_START:
                if (!line.trim().endsWith("{")) {
                    throw new IllegalSjavaFileException("Invalid block start line ending");
                }
                break;
            case BLOCK_END:
                if (!line.trim().equals("}")) {
                    throw new IllegalSjavaFileException("Invalid block end line");
                }
                break;
            case VARIABLE_DECLARATION:
            case RETURN_STATEMENT:
                if (!line.trim().endsWith(";")) {
                    throw new IllegalSjavaFileException("Missing semicolon at line end");
                }
                break;
            case COMMENT:
            case EMPTY:
                // No validation needed for these types
                break;
            default:
                throw new IllegalSjavaFileException("Invalid line format");
        }
    }
}