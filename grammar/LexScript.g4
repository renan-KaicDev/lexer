// ============================================================
//  LexScript.g4 — Gramática ANTLR4 para a linguagem LexScript
//
//  Linguagem imperativa tipada, com sintaxe inspirada em C/Java,
//  cobrindo os requisitos mínimos:
//    1. Declaração de variável
//    2. Atribuição
//    3. Expressões aritméticas
//    4. Expressões relacionais
//    5. Estrutura condicional (if / else if / else)
//    6. Estrutura de repetição (while e for)
//    7. Definição de funções com argumentos e retorno
// ============================================================

grammar LexScript;

// ============================================================
// REGRAS DO PARSER (Sintaxe)
// ============================================================

// Ponto de entrada: um programa é composto por declarações de
// funções e/ou declarações de variáveis globais, terminando no EOF.
program
    : topLevelDecl* EOF
    ;

// Nível superior: função ou variável global
topLevelDecl
    : funcDecl
    | varDecl
    ;

// -----------------------------------------------------------------
// 7. DEFINIÇÃO DE FUNÇÕES
//    Sintaxe: func <tipo> <nome>(<params>) { <corpo> }
//    Tipo de retorno 'void' indica que a função não retorna valor.
// -----------------------------------------------------------------
funcDecl
    : KW_FUNC returnType ID '(' paramList? ')' block
    ;

// Tipo de retorno pode ser void ou qualquer tipo primitivo
returnType
    : KW_VOID
    | type
    ;

// Lista de parâmetros formais separados por vírgula
paramList
    : param (',' param)*
    ;

// Um parâmetro: <tipo> <nome>
param
    : type ID
    ;

// -----------------------------------------------------------------
// Bloco: sequência de instruções delimitadas por { }
// -----------------------------------------------------------------
block
    : '{' stmt* '}'
    ;

// -----------------------------------------------------------------
// INSTRUÇÕES (Statements)
// -----------------------------------------------------------------
stmt
    : varDecl          // Declaração de variável local
    | assignStmt       // Atribuição simples
    | ifStmt           // Condicional
    | whileStmt        // Repetição while
    | forStmt          // Repetição for
    | returnStmt       // Retorno de função
    | exprStmt         // Expressão seguida de ';' (ex: chamada de função)
    | block            // Bloco aninhado
    ;

// -----------------------------------------------------------------
// 1. DECLARAÇÃO DE VARIÁVEL
//    Sintaxe: var <tipo> <nome> [= <expr>] ;
// -----------------------------------------------------------------
varDecl
    : KW_VAR type ID ('=' expr)? ';'
    ;

// -----------------------------------------------------------------
// 2. ATRIBUIÇÃO
//    Sintaxe: <nome> = <expr> ;
//    Também suporta operadores compostos: +=  -=  *=  /=
// -----------------------------------------------------------------
assignStmt
    : ID assignOp expr ';'
    ;

assignOp
    : '='
    | '+='
    | '-='
    | '*='
    | '/='
    ;

// -----------------------------------------------------------------
// 5. ESTRUTURA CONDICIONAL
//    Sintaxe: if (<cond>) <bloco> [else if (<cond>) <bloco>]* [else <bloco>]
// -----------------------------------------------------------------
ifStmt
    : KW_IF '(' expr ')' block
      (KW_ELSE KW_IF '(' expr ')' block)*
      (KW_ELSE block)?
    ;

// -----------------------------------------------------------------
// 6. ESTRUTURA DE REPETIÇÃO — WHILE
//    Sintaxe: while (<cond>) <bloco>
// -----------------------------------------------------------------
whileStmt
    : KW_WHILE '(' expr ')' block
    ;

// -----------------------------------------------------------------
// 6. ESTRUTURA DE REPETIÇÃO — FOR
//    Sintaxe: for (<init>; <cond>; <update>) <bloco>
//    <init>   pode ser uma declaração de variável ou atribuição
//    <update> é uma expressão de atribuição (sem ';' final)
// -----------------------------------------------------------------
forStmt
    : KW_FOR '(' forInit expr ';' forUpdate ')' block
    ;

