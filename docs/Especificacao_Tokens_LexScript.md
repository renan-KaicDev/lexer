# Especificação de Tokens — LexScript

> **Referência:** gramática [`LexScript.g4`](file:///home/machine/Downloads/lexer/LexScript.g4).  
> Esta versão substitui a especificação anterior de *MiniLang*, que descrevia outro conjunto de tokens.

---

## Sumário

1. [Palavras-chave e tipos](#1-palavras-chave-e-tipos)
2. [Literais e identificadores](#2-literais-e-identificadores)
3. [Operadores e delimitadores](#3-operadores-e-delimitadores)
4. [Ignorados (whitespace e comentários)](#4-ignorados-whitespace-e-comentários)
5. [Observações de compatibilidade](#5-observações-de-compatibilidade)

---

## 1. Palavras-chave e tipos

Lexemas fixos reconhecidos primeiro como `ID` pelo AFD e depois **reclassificados** pela função `reclassificar()` no [`lexer_aluno.c`](file:///home/machine/Downloads/lexer/lexer_aluno.c).  
O token ANTLR e o tipo emitido pelo lexer C são **idênticos** para todas as palavras-chave.

| Lexema | Token ANTLR | Tipo emitido pelo lexer C |
|--------|-------------|--------------------------|
| `func` | `KW_FUNC` | `KW_FUNC` |
| `var` | `KW_VAR` | `KW_VAR` |
| `if` | `KW_IF` | `KW_IF` |
| `else` | `KW_ELSE` | `KW_ELSE` |
| `while` | `KW_WHILE` | `KW_WHILE` |
| `for` | `KW_FOR` | `KW_FOR` |
| `return` | `KW_RETURN` | `KW_RETURN` |
| `void` | `KW_VOID` | `KW_VOID` |
| `and` | `KW_AND` | `KW_AND` |
| `or` | `KW_OR` | `KW_OR` |
| `int` | `KW_INT` | `KW_INT` |
| `float` | `KW_FLOAT` | `KW_FLOAT` |
| `bool` | `KW_BOOL` | `KW_BOOL` |
| `string` | `KW_STRING` | `KW_STRING` |
| `char` | `KW_CHAR` | `KW_CHAR` |

---

## 2. Literais e identificadores

Reconhecidos por **AFDs dedicados** (números, identificadores) ou por tratamento direto de ponteiro (strings e chars). A expressão regular na coluna do meio é a notação usada pela gramática ANTLR4.

| Expressão regular (ANTLR) | Token ANTLR | Tipo emitido pelo lexer C | Exemplos válidos |
|---------------------------|-------------|--------------------------|-----------------|
| `'true' \| 'false'` | `BOOL_LIT` | `BOOL_LIT` | `true`, `false` |
| `[0-9]+ '.' [0-9]+` | `FLOAT_LIT` | `FLOAT_LIT` | `3.14`, `0.5`, `100.0` |
| `[0-9]+` | `INT_LIT` | `INT_LIT` | `0`, `42`, `255` |
| `'"' (~["\\\r\n] \| '\\' .)* '"'` | `STRING_LIT` | `STRING_LIT` | `"hello"`, `"a\nb"` |
| `'\'' (~['\\\r\n] \| '\\' .) '\''` | `CHAR_LIT` | `CHAR_LIT` | `'a'`, `'\n'`, `'\\'` |
| `[a-zA-Z_][a-zA-Z_0-9]*` | `ID` | `ID` | `x`, `_soma`, `calcular2` |

> **Regra de ouro — maximal munch:** o lexer sempre consome o maior prefixo possível.  
> `3.14` → `FLOAT_LIT` (4 chars), **não** `INT_LIT` `3` + ponto solto.  
> `==` → `OP_EQ` (2 chars), **não** dois `ATRIB` separados.

### Regras de formatação de nomes (`ID`)

| Regra | Válido | Inválido |
|-------|--------|---------|
| Começa com letra ou `_` | `soma`, `_total`, `X` | `2x`, `@var` |
| Continua com letra, dígito ou `_` | `calc2`, `meu_nome` | `meu-nome`, `a b` |
| Sensível a maiúsculas | `Soma ≠ soma` | — |
| Palavras-chave são reservadas | — | `var`, `if`, `func` como nomes |

### Formato de nomes — convenções recomendadas

Embora a gramática não imponha convenção de nomenclatura além da sintaxe do `ID`, a linguagem segue as mesmas boas práticas de C/Java:

| Elemento | Convenção | Exemplo |
|----------|-----------|---------|
| Variável local | `camelCase` ou `snake_case` | `totalItens`, `total_itens` |
| Parâmetro de função | `camelCase` ou `snake_case` | `valorEntrada`, `valor_entrada` |
| Nome de função | `camelCase` | `calcularSoma`, `lerArquivo` |
| Constante (semântica) | `UPPER_SNAKE_CASE` | `MAX_TAMANHO` |
| Prefixo `_` | Reservado a uso interno | `_aux`, `_tmp` |

---

## 3. Operadores e delimitadores

Os símbolos abaixo são **literais implícitos** nas regras do parser — o ANTLR não os nomeia com tokens explícitos na gramática (gera nomes internos `T__n` em `LexScript.tokens`). O adaptador [`TokenFileParser.java`](file:///home/machine/Downloads/lexer/TokenFileParser.java) consulta `LexScript.tokens` para obter o tipo numérico correto e faz o mapeamento via `literalFor()`.

### Operadores de atribuição

| Lexema | Tipo ANTLR | Tipo emitido pelo lexer C | Descrição |
|--------|-----------|--------------------------|-----------|
| `=` | literal implícito | `ATRIB` | Atribuição simples |
| `+=` | literal implícito | `OP_ADD_ASSIGN` | Atribuição com adição |
| `-=` | literal implícito | `OP_SUB_ASSIGN` | Atribuição com subtração |
| `*=` | literal implícito | `OP_MUL_ASSIGN` | Atribuição com multiplicação |
| `/=` | literal implícito | `OP_DIV_ASSIGN` | Atribuição com divisão |

### Operadores relacionais e de igualdade

| Lexema | Tipo ANTLR | Tipo emitido pelo lexer C | Descrição |
|--------|-----------|--------------------------|-----------|
| `==` | literal implícito | `OP_EQ` | Igualdade |
| `!=` | literal implícito | `OP_NE` | Diferença |
| `<` | literal implícito | `MENOR` | Menor que |
| `>` | literal implícito | `MAIOR` | Maior que |
| `<=` | literal implícito | `OP_LE` | Menor ou igual |
| `>=` | literal implícito | `OP_GE` | Maior ou igual |

### Operadores aritméticos e unário

| Lexema | Tipo ANTLR | Tipo emitido pelo lexer C | Descrição |
|--------|-----------|--------------------------|-----------|
| `+` | literal implícito | `OP_PLUS` | Adição |
| `-` | literal implícito | `OP_MINUS` | Subtração / negação unária |
| `*` | literal implícito | `OP_MUL` | Multiplicação |
| `/` | literal implícito | `OP_DIV` | Divisão |
| `%` | literal implícito | `OP_MOD` | Módulo (resto) |
| `!` | literal implícito | `OP_NOT` | Negação lógica (unária) |

### Delimitadores e pontuação

| Lexema | Tipo ANTLR | Tipo emitido pelo lexer C | Descrição |
|--------|-----------|--------------------------|-----------|
| `(` | literal implícito | `PAR_ESQ` | Parêntese esquerdo |
| `)` | literal implícito | `PAR_DIR` | Parêntese direito |
| `{` | literal implícito | `CHAVE_ESQ` | Chave esquerda (início de bloco) |
| `}` | literal implícito | `CHAVE_DIR` | Chave direita (fim de bloco) |
| `;` | literal implícito | `PV` | Ponto e vírgula |
| `,` | literal implícito | `VIRGULA` | Vírgula (separador de parâmetros/argumentos) |

---

## 4. Ignorados (whitespace e comentários)

Esses padrões são consumidos pelo lexer **sem gerar token**. Na gramática ANTLR usam `-> skip`; no lexer C são descartados diretamente em `lexer_proximo()` antes de qualquer tentativa de reconhecimento.

| Regra ANTLR | Padrão / descrição | Comportamento |
|-------------|-------------------|--------------|
| `WS` | `[ \t\r\n]+` | Ignora espaços, tabulações e quebras de linha |
| `LINE_COMMENT` | `//` até o fim da linha | Ignora comentário de linha |
| `BLOCK_COMMENT` | `/* ... */`, até o **primeiro** fechamento | Ignora comentário de bloco |

> **Atenção:** comentário de bloco sem `*/` de fechamento causa **erro** no lexer C com mensagem de posição:  
> `Comentario de bloco nao terminado em L<linha>:C<coluna>`

---

## 5. Observações de compatibilidade

- **Operadores lógicos são palavras**, não símbolos: `and` → `KW_AND`, `or` → `KW_OR`.  
  `&&` e `||` **não pertencem** a esta gramática e causam erro léxico.

- **`break` e `continue` não são palavras-chave** de LexScript. Se usados em código, serão reconhecidos como `ID`, gerando erro sintático posteriormente.

- **Colchetes `[ ]` não são delimitadores** definidos pela gramática. Seu uso causa erro léxico.

- O lexer C emite nomes **próprios** para pontuação e operadores (ex: `ATRIB`, `OP_EQ`).  
  [`TokenFileParser.java`](file:///home/machine/Downloads/lexer/TokenFileParser.java) os mapeia para os literais e tipos definidos por `LexScript.tokens` antes de construir os `CommonToken`.

- A gramática **exige ponto e vírgula** (`;`) em declarações (`varDecl`), atribuições (`assignStmt`) e retornos (`returnStmt`), conforme as regras sintáticas de [`LexScript.g4`](file:///home/machine/Downloads/lexer/LexScript.g4).

---

## Referência rápida — todos os tokens

| Categoria | Tokens |
|-----------|--------|
| Palavras-chave de controle | `KW_IF` `KW_ELSE` `KW_WHILE` `KW_FOR` `KW_RETURN` |
| Palavras-chave de estrutura | `KW_FUNC` `KW_VAR` `KW_VOID` |
| Tipos primitivos | `KW_INT` `KW_FLOAT` `KW_BOOL` `KW_STRING` `KW_CHAR` |
| Operadores lógicos (palavras) | `KW_AND` `KW_OR` |
| Literais | `BOOL_LIT` `INT_LIT` `FLOAT_LIT` `STRING_LIT` `CHAR_LIT` |
| Identificador | `ID` |
| Atribuição | `ATRIB` `OP_ADD_ASSIGN` `OP_SUB_ASSIGN` `OP_MUL_ASSIGN` `OP_DIV_ASSIGN` |
| Igualdade/Relacional | `OP_EQ` `OP_NE` `MENOR` `MAIOR` `OP_LE` `OP_GE` |
| Aritmético/Unário | `OP_PLUS` `OP_MINUS` `OP_MUL` `OP_DIV` `OP_MOD` `OP_NOT` |
| Delimitadores | `PAR_ESQ` `PAR_DIR` `CHAVE_ESQ` `CHAVE_DIR` `PV` `VIRGULA` |
| Ignorados | `WS` `LINE_COMMENT` `BLOCK_COMMENT` |
