# LexScript — Compilador Didático

Implementação das três primeiras fases de um compilador para a linguagem **LexScript**: análise léxica, sintática e semântica.

## Estrutura do projeto

```
lexer/
├── src/
│   ├── lexer/
│   │   └── lexer_aluno.c          # Lexer em C (AFDs manuais)
│   └── semantic/
│       ├── TokenFileParser.java   # Adaptador TSV → parser ANTLR
│       ├── TabelaDeSimbolos.java  # Tabela de símbolos com escopos
│       ├── SemanticVisitor.java   # Visitor de análise semântica
│       └── SemanticMain.java      # Ponto de entrada semântico
├── grammar/
│   └── LexScript.g4               # Gramática ANTLR4 da linguagem
├── examples/
│   ├── codigo_entrada.txt         # Exemplo de código válido
│   └── codigo_erros.txt           # Exemplo com erros semânticos
├── docs/
│   ├── cmd.md                     # Comandos de build e execução
│   ├── Especificacao_Tokens_LexScript.md  # Especificação de tokens (Markdown)
│   └── Especificacao_Tokens_MiniLang.docx # Especificação original (Word)
├── antlr/
│   └── antlr-4.13.2-complete.jar  # Runtime ANTLR (não rastrear no git)
├── build/          # Gerado — saída do gcc e do ANTLR (git-ignored)
└── resultado/      # Gerado — tokens.tsv e arvore.txt (git-ignored)
```

## Pipeline

```
src/lexer/lexer_aluno.c
        │  gcc
        ▼
build/lexer_aluno ──(execução)──► resultado/tokens.tsv
                                          │
                              TokenFileParser.java
                                          │  ANTLR4 + grammar/LexScript.g4
                                          ▼
                              resultado/arvore.txt  (árvore sintática)
                                          │
                              SemanticMain.java
                            + SemanticVisitor.java
                            + TabelaDeSimbolos.java
                                          ▼
                              Relatório de erros semânticos
```

## Início rápido

Consulte [`docs/cmd.md`](docs/cmd.md) para o passo a passo completo de build e execução.

```bash
ANTLR_JAR=antlr/antlr-4.13.2-complete.jar
mkdir -p build/antlr build/classes resultado

# 1. Compilar e rodar o lexer C
gcc -std=c11 -Wall -Wextra -pedantic src/lexer/lexer_aluno.c -o build/lexer_aluno
./build/lexer_aluno examples/codigo_entrada.txt resultado/tokens.tsv

# 2. Gerar parser ANTLR
java -jar "$ANTLR_JAR" -Dlanguage=Java -visitor -o build/antlr grammar/LexScript.g4

# 3. Compilar Java
javac -cp "$ANTLR_JAR" -d build/classes \
  build/antlr/*.java \
  src/semantic/TokenFileParser.java \
  src/semantic/TabelaDeSimbolos.java \
  src/semantic/SemanticVisitor.java \
  src/semantic/SemanticMain.java

# 4. Gerar árvore sintática
java -cp "build/classes:$ANTLR_JAR" TokenFileParser \
  resultado/tokens.tsv build/antlr/LexScript.tokens resultado/arvore.txt

# 5. Análise semântica
java -cp "build/classes:$ANTLR_JAR" SemanticMain \
  resultado/tokens.tsv build/antlr/LexScript.tokens
```

## Documentação

| Arquivo | Descrição |
|---------|-----------|
| [`docs/cmd.md`](docs/cmd.md) | Guia de build e execução |
| [`docs/Especificacao_Tokens_LexScript.md`](docs/Especificacao_Tokens_LexScript.md) | Especificação completa de tokens |
| [`grammar/LexScript.g4`](grammar/LexScript.g4) | Gramática formal da linguagem |
