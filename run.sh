#!/usr/bin/env bash
# =============================================================
#  run.sh — Pipeline completo do compilador LexScript
#  Uso: ./run.sh <arquivo-fonte>
#  Ex.: ./run.sh examples/codigo_entrada.txt
# =============================================================

set -euo pipefail

# ── Cores ────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

step()  { echo -e "\n${CYAN}${BOLD}=== $* ===${RESET}"; }
ok()    { echo -e "${GREEN}✓ $*${RESET}"; }
warn()  { echo -e "${YELLOW}⚠ $*${RESET}"; }
die()   { echo -e "${RED}✗ ERRO: $*${RESET}" >&2; exit 1; }

# ── Configuração ─────────────────────────────────────────────
ANTLR_JAR="antlr/antlr-4.13.2-complete.jar"
SRC_ARQUIVO="${1:-}"

# ── Validações iniciais ───────────────────────────────────────
[[ -z "$SRC_ARQUIVO" ]] && die "Informe o arquivo fonte.\n  Uso: $0 <arquivo>"
[[ -f "$SRC_ARQUIVO" ]] || die "Arquivo não encontrado: $SRC_ARQUIVO"
[[ -f "$ANTLR_JAR"   ]] || die "JAR do ANTLR não encontrado: $ANTLR_JAR"

# Nomes derivados do arquivo de entrada
BASENAME="$(basename "$SRC_ARQUIVO" .txt)"
TOKENS_TSV="resultado/${BASENAME}_tokens.tsv"
ARVORE_TXT="resultado/${BASENAME}_arvore.txt"

echo -e "${BOLD}Fonte : ${SRC_ARQUIVO}${RESET}"
echo -e "${BOLD}Tokens: ${TOKENS_TSV}${RESET}"
echo -e "${BOLD}Árvore: ${ARVORE_TXT}${RESET}"

# ── 1. Diretórios ─────────────────────────────────────────────
step "1. Preparando diretórios"
mkdir -p build/antlr build/classes resultado
ok "build/antlr  build/classes  resultado"

# ── 2. Lexer C ────────────────────────────────────────────────
step "2. Compilando lexer C"
if [[ ! -f build/lexer_aluno ]] || \
   [[ src/lexer/lexer_aluno.c -nt build/lexer_aluno ]]; then
    gcc -std=c11 -Wall -Wextra -pedantic \
        src/lexer/lexer_aluno.c -o build/lexer_aluno
    ok "build/lexer_aluno compilado"
else
    ok "build/lexer_aluno já atualizado (pulando recompilação)"
fi

# ── 3. Análise léxica ─────────────────────────────────────────
step "3. Análise léxica → $TOKENS_TSV"
./build/lexer_aluno "$SRC_ARQUIVO" "$TOKENS_TSV"
TOKEN_COUNT=$(( $(wc -l < "$TOKENS_TSV") - 1 ))   # desconta cabeçalho
ok "$TOKEN_COUNT tokens gerados"

# ── 4. Geração ANTLR ─────────────────────────────────────────
step "4. Gerando parser ANTLR"
GRAMMAR_SRC="grammar/LexScript.g4"
ANTLR_MARKER="build/antlr/grammar/LexScriptParser.java"
if [[ ! -f "$ANTLR_MARKER" ]] || \
   [[ "$GRAMMAR_SRC" -nt "$ANTLR_MARKER" ]]; then
    java -jar "$ANTLR_JAR" -Dlanguage=Java -visitor \
         -o build/antlr "$GRAMMAR_SRC"
    ok "Arquivos ANTLR gerados em build/antlr/grammar/"
else
    ok "Gramática não mudou (pulando geração ANTLR)"
fi

# ── 5. Compilação Java ────────────────────────────────────────
step "5. Compilando Java"
JAVA_MARKER="build/classes/LexScriptParser.class"
JAVA_SOURCES_NEWER=false
for f in build/antlr/grammar/*.java \
          src/semantic/TokenFileParser.java \
          src/semantic/TabelaDeSimbolos.java \
          src/semantic/SemanticVisitor.java \
          src/semantic/SemanticMain.java; do
    if [[ ! -f "$JAVA_MARKER" ]] || [[ "$f" -nt "$JAVA_MARKER" ]]; then
        JAVA_SOURCES_NEWER=true; break
    fi
done

if $JAVA_SOURCES_NEWER; then
    javac -cp "$ANTLR_JAR" -d build/classes \
        build/antlr/grammar/*.java \
        src/semantic/TokenFileParser.java \
        src/semantic/TabelaDeSimbolos.java \
        src/semantic/SemanticVisitor.java \
        src/semantic/SemanticMain.java
    ok "Classes compiladas em build/classes/"
else
    ok "Classes já atualizadas (pulando recompilação Java)"
fi

# ── 6. Árvore sintática ───────────────────────────────────────
step "6. Análise sintática → $ARVORE_TXT"
java -cp "build/classes:$ANTLR_JAR" TokenFileParser \
    "$TOKENS_TSV" build/antlr/grammar/LexScript.tokens "$ARVORE_TXT"
ok "Árvore sintática gerada"
echo ""
cat "$ARVORE_TXT"

# ── 7. Análise semântica ──────────────────────────────────────
step "7. Análise semântica"
set +e
java -cp "build/classes:$ANTLR_JAR" SemanticMain \
    "$TOKENS_TSV" build/antlr/grammar/LexScript.tokens
EXIT_CODE=$?
set -e

echo ""
if [[ $EXIT_CODE -eq 0 ]]; then
    echo -e "${GREEN}${BOLD}Pipeline concluído sem erros. ✓${RESET}"
else
    echo -e "${RED}${BOLD}Pipeline concluído com erros semânticos (código $EXIT_CODE).${RESET}"
    exit $EXIT_CODE
fi
