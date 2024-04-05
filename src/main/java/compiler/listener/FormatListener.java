package compiler.listener;

import compiler.gen.parser.SysYParser;
import compiler.gen.parser.SysYParserBaseListener;
import compiler.utils.Utils;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.HashSet;

public class FormatListener extends SysYParserBaseListener {
    HashSet<Integer> onePreBlankSet = new HashSet<>(); // 前面跟一个空格
    HashSet<Integer> oneSuccBlankSet = new HashSet<>(); // 后面跟一个空格
    HashSet<Integer> keywordsSet = new HashSet<>(); // 关键字，高亮为亮青色LightCyan
    HashSet<Integer> operatorsSet = new HashSet<>(); // 运算符，高亮为亮红色LightRed
    HashSet<Token> funcTokens = new HashSet<>(); // 函数名，高亮为亮黄色
    HashSet<Token> declTokens = new HashSet<>();
    HashSet<Token> blockRBrace = new HashSet<>(); // 基本块中的右花括号，需要换行
    HashSet<Token> blockLBrace = new HashSet<>(); // 基本块中的左花括号，需要换行
    HashSet<Token> funcRParen = new HashSet<>(); // int main() {}
    HashMap<Integer, String> bracktColor = new HashMap<>(); // 由于彩虹括号的颜色具有回环性质，那么利用深度%6可以快速找出对应的颜色
    HashSet<SysYParser.StmtContext> lastStmt = new HashSet<>(); // 一个基本块中的最后一条语句后不应该打印额外空行
    HashSet<SysYParser.DeclContext> lastDecl = new HashSet<>();
    int blockDepth = 0; // 初始深度为0
    int bracktDepth = -1;
    boolean inDecl = false; // 判断是否需要下划线
    boolean fisrtFunc = false; // 是否已定义第一个函数

    @Override
    public void enterProgram(SysYParser.ProgramContext ctx) {
        // 初始化
        for (int i = 12; i <= 24; i++) {
            onePreBlankSet.add(i);
        }
        for (int i = 1; i <= 7; i++) {
            oneSuccBlankSet.add(i);
        }
        for (int i = 12; i <= 24; i++) {
            oneSuccBlankSet.add(i);
        }
        oneSuccBlankSet.add(31);
        for (int i = 1; i <= 8; i++) {
            keywordsSet.add(i);
        }
        for (int i = 10; i <= 24; i++) {
            operatorsSet.add(i);
        }
        operatorsSet.add(31); // comma
        operatorsSet.add(32); // semicolon
        bracktColor.put(-1, "LightRed");
        bracktColor.put(0, "LightRed");
        bracktColor.put(1, "LightGreen");
        bracktColor.put(2, "LightYellow");
        bracktColor.put(3, "LightBlue");
        bracktColor.put(4, "LightMagenta");
        bracktColor.put(5, "LightCyan");
        super.enterProgram(ctx);
    }

    @Override
    public void exitProgram(SysYParser.ProgramContext ctx) {
        System.out.println();
    }

    @Override
    public void enterFuncDef(SysYParser.FuncDefContext ctx) {
        blockDepth = 0;
        if (!fisrtFunc) {
            fisrtFunc = true;
        } else {
            System.out.println();
        }
        funcTokens.add(ctx.IDENT().getSymbol());
        funcRParen.add(ctx.R_PAREN().getSymbol());
        bracktDepth = -1;
        super.enterFuncDef(ctx);
    }

    @Override
    public void enterExpParen(SysYParser.ExpParenContext ctx) {
        bracktDepth++;
        super.enterExpParen(ctx);
    }

    @Override
    public void exitExpParen(SysYParser.ExpParenContext ctx) {
        bracktDepth--;
        super.exitExpParen(ctx);
    }

    @Override
    public void enterExpCallFunc(SysYParser.ExpCallFuncContext ctx) {
        funcTokens.add(ctx.IDENT().getSymbol());
        super.enterExpCallFunc(ctx);
    }

