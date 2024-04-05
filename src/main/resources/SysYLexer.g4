lexer grammar SysYLexer;

// 1
CONST: 'const';

// 2
INT: 'int';

// 3
VOID: 'void';

// 4
IF: 'if';

// 5
ELSE: 'else';

// 6
WHILE: 'while';

// 7
BREAK: 'break';

// 8
CONTINUE: 'continue';

// 9
RETURN: 'return';

// 10
PLUS: '+';

// 11
MINUS: '-';

// 12
MUL: '*';

// 13
DIV: '/';

// 14
MOD: '%';

// 15
ASSIGN: '=';

// 16
EQ: '==';

// 17
NEQ: '!=';

// 18
LT: '<';

// 19
GT: '>';

// 20
LE: '<=';

// 21
GE: '>=';

// 22
NOT: '!';

// 23
AND: '&&';

// 24
OR: '||';

// 25
L_PAREN: '(';

// 26
R_PAREN: ')';

// 27
L_BRACE: '{';

// 28
R_BRACE: '}';

// 29
L_BRACKT: '[';

// 30
R_BRACKT: ']';

// 31
COMMA: ',';

// 32
SEMICOLON: ';';

// 33
IDENT: [_a-zA-Z] [_a-zA-Z0-9]*;

// 34
INTEGER_CONST: DECIMAL_CONST | OCTAL_CONST | HEXADECIMAL_CONST;

// 35
fragment DECIMAL_CONST: '0' | [1-9] [0-9]*;

// 36
fragment OCTAL_CONST: '0' [0-7]+;

// 37
fragment HEXADECIMAL_CONST: ('0x' | '0X') [a-fA-F0-9]+;

// 38
WS: [ \r\n\t]+ -> skip;

// 39
LINE_COMMENT: '//' .*? '\n' -> skip;

// 40
MULTILINE_COMMENT: '/*' .*? '*/' -> skip;