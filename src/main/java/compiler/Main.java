package compiler;

import compiler.gen.parser.SysYLexer;
import compiler.gen.parser.SysYParser;
import compiler.listener.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.bytedeco.javacpp.BytePointer;

import java.io.IOException;
import java.util.List;

import static org.bytedeco.llvm.global.LLVM.LLVMDisposeMessage;
import static org.bytedeco.llvm.global.LLVM.LLVMPrintModuleToFile;


public class Main {
    public static final BytePointer error = new BytePointer();

    public static void main(String[] args) throws IOException {
        String source = "./test.txt";
        // 获取输入流并提供给词法分析器
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer lexer = new SysYLexer(input);
        // 进行词法错误检查
        /* 下方代码若不注释掉，将影响语法分析
        lexer.removeErrorListeners();
        LexerErrorListener lexerErrorListener = new LexerErrorListener();
        lexer.addErrorListener(lexerErrorListener);
        List<? extends Token> myTokens = lexer.getAllTokens(); // 触发词法错误检查
        if (lexerErrorListener.hasLexError()) {
            return; // 存在词法错误，退出程序
        }
        */
        lexer.removeErrorListeners();
        // 获取词素流并提供给语法分析器
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SysYParser parser = new SysYParser(tokens);
        parser.removeErrorListeners();
        ParserErrorListener parserErrorListener = new ParserErrorListener();
        parser.addErrorListener(parserErrorListener);
        // 触发语法分析
        ParseTree tree = parser.program();
        if (parserErrorListener.hasSynError()) {
            return; // 存在语法错误，退出程序
        }
        // 代码染色格式化
        ParseTreeWalker walker = new ParseTreeWalker();
        FormatListener formatListener = new FormatListener();
        walker.walk(formatListener, tree);
        // 进行语义检查
        SemanticCheckListener semanticCheckListener = new SemanticCheckListener();
        semanticCheckListener.setTokens(tokens);
        walker.walk(semanticCheckListener, tree);
        if (semanticCheckListener.error) {
            return; // 存在语义错误
        }
        // 生成LLVM IR
        IRListener irListener = new IRListener();
        walker.walk(irListener, tree);
        if (LLVMPrintModuleToFile(irListener.getModule(), "./res.txt", error) != 0) {
            LLVMDisposeMessage(error);
        }
    }
}
