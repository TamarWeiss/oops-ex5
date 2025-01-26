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
    private static class Variable {
        final Types type;
        final boolean isFinal;
        boolean isInitialized;

        Variable(Types type, boolean isFinal, boolean isInitialized) {
            this.type = type;
            this.isFinal = isFinal;
            this.isInitialized = isInitialized;
        }
    }

    /** Represents a single scope level */
    private static class Scope {
        final Map<String, Variable> variables;
        final boolean isMethodScope;
        final Set<String> methodParameters;

        Scope(boolean isMethodScope) {
            this.variables = new HashMap<>();
            this.isMethodScope = isMethodScope;
            this.methodParameters = isMethodScope ? new HashSet<>() : null;
        }
    }

    private final Map<String, Variable> globalScope;
    private final Deque<Scope> scopeStack;
    private boolean inMethod;

    private int nestingLevel = 0;
    private static final int MAX_NESTING_LEVEL = Integer.MAX_VALUE;

    public ScopeValidator() {
        this.globalScope = new HashMap<>();
        this.scopeStack = new ArrayDeque<>();
        this.inMethod = false;
    }

    /**
     * Enters a new scope (method or block)
     *
     * @param isMethodScope true if this is a method scope, false for block scope
     * @throws IllegalSjavaFileException if the maximum nesting level is exceeded
     */
    public void enterScope(boolean isMethodScope) throws IllegalSjavaFileException {
        nestingLevel++;
        if (nestingLevel > MAX_NESTING_LEVEL) {
            throw new IllegalSjavaFileException("Maximum nesting level exceeded");
        }
        if (isMethodScope) {
            inMethod = true;
        }
        scopeStack.push(new Scope(isMethodScope));
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
        if (methodScope.methodParameters.contains(name)) {
            throw new IllegalSjavaFileException("Duplicate parameter name: " + name);
        }

        methodScope.methodParameters.add(name);
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

        Map<String, Variable> currentScope = scopeStack.isEmpty() ? globalScope : scopeStack.peek().variables;

        // Check for variable redeclaration in the current scope
        if (currentScope.containsKey(name)) {
            throw new IllegalSjavaFileException(
                    "Variable already declared in current scope: " + name
            );
        }

        // For global variables, ensure no other global has the same name
        if (currentScope == globalScope) {
            Variable existing = findVariable(name);
            if (existing != null) {
                throw new IllegalSjavaFileException("Global variable name conflict: " + name);
            }
        }

        // Add the variable to the current scope
        currentScope.put(name, new Variable(type, isFinal, isInitialized));
    }

    /**
     * Validates variable assignment
     *
     * @param name the variable's name
     * @throws IllegalSjavaFileException if the variable could not be assigned properly
     */
    public void validateAssignment(String name) throws IllegalSjavaFileException {
        Variable var = findVariable(name);
        if (var == null) {
            throw new IllegalSjavaFileException("Variable not declared: " + name);
        }
        if (var.isFinal && var.isInitialized) {
            throw new IllegalSjavaFileException("Cannot reassign final variable: " + name);
        }
        var.isInitialized = true;
    }

    /**
     * Validates variable access and returns its type
     *
     * @param name the variable's name
     * @return the variable's type
     * @throws IllegalSjavaFileException if the variable is not declared or found
     */
    public Types getVariableType(String name) throws IllegalSjavaFileException {
        Variable var = findVariable(name);
        if (var == null) {
            throw new IllegalSjavaFileException("Variable not declared: " + name);
        }
        return var.type;
    }

    public void validateVariableInitialisation(String name) throws IllegalSjavaFileException {
        Variable var = findVariable(name);
        // For local variables, check initialization
        if (!isGlobalVariable(name) && !var.isInitialized) {
            throw new IllegalSjavaFileException("Local variable not initialized: " + name);
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
     * @return the variable with said name if present, null otherwise
     */
    private Variable findVariable(String name) {
        // Check local scopes from innermost to outermost
        for (Scope scope : scopeStack) {
            Variable var = scope.variables.get(name);
            if (var != null) {
                return var;
            }
        }
        // Check global scope
        return globalScope.get(name);
    }

    /**
     * Checks if a variable is in the global scope
     *
     * @param name the variable's name
     * @return true of the variable is global
     */
    private boolean isGlobalVariable(String name) {
        return globalScope.containsKey(name);
    }

    /** Resets the validator state */
    public void reset() {
        globalScope.clear();
        scopeStack.clear();
        inMethod = false;
        nestingLevel = 0;
    }
}