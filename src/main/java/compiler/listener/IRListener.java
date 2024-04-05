package compiler.listener;

import compiler.Func;
import compiler.gen.parser.SysYParser;
import compiler.gen.parser.SysYParserBaseListener;
import compiler.scope.GlobalScope;
import compiler.scope.LocalScope;
import compiler.scope.Scope;
import compiler.symbol.BaseSymbol;
import compiler.symbol.Symbol;
import compiler.type.Arr;
import compiler.type.Int;
import compiler.type.Type;
import compiler.type.Void;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.llvm.global.LLVM.*;

public class IRListener extends SysYParserBaseListener {
    LLVMModuleRef module;
    LLVMBuilderRef builder;
    LLVMTypeRef i32Type;

    private GlobalScope globalScope = null;
    private Scope currentScope = null;
    private Func currentFunc = null; // the function now entering!
    private int cnt = 0; // used to name different local scope
    private int LLVMCnt = 1; // used to name different LLVMValueRef

    private final ParseTreeProperty<Boolean> isConstExp = new ParseTreeProperty<>();
    private final ParseTreeProperty<Integer> constExpVal = new ParseTreeProperty<>();
    private final ParseTreeProperty<LLVMValueRef> llvmValueRefs = new ParseTreeProperty<>();
    private final Map<String, LLVMValueRef> retValues = new HashMap<>();
    private final Map<Func, LLVMTypeRef> funcTypes = new HashMap<>();

    private String generateName() {
        String name = LLVMCnt + "";
        LLVMCnt++;
        return name;
    }

    public IRListener() {
        super();
        // 初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();
        // 创建module
        this.module = LLVMModuleCreateWithName("moudle");
        // 初始化IRBuilder，后续将使用这个builder去生成LLVM IR
        this.builder = LLVMCreateBuilder();
        // 考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
        this.i32Type = LLVMInt32Type();
    }

    public LLVMModuleRef getModule() {
        return this.module;
    }

    private Type getBasicType(String s) {
        if (s.equals("int")) {
            return new Int();
        } else if (s.equals("void")) {
            return new Void();
        } else {
            return null;
        }
    }

    @Override
    public void enterCompUnit(SysYParser.CompUnitContext ctx) {
        // create a global scope
        this.globalScope = new GlobalScope(null);
        this.currentScope = this.globalScope;
    }

    @Override
    public void exitCompUnit(SysYParser.CompUnitContext ctx) {
        this.currentScope = null;
    }

