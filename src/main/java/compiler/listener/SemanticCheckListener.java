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
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SemanticCheckListener extends SysYParserBaseListener {
    public boolean error = false; // mark if there are any semantic errors
    private GlobalScope globalScope = null;
    private Scope currentScope = null;
    private Func currentFunc = null; // the function now entering!
    private int cnt = 0; // used to name different local scope
    public List<Scope> scopes = new LinkedList<>(); // record every scope in the file
    private CommonTokenStream tokens = null; // tokens generated by lexer
    private boolean badFunc = false; // skip all scan in an overlap function
    private ParseTreeProperty<Type> map = new ParseTreeProperty<>();

    private Type getBasicType(String s) {
        if (s.equals("int")) {
            return new Int();
        } else if (s.equals("void")) {
            return new Void();
        } else {
            return null;
        }
    }

    public void setTokens(CommonTokenStream tokens) {
        this.tokens = tokens;
    }

    @Override
    public void enterCompUnit(SysYParser.CompUnitContext ctx) {
        // get start line
        int start = ctx.start.getLine();
        // System.out.println(start);
        // create a global scope
        this.globalScope = new GlobalScope(null, start);
        this.currentScope = this.globalScope;
        // ---------- debug --------------
        this.scopes.add(this.globalScope);
    }

    @Override
    public void exitCompUnit(SysYParser.CompUnitContext ctx) {
        // get end line
        // int end = ctx.EOF().getSymbol().getLine();
        // this.currentScope.setEnd(end);
        this.currentScope = null;
    }

    @Override
    // 0. handle errors: 4
    // 1. create a new local scope
    // 2. add this function (a symbol) to the global scope symbol table
    public void enterFuncDef(SysYParser.FuncDefContext ctx) {
        // get start line
        int line = ctx.start.getLine();
        // get func name
        String name = ctx.IDENT().getText();
        // check whether this name is used for another function or global variable or
        // gloabl const
        Symbol overlap = this.globalScope.resolve(name);
        if (overlap != null) {
            // bad function!
            System.err.println(errorMsg(4, line));
            this.currentFunc = null;
            this.currentScope = this.globalScope;
            this.badFunc = true;
            return;
        }
        // get return type
        Type retType = getBasicType(ctx.funcType().getText());
        Func func = new Func(name, this.globalScope, line, retType);
        // add this func to the symbol table of global scope
        this.globalScope.define(func);
        // change current scope
        this.currentScope = func;
        this.currentFunc = func;
        // the next line is for debugging
        this.scopes.add(func);
    }

    @Override
    public void exitFuncDef(SysYParser.FuncDefContext ctx) {
        // exit this scope
        this.currentScope = this.globalScope;
        this.currentFunc = null;
        if (this.badFunc) {
            // todo: very important!!!!
            badFunc = false; // badFunc is finally gone...
        }
    }

    @Override
    public void enterBlock(SysYParser.BlockContext ctx) {
        if (badFunc) {
            return;
        }
        // create a new local scope
        LocalScope scope = new LocalScope("LocalScope" + cnt, this.currentScope, ctx.start.getLine());
        cnt++;
        currentScope = scope;
        // -------------- debug
        this.scopes.add(scope);
    }

    @Override
    public void exitBlock(SysYParser.BlockContext ctx) {
        if (badFunc) {
            return;
        }
        // leave this scope
        this.currentScope = this.currentScope.getEnclosingScope(); // good block
    }

    @Override
    // add param to the scope of this function
    // handle error: 3
    public void enterFuncFParam(SysYParser.FuncFParamContext ctx) {
        if (badFunc) {
            return;
        }
        Type type = getBasicType(ctx.bType().getText()); // basic type
        // get name of this arg
        String name = ctx.IDENT().getText();
        // get line
        int line = ctx.start.getLine();
        // handle error
        Symbol overlap = this.currentScope.resolve(name);
        if (overlap != null) {
            Scope overlapScope = overlap.getScope();
            if (this.currentScope.equals(overlapScope)) {
                // report only in the same scope
                System.err.println(errorMsg(3, line));
                return;
            }
        }
        // if it's an array
        if (ctx.L_BRACKT().size() != 0) {
            int dimension = ctx.L_BRACKT().size();
            Arr arr = new Arr();
            arr.setDimension(dimension);
            arr.setElementType(type);
            type = arr;
        }
        this.currentFunc.addArg(type);
        // create a base symbol
        Symbol symbol = new BaseSymbol(name, type, this.currentScope, true, line);
        // get row, col of this ident
        List<Integer> pos = new ArrayList<>(2);
        Interval interval = ctx.getSourceInterval();
        // get token of the ident
        Token token = this.tokens.get(interval.a + 1);
        pos.add(token.getLine());
        pos.add(token.getCharPositionInLine());
        symbol.addPos(pos);
        // System.err.println("The line is " + pos.get(0) + " The col is " +
        // pos.get(1));
        // add to the symbol table
        this.currentScope.define(symbol);
    }

    @Override
    // "int a, b, c" is also okay
    // handle error: 3
    public void enterVarDecl(SysYParser.VarDeclContext ctx) {
        if (badFunc) {
            return;
        }
        Type type = getBasicType(ctx.bType().getText()); // basic type
        for (var each : ctx.varDef()) {
            // get name
            String name = each.IDENT().getText();
            // get line
            int line = each.start.getLine();
            // check whether no not declared
            Symbol overlap = this.currentScope.resolve(name);
            if (overlap != null) {
                Scope overlapScope = overlap.getScope();
                // todo: is this right?
                /**
                 * int func(int a) {
                 * int a = 3; // overlap in C/C++
                 * }
                 */

                if (this.currentScope.equals(overlapScope)
                        || (overlapScope.equals(this.currentScope.getEnclosingScope())
                                && overlapScope instanceof Func)) {
                    System.err.println(errorMsg(3, line));
                    continue;
                }
            }
            // no error
            // todo: refact if-else
            if (each.L_BRACKT().size() != 0) {
                // it's an array
                int dimension = each.L_BRACKT().size();
                int needDimention = each.L_BRACKT().size();
                Arr arr = new Arr(); // new type
                arr.setDimension(dimension);
                arr.setElementType(type);
                // init or not?
                boolean init = false;
                if (each.ASSIGN() != null) {
                    init = true;
                }
                // create a symbol
                Symbol symbol = new BaseSymbol(name, arr, this.currentScope, init, line);
                // get row, col of this ident
                List<Integer> pos = new ArrayList<>(2);
                Interval interval = each.getSourceInterval();
                // get token of the ident
                Token token = this.tokens.get(interval.a);
                pos.add(token.getLine());
                pos.add(token.getCharPositionInLine());
                symbol.addPos(pos);
                // System.err.println("The line is " + pos.get(0) + " The col is " +
                // pos.get(1));
                // add to the symbol table
                this.currentScope.define(symbol);
            } else {
                // not an array
                // init or not?
                boolean init = false;
                if (each.ASSIGN() != null) {
                    init = true;
                }
                // create a symbol
                Symbol symbol = new BaseSymbol(name, type, this.currentScope, init, line);
                // get row, col of this ident
                List<Integer> pos = new ArrayList<>(2);
                Interval interval = each.getSourceInterval();
                Token token = this.tokens.get(interval.a);
                pos.add(token.getLine());
                pos.add(token.getCharPositionInLine());
                symbol.addPos(pos);
                // System.err.println("The line is " + pos.get(0) + " The col is " +
                // pos.get(1));
                // add to the symbol table
                this.currentScope.define(symbol);
            }
        }
    }

    @Override
    // consider const array
    // const int a = 3, b = 4;
    // handle error: 3
    public void enterConstDecl(SysYParser.ConstDeclContext ctx) {
        if (badFunc) {
            return;
        }
        if (this.currentScope.getName() != "GlobalScope" && this.currentFunc == null) {
            return;
        }
        Type type = getBasicType(ctx.bType().getText()); // basic type
        for (var each : ctx.constDef()) {
            // get name
            String name = each.IDENT().getText();
            // get line
            int line = each.start.getLine();
            // check whether no not declared
            Symbol overlap = this.currentScope.resolve(name);
            if (overlap != null) {
                Scope overlapScope = overlap.getScope();
                if (this.currentScope.equals(overlapScope) ||
                        (overlapScope.equals(this.currentScope.getEnclosingScope())) && overlapScope instanceof Func) {
                    System.err.println(errorMsg(3, line));
                    // should't return !
                    continue;
                }
            }

            // todo: refact if-else
            if (each.L_BRACKT().size() != 0) {
                // if it's an array
                int dimension = each.L_BRACKT().size();
                Arr arr = new Arr(); // new type
                arr.setDimension(dimension);
                arr.setElementType(type);
                // create a symbol
                Symbol symbol = new BaseSymbol(name, arr, this.currentScope, true, line);
                // mark it as const
                symbol.markConst();
                Interval interval = each.getSourceInterval();
                Token token = tokens.get(interval.a);
                List<Integer> pos = new ArrayList<>(2);
                pos.add(token.getLine());
                pos.add(token.getCharPositionInLine());
                symbol.addPos(pos);
                // add to the symbol table
                this.currentScope.define(symbol);
            } else {
                // not an array
                // create a symbol
                Symbol symbol = new BaseSymbol(name, type, this.currentScope, true, line);
                // mark it as const
                symbol.markConst();
                Interval interval = each.getSourceInterval();
                Token token = tokens.get(interval.a);
                List<Integer> pos = new ArrayList<>(2);
                pos.add(token.getLine());
                pos.add(token.getCharPositionInLine());
                symbol.addPos(pos);
                // add to the symbol table
                this.currentScope.define(symbol);
            }
        }
    }

    @Override
    // left value should be defined already
    // if not defined, return immediately
    // handle error: 9
    public void enterLVal(SysYParser.LValContext ctx) {
        if (badFunc) {
            return;
        }
        // get name of this ident
        String name = ctx.IDENT().getText();
        // getline
        int line = ctx.start.getLine();
        // try get symbol with the name
        Symbol symbol = this.currentScope.resolve(name);
        if (symbol == null) {
            // todo handle error
            return; // undeclared left value
        }
        if (ctx.L_BRACKT().size() != 0) {
            // this is an array
            if (symbol.getType() != "Arr") {
                // todo: handle error
                // System.err.println(errorMsg(9, line));
                return;
            }
        }
        // no error!
        Interval interval = ctx.getSourceInterval();
        Token token = tokens.get(interval.a);
        // add postion to the symbol
        List<Integer> pos = new ArrayList<>(2);
        pos.add(token.getLine());
        pos.add(token.getCharPositionInLine());
        // System.err.println("Line is " + pos.get(0) + " Col is " + pos.get(1));
        symbol.addPos(pos);
    }

    @Override
    public void exitExpNum(SysYParser.ExpNumContext ctx) {
        if (badFunc) {
            return;
        }
        map.put(ctx, new Int());
    }

    @Override
    public void exitExpParen(SysYParser.ExpParenContext ctx) {
        if (badFunc) {
            return;
        }
        map.put(ctx, map.get(ctx.exp()));
        // System.out.println(map.get(ctx).toString());
    }

    @Override
    // todo
    // lVal # expLVal
    // lVal: IDENT (L_BRACKT exp R_BRACKT)*
    // handle array and its possible errors
    public void exitExpLVal(SysYParser.ExpLValContext ctx) {
        if (badFunc) {
            return;
        }
        // get line
        int line = ctx.start.getLine();
        String text = ctx.lVal().getText();
        String name = ctx.lVal().IDENT().toString();
        // resolve this name
        Symbol symbol = this.currentScope.resolve(name);
        if (symbol == null) {
            System.err.println(errorMsg(1, line));
            map.put(ctx, new Void());
            return;
        }
        if (symbol.getType().toString().equals("Func")) {
            map.put(ctx, (Func) symbol);
            return;
        }
        // if it is an arr
        int givenDimention = count(text, '[');
        if (givenDimention == 0) {
            // it is an int or a function
            // todo: notice:
            // dont't report a function as a left value here
            map.put(ctx, symbol.getRealType());
            return;
        }
        // it is expected to be an arr
        if (!symbol.getType().toString().equals("Arr")) {
            System.err.println(errorMsg(9, line));
            map.put(ctx, new Void());
            return;
        }
        Arr arr0 = (Arr) symbol.getRealType();
        Arr arr = new Arr();
        // record its dimention
        // report if the dimention < 0
        if (arr0.getDimension() - givenDimention < 0) {
            System.err.println(errorMsg(9, line));
            map.put(ctx, new Void());
        } else {
            arr.setDimension(arr0.getDimension() - givenDimention);
            map.put(ctx, arr);
        }
    }

    @Override
    // todo null pointer error!
    public void exitExpCallFunc(SysYParser.ExpCallFuncContext ctx) {
        if (badFunc) {
            return;
        }
        // get function name
        String name = ctx.IDENT().getText();
        // get line
        int line = ctx.start.getLine();
        // check if this function is declared
        Symbol func = this.currentScope.resolve(name);
        if (func == null) {
            System.err.println(errorMsg(2, line));
            map.put(ctx, new Void());
            return;
        }
        if (func.getType() != "Func") {
            System.err.println(errorMsg(10, line));
            map.put(ctx, new Void());
            return;
        }
        // handle error 8
        // now we know func is indeed a func
        // get func params
        // no given args
        // get params from function signature
        var realFuc = (Func) func;
        var signatures = realFuc.getArgType();
        var tmp = ctx.funcRParams();
        if (tmp == null) {
            /*
             * int add() {
             * }
             * this is ok in C/C++
             */
            if (signatures.size() == 0) {
                map.put(ctx, realFuc.retType);
                return;
            } else {
                System.err.println(errorMsg(8, line));
                map.put(ctx, new Void());
                return;
            }
        }
        // make sure tmp is not NULL!!!
        var params = tmp.param();
        if (signatures.size() != params.size()) {
            System.err.println(errorMsg(8, line));
            map.put(ctx, new Void());
            return;
        }
        for (int i = 0; i < signatures.size(); i++) {
            Type needType = signatures.get(i);
            // todo very important!!!
            Type givenType = map.get(params.get(i).exp());
            if (givenType.toString().equals("Void")) {
                return;
            }
            if (needType.toString().equals("Int")) {
                if (givenType.toString().equals("Int")) {
                    continue;
                }
                if (givenType.toString().equals("Arr")) {
                    Arr arr = (Arr) givenType;
                    if (arr.dimension == 0) {
                        continue;
                    }
                }
                System.err.println(errorMsg(8, line));
                map.put(ctx, new Void());
                // todo: should we continue look through other params?
                return;

            } else {
                // need type is int[] (make sure it is one dimention)
                if (!givenType.toString().equals("Arr")) {
                    System.err.println(errorMsg(8, line));
                    map.put(ctx, new Void());
                    // todo: should we continue look through other params?
                    return;
                }
                Arr arr = (Arr) givenType;
                if (arr.dimension != 1) {
                    System.err.println(errorMsg(8, line));
                    map.put(ctx, new Void());
                    // todo: should we continue look through other params?
                    return;
                }
            }
        }
        map.put(ctx, ((Func) func).retType);
    }

    @Override
    public void exitExpUnary(SysYParser.ExpUnaryContext ctx) {
        if (badFunc) {
            return;
        }
        int line = ctx.start.getLine();
        // get type
        Type type = map.get(ctx.exp());
        if (type.equals("Void")) {
            map.put(ctx, new Void());
            return;
        }
        if (type.equals("Func")) {
            System.err.println(errorMsg(6, line));
            map.put(ctx, new Void());
            return;
        }
        // handle arr here
        if (type.toString().equals("Arr")) {
            Arr arr = (Arr) type;
            if (arr.dimension != 0) {
                System.err.println(errorMsg(6, line));
                map.put(ctx, new Void());
                return;
            }
        }
        map.put(ctx, new Int());
    }

    @Override
    public void exitExpMul(SysYParser.ExpMulContext ctx) {
        if (badFunc) {
            return;
        }
        int line = ctx.start.getLine();
        Type type1 = map.get(ctx.exp(0));
        Type type2 = map.get(ctx.exp(1));
        if (type1.toString().equals("Void") || type2.toString().equals("Void")) {
            map.put(ctx, new Void());
            return;
        }
        if (type1.toString().equals("Func") || type2.toString().equals("Func")) {
            map.put(ctx, new Void());
            System.err.println(errorMsg(6, line));
            return;
        }
        if (type1.toString().equals("Arr")) {
            Arr arr = (Arr) type1;
            if (arr.dimension != 0) {
                map.put(ctx, new Void());
                System.err.println(errorMsg(6, line));
                return;
            }
        }
        if (type2.toString().equals("Arr")) {
            Arr arr = (Arr) type2;
            if (arr.dimension != 0) {
                map.put(ctx, new Void());
                System.err.println(errorMsg(6, line));
                return;
            }
        }
        map.put(ctx, new Int());
    }

    @Override
    public void exitExpPlus(SysYParser.ExpPlusContext ctx) {
        if (badFunc) {
            return;
        }
        int line = ctx.start.getLine();
        Type type1 = map.get(ctx.exp(0));
        Type type2 = map.get(ctx.exp(1));
        if (type1.toString().equals("Void") || type2.toString().equals("Void")) {
            map.put(ctx, new Void());
            return;
        }
        if (type1.toString().equals("Func") || type2.toString().equals("Func")) {
            map.put(ctx, new Void());
            System.err.println(errorMsg(6, line));
            return;
        }
        if (type1.toString().equals("Arr")) {
            Arr arr = (Arr) type1;
            if (arr.dimension != 0) {
                map.put(ctx, new Void());
                System.err.println(errorMsg(6, line));
                return;
            }
        }
        if (type2.toString().equals("Arr")) {
            Arr arr = (Arr) type2;
            if (arr.dimension != 0) {
                map.put(ctx, new Void());
                System.err.println(errorMsg(6, line));
                return;
            }
        }
        map.put(ctx, new Int());
    }

    // todo: handle conditions or not?
    @Override
    public void exitCond(SysYParser.CondContext ctx) {
        if (badFunc) {
            return;
        }
        int line = ctx.start.getLine();
        var exp = ctx.exp();
        if (exp == null) {
            return;
        }
        Type type = map.get(exp);
        if (type.toString().equals("Void")) {
            return;
        }
        if (type.toString().equals("Int")) {
            return;
        }
        if (type.toString().equals("Arr")) {
            Arr arr = (Arr) type;
            if (arr.dimension == 0) {
                return;
            }
        }
        System.err.println(errorMsg(6, line));
    }

    @Override
    // todo
    public void exitStmtAssign(SysYParser.StmtAssignContext ctx) {
        if (badFunc) {
            return;
        }
        int line = ctx.start.getLine();
        String name = ctx.lVal().getText();
        // todo: what if left side is an arr like a[1]
        if (name.contains("[")) {
            name = name.substring(0, name.indexOf("["));
        }
        Symbol symbol = this.currentScope.resolve(name);
        if (symbol == null) {
            System.err.println(errorMsg(1, line));
            return;
        }
        if (symbol.getType().equals("Func")) {
            System.err.println(errorMsg(11, line));
            return;
        }
        Type right = map.get(ctx.exp());
        if (right.toString().equals("Void")) {
            return;
        }
        if (!name.contains("[")) {
            if (right.toString().equals("Int")) {
                return;
            }
            if (right.toString().equals("Arr")) {
                Arr arr = (Arr) right;
                if (((Arr) right).dimension == 0) {
                    return;
                }
            }
        } else {
            Arr arr0 = (Arr) symbol.getRealType();
            int leftDimention = arr0.dimension - count(name, '[');
            if (leftDimention == 0 && right.toString().equals("Int")) {
                return;
            }
            if (right.toString().equals("Arr")) {
                Arr arr1 = (Arr) right;
                if (leftDimention == arr1.dimension) {
                    return;
                }
            }
        }
        System.err.println(errorMsg(5, line));
    }

    @Override
    // todo
    public void exitStmtReturn(SysYParser.StmtReturnContext ctx) {
        if (badFunc) {
            return;
        }
        int line = ctx.start.getLine();
        Type type = map.get(ctx.exp());
        Func func = this.currentFunc;
        if (type == null) {
            if (func.retType.toString().equals("Void")) {
                return;
            } else {
                System.err.println(errorMsg(7, line));
                return;
            }
        }
        // given return type is not null
        if (func.retType.toString().equals("Void")) {
            // need void return
            if (type.toString().equals("Void")) {
                return;
            } else {
                System.err.println(errorMsg(7, line));
                return;
            }
        }
        // need int
        // todo: what if the return type is void???
        if (type.toString().equals("Int") || type.toString().equals("Void")) {
            return;
        }
        if (type.toString().equals("Arr")) {
            Arr arr = (Arr) type;
            if (arr.dimension == 0) {
                return;
            }
        }
        System.err.println(errorMsg(7, line));
        return;
    }

    @Override
    public void exitConstDef(SysYParser.ConstDefContext ctx) {
        if (badFunc) {
            return;
        }
        int line = ctx.start.getLine();
        // get type of right hand side
        if (ctx.constInitVal() == null) {
            return;
        }
        if (ctx.constInitVal().constExp() == null) {
            return;
        }
        if (ctx.constInitVal().constExp().exp() == null) {
            return;
        }
        Type right = map.get(ctx.constInitVal().constExp().exp());
        if (right.toString().equals("Void")) {
            return;
        }
        // get type of left hand side
        Type left = this.currentScope.resolve(ctx.IDENT().toString()).getRealType();
        if (left.toString().equals("Int")) {
            if (right.toString().equals("Int")) {
                return;
            }
            if (right.toString().equals("Arr")) {
                Arr arr = (Arr) right;
                if (arr.dimension == 0) {
                    return;
                }
            }
        } else {
            // left is an arr
            if (right.toString().equals("Arr")) {
                Arr arr = (Arr) right;
                Arr arr0 = (Arr) left;
                if (arr0.dimension == arr.dimension) {
                    return;
                }
            }
        }
        System.err.println(errorMsg(5, line));
    }

    @Override
    public void exitVarDef(SysYParser.VarDefContext ctx) {
        if (badFunc) {
            return;
        }
        int line = ctx.start.getLine();
        // get type of right hand side
        if (ctx.initVal() == null) {
            return;
        }
        if (ctx.initVal().exp() == null) {
            return;
        }
        Type right = map.get(ctx.initVal().exp());
        if (right.toString().equals("Void")) {
            return;
        }
        // get type of left hand side
        Type left = this.currentScope.resolve(ctx.IDENT().toString()).getRealType();
        if (left.toString().equals("Int")) {
            if (right.toString().equals("Int")) {
                return;
            }
            if (right.toString().equals("Arr")) {
                Arr arr = (Arr) right;
                if (arr.dimension == 0) {
                    return;
                }
            }
        } else {
            // left is an arr
            if (right.toString().equals("Arr")) {
                Arr arr = (Arr) right;
                Arr arr0 = (Arr) left;
                if (arr0.dimension == arr.dimension) {
                    return;
                }
            }
        }
        System.err.println(errorMsg(5, line));
    }

    public Symbol getSymbol(int row, int col) {
        for (Scope scope : this.scopes) {
            for (Symbol symbol : scope.getSymbols().values()) {
                for (List<Integer> pos : symbol.getPos()) {
                    if (pos.get(0) == row && pos.get(1) == col) {
                        return symbol;
                    }
                }
            }
        }
        System.err.println("Why can't I find this symbol???");
        return null;
    }

    private String errorMsg(int errNo, int lineNo) {
        String[] error = new String[] {
                "",
                "Undeclared variable!", // variable is not declared but used
                "Undefined function!", // function is not declared but called
                "Duplicate variable!", // variable name is the same with another variable or function(global variable)
                "Duplicate function!", // function name is the same with another function, or a global variable
                "Unmathched assign type!", // different type in the left and right of "="
                "Unmatched operand type", // need an int operand, given another type indeed
                "Unmatched return type!", // return type is different from the funtion signature
                "Unmatched argument type!", // argument type is different from the funtion signature
                "Illegal [] operation!", // use "[]" on an int or funtion
                "Illegal () operation!", // use "()" to call function on an int or function
                "Assigning function is illegal!", // the left hand side of an "=" is a function type!
        };
        StringBuilder sb = new StringBuilder();
        sb.append("Error type ").append(errNo).append(" at Line ").append(lineNo).append(":").append(error[errNo]);
        this.error = true;
        return sb.toString();
    }

    private int count(String s, char c) {
        int res = 0;
        for (char ch : s.toCharArray()) {
            if (ch == c) {
                res++;
            }
        }
        return res;
    }

    private boolean isDigit(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }
}