forInit
    : varDecl       // var int i = 0;
    | assignStmt    // i = 0;
    ;

forUpdate
    : ID assignOp expr    // i = i + 1  |  i += 1
    ;

// -----------------------------------------------------------------
// RETORNO DE FUNÇÃO
//    Sintaxe: return [<expr>] ;
// -----------------------------------------------------------------
returnStmt
    : KW_RETURN expr? ';'
    ;

// -----------------------------------------------------------------
// EXPRESSÃO COMO INSTRUÇÃO (ex: chamada de função)
// -----------------------------------------------------------------
exprStmt
    : expr ';'
    ;

// -----------------------------------------------------------------
// 3 & 4. EXPRESSÕES — Aritméticas e Relacionais
//
//  Precedência (maior → menor). No ANTLR, em regras recursivas à
//  esquerda, a PRIMEIRA alternativa tem a MAIOR precedência:
//    ! - (unário)
//    * / % (multiplicativo)
//    + - (aditivo)
//    < > <= >= (relacional)
//    == != (igualdade)
//    and
//    or
//    () chamada de função, literais, identificadores
// -----------------------------------------------------------------
expr
    : ('!' | '-') expr                          # unary
    | expr ('*' | '/' | '%') expr               # multiplicative
    | expr ('+' | '-') expr                     # additive
    | expr ('<' | '>' | '<=' | '>=') expr       # relational
    | expr ('==' | '!=') expr                   # equality
    | expr KW_AND expr                          # logicalAnd
    | expr KW_OR expr                           # logicalOr
    | ID '(' argList? ')'                       # funcCall
    | '(' expr ')'                              # parens
    | ID                                        # identifier
    | literal                                   # literalExpr
    ;

// Lista de argumentos reais em chamada de função
argList
    : expr (',' expr)*
    ;

// -----------------------------------------------------------------
// TIPOS PRIMITIVOS
// -----------------------------------------------------------------
type
    : KW_INT
    | KW_FLOAT
    | KW_BOOL
    | KW_STRING
    | KW_CHAR
    ;

// -----------------------------------------------------------------
// LITERAIS
// -----------------------------------------------------------------
literal
    : INT_LIT       // ex: 42
    | FLOAT_LIT     // ex: 3.14
    | BOOL_LIT      // true | false
    | STRING_LIT    // ex: "hello"
    | CHAR_LIT      // ex: 'a'
    ;

// ============================================================
// REGRAS DO LEXER (Tokens)
// ============================================================

// --- Palavras-chave ---
KW_FUNC   : 'func';
KW_VAR    : 'var';
KW_IF     : 'if';
KW_ELSE   : 'else';
KW_WHILE  : 'while';
KW_FOR    : 'for';
KW_RETURN : 'return';
KW_VOID   : 'void';
KW_AND    : 'and';
KW_OR     : 'or';

// --- Tipos ---
KW_INT    : 'int';
KW_FLOAT  : 'float';
KW_BOOL   : 'bool';
KW_STRING : 'string';
KW_CHAR   : 'char';

// --- Literais booleanos ---
BOOL_LIT  : 'true' | 'false';

// --- Literais numéricos ---
FLOAT_LIT : [0-9]+ '.' [0-9]+;
INT_LIT   : [0-9]+;

// --- Literais de cadeia de caracteres e caractere ---
STRING_LIT : '"' (~["\\\r\n] | '\\' .)* '"';
CHAR_LIT   : '\'' (~['\\\r\n] | '\\' .) '\'';

// --- Identificador ---
ID : [a-zA-Z_][a-zA-Z_0-9]*;

// --- Ignorados (espaços e comentários) ---
WS           : [ \t\r\n]+       -> skip;
LINE_COMMENT : '//' ~[\r\n]*    -> skip;
BLOCK_COMMENT: '/*' .*? '*/'    -> skip;

