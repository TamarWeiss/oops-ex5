package ex5.parser;

import ex5.IllegalSjavaFileException;

/** Represents valid primitive types in s-Java */
public enum Types {
    /** int type */
    INT("int"),
    /** double type */
    DOUBLE("double"),
    /** String type */
    STRING("String"),
    /** boolean type */
    BOOLEAN("boolean"),
    /** char type */
    CHAR("char");

    private static final String UNKNOWN_TYPE_ERR = "Unknown type: ";
    private final String type;

    Types(final String type) {
        this.type = type;
    }

    /**
     * convert an enum entry into string
     *
     * @return the enum's string value
     */
    @Override
    public String toString() {
        return type;
    }

    /**
     * Returns a type according to a corresponding string
     *
     * @return the corresponding type
     * @throws IllegalSjavaFileException if no such type is found
     */
    public static Types getType(final String type) throws IllegalSjavaFileException {
        for (final Types value : Types.values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        throw new IllegalSjavaFileException(UNKNOWN_TYPE_ERR + type);
    }
}