package compiler.scope;

public class GlobalScope extends BaseScope {
    public GlobalScope(Scope enclosingScope) {
        super("GlobalScope", enclosingScope);
    }
    public GlobalScope(Scope enclosingScope, int start) {
        super("GlobalScope", enclosingScope, start);
    }
}
