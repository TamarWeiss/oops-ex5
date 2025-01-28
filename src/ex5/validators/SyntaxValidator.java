package ex5.validators;

import ex5.IllegalSjavaFileException;
import ex5.parser.Types;

import java.util.regex.Pattern;

/** Validates syntax rules specific to s-Java */
public class SyntaxValidator {
    // Regex patterns for comment validation
    private static final String PATTERN_COMMENT_START = "^\\s*//";
    private static final String PATTERN_COMMENT_BODY = ".*$";
    private static final String PATTERN_VALID_COMMENT = PATTERN_COMMENT_START + PATTERN_COMMENT_BODY;

    // Invalid comment patterns
    private static final String MULTI_LINE_START = "/\\*";
    private static final String MULTI_LINE_END = "\\*/";
    private static final String LEADING_SPACE_COMMENT = "^\\s+//";
    private static final String PATTERN_INVALID_COMMENT = MULTI_LINE_START + "|" + MULTI_LINE_END + "|" + LEADING_SPACE_COMMENT;

    // Whitespace validation patterns
    private static final String PATTERN_KEYWORD_GROUP = "(?:void|final|" + Types.LEGAL_TYPES + ")";
    private static final String PATTERN_NO_SPACE = "(?!\\s+)";
    private static final String PATTERN_WORD = "\\w+";
    private static final String PATTERN_REQUIRED_WHITESPACE = PATTERN_KEYWORD_GROUP + PATTERN_NO_SPACE + PATTERN_WORD;

    // Missing space pattern components
    private static final String PATTERN_TYPE_GROUP = "(void|final|" + Types.LEGAL_TYPES + ")";
    private static final String PATTERN_WORD_GROUP = "(\\w+)";
    private static final String PATTERN_MISSING_REQUIRED_SPACE = PATTERN_TYPE_GROUP + PATTERN_WORD_GROUP;

    // Method declaration pattern components
    private static final String PATTERN_TYPE = "(" + Types.LEGAL_TYPES + ")";
    private static final String PATTERN_METHOD_PARAMS = "\\s*\\(.*";
    private static final String PATTERN_INVALID_METHOD = "\\s*" + PATTERN_TYPE + "\\s+" + PATTERN_WORD + PATTERN_METHOD_PARAMS;

    // Operators pattern
    private static final String PATTERN_OPERATORS = ".*[+\\-*/%].*";
    private static final String PATTERN_STRING_LITERAL = ".*['\"].*";

    // Array syntax
    private static final String ARRAY_OPEN = "[";
    private static final String ARRAY_CLOSE = "]";

    // Error messages
    private static final String ERR_MULTILINE_COMMENT = "Multi-line comments are not allowed";
    private static final String ERR_INLINE_COMMENT = "Inline comments are not allowed in s-Java";
    private static final String ERR_INVALID_COMMENT = "Invalid comment format";
    private static final String ERR_MISSING_WHITESPACE = "Missing required whitespace after keyword";
    private static final String ERR_INVALID_METHOD = "Invalid declaration: appears to be a method declaration with non-void return type. " +
            "Only void methods are supported in s-Java";
    private static final String ERR_MISSING_TYPE_SPACE = "Missing required space between type and identifier";
    private static final String ERR_INVALID_COMMENT_SYNTAX = "Invalid comment syntax";
    private static final String ERR_NO_OPERATORS = "Operators are not allowed in s-Java";
    private static final String ERR_NO_ARRAYS = "Arrays are not supported in s-Java";

    // Compiled patterns for performance
    private static final Pattern VALID_COMMENT = Pattern.compile(PATTERN_VALID_COMMENT);
    private static final Pattern INVALID_COMMENT = Pattern.compile(PATTERN_INVALID_COMMENT);
    private static final Pattern REQUIRED_WHITESPACE = Pattern.compile(PATTERN_REQUIRED_WHITESPACE);
    private static final Pattern MISSING_REQUIRED_SPACE = Pattern.compile(".*" + PATTERN_MISSING_REQUIRED_SPACE + ".*");
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

        // Now check for inline comments in non-comment lines
        if (line.contains("//")) {
            throw new IllegalSjavaFileException(ERR_INLINE_COMMENT);
        }

        // Check for invalid comment placement
        if (INVALID_COMMENT.matcher(line).matches()) {
            throw new IllegalSjavaFileException(ERR_INVALID_COMMENT);
        }

        // Check required whitespace after keywords
        if (REQUIRED_WHITESPACE.matcher(line).matches()) {
            throw new IllegalSjavaFileException(ERR_MISSING_WHITESPACE);
        }

        if (INVALID_METHOD_DECLARATION.matcher(line).matches()) {
            throw new IllegalSjavaFileException(ERR_INVALID_METHOD);
        }
    }

    public void validateRequiredSpaces(String line) throws IllegalSjavaFileException {
        if (MISSING_REQUIRED_SPACE.matcher(line).matches()) {
            throw new IllegalSjavaFileException(ERR_MISSING_TYPE_SPACE);
        }
    }

    /**
     * Validates comment line syntax
     *
     * @param line The line to validate
     * @throws IllegalSjavaFileException if comment syntax is invalid
     */
    public void validateCommentSyntax(String line) throws IllegalSjavaFileException {
        if (!VALID_COMMENT.matcher(line).matches()) {
            throw new IllegalSjavaFileException(ERR_INVALID_COMMENT_SYNTAX);
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