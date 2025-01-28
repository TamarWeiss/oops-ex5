package ex5.parser;

/** Represents different types of lines in s-Java
 * for use in parsing and validation */
public enum LineType {
    /** method declaration */
    METHOD_DECLARATION,
    /** variable declaration */
    VARIABLE_DECLARATION,
    /** comment */
    COMMENT,
    /** variable assignment */
    VARIABLE_ASSIGNMENT,
    /** method call */
    METHOD_CALL,
    /** empty line */
    EMPTY,
    /** return statement */
    RETURN_STATEMENT,
    /** block start */
    BLOCK_START,
    /** block end */
    BLOCK_END,
    /** invalid line */
    INVALID
}
