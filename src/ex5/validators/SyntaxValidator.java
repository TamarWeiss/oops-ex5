package ex5.validators;

import ex5.IllegalSjavaFileException;

import java.util.regex.Pattern;

import static ex5.Constants.*;

/** Validates syntax rules specific to s-Java */
public class SyntaxValidator {
    // Invalid comment patterns
    private static final String COMMENT = "//";
    private static final String MULTI_LINE_START = "/*";
    private static final String MULTI_LINE_END = "*/";

    // Whitespace validation patterns
    private static final String PATTERN_TYPE_GROUP = "(" + VOID + "|" + FINAL + "|" + LEGAL_TYPES + ")";
    private static final String PATTERN_NO_SPACE = "(?!" + WHITESPACE + ")";
    private static final String PATTERN_WORD = "\\w+";
    private static final String PATTERN_REQUIRED_WHITESPACE = PATTERN_TYPE_GROUP + PATTERN_NO_SPACE
                                                              + PATTERN_WORD;

    // Method declaration pattern components
    private static final String PATTERN_TYPE = "(" + LEGAL_TYPES + ")";
    private static final String PATTERN_METHOD_PARAMS = "\\s*\\(.*";
    private static final String PATTERN_INVALID_METHOD = "\\s*" + PATTERN_TYPE + WHITESPACE + PATTERN_WORD
                                                         + PATTERN_METHOD_PARAMS;
    // Operators pattern
    private static final String PATTERN_OPERATORS = ".*[+\\-*/%].*";
    private static final String PATTERN_STRING_LITERAL = ".*['\"].*";

    // Array syntax
    private static final String ARRAY_OPEN = "[";
    private static final String ARRAY_CLOSE = "]";

    // Error messages
    private static final String ERR_MULTILINE_COMMENT = "Multi-line comments are not allowed";
    private static final String ERR_INLINE_COMMENT = "Inline comments are not allowed in s-Java";
    private static final String ERR_INVALID_COMMENT = "Forbidden leading whitespace before comment";
    private static final String ERR_MISSING_WHITESPACE = "Missing required whitespace after keyword";
    private static final String ERR_INVALID_METHOD =
            "Invalid declaration: appears to be a method declaration with non-void return type. " +
            "Only void methods are supported in s-Java";
    private static final String ERR_NO_OPERATORS = "Operators are not allowed in s-Java";
    private static final String ERR_NO_ARRAYS = "Arrays are not supported in s-Java";

    // Compiled patterns for performance
    private static final Pattern REQUIRED_WHITESPACE = Pattern.compile(PATTERN_REQUIRED_WHITESPACE);
    private static final Pattern INVALID_METHOD_DECLARATION = Pattern.compile(PATTERN_INVALID_METHOD);

    /**
     * Validates general line syntax
     *
     * @param line The line to validate
     * @throws IllegalSjavaFileException if syntax is invalid
     */
    public void validateLineSyntax(String line) throws IllegalSjavaFileException {
        // Check for illegal comment styles
        if (line.contains(MULTI_LINE_START) || line.contains(MULTI_LINE_END)) {
            throw new IllegalSjavaFileException(ERR_MULTILINE_COMMENT);
        }

        // Check for invalid comment placement
        if (line.trim().startsWith(COMMENT) && !line.startsWith(COMMENT)) {
            throw new IllegalSjavaFileException(ERR_INVALID_COMMENT);
        }

        // Now check for inline comments in non-comment lines
        if (line.contains(COMMENT) && !line.startsWith(COMMENT)) {
            throw new IllegalSjavaFileException(ERR_INLINE_COMMENT);
        }

        // Check required whitespace after keywords
        if (REQUIRED_WHITESPACE.matcher(line).matches()) {
            throw new IllegalSjavaFileException(ERR_MISSING_WHITESPACE);
        }

        if (INVALID_METHOD_DECLARATION.matcher(line).matches()) {
            throw new IllegalSjavaFileException(ERR_INVALID_METHOD);
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
        if (line.matches(PATTERN_OPERATORS) && !line.matches(PATTERN_STRING_LITERAL)) {
            throw new IllegalSjavaFileException(ERR_NO_OPERATORS);
        }
    }

    /**
     * Validates array absence (not allowed in s-Java)
     *
     * @param line The line to validate
     * @throws IllegalSjavaFileException if array syntax is found
     */
    public void validateNoArrays(String line) throws IllegalSjavaFileException {
        if (line.contains(ARRAY_OPEN) || line.contains(ARRAY_CLOSE)) {
            throw new IllegalSjavaFileException(ERR_NO_ARRAYS);
        }
    }
}