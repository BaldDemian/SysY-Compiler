package compiler.listener;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class LexerErrorListener extends BaseErrorListener {
    private boolean used = false;

    public boolean hasLexError() {
        return this.used;
    }
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        if (!used) {
            // 被调用时将改为true
            used = true;
        }
        System.err.println("Error type A at Line " + line + ": " + msg);
    }
}
