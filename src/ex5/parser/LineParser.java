package ex5.parser;

import java.util.Map;
import java.util.regex.Pattern;

import static ex5.Constants.*;

/**
 * Parser for identifying and categorizing different types of lines in s-Java code
 * uses regex patterns to determine the type of given line
 */
public class LineParser extends BaseParser {
    // Line type patterns
    private static final String METHOD_PATTERN = "^\\s*+" + VOID + WHITESPACE + IDENTIFIER
                                                 + "\\s*\\([^)]*\\)" + "\\s*\\{\\s*$";
    private static final String VARIABLE_PATTERN = "^\\s*(" + FINAL + WHITESPACE + ")?(" + Types.LEGAL_TYPES
                                                   + ")" + WHITESPACE + ".*";
    private static final String COMMENT_PATTERN = "^//.*";
    private static final String EMPTY_LINE_PATTERN = "^\\s*$";
    private static final String BLOCK_START_PATTERN = "^\\s*(if|while)\\s*\\([^)]+\\)\\s*\\{\\s*$";
    private static final String BLOCK_END_PATTERN = "^\\s*}\\s*$";
    private static final String VARIABLE_ASSIGNMENT_PATTERN = "^\\s*" + IDENTIFIER + "\\s*=\\s*.+;\\s*$";
    private static final String METHOD_CALL_PATTERN = "^\\s*" + IDENTIFIER + "\\s*\\([^)]*\\)\\s*;\\s*$";

    private final Map<String, LineType> map = Map.of(
            EMPTY_LINE_PATTERN, LineType.EMPTY,
            COMMENT_PATTERN, LineType.COMMENT,
            METHOD_PATTERN, LineType.METHOD_DECLARATION,
            VARIABLE_ASSIGNMENT_PATTERN, LineType.VARIABLE_ASSIGNMENT,
            VARIABLE_PATTERN, LineType.VARIABLE_DECLARATION,
            RETURN_PATTERN, LineType.RETURN_STATEMENT,
            BLOCK_START_PATTERN, LineType.BLOCK_START,
            BLOCK_END_PATTERN, LineType.BLOCK_END,
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
            if (Pattern.matches(entry.getKey(), line)) {
                return entry.getValue();
            }
        }
        return LineType.INVALID;
    }
}