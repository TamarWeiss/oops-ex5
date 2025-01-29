package ex5;

import ex5.parser.Types;

import java.util.Arrays;

/** Constants used throughout the s-Java verification process */
public class Constants {
    /** final keyword */
    public static final String FINAL = "final";
    /** void keyword */
    public static final String VOID = "void";
    /** Identifier pattern */
    public static final String WHITESPACE = "\\s+";
    /** Identifier pattern */
    public static final String COMMA = "\\s*,\\s*";
    /** Identifier pattern */
    public static final String EQUALS = "\\s*=\\s*";
    /** Identifier pattern */
    public static final String SEMICOLON = "\\s*;\\s*";
    /** Identifier pattern */
    public static final String TRUE = "true";
    /** Identifier pattern */
    public static final String FALSE = "false";
    /** Identifier pattern */
    public static final String LEGAL_TYPES = String.join(
            "|", Arrays.stream(Types.values()).map(Types::toString).toList()
    );
}
