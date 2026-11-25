# Build e Execução

Todos os comandos devem ser executados **a partir da raiz do projeto**.

## Sumário

1. [Configuração](#1-configuração)
2. [Lexer (C)](#2-lexer-c)
3. [Geração do ANTLR](#3-geração-do-antlr)
4. [Compilação Java](#4-compilação-java)
5. [Execução](#5-execução)
6. [Ver resultados](#6-ver-resultados)

---

## 1. Configuração

Define o caminho do JAR do ANTLR e cria os diretórios de saída.

```bash
ANTLR_JAR=antlr/antlr-4.13.2-complete.jar

mkdir -p build/antlr build/classes resultado
```

## 2. Lexer (C)

Compila o lexer e gera o arquivo de tokens a partir do código de entrada.

```bash
gcc -std=c11 -Wall -Wextra -pedantic src/lexer/lexer_aluno.c -o build/lexer_aluno
./build/lexer_aluno examples/codigo_entrada.txt resultado/tokens.tsv
```

## 3. Geração do ANTLR

Gera os arquivos Java do parser, com suporte a Visitor.

> **Nota:** o ANTLR espelha o caminho do arquivo `.g4` dentro do diretório `-o`.
> Como o arquivo está em `grammar/LexScript.g4`, os fontes Java ficam em
> `build/antlr/grammar/` e o arquivo de vocabulário fica em
> `build/antlr/grammar/LexScript.tokens`.

```bash
java -jar "$ANTLR_JAR" -Dlanguage=Java -visitor -o build/antlr grammar/LexScript.g4
```

## 4. Compilação Java

Compila de uma só vez o parser gerado, o `TokenFileParser` e a análise semântica.

```bash
javac -cp "$ANTLR_JAR" -d build/classes \
  build/antlr/grammar/*.java \
  src/semantic/TokenFileParser.java \
  src/semantic/TabelaDeSimbolos.java \
  src/semantic/SemanticVisitor.java \
  src/semantic/SemanticMain.java
```

## 5. Execução

### 5.1 Parser (árvore sintática)

Gera `resultado/arvore.txt`.

```bash
java -cp "build/classes:$ANTLR_JAR" TokenFileParser \
  resultado/tokens.tsv build/antlr/grammar/LexScript.tokens resultado/arvore.txt
```

### 5.2 Análise semântica

**Código sem erros:**

```bash
java -cp "build/classes:$ANTLR_JAR" SemanticMain \
  resultado/tokens.tsv build/antlr/grammar/LexScript.tokens
```

**Código com erros semânticos** (declaração duplicada e uso antes da declaração):

```bash
./build/lexer_aluno examples/codigo_erros.txt resultado/tokens_erros.tsv
java -cp "build/classes:$ANTLR_JAR" SemanticMain \
  resultado/tokens_erros.tsv build/antlr/grammar/LexScript.tokens
```

## 6. Ver resultados

```bash
cat resultado/tokens.tsv
cat resultado/arvore.txt
```

## Arquivos gerados

| Arquivo | Descrição |
|---|---|
| `build/lexer_aluno` | Executável do lexer |
| `build/antlr/grammar/` | Parser/Visitor gerados pelo ANTLR |
| `build/classes/` | Classes Java compiladas |
| `resultado/tokens.tsv` | Tokens do código válido |
| `resultado/tokens_erros.tsv` | Tokens do código com erros |
| `resultado/arvore.txt` | Árvore sintática |
