# Execute a partir da raiz do projeto.

ANTLR_JAR=antlr/antlr-4.13.2-complete.jar

# ── Pré-requisitos ────────────────────────────────────────────────────────────
mkdir -p build/antlr build/classes resultado

# Compilar o lexer C e gerar os tokens:
gcc -std=c11 -Wall -Wextra -pedantic lexer_aluno.c -o build/lexer_aluno
./build/lexer_aluno codigo_entrada.txt resultado/tokens.tsv

# Gerar arquivos ANTLR (com suporte a Visitor):
java -jar "$ANTLR_JAR" -Dlanguage=Java -visitor -o build/antlr LexScript.g4

# ── Compilação ────────────────────────────────────────────────────────────────
# Compila parser + TokenFileParser + análise semântica de uma vez:
javac -cp "$ANTLR_JAR" -d build/classes \
  build/antlr/*.java \
  TokenFileParser.java \
  TabelaDeSimbolos.java \
  SemanticVisitor.java \
  SemanticMain.java

# ── Execução — Parser original (árvore) ──────────────────────────────────────
java -cp "build/classes:$ANTLR_JAR" TokenFileParser \
  resultado/tokens.tsv build/antlr/LexScript.tokens resultado/arvore.txt

# ── Execução — Análise Semântica ─────────────────────────────────────────────
# Código sem erros:
java -cp "build/classes:$ANTLR_JAR" SemanticMain \
  resultado/tokens.tsv build/antlr/LexScript.tokens

# Código com erros semânticos (duplicata + uso antes da declaração):
./build/lexer_aluno codigo_erros.txt resultado/tokens_erros.tsv
java -cp "build/classes:$ANTLR_JAR" SemanticMain \
  resultado/tokens_erros.tsv build/antlr/LexScript.tokens

# ── Ver resultados ────────────────────────────────────────────────────────────
cat resultado/tokens.tsv
cat resultado/arvore.txt
