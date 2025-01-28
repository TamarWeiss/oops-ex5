package ex5.parser;

/** Represents different types of lines in s-Java
 * for use in parsing and validation */
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