    @Override
    public void enterVarDef(SysYParser.VarDefContext ctx) {
        if (!ctx.L_BRACKT().isEmpty()) {
            bracktDepth++;
        }
        declTokens.add(ctx.IDENT().getSymbol());
        super.enterVarDef(ctx);
    }

    @Override
    public void exitVarDef(SysYParser.VarDefContext ctx) {
        if (!ctx.L_BRACKT().isEmpty()) {
            bracktDepth--;
        }
        super.exitVarDef(ctx);
    }

    @Override
    public void enterFuncFParam(SysYParser.FuncFParamContext ctx) {
        bracktDepth++;
        super.enterFuncFParam(ctx);
    }

    @Override
    public void exitFuncFParam(SysYParser.FuncFParamContext ctx) {
        bracktDepth--;
        super.exitFuncFParam(ctx);
    }

    @Override
    public void enterConstDef(SysYParser.ConstDefContext ctx) {
        if (!ctx.L_BRACKT().isEmpty()) {
            bracktDepth++;
        }
        declTokens.add(ctx.IDENT().getSymbol());
        super.enterConstDef(ctx);
    }

    @Override
    public void exitConstDef(SysYParser.ConstDefContext ctx) {
        if (!ctx.L_BRACKT().isEmpty()) {
            bracktDepth--;
        }
        super.exitConstDef(ctx);
    }

    @Override
    public void enterDecl(SysYParser.DeclContext ctx) {
        inDecl = true;
        // 打印缩进
        for (int i = 0; i < blockDepth * 4; i++) {
            System.out.print(" ");
        }
        System.out.print(Utils.getSGR("Underlined"));
        super.enterDecl(ctx);
    }

    @Override
    public void exitDecl(SysYParser.DeclContext ctx) {
        inDecl = false;
        System.out.print(Utils.getSGR("Reset"));
        if (!lastDecl.contains(ctx)) {
            System.out.println(); // 每条语句单独一行
        }
        super.exitDecl(ctx);
    }

    @Override
    public void exitStmtAssign(SysYParser.StmtAssignContext ctx) {
        if (!lastStmt.contains(ctx)) {
            System.out.println();
        }
        super.exitStmtAssign(ctx);
    }

    @Override
    public void exitStmtExp(SysYParser.StmtExpContext ctx) {
        if (!lastStmt.contains(ctx)) {
            System.out.println();
        }
        super.exitStmtExp(ctx);
    }

    @Override
    public void exitStmtIf(SysYParser.StmtIfContext ctx) {
        if (!lastStmt.contains(ctx)) {
            System.out.println();
        }
        super.exitStmtIf(ctx);
    }

    @Override
    public void exitStmtWhile(SysYParser.StmtWhileContext ctx) {
        if (!lastStmt.contains(ctx)) {
            System.out.println();
        }
        super.exitStmtWhile(ctx);
    }

    @Override
    public void exitStmtBreak(SysYParser.StmtBreakContext ctx) {
        if (!lastStmt.contains(ctx)) {
            System.out.println();
        }
        super.exitStmtBreak(ctx);
    }

    @Override
    public void exitStmtReturn(SysYParser.StmtReturnContext ctx) {
        if (!lastStmt.contains(ctx)) {
            System.out.println();
        }
        super.exitStmtReturn(ctx);
    }

    @Override
    public void enterBlock(SysYParser.BlockContext ctx) {
        blockRBrace.add(ctx.R_BRACE().getSymbol());
        blockLBrace.add(ctx.L_BRACE().getSymbol());
        int sz = ctx.blockItem().size();
        SysYParser.BlockItemContext lastBlockItem = ctx.blockItem().get(sz - 1);
        if (lastBlockItem.decl() != null) {
            lastDecl.add(lastBlockItem.decl());
        } else if (lastBlockItem.stmt() != null) {
            lastStmt.add(lastBlockItem.stmt());
        }
        blockDepth++;
        bracktDepth++;
        super.enterBlock(ctx);
    }

