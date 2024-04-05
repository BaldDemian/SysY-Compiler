package compiler;

import compiler.scope.BaseScope;
import compiler.scope.Scope;
import compiler.symbol.Symbol;
import compiler.type.Type;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.LinkedList;
import java.util.List;

public class Func extends BaseScope implements Type, Symbol {
    // a function has name, return type, arguments list
    // and every argument has its type
    public Type retType; // type of the return value
    public List<Type> argType = new LinkedList<>(); // type of arguments
    public LLVMValueRef llvmValueRef;

    public List<Type> getArgType() {
        return this.argType;
    }
    public Func(String name, Scope enclosingScope, int start, Type retType) {
        super(name, enclosingScope, start);
        this.retType = retType;
    }
    public Func(String name, Scope enclosingScope, Type retType) {
        super(name, enclosingScope);
        this.retType = retType;
    }

    @Override
    public String toString() {
        return "Func";
    }

    @Override
    public String getType() {
        return "Func";
    }

    public Type getRetType() {
        return this.retType;
    }

    @Override
    public Type getRealType() {
        return null;
    }

    public Scope getScope() {
        return this.getEnclosingScope();
    }

    public void setLlvmValueRef(LLVMValueRef llvmValueRef) {
        this.llvmValueRef = llvmValueRef;
    }
    @Override
    public LLVMValueRef getLLVMValueRef() {
        return llvmValueRef;
    }

    @Override
    public void setLLVMValueRef(LLVMValueRef llvmValueRef) {
        this.llvmValueRef = llvmValueRef;
    }

    @Override
    public void addPos(List<Integer> pos) {

    }

    @Override
    public List<List<Integer>> getPos() {
        return null;
    }

    @Override
    public void markConst() {

    }

    @Override
    public boolean isConst() {
        return false;
    }

    public void addArg(Type type) {
        this.argType.add(type);
    }
}
