package cop5556sp17;

import cop5556sp17.AST.Dec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * This class describes a LeBlanc-Cook symbol table
 */
public class SymbolTable
{
    private class Pair<K, V>
    {
        private K key;
        private V value;

        public Pair(K key, V value)
        {
            this.key = key;
            this.value = value;
        }

        public K getKey()
        {
            return this.key;
        }

        public V getValue()
        {
            return this.value;
        }

        public boolean equals(Object other)
        {
            if( null == other )
            {
                return false;
            }
            if( !other.getClass().equals(this.getClass()) )
            {
                return false;
            }
            if( this == other )
            {
                return true;
            }
            Pair<K, V> _other = ( Pair<K, V> ) other;
            return _other.getKey() == this.key && _other.getValue() == this.value;
        }
    }

    // Records the current scope's lexical level number
    // Is a monotonic function of the lexical levels in a program
    // For uniqueness
    public int currentScope;
    private int lexicalLevel;

    // Global table storing all symbols and their declarations
    // Maps identifier name to a list
    private Map<String, Map<Integer, Dec>> decTable;

    // Scope stack that stores the most recently seen scope
    // data. Used to find which scope information to pop when leaving the scope
    private Stack<Pair<Integer, String>> scopeStack;

    private List<Integer> scopeArray()
    {
        return this.scopeStack.stream().
            map(Pair::getKey).collect(Collectors.toList());
    }

    /**
     * Called when block entered.
     * It increments the currentScope counter
     */
    public void enterScope()
    {
        this.lexicalLevel += 1;
        this.currentScope = this.lexicalLevel;
    }


    /**
     * Called upon leaving scope
     * All names defined in that scope are popped from the scope
     * when leaving said scope
     */
    public void leaveScope()
    {
        // Pop off all information pertinent to the current scope number
        while( scopeStack.peek().getKey() == this.currentScope )
        {
            scopeStack.pop();
        }
        this.currentScope = scopeStack.peek().getKey();
    }

    /**
     * Adds an identifier to the symbol table given it's name and
     * declaration
     *
     * @param ident identifier to be added to the symbol table
     * @param dec   declaration for the identifier
     * @return true if element was successfully inserted` into the symbol table
     * <br>false if the identifier has already been declared on the scope stack
     */
    public boolean insert(String ident, Dec dec)
    {
        // Is there a local declaration already?
        Pair<Integer, String> p = new Pair<>(this.currentScope, ident);
        Dec prev;
        if( scopeStack.search(p) > 0 )
        {
            return false;
        }

        Map<Integer, Dec> scope = this.decTable.get(ident);
        if( null == scope )
        {
            scope = new HashMap<>();
        }

        // Is there a previous declaration on the symbol
        // putifabsent only modifies if necessary
        prev = scope.putIfAbsent(this.currentScope, dec);
        this.decTable.put(ident, scope);

        // Also add var to scope stack
        this.scopeStack.push(new Pair<>(this.currentScope, ident));

        return null == prev;
    }

    /**
     * Returns an existing declaration corresponding to the identifier
     * This only checks whether the symbol table has the identifier,
     * and does not check if the scope stack has seen the declaration already
     *
     * @param ident Identifier whose declaration needs to be looked up
     * @return null if there is no declaration or if the list is empty
     */
    public Dec lookup(String ident)
    {
        Map<Integer, Dec> scope = this.decTable.get(ident);
        Dec d;
        String name;

        // This identifier name hasn't been encountered yet
        // i.e undeclared identifier
        if( null == scope )
        {
            return null;
        }

        // There is at least one declaration using this identifier name
        // Find and return the most recent declaration on the stack
//        List<Integer> scs = this.scopeArray();
//
//        for( int sc : scs )
//        {
//            d = scope.get(sc);
//            if( null != d )
//            {
//                name = d.getIdent().getText();
//                if( ident.equals(name) )
//                {
//                    return d;
//                }
//            }
//        }

        Stack<Pair<Integer, String>> stack = (( Stack<Pair<Integer, String>> ) this.scopeStack.clone());

        while( !stack.isEmpty() )
        {
            boolean found = ident.equals(stack.peek().getValue());

            if( found )
            {
                Pair<Integer, String> res = stack.pop();
                return scope.get(res.getKey());
            }
            stack.pop();
        }


        // If we still haven't returned, then the identifier doesn't exist
        return null;
    }

    public SymbolTable()
    {
        // Top level scope
        this.currentScope = 0;
        this.lexicalLevel = 0;
        this.decTable = new HashMap<>();
        this.scopeStack = new Stack<>();

        // Push some data onto the stack to represent the top level scope
        // I don't think anything will ever clash with this name
        scopeStack.push(new Pair<Integer, String>(-1, ""));
    }


    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        Dec d;


        for( String key : this.decTable.keySet() )
        {
            for( int l : this.decTable.get(key).keySet() )
            {
                d = this.decTable.get(key).get(l);
                sb.append(String.format(
                    "(%s, (%d, %s)), ",
                    key, l,
                    d.getFirstToken().getText()
                ));
            }
        }
        return sb.toString();
    }

}