    @Override
    public void exitBlock(SysYParser.BlockContext ctx) {
        // 恢复先前的深度
        blockDepth--;
        bracktDepth--;
        super.exitBlock(ctx);
    }

    @Override
    public void enterStmtAssign(SysYParser.StmtAssignContext ctx) {
        // 打印缩进
        for (int i = 0; i < blockDepth * 4; i++) {
            System.out.print(" ");
        }
        super.enterStmtAssign(ctx);
    }

    @Override
    public void enterStmtExp(SysYParser.StmtExpContext ctx) {
        // 打印缩进
        for (int i = 0; i < blockDepth * 4; i++) {
            System.out.print(" ");
        }
        super.enterStmtExp(ctx);
    }


    @Override
    public void enterStmtIf(SysYParser.StmtIfContext ctx) {
        // 打印缩进
        for (int i = 0; i < blockDepth * 4; i++) {
            System.out.print(" ");
        }
        super.enterStmtIf(ctx);
    }

    @Override
    public void enterStmtWhile(SysYParser.StmtWhileContext ctx) {
        // 打印缩进
        for (int i = 0; i < blockDepth * 4; i++) {
            System.out.print(" ");
        }
        super.enterStmtWhile(ctx);
    }

    @Override
    public void enterStmtBreak(SysYParser.StmtBreakContext ctx) {
        // 打印缩进
        for (int i = 0; i < blockDepth * 4; i++) {
            System.out.print(" ");
        }
        super.enterStmtBreak(ctx);
    }

    @Override
    public void enterStmtContinue(SysYParser.StmtContinueContext ctx) {
        // 打印缩进
        for (int i = 0; i < blockDepth * 4; i++) {
            System.out.print(" ");
        }
        super.enterStmtContinue(ctx);
    }

    @Override
    public void enterStmtReturn(SysYParser.StmtReturnContext ctx) {
        // 打印缩进
        for (int i = 0; i < blockDepth * 4; i++) {
            System.out.print(" ");
        }
        super.enterStmtReturn(ctx);
    }

    @Override
    public void exitStmtContinue(SysYParser.StmtContinueContext ctx) {
        System.out.println();
        super.exitStmtContinue(ctx);
    }

    @Override
    public void enterConstInitVal(SysYParser.ConstInitValContext ctx) {
        bracktDepth++;
    }

    @Override
    public void exitConstInitVal(SysYParser.ConstInitValContext ctx) {
        bracktDepth--;
    }

    @Override
    public void enterInitVal(SysYParser.InitValContext ctx) {
        bracktDepth++;
    }

