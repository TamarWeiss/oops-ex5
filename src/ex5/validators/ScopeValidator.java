package ex5.validators;

import ex5.IllegalSjavaFileException;
import ex5.parser.Types;

import java.util.*;

/**
 * Manages scope and variable tracking for s-Java verification.
 * Handles both global and local scopes, variable declarations, and access validation.
 */
public class ScopeValidator {
    /** Represents a variable and its properties */
    public static class Variable {
        private final Types type;
        private final boolean isFinal;
        private boolean isInitialized;

        public Variable(Types type, boolean isFinal, boolean isInitialized) {
            this.type = type;
            this.isFinal = isFinal;
            this.isInitialized = isInitialized;
        }

        public Types getType() {
            return type;
        }

        public boolean isFinal() {
            return isFinal;
        }

        public boolean isInitialized() {
            return isInitialized;
        }

        public void setInitialized(boolean initialized) {
            isInitialized = initialized;
        }
    }

    /** Represents a single scope level */
    private record Scope(boolean isMethodScope, Map<String, Variable> variables) { }

    private final Scope globalScope = new Scope(false, new HashMap<>());
    private final Deque<Scope> scopeStack = new ArrayDeque<>();
    private boolean inMethod = false;
    private int nestingLevel = 0;
    private static final int MAX_NESTING_LEVEL = Integer.MAX_VALUE;

    /**
     * Enters a new scope (method or block)
     *
     * @param isMethodScope true if this is a method scope, false for block scope
     * @throws IllegalSjavaFileException if the maximum nesting level is exceeded
     */
    public void enterScope(boolean isMethodScope) throws IllegalSjavaFileException {
        if (nestingLevel == MAX_NESTING_LEVEL) {
            throw new IllegalSjavaFileException("Maximum nesting level reached");
        }
        nestingLevel++;
        if (isMethodScope) {
            inMethod = true;
        }
        scopeStack.push(new Scope(isMethodScope, new HashMap<>()));
    }

    /**
     * Exits the current scope
     *
     * @param isMethodEnd true if this is ending a method, false for block
     * @throws IllegalSjavaFileException for invalid or mismatched scope end
     */
    public void exitScope(boolean isMethodEnd) throws IllegalSjavaFileException {
        nestingLevel--;
        if (scopeStack.isEmpty()) {
            throw new IllegalSjavaFileException("Unexpected scope end");
        }

        Scope scope = scopeStack.pop();
        if (isMethodEnd) {
            if (!scope.isMethodScope) {
                throw new IllegalSjavaFileException("Mismatched scope end");
            }
            inMethod = false;
        }
    }

    /**
     * Declares a method parameter
     *
     * @param name    the parameter's name
     * @param type    the parameter's type
     * @param isFinal if the parameter is final
     * @throws IllegalSjavaFileException invalid parameter declaration
     */
    public void declareParameter(String name, Types type, boolean isFinal) throws IllegalSjavaFileException {
        if (scopeStack.isEmpty() || !scopeStack.peek().isMethodScope) {
            throw new IllegalSjavaFileException("Method parameter declaration outside method scope");
        }

        Scope methodScope = scopeStack.peek();
        if (methodScope.variables.containsKey(name)) {
            throw new IllegalSjavaFileException("Duplicate parameter name: " + name);
        }

        methodScope.variables.put(name, new Variable(type, isFinal, true));
    }

    /**
     * Declares a variable in the current scope
     *
     * @param name          the variable's name
     * @param type          the variable's type
     * @param isFinal       if the variable is final
     * @param isInitialized if the variable is initialized
     * @throws IllegalSjavaFileException for invalid variable declaration
     */
    public void declareVariable(String name, Types type, boolean isFinal, boolean isInitialized)
    throws IllegalSjavaFileException {
        // Validate variable name doesn't start with double underscore
        if (name.startsWith("__")) {
            throw new IllegalSjavaFileException(
                    "Variable names cannot start with double underscore: " + name
            );
        }

        Scope currentScope = scopeStack.isEmpty() ? globalScope : scopeStack.peek();

        // Check for variable redeclaration in the current scope
        if (currentScope.variables.containsKey(name)) {
            throw new IllegalSjavaFileException("Variable already declared in current scope: " + name);
        }

        // For global variables, ensure no other global has the same name
        if (currentScope == globalScope) {
            try {
                findVariable(name);
                throw new IllegalSjavaFileException("Global variable name conflict: " + name);
            } catch (IllegalSjavaFileException ignored) { } //no global conflict found, continue
        }

        // Add the variable to the current scope
        currentScope.variables.put(name, new Variable(type, isFinal, isInitialized));
    }

    /**
     * Validates variable assignment
     *
     * @param name the variable's name
     * @throws IllegalSjavaFileException if the variable could not be assigned properly
     */
    public void validateAssignment(String name) throws IllegalSjavaFileException {
        Variable var = findVariable(name);
        if (var.isFinal() && var.isInitialized()) {
            throw new IllegalSjavaFileException("Cannot reassign final variable: " + name);
        }
        var.setInitialized(true);
    }

    /**
     * Validates variable access and returns its type
     *
     * @param name the variable's name
     * @return the variable's type
     * @throws IllegalSjavaFileException if said variable is not declared prior,
     *                                   or improper use of uninitialized variable
     */
    public Types getVariableType(String name) throws IllegalSjavaFileException {
        return findVariable(name).getType();
    }

    public void validateVariableInitialization(String name) throws IllegalSjavaFileException {
        if (!findVariable(name).isInitialized()) {
            throw new IllegalSjavaFileException("Local variable " + name + " not initialized");
        }
    }

    /**
     * Checks if currently in a method scope
     *
     * @return True if currently inside a method
     */
    public boolean isInMethod() {
        return inMethod;
    }

    /**
     * Checks if the current scope is a method's outermost scope
     *
     * @return true if we reached the method's end
     */
    public boolean isMethodEnd() {
        return !scopeStack.isEmpty() && scopeStack.peek().isMethodScope;
    }

    /**
     * Finds a variable in the current scope chain
     *
     * @param name the variable's name
     * @return the variable with said name.
     * @throws IllegalSjavaFileException if variable is not found
     */
    private Variable findVariable(String name) throws IllegalSjavaFileException {
        // Check local scopes from innermost to outermost
        Variable var;
        for (Scope scope : scopeStack) {
            if ((var = scope.variables.get(name)) != null) {
                return var;
            }
        }

        // Check global scope
        if ((var = globalScope.variables.get(name)) == null) {
            throw new IllegalSjavaFileException("Variable not declared: " + name);
        }
        return var;
    }

    /**
     * Checks if a variable is in the global scope
     *
     * @param name the variable's name
     * @return true of the variable is global
     */
    private boolean isGlobalVariable(String name) {
        return globalScope.variables.containsKey(name);
    }

    /** Resets the validator state */
    public void reset() {
        globalScope.variables.clear();
        scopeStack.clear();
        inMethod = false;
        nestingLevel = 0;
    }
}