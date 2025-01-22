package ex5.parser;

import ex5.IllegalSjavaFileException;

import java.util.Map;
import java.util.regex.Pattern;

/** Parser for identifying and categorizing different types of lines in s-Java code */
public class LineParser extends BaseParser {
    // Line type patterns
    private static final String METHOD_PATTERN = "^\\s*void\\s+" + IDENTIFIER + "\\s*\\([^)]*\\)\\s*\\{\\s*$";
    private static final String VARIABLE_PATTERN =
            "^\\s*(final\\s+)?(" + String.join("|", LEGAL_TYPES) + ")\\s+.*";
    private static final String COMMENT_PATTERN = "^\\s*//.*";
    private static final String EMPTY_LINE_PATTERN = "^\\s*$";
    private static final String RETURN_PATTERN = "^\\s*return\\s*;\\s*$";
    private static final String BLOCK_START_PATTERN = "^\\s*(if|while)\\s*\\([^)]+\\)\\s*\\{\\s*$";
    private static final String BLOCK_END_PATTERN = "^\\s*}\\s*$";
    private static final String VARIABLE_ASSIGNMENT_PATTERN =
            "^\\s*" + IDENTIFIER + "\\s*=\\s*.+;\\s*$";
    private static final String METHOD_CALL_PATTERN =
            "^\\s*" + IDENTIFIER + "\\s*\\([^)]*\\)\\s*;\\s*$";
    private static final String VALID_COMMENT_PATTERN = "^//.*$";  // No spaces before //
    private static final String COMMENT_WITH_SPACES_PATTERN = "^\\s+//.*$";  // To explicitly catch invalid comments

    /** Represents different types of lines in s-Java */
    public enum LineType {
        METHOD_DECLARATION,
        VARIABLE_DECLARATION,
        COMMENT,
        VARIABLE_ASSIGNMENT,
        METHOD_CALL,
        EMPTY,
        RETURN_STATEMENT,
        BLOCK_START,
        BLOCK_END,
        INVALID
    }

    Map<String, LineType> map = Map.of(
            EMPTY_LINE_PATTERN, LineType.EMPTY,
            COMMENT_PATTERN, LineType.COMMENT,
            METHOD_PATTERN, LineType.METHOD_DECLARATION,
            VARIABLE_PATTERN, LineType.VARIABLE_DECLARATION,
            RETURN_PATTERN, LineType.RETURN_STATEMENT,
            BLOCK_START_PATTERN, LineType.BLOCK_START,
            BLOCK_END_PATTERN, LineType.BLOCK_END,
            VARIABLE_ASSIGNMENT_PATTERN, LineType.VARIABLE_ASSIGNMENT,
            METHOD_CALL_PATTERN, LineType.METHOD_CALL

    );

    /**
     * Determines the type of given line
     *
     * @param line The line to analyze
     * @return The type of the line
     */
    public LineType getLineType(String line) {
        for (Map.Entry<String, LineType> entry : map.entrySet()) {
            if(Pattern.matches(entry.getKey(), line)) {
                return entry.getValue();
            }
        }
        return LineType.INVALID;
    }

    /**
     * Validates that a line ends properly, according to s-Java rules
     *
     * @param line The line to validate
     * @throws IllegalSjavaFileException if the line ending is invalid
     */
    public void validateLineEnding(String line, LineType type) throws IllegalSjavaFileException {
        switch (type) {
            case METHOD_DECLARATION:
            case BLOCK_START:
                if (!line.trim().endsWith("{")) {
                    throw new IllegalSjavaFileException("Invalid block start line ending", -1);
                }
                break;
            case BLOCK_END:
                if (!line.trim().equals("}")) {
                    throw new IllegalSjavaFileException("Invalid block end line", -1);
                }
                break;
            case VARIABLE_DECLARATION:
            case RETURN_STATEMENT:
                if (!line.trim().endsWith(";")) {
                    throw new IllegalSjavaFileException("Missing semicolon at line end", -1);
                }
                break;
            case COMMENT:
            case EMPTY:
                break; // No validation needed for these types
            default:
                throw new IllegalSjavaFileException("Invalid line format", -1);
        }
    }
}