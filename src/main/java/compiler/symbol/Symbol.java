package compiler.symbol;
import compiler.scope.Scope;
import compiler.type.Type;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.List;

public interface Symbol {
    String getName();

    void setName(String name);

    String getType();

    Type getRealType();

    Scope getScope(); // get scope of this symbol

    LLVMValueRef getLLVMValueRef();

    void setLLVMValueRef(LLVMValueRef llvmValueRef);
    void addPos(List<Integer> pos);

    List<List<Integer>> getPos();

    void markConst();

    boolean isConst();
}
