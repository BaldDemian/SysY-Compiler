package compiler.scope;


import compiler.symbol.Symbol;

import java.util.Map;

public interface Scope {
    void setName(String name); // set name of this scope

    String getName(); // get name of this scope

    void setEnd(int end); // set the end line number of this scope

    void define(Symbol symbol); // define(insert) a symbol in this scope
    void define(String name, Symbol symbol); // define(insert) a symbol in this scope

    Symbol resolve(String name); // Search a symbol using its name

    Map<String, Symbol> getSymbols(); // Return symbol table of this scope

    Scope getEnclosingScope(); // get outer(parent) scope of this scope
}