    @Override
    public void exitInitVal(SysYParser.InitValContext ctx) {
        bracktDepth--;
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        System.out.print(Utils.getSGR("White")); // 默认高亮为白色
        Token token = node.getSymbol();
        int typeIndex = token.getType(); // 从1开始
        if (typeIndex == -1) {
            return; // EOF不处理
        }
        String text = node.getText();
        // 特判 return
        if (typeIndex == 9) {
            System.out.print(Utils.getSGR("LightCyan")); // 亮青色
            System.out.print("return");
            if (node.getParent().getChildCount() != 2) {
                // 情况1：return stmt;
                System.out.print(" ");
            }  // 情况2：return;

            return;
        }
        // 特判 +/-
        if (typeIndex == 10 || typeIndex == 11) {
            if (node.getParent().getChildCount() == 1) {
                // 单目操作符，形如 +3/-3
                System.out.print(Utils.getSGR("LightRed"));
                System.out.print(node.getText());
            } else {
                // 双目操作符，形如 a+b/a-b
                // 需额外打印一个前导空格，一个后继空格
                if (inDecl) {
                    // 空格不需要任何样式
                    System.out.print(Utils.getSGR("Reset") + " ");
                    // 恢复下划线
                    System.out.print(Utils.getSGR("Underlined"));
                } else {
                    System.out.print(" ");
                }
                System.out.print(Utils.getSGR("LightRed"));
                System.out.print(node.getText());
                if (inDecl) {
                    // 空格不需要任何样式
                    System.out.print(Utils.getSGR("Reset") + " ");
                    // 恢复下划线
                    System.out.print(Utils.getSGR("Underlined"));
                } else {
                    System.out.print(" ");
                }
            }
            return;
        }
        // 特判 !
        if (typeIndex == 22) {
            System.out.print(Utils.getSGR("LightRed"));
            System.out.print(node.getText());
            return;
        }
        // 特判 {
        if (typeIndex == 27) {
            if (blockLBrace.contains(token)) {
                for (int i = 0; i < (blockDepth - 1) * 4; i++) {
                    System.out.print(" ");
                }
                String color = bracktColor.get(bracktDepth % 6);
                System.out.print(Utils.getSGR(color));
                System.out.print("{");
                System.out.print(Utils.getSGR("ResetFore"));
                System.out.println();
                return;
            }
        }
        // 特判 }
        if (typeIndex == 28) {
            if (blockRBrace.contains(token)) {
                // 未用于声明语句（即数组赋值）的右花括号另起一行
                System.out.println();
                // 打印缩进
                for (int i = 0; i < (blockDepth - 1) * 4; i++) {
                    System.out.print(" ");
                }
                String color = bracktColor.get(bracktDepth % 6);
                System.out.print(Utils.getSGR(color));
                System.out.print("}");
                System.out.print(Utils.getSGR("ResetFore"));
                return;
            }
        }
        // 其他一般终结符
        // 打印前导的空格
        if (onePreBlankSet.contains(typeIndex)) {
            if (inDecl) {
                // 空格不需要任何样式
                System.out.print(Utils.getSGR("Reset") + " ");
                // 恢复下划线
                System.out.print(Utils.getSGR("Underlined"));
            } else {
                System.out.print(" ");
            }
        }
        // 打印用于控制颜色或是下换线的转义序列
        if (operatorsSet.contains(typeIndex)) {
            // 操作符，亮红色
            System.out.print(Utils.getSGR("LightRed"));
        }
        if (keywordsSet.contains(typeIndex)) {
            // 关键字，亮青色
            System.out.print(Utils.getSGR("LightCyan"));
        }
        if (typeIndex == 34) {
            // 整型字面量，品红色
            System.out.print(Utils.getSGR("Magenta"));
        }
        if (typeIndex == 33) {
            // 标识符，可能是变量名或函数名
            // 函数名又有函数定义和函数调用
            if (funcTokens.contains(node.getSymbol())) {
                System.out.print(Utils.getSGR("LightYellow"));
            } else if (declTokens.contains(node.getSymbol())) {
                System.out.print(Utils.getSGR("LightMagenta"));
            }
        }
        if (typeIndex >= 25 && typeIndex <= 30) {
            String color = bracktColor.get(bracktDepth % 6);
            System.out.print(Utils.getSGR(color));
            System.out.print(text);
            System.out.print(Utils.getSGR("ResetFore"));
            if (funcRParen.contains(token)) {
                // 打印一个空格
                if (inDecl) {
                    // 空格不需要任何样式
                    System.out.print(Utils.getSGR("Reset") + " ");
                    // 恢复下划线
                    System.out.print(Utils.getSGR("Underlined"));
                } else {
                    System.out.print(" ");
                }
            }
            return;
        }
        // 打印text
        System.out.print(text);

        // 打印后续的空格
        if (oneSuccBlankSet.contains(typeIndex)) {
            if (inDecl) {
                // 空格不需要任何样式
                System.out.print(Utils.getSGR("Reset") + " ");
                // 恢复下划线
                System.out.print(Utils.getSGR("Underlined"));
            } else {
                System.out.print(" ");
            }
        }
        super.visitTerminal(node);
    }

}
