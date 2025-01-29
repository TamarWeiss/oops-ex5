package ex5.validators;

import ex5.IllegalSjavaFileException;
import ex5.parser.Types;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages scope and variable tracking for s-Java verification.
 * Handles both global and local scopes, variable declarations, and access validation.
 */
public class ScopeValidator {
    // Constants for error messages
    private static final String ERR_MAX_NESTING = "Maximum nesting level reached";
    private static final String ERR_UNEXPECTED_SCOPE = "Unexpected scope end";
    private static final String ERR_MISMATCHED_SCOPE = "Mismatched scope end";
    private static final String ERR_PARAM_OUTSIDE = "Method parameter declaration outside method scope";
    private static final String ERR_DUPLICATE_PARAM = "Duplicate parameter name: ";
    private static final String ERR_DOUBLE_UNDERSCORE = "Variable names cannot start with __: ";
    private static final String ERR_VAR_REDECLARED = "Variable already declared in current scope: ";
    private static final String ERR_GLOBAL_CONFLICT = "Global variable name conflict: ";
    private static final String ERR_FINAL_REASSIGN = "Cannot reassign final variable: ";
    private static final String ERR_VAR_NOT_DECLARED = "Variable not declared: ";
    private static final String ERR_VAR_NOT_INIT = "Local variable %s not initialized";

    // Identifier validation constants
    private static final String DOUBLE_UNDERSCORE_PREFIX = "__";

    // Scope limits
    private static final int INITIAL_NESTING_LEVEL = 0;
    private static final int MAX_NESTING_LEVEL = Integer.MAX_VALUE;

    /** Represents a single scope level */
    private record Scope(boolean isMethodScope, Map<String, Variable> variables) { }

    private final Scope globalScope = new Scope(false, new HashMap<>());
    private final Deque<Scope> scopeStack = new ArrayDeque<>();
    private boolean inMethod = false;
    private int nestingLevel = INITIAL_NESTING_LEVEL;

    /**
     * Enters a new scope (method or block)
     *
     * @param isMethodScope true if this is a method scope, false for block scope
     * @throws IllegalSjavaFileException if the maximum nesting level is exceeded
     */
    public void enterScope(boolean isMethodScope) throws IllegalSjavaFileException {
        if (nestingLevel == MAX_NESTING_LEVEL) {
            throw new IllegalSjavaFileException(ERR_MAX_NESTING);
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
            throw new IllegalSjavaFileException(ERR_UNEXPECTED_SCOPE);
        }

        Scope scope = scopeStack.pop();
        if (isMethodEnd) {
            if (!scope.isMethodScope) {
                throw new IllegalSjavaFileException(ERR_MISMATCHED_SCOPE);
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
        Scope methodScope = getCurrentScope();

        if (!isMethodEnd()) {
            throw new IllegalSjavaFileException(ERR_PARAM_OUTSIDE);
        }

        if (methodScope.variables.containsKey(name)) {
            throw new IllegalSjavaFileException(ERR_DUPLICATE_PARAM + name);
        }

        methodScope.variables.put(name, new Variable(name, type, isFinal, true));
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
        if (name.startsWith(DOUBLE_UNDERSCORE_PREFIX)) {
            throw new IllegalSjavaFileException(ERR_DOUBLE_UNDERSCORE + name);
        }

        Scope currentScope = getCurrentScope();

        // Check for variable redeclaration in the current scope
        if (currentScope.variables.containsKey(name)) {
            throw new IllegalSjavaFileException(ERR_VAR_REDECLARED + name);
        }

        // For global variables, ensure no other global has the same name
        if (currentScope == globalScope) {
            try {
                findVariable(name);
                throw new IllegalSjavaFileException(ERR_GLOBAL_CONFLICT + name);
            } catch (IllegalSjavaFileException ignored) { } //no global conflict found, continue
        }

        // Add the variable to the current scope
        currentScope.variables.put(name, new Variable(name, type, isFinal, isInitialized));
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
            throw new IllegalSjavaFileException(ERR_FINAL_REASSIGN + name);
        }

        if (isGlobalVariable(name) && getCurrentScope() != globalScope) {
            //declare an identical variable in the current scope, instead of overriding the global one
            declareVariable(name, var.getType(), var.isFinal(), true);
        }
        else {
            var.setInitialized(true);
        }
    }

    /**
     * Finds and returns a variable's type
     *
     * @param name the variable's name
     * @return the variable's type
     * @throws IllegalSjavaFileException if said variable is not declared prior
     */
    public Types getVariableType(String name) throws IllegalSjavaFileException {
        return findVariable(name).getType();
    }

    /**
     * Validates that a variable is initialized
     *
     * @param name the variable's name
     * @throws IllegalSjavaFileException if the variable is not initialized
     */
    public void validateVariableInitialization(String name) throws IllegalSjavaFileException {
        if (!findVariable(name).isInitialized()) {
            throw new IllegalSjavaFileException(String.format(ERR_VAR_NOT_INIT, name));
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
        Scope currentScope = getCurrentScope();
        return currentScope != globalScope && currentScope.isMethodScope;
    }

    /** Resets the validator state */
    public void reset() {
        globalScope.variables.clear();
        scopeStack.clear();
        inMethod = false;
        nestingLevel = INITIAL_NESTING_LEVEL;
    }

    //---------------------------------------private methods------------------------------------------------

    /**
     * Fetches the current scope
     *
     * @return the current scope
     */
    private Scope getCurrentScope() {
        return scopeStack.isEmpty() ? globalScope : scopeStack.peek();
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
            throw new IllegalSjavaFileException(ERR_VAR_NOT_DECLARED + name);
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
}