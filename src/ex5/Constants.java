package ex5;

import ex5.parser.Types;

import java.util.Arrays;

public class Constants {
    public static final String FINAL = "final";
    public static final String VOID = "void";
    public static final String WHITESPACE = "\\s+";
    public static final String COMMA = "\\s*,\\s*";
    public static final String EQUALS = "\\s*=\\s*";
    public static final String SEMICOLON = "\\s*;\\s*";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String LEGAL_TYPES = String.join(
            "|", Arrays.stream(Types.values()).map(Types::toString).toList()
    );
}
