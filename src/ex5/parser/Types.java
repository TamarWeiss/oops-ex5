package ex5.parser;

import ex5.IllegalSjavaFileException;

public enum Types {
    INT("int"),
    DOUBLE("double"),
    STRING("String"),
    BOOLEAN("boolean"),
    CHAR("char");

    private final String type;

    Types(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static Types getType(final String type) throws IllegalSjavaFileException {
        for (final Types value : Types.values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        throw new IllegalSjavaFileException("Unknown type: " + type, -1);
    }
}