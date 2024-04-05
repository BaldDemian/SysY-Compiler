parser grammar SysYParser;

options {
    tokenVocab = SysYLexer; // import lexer rules
}

// 0
program: compUnit;

// 1
compUnit: (funcDef | decl)+ EOF;

// 2
decl: constDecl | varDecl; // declare

// 3
constDecl: CONST bType constDef (COMMA constDef)* SEMICOLON; // 'const int a, b;' is ok

// 4
bType: INT; // basic type

// 5
constDef: IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal; // const define

// 6
constInitVal: constExp
            | L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE;

// 7
varDecl: bType varDef (COMMA varDef)* SEMICOLON;

// 8
varDef: IDENT (L_BRACKT constExp R_BRACKT)*
        | IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN initVal;

// 9
initVal: exp
        | L_BRACE (initVal (COMMA initVal)*)?R_BRACE;

// 10
funcDef: funcType IDENT L_PAREN funcFParams? R_PAREN block;

// 11
funcType: VOID | INT;

// 12
funcFParams: funcFParam (COMMA funcFParam)*;

// 13
funcFParam: bType IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)?;

// 14
block: L_BRACE blockItem* R_BRACE;

// 15
blockItem: decl | stmt;

// 16
stmt: lVal ASSIGN exp SEMICOLON # stmtAssign
    | (exp)? SEMICOLON # stmtExp
    | block # stmtBlock
    | IF L_PAREN cond R_PAREN stmt (ELSE stmt)? # stmtIf
    | WHILE L_PAREN cond R_PAREN stmt # stmtWhile
    | BREAK SEMICOLON # stmtBreak
    | CONTINUE SEMICOLON # stmtContinue
    | RETURN (exp)? SEMICOLON # stmtReturn
    ;

// 17
exp: L_PAREN exp R_PAREN # expParen
    | lVal # expLVal
    | number # expNum
    | IDENT L_PAREN funcRParams? R_PAREN # expCallFunc
    | unaryOp exp # expUnary
    | exp (MUL | DIV | MOD) exp # expMul
    | exp (PLUS | MINUS) exp # expPlus
    ;

// 18
cond: exp
    | cond (LT | GT | LE | GE) cond
    | cond (EQ | NEQ) cond
    | cond AND cond
    | cond OR cond ;

// 19
lVal: IDENT (L_BRACKT exp R_BRACKT)*; // a[3]

// 20
number: INTEGER_CONST;

// 21
unaryOp: PLUS
    | MINUS
    | NOT;

// 22
funcRParams: param (COMMA param)*;

// 23
param: exp;

// 24
constExp: exp;