    @Override
    public void enterFuncDef(SysYParser.FuncDefContext ctx) {
        // get func name
        String name = ctx.IDENT().getText();
        // get return type
        Type retType = getBasicType(ctx.funcType().getText());
        Func func = new Func(name, this.globalScope, retType);
        retValues.put(name, LLVMConstInt(i32Type, 0, 0));
        // define in the global scope
        this.globalScope.define(name, func);
        // change current scope
        this.currentScope = func;
        this.currentFunc = func;
        // build return type
        LLVMTypeRef returnType;
        if (retType.toString().equals("Void")) {
            returnType = LLVMVoidType();
        } else {
            returnType = i32Type;
        }
        // build argument types
        int s = 0;
        if (ctx.funcFParams() != null) {
            s = ctx.funcFParams().funcFParam().size();
        }
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(s);
        for (int i = 0; i < s; i++) {
            argumentTypes = argumentTypes.put(i, i32Type);
        }
        // build function type
        LLVMTypeRef ft = LLVMFunctionType(returnType, argumentTypes, /* argumentCount */ s, /* isVariadic */ 0);
        funcTypes.put(func, ft);
        // build function value
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/name, ft);
        func.setLlvmValueRef(function);
        // add function to the basic block
        LLVMBasicBlockRef block1 = LLVMAppendBasicBlock(function, /*blockName:String*/currentFunc.getName() + "Entry");
        // change basic block
        LLVMPositionBuilderAtEnd(builder, block1); //后续生成的指令将追加在block1的后面
    }

    @Override
    public void exitFuncFParams(SysYParser.FuncFParamsContext ctx) {
        for (int i = 0; i < ctx.funcFParam().size(); i++) {
            var each = ctx.funcFParam().get(i);
            // get name of this arg
            String name = each.IDENT().getText();
            this.currentFunc.addArg(new Int());
            // alloca
            LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, generateName());
            // define in the scope
            Int type = new Int();
            type.setConst(false); // this is a var integer
            Symbol symbol = new BaseSymbol(name, type, this.currentScope, pointer);
            this.currentScope.define(name, symbol);
            // store
            LLVMBuildStore(builder, LLVMGetParam(this.currentFunc.llvmValueRef, i), pointer);
        }
    }

    @Override
    public void exitFuncDef(SysYParser.FuncDefContext ctx) {
        if (currentFunc.getRetType().toString().equals("Void")) {
            LLVMBuildRetVoid(builder);
        } else {
            LLVMBuildRet(builder, /*result:LLVMValueRef*/retValues.get(this.currentFunc.getName()));
        }
        // exit this scope
        this.currentScope = this.globalScope;
        this.currentFunc = null;
    }

    @Override
    public void exitStmtReturn(SysYParser.StmtReturnContext ctx) {
        /*
        return 1;
        return 1+a+3;
        return a[1];
        return add(1, 3)
         */
        if (ctx.exp() == null) {
            return;
        }
        if (isConstExp.get(ctx.exp())) {
            int val = constExpVal.get(ctx.exp());
            retValues.put(this.currentFunc.getName(), LLVMConstInt(i32Type, val, 0));
        } else {
            retValues.put(this.currentFunc.getName(), llvmValueRefs.get(ctx.exp()));
        }
    }

    @Override
    public void enterBlock(SysYParser.BlockContext ctx) {
        // create a new local scope
        LocalScope scope = new LocalScope("LocalScope" + cnt, this.currentScope);
        cnt++;
        currentScope = scope;
    }

    @Override
    public void exitBlock(SysYParser.BlockContext ctx) {
        // leave this scope
        this.currentScope = this.currentScope.getEnclosingScope();
    }


    // exp
    @Override
    // + -
    public void exitExpPlus(SysYParser.ExpPlusContext ctx) {
        var left = ctx.exp(0);
        var right = ctx.exp(1);
        // check if left exp and right exp are both const
        if (isConstExp.get(left) && isConstExp.get(right)) {
            /* be like:
             *  1 + 3; or
             *  const int a = 3; 1 + a;
             */
            isConstExp.put(ctx, true);
            if (ctx.PLUS() != null) {
                // 做加法
                int res = constExpVal.get(left) + constExpVal.get(right);
                constExpVal.put(ctx, res);
            } else {
                int res = constExpVal.get(left) - constExpVal.get(right);
                constExpVal.put(ctx, res);
            }
        } else {
            /* be like:
             * int a, b; a + b
             */
            isConstExp.put(ctx, false);
            LLVMValueRef lhs;
            LLVMValueRef rhs;

            if (isConstExp.get(left)) {
                lhs = LLVMConstInt(i32Type, constExpVal.get(left), 0);
            } else {
                lhs = llvmValueRefs.get(left);
            }
            if (isConstExp.get(right)) {
                rhs = LLVMConstInt(i32Type, constExpVal.get(right), 0);
            } else {
                rhs = llvmValueRefs.get(right);
            }
            String name = generateName();
            if (ctx.PLUS() != null) {
                LLVMValueRef res = LLVMBuildAdd(builder, lhs, rhs, name);
                llvmValueRefs.put(ctx, res);
            } else {
                LLVMValueRef res = LLVMBuildSub(builder, lhs, rhs, name);
                llvmValueRefs.put(ctx, res);
            }
        }
    }

    @Override
    // * / %
    public void exitExpMul(SysYParser.ExpMulContext ctx) {
        var left = ctx.exp(0);
        var right = ctx.exp(1);
        // check if left exp and right exp are both const
        if (isConstExp.get(left) && isConstExp.get(right)) {
            isConstExp.put(ctx, true);
            if (ctx.MUL() != null) {
                int res = constExpVal.get(left) * constExpVal.get(right);
                constExpVal.put(ctx, res);
            } else if (ctx.DIV() != null) {
                int res = constExpVal.get(left) / constExpVal.get(right);
                constExpVal.put(ctx, res);
            } else {
                // mod
                int res = constExpVal.get(left) % constExpVal.get(right);
                constExpVal.put(ctx, res);
            }
        } else {
            isConstExp.put(ctx, false);
            LLVMValueRef lhs;
            LLVMValueRef rhs;

            if (isConstExp.get(left)) {
                lhs = LLVMConstInt(i32Type, constExpVal.get(left), 0);
            } else {
                lhs = llvmValueRefs.get(left);
            }
            if (isConstExp.get(right)) {
                rhs = LLVMConstInt(i32Type, constExpVal.get(right), 0);
            } else {
                rhs = llvmValueRefs.get(right);
            }

            String name = generateName();
            if (ctx.MUL() != null) {
                LLVMValueRef res = LLVMBuildMul(builder, lhs, rhs, name);
                llvmValueRefs.put(ctx, res);
            } else if (ctx.DIV() != null) {
                LLVMValueRef res = LLVMBuildSDiv(builder, lhs, rhs, name);
                llvmValueRefs.put(ctx, res);
            } else {
                // mod
                LLVMValueRef res = LLVMBuildSRem(builder, lhs, rhs, name);
                llvmValueRefs.put(ctx, res);
            }
        }
    }

    // ! + -
    @Override
    public void exitExpUnary(SysYParser.ExpUnaryContext ctx) {
        var left = ctx.exp();
        if (isConstExp.get(left)) {
            isConstExp.put(ctx, true);
            if (ctx.unaryOp().PLUS() != null) {
                int res = constExpVal.get(ctx);
                constExpVal.put(ctx, res);
            } else if (ctx.unaryOp().MINUS() != null) {
                int res = -constExpVal.get(ctx);
                constExpVal.put(ctx, res);
            } else {
                // !
                int res = constExpVal.get(ctx);
                if (res != 0) {
                    res = 0;
                } else {
                    res = 1;
                }
                constExpVal.put(ctx, res);
            }
        } else {
            isConstExp.put(ctx, false);
            // the operand is a var
            LLVMValueRef operand = llvmValueRefs.get(left);
            String name = generateName();
            if (ctx.unaryOp().PLUS() != null) {
                // do nothing
                llvmValueRefs.put(ctx, operand);
            } else if (ctx.unaryOp().MINUS() != null) {
                LLVMValueRef zero = LLVMConstInt(i32Type, 0, 0);
                LLVMValueRef res = LLVMBuildSub(builder, zero, operand, name);
                llvmValueRefs.put(ctx, res);
            } else {
                /* !a will be transferred into:
                 *   %5 = icmp ne i32 %4, 0
                 *   %6 = xor i1 %5, true
                 *   %7 = zext i1 %6 to i32
                 */
                String name0 = generateName();
                String name1 = generateName();
                String name2 = generateName();
                LLVMValueRef zero = LLVMConstInt(i32Type, 0, 0);
                LLVMValueRef one = LLVMConstInt(i32Type, 1, 0);
                // ne -> 33;
                LLVMValueRef tmp0 = LLVMBuildICmp(builder, 33, operand, zero, name0);
                LLVMValueRef tmp1 = LLVMBuildXor(builder, tmp0, one, name1);
                LLVMValueRef tmp2 = LLVMBuildZExt(builder, tmp1, i32Type, name2);
                llvmValueRefs.put(ctx, tmp2);
            }
        }
    }

    @Override
    public void exitExpParen(SysYParser.ExpParenContext ctx) {
        var inner = ctx.exp();
        if (isConstExp.get(inner)) {
            isConstExp.put(ctx, true);
            constExpVal.put(ctx, constExpVal.get(inner));
        } else {
            isConstExp.put(ctx, false);
            llvmValueRefs.put(ctx, llvmValueRefs.get(inner));
        }
    }

    @Override
    public void exitExpNum(SysYParser.ExpNumContext ctx) {
        isConstExp.put(ctx, true);
        // 注意进制转换
        String text = ctx.getText();
        if (text.startsWith("0X") || text.startsWith("0x")) {
            // hex
            int decimal = Integer.parseInt(text.substring(2), 16);
            constExpVal.put(ctx, decimal);
        } else if (!text.equals("0") && text.startsWith("0")) {
            // oct
            int decimal = Integer.parseInt(text.substring(1), 8);
            constExpVal.put(ctx, decimal);
        } else {
            constExpVal.put(ctx, Integer.parseInt(text));
        }
    }

    @Override
    // left value is an ident
    public void exitExpLVal(SysYParser.ExpLValContext ctx) {
        String name = ctx.lVal().IDENT().toString();
        Symbol symbol = this.currentScope.resolve(name);
        String type = symbol.getType();
        if (type.equals("Int")) {
            Int realType = (Int) symbol.getRealType();
            if (realType.getConst()) {
                // this is a const integer
                isConstExp.put(ctx, true);
                constExpVal.put(ctx, realType.getVal());
            } else {
                // this is a var
                isConstExp.put(ctx, false);
                // load
                LLVMValueRef loaded = LLVMBuildLoad2(builder, i32Type, symbol.getLLVMValueRef(), generateName());
                llvmValueRefs.put(ctx, loaded);
            }
        } else {
            // Arr
            // todo: handle arr!
            isConstExp.put(ctx, false);
            // get index
            var exp = ctx.lVal().exp(0);
            if (isConstExp.get(exp)) {
                int val = constExpVal.get(exp);
                // get the element from the array
                // we get the arr first
                Arr arr = (Arr) symbol.getRealType();
                LLVMValueRef vectorPointer = arr.vectorPointer;
                PointerPointer<Pointer> pointerPointer = new PointerPointer<>(new LLVMValueRef[]{
                        LLVMConstInt(i32Type, 0, 0),
                        LLVMConstInt(i32Type, val, 0),
                });
                LLVMValueRef res = LLVMBuildGEP(builder, vectorPointer, pointerPointer, 2, generateName());
                LLVMValueRef loaded = LLVMBuildLoad2(builder, i32Type, res, generateName());
                llvmValueRefs.put(ctx, loaded);
            } else {
                LLVMValueRef val = llvmValueRefs.get(exp);
                Arr arr = (Arr) symbol.getRealType();
                LLVMValueRef vectorPointer = arr.vectorPointer;
                PointerPointer<Pointer> pointerPointer = new PointerPointer<>(new LLVMValueRef[]{
                        LLVMConstInt(i32Type, 0, 0),
                        val,
                });
                LLVMValueRef res = LLVMBuildGEP(builder, vectorPointer, pointerPointer, 2, generateName());
                LLVMValueRef loaded = LLVMBuildLoad2(builder, i32Type, res, generateName());
                llvmValueRefs.put(ctx, loaded);
            }
        }
    }

    @Override
    public void exitExpCallFunc(SysYParser.ExpCallFuncContext ctx) {
        isConstExp.put(ctx, false);
        String name = ctx.IDENT().getText();
        Func func = (Func) this.globalScope.resolve(name);
        int s = func.argType.size();
        Pointer[] args = new Pointer[s];
        for (int i = 0; i < s; i++) {
            var argument = ctx.funcRParams().param(i).exp();
            if (isConstExp.get(argument)) {
                // ExpNum or ExpLVal
                int val = constExpVal.get(argument);
                LLVMValueRef llvmValueRef = LLVMConstInt(i32Type, val, 0);
                args[i] = llvmValueRef;
            } else {
                // ExpLVal
                LLVMValueRef llvmValueRef = llvmValueRefs.get(argument);
                args[i] = llvmValueRef;
            }
        }
        if (func.retType.toString().equals("Void")) {
            LLVMValueRef res = LLVMBuildCall2(builder, funcTypes.get(func), func.llvmValueRef, new PointerPointer<>(s).put(args), s, "");
        } else {
            LLVMValueRef res = LLVMBuildCall2(builder, funcTypes.get(func), func.llvmValueRef, new PointerPointer<>(s).put(args), s, generateName());
            llvmValueRefs.put(ctx, res);
        }
    }

    @Override
    public void exitVarDecl(SysYParser.VarDeclContext ctx) {
        for (var each : ctx.varDef()) {
            String name = each.IDENT().getText();
            if (each.L_BRACKT().size() == 0) {
                // Integer
                // alloca
                LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, generateName());
                // define in the scope
                Int type = new Int();
                type.setConst(false); // this is a var integer
                Symbol symbol = new BaseSymbol(name, type, this.currentScope, pointer);
                this.currentScope.define(name, symbol);
                // if this var is assigned
                if (each.ASSIGN() != null) {
                    if (isConstExp.get(each.initVal().exp())) {
                        // exp is expLVal or exp Num
                        /* be like:
                        int a = 3; or
                        const int a = 3;
                        int b = 3;
                         */
                        int val = constExpVal.get(each.initVal().exp());
                        type.setVal(val);
                        // store
                        LLVMBuildStore(builder, LLVMConstInt(i32Type, val, 0), pointer);
                    } else {
                        /*
                        be like:
                        int a = 3;
                        int b = a;
                        int c = d[1];
                         */
                        // store
                        LLVMBuildStore(builder, llvmValueRefs.get(each.initVal().exp()), pointer);
                    }
                }
            } else {
                // Arr
                // get length, length must be const expression like a number or a const
                int len = constExpVal.get(each.constExp(0).exp());
                LLVMTypeRef vectorType = LLVMVectorType(i32Type, len);
                LLVMValueRef vectorPointer = LLVMBuildAlloca(builder, vectorType, generateName());
                Arr arr = new Arr(len);
                arr.setLen(len);
                arr.vectorPointer = vectorPointer;
                arr.vectorType = vectorType;
                arr.setConst(false);
                int given = each.initVal().initVal().size();
                // given > 0 && given <= len
                for (int i = 0; i < len; i++) {
                    // get the pointer of the element at the given index i
                    PointerPointer<Pointer> pointerPointer = new PointerPointer<>(new LLVMValueRef[]{
                            LLVMConstInt(i32Type, 0, 0),
                            LLVMConstInt(i32Type, i, 0)
                    });
                    LLVMValueRef pointer = LLVMBuildGEP(builder, vectorPointer, pointerPointer, 2, generateName());
                    // store the value
                    if (i < given) {
                        var exp = each.initVal().initVal(i).exp();
                        if (isConstExp.get(exp)) {
                            int val = constExpVal.get(exp);
                            LLVMValueRef toStore = LLVMConstInt(i32Type, val, 0);
                            LLVMBuildStore(builder, toStore, pointer);
                        } else {
                            LLVMValueRef toStore = llvmValueRefs.get(exp);
                            LLVMBuildStore(builder, toStore, pointer);
                        }
                    } else {
                        // store 0
                        LLVMValueRef toStore = LLVMConstInt(i32Type, 0, 0);
                        LLVMBuildStore(builder, toStore, pointer);
                    }
                }
                Symbol symbol = new BaseSymbol(name, arr, this.currentScope, null); // todo:???
                this.currentScope.define(name, symbol);
            }
        }
    }

    @Override
    public void exitConstDecl(SysYParser.ConstDeclContext ctx) {
        for (var each : ctx.constDef()) {
            String name = each.IDENT().getText();
            if (each.L_BRACKT().size() == 0) {
                // Integer
                // alloca
                LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, generateName());
                // define in the scope
                Int type = new Int();
                type.setConst(true); // this is a var integer
                Symbol symbol = new BaseSymbol(name, type, this.currentScope, pointer);
                this.currentScope.define(name, symbol);
                if (isConstExp.get(each.constInitVal().constExp().exp())) {
                    // exp is expLVal or exp Num
                    int val = constExpVal.get(each.constInitVal().constExp().exp());
                    type.setVal(val);
                    // store
                    LLVMBuildStore(builder, LLVMConstInt(i32Type, val, 0), pointer);
                } else {
                        /*
                        be like:
                        int a = 3;
                        int b = a;
                        int c = d[1];
                         */
                    // store
                    LLVMBuildStore(builder, llvmValueRefs.get(each.constInitVal().constExp().exp()), pointer);
                }
            } else {
                // Arr
                int len = constExpVal.get(each.constExp(0).exp());
                LLVMTypeRef vectorType = LLVMVectorType(i32Type, len);
                LLVMValueRef vectorPointer = LLVMBuildAlloca(builder, vectorType, generateName());
                Arr arr = new Arr(len);
                arr.setLen(len);
                arr.vectorPointer = vectorPointer;
                arr.vectorType = vectorType;
                arr.setConst(true);
                //int given = each.initVal().initVal().size();
                int given = each.constInitVal().constInitVal().size();
                // given > 0 && given <= len
                for (int i = 0; i < len; i++) {
                    // get the pointer of the element at the given index i
                    PointerPointer<Pointer> pointerPointer = new PointerPointer<>(new LLVMValueRef[]{
                            LLVMConstInt(i32Type, 0, 0),
                            LLVMConstInt(i32Type, i, 0)
                    });
                    LLVMValueRef pointer = LLVMBuildGEP(builder, vectorPointer, pointerPointer, 2, generateName());
                    // store the value
                    if (i < given) {
                        var exp = each.constInitVal().constInitVal(i).constExp().exp();
                        if (isConstExp.get(exp)) {
                            int val = constExpVal.get(exp);
                            LLVMValueRef toStore = LLVMConstInt(i32Type, val, 0);
                            LLVMBuildStore(builder, toStore, pointer);
                        } else {
                            LLVMValueRef toStore = llvmValueRefs.get(exp);
                            LLVMBuildStore(builder, toStore, pointer);
                        }
                    } else {
                        // store 0
                        LLVMValueRef toStore = LLVMConstInt(i32Type, 0, 0);
                        LLVMBuildStore(builder, toStore, pointer);
                    }
                }
                Symbol symbol = new BaseSymbol(name, arr, this.currentScope, null); // todo:???
                this.currentScope.define(name, symbol);
            }
        }
    }

    @Override
    public void exitStmtAssign(SysYParser.StmtAssignContext ctx) {
        String name = ctx.lVal().IDENT().getText();
        Symbol symbol = this.currentScope.resolve(name);
        if (symbol.getType().equals("Int")) {
            var rhs = ctx.exp();
            if (isConstExp.get(rhs)) {
                // right hand side is a const value
                int val = constExpVal.get(rhs);
                Int type = (Int) symbol.getRealType();
                type.setVal(val);
                LLVMBuildStore(builder, LLVMConstInt(i32Type, val, 0), symbol.getLLVMValueRef());
            } else {
                // right hand side is a variable
                LLVMBuildStore(builder, llvmValueRefs.get(rhs), symbol.getLLVMValueRef());
            }
        } else {
            // todo: handle Arr
            // get the index
            var exp = ctx.lVal().exp(0);
            if (isConstExp.get(exp)) {
                int val = constExpVal.get(exp);
                Arr arr = (Arr) symbol.getRealType();
                LLVMValueRef vectorPointer = arr.vectorPointer;
                PointerPointer<Pointer> pointerPointer = new PointerPointer<>(new LLVMValueRef[]{
                        LLVMConstInt(i32Type, 0, 0),
                        LLVMConstInt(i32Type, val, 0),
                });
                LLVMValueRef res = LLVMBuildGEP(builder, vectorPointer, pointerPointer, 2, generateName());
                var rhs = ctx.exp();
                if (isConstExp.get(rhs)) {
                    // right hand side is a const value
                    int right = constExpVal.get(rhs);
                    LLVMBuildStore(builder, LLVMConstInt(i32Type, right, 0), res);
                } else {
                    // right hand side is a variable
                    LLVMBuildStore(builder, llvmValueRefs.get(rhs), res);
                }
            } else {
                LLVMValueRef val = llvmValueRefs.get(exp);
                Arr arr = (Arr) symbol.getRealType();
                LLVMValueRef vectorPointer = arr.vectorPointer;
                PointerPointer<Pointer> pointerPointer = new PointerPointer<>(new LLVMValueRef[]{
                        LLVMConstInt(i32Type, 0, 0),
                        val,
                });
                LLVMValueRef res = LLVMBuildGEP(builder, vectorPointer, pointerPointer, 2, generateName());
                var rhs = ctx.exp();
                if (isConstExp.get(rhs)) {
                    // right hand side is a const value
                    int right = constExpVal.get(rhs);
                    LLVMBuildStore(builder, LLVMConstInt(i32Type, right, 0), res);
                } else {
                    // right hand side is a variable
                    LLVMBuildStore(builder, llvmValueRefs.get(rhs), res);
                }
            }
        }
    }

}
