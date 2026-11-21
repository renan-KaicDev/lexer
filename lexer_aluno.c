/*
 * Uso:
 *   ./lexer_aluno [arquivo-fonte] [arquivo-tokens]
 *
 * Ordem sugerida:
 *   TODO 1-4 : primitivas do AFD
 *   TODO 5-6 : construtores genéricos (literal, trie)
 *   TODO 7-8 : construtores específicos (ID, NUM)
 *   TODO 9-10: motor do lexer
 *   Config   : no topo do arquivo
 */

#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* =====================================================================
 * 1. TOKEN
 * ===================================================================== */

typedef struct {
  char tipo[32];
  char valor[256];
  int linha;
  int coluna;
} Token;

/* =====================================================================
 * 2. AFD — tabela de transições
 * ===================================================================== */

#define MAX_ESTADOS 32 /* suficiente para os AFDs desta linguagem */
#define NUM_SIMBOLOS 256

typedef struct {
  char nome[32];
  int transicoes[MAX_ESTADOS][NUM_SIMBOLOS]; /* -1 = sem transição */
  bool finais[MAX_ESTADOS];
  int inicial;
  int prox_estado;
} AFD;

void afd_init(AFD* afd, const char* nome) {
  snprintf(afd->nome, sizeof afd->nome, "%s", nome);
  for (int estado = 0; estado < MAX_ESTADOS; estado++) {
    afd->finais[estado] = false;
    for (int simbolo = 0; simbolo < NUM_SIMBOLOS; simbolo++) {
      afd->transicoes[estado][simbolo] = -1;
    }
  }
  afd->inicial = 0;
  afd->prox_estado = 1;
}

int afd_novo_estado(AFD* afd) {
  if (afd->prox_estado >= MAX_ESTADOS) {
    fprintf(stderr, "Limite de estados excedido no AFD '%s'\n", afd->nome);
    exit(1);
  }
  return afd->prox_estado++;
}

void afd_transicao(AFD* afd, int origem, const char* simbolos, int destino) {
  for (const char* p = simbolos; *p; p++) {
    afd->transicoes[origem][(unsigned char)*p] = destino;
  }
}

void afd_marcar_final(AFD* afd, int estado) {
  afd->finais[estado] = true;
}

int afd_maior_prefixo(const AFD* afd, const char* texto) {
  int estado = afd->inicial;
  int ultimo = afd->finais[estado] ? 0 : -1;
  for (int i = 0; texto[i]; i++) {
    int prox = afd->transicoes[estado][(unsigned char)texto[i]];
    if (prox < 0) break;
    estado = prox;
    if (afd->finais[estado]) ultimo = i + 1;
  }
  return ultimo;
}

/* =====================================================================
 * 3. CONFIGURAÇÃO DA LINGUAGEM
 * ---------------------------------------------------------------------
 * Edite APENAS esta seção para adaptar o lexer à sua linguagem.
 * ===================================================================== */

#define LETRAS_ID "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
#define INICIAIS_ID LETRAS_ID "_"
#define CONTINUACAO_ID LETRAS_ID "0123456789_"
#define DIGITOS "0123456789"

typedef struct {
  const char* lexema;
  const char* tipo;
} Entrada;

/* Palavras reservadas (reclassificadas a partir de ID). Sentinela {NULL,NULL}.
 */
static const Entrada PALAVRAS_RESERVADAS[] = {{"else", "KW_ELSE"},
                                              {"if", "KW_IF"},
                                              {"while", "KW_WHILE"},
                                              {"for", "KW_FOR"},
                                              {"return", "KW_RETURN"},
                                              {"func", "KW_FUNC"},
                                              {"var", "KW_VAR"},
                                              {"void", "KW_VOID"},
                                              {"and", "KW_AND"},
                                              {"or", "KW_OR"},
                                              {"int", "KW_INT"},
                                              {"float", "KW_FLOAT"},
                                              {"bool", "KW_BOOL"},
                                              {"string", "KW_STRING"},
                                              {"char", "KW_CHAR"},
                                              {"true", "BOOL_LIT"},
                                              {"false", "BOOL_LIT"},
                                              {NULL, NULL}};

/* Operadores + símbolos simples (reconhecidos por um único trie "FIXO"). */
static const Entrada FIXOS[] = {/* operadores compostos */
                                {"+=", "OP_ADD_ASSIGN"},
                                {"-=", "OP_SUB_ASSIGN"},
                                {"*=", "OP_MUL_ASSIGN"},
                                {"/=", "OP_DIV_ASSIGN"},
                                {"==", "OP_EQ"},
                                {"!=", "OP_NE"},
                                {"<=", "OP_LE"},
                                {">=", "OP_GE"},
                                {"=", "ATRIB"},
                                {"<", "MENOR"},
                                {">", "MAIOR"},
                                {"+", "OP_PLUS"},
                                {"-", "OP_MINUS"},
                                {"*", "OP_MUL"},
                                {"/", "OP_DIV"},
                                {"%", "OP_MOD"},
                                {"!", "OP_NOT"},
                                {",", "VIRGULA"},
                                /* símbolos simples */
                                {"(", "PAR_ESQ"},
                                {")", "PAR_DIR"},
                                {"{", "CHAVE_ESQ"},
                                {"}", "CHAVE_DIR"},
                                {";", "PV"},
                                {NULL, NULL}};

/* =====================================================================
 * 4. CONSTRUTORES GENÉRICOS DE AFDs
 * ===================================================================== */

void afd_literal(AFD* afd, const char* nome, const char* texto) {
  afd_init(afd, nome);
  int atual = afd->inicial;
  for (const char* p = texto; *p; p++) {
    int prox = afd_novo_estado(afd);
    char simbolo[2] = {*p, '\0'};
    afd_transicao(afd, atual, simbolo, prox);
    atual = prox;
  }
  afd_marcar_final(afd, atual);
}

void afd_conjunto_literais(AFD* afd, const char* nome, const Entrada* ents) {
  afd_init(afd, nome);
  for (int i = 0; ents[i].lexema; i++) {
    int atual = afd->inicial;
    for (const char* p = ents[i].lexema; *p; p++) {
      int prox = afd->transicoes[atual][(unsigned char)*p];
      if (prox < 0) {
        prox = afd_novo_estado(afd);
        char simbolo[2] = {*p, '\0'};
        afd_transicao(afd, atual, simbolo, prox);
      }
      atual = prox;
    }
    afd_marcar_final(afd, atual);
  }
}

/* =====================================================================
 * 5. CONSTRUTORES ESPECÍFICOS — ID e NUM
 * ===================================================================== */

void afd_identificador(AFD* afd) {
  afd_init(afd, "ID");
  int estado = afd_novo_estado(afd);
  afd_transicao(afd, afd->inicial, INICIAIS_ID, estado);
  afd_transicao(afd, estado, CONTINUACAO_ID, estado);
  afd_marcar_final(afd, estado);
}

void afd_numero(AFD* afd) {
  afd_init(afd, "NUM");
  int estado = afd_novo_estado(afd);
  afd_transicao(afd, afd->inicial, DIGITOS, estado);
  afd_transicao(afd, estado, DIGITOS, estado);
  afd_marcar_final(afd, estado);
}

void afd_float(AFD* afd) {
  afd_init(afd, "FLOAT_LIT");
  int inteiro = afd_novo_estado(afd);
  int ponto = afd_novo_estado(afd);
  int fracao = afd_novo_estado(afd);
  afd_transicao(afd, afd->inicial, DIGITOS, inteiro);
  afd_transicao(afd, inteiro, DIGITOS, inteiro);
  afd_transicao(afd, inteiro, ".", ponto);
  afd_transicao(afd, ponto, DIGITOS, fracao);
  afd_transicao(afd, fracao, DIGITOS, fracao);
  afd_marcar_final(afd, fracao);
}

/* =====================================================================
 * 6. RECLASSIFICAÇÃO
 * ===================================================================== */

const char* reclassificar(const char* tipo, const char* valor) {
  const Entrada* entradas = NULL;
  if (strcmp(tipo, "ID") == 0) {
    entradas = PALAVRAS_RESERVADAS;
  } else if (strcmp(tipo, "FIXO") == 0) {
    entradas = FIXOS;
  }
  if (entradas) {
    for (int i = 0; entradas[i].lexema; i++) {
      if (strcmp(entradas[i].lexema, valor) == 0) return entradas[i].tipo;
    }
  }
  return tipo;
}

/* =====================================================================
 * 7. LEXER
 * ===================================================================== */

#define MAX_REGRAS 8

typedef struct {
  AFD afd;
  const char* tipo;
} Regra;

typedef struct {
  const char* entrada;
  size_t pos;
  int linha;
  int coluna;
  Regra regras[MAX_REGRAS];
  int n_regras;
} Lexer;

void lexer_init(Lexer* lx, const char* entrada) {
  lx->entrada = entrada;
  lx->pos = 0;
  lx->linha = 1;
  lx->coluna = 1;
  lx->n_regras = 0;
}

void lexer_add_regra(Lexer* lx, const char* tipo, const AFD* afd) {
  if (lx->n_regras >= MAX_REGRAS) {
    fprintf(stderr, "Limite de regras do lexer excedido\n");
    exit(1);
  }
  lx->regras[lx->n_regras].afd = *afd;
  lx->regras[lx->n_regras].tipo = tipo;
  lx->n_regras++;
}

static void lexer_avancar(Lexer* lx, int n) {
  for (int i = 0; i < n; i++) {
    if (lx->entrada[lx->pos] == '\0') return;
    if (lx->entrada[lx->pos] == '\n') {
      lx->linha++;
      lx->coluna = 1;
    } else {
      lx->coluna++;
    }
    lx->pos++;
  }
}

bool lexer_proximo(Lexer* lx, Token* out) {
  /* Espaços e comentários são ignorados, como WS/LINE_COMMENT/BLOCK_COMMENT. */
  for (;;) {
    while (lx->entrada[lx->pos] == ' ' || lx->entrada[lx->pos] == '\t' ||
           lx->entrada[lx->pos] == '\r' || lx->entrada[lx->pos] == '\n') {
      lexer_avancar(lx, 1);
    }
    if (lx->entrada[lx->pos] == '/' && lx->entrada[lx->pos + 1] == '/') {
      while (lx->entrada[lx->pos] && lx->entrada[lx->pos] != '\n')
        lexer_avancar(lx, 1);
      continue;
    }
    if (lx->entrada[lx->pos] == '/' && lx->entrada[lx->pos + 1] == '*') {
      int linha_inicio = lx->linha;
      int coluna_inicio = lx->coluna;
      lexer_avancar(lx, 2);
      while (lx->entrada[lx->pos] &&
             !(lx->entrada[lx->pos] == '*' && lx->entrada[lx->pos + 1] == '/'))
        lexer_avancar(lx, 1);
      if (!lx->entrada[lx->pos]) {
        fprintf(stderr, "Comentario de bloco nao terminado em L%d:C%d\n",
                linha_inicio, coluna_inicio);
        exit(1);
      }
      lexer_avancar(lx, 2);
      continue;
    }
    break;
  }
  if (lx->entrada[lx->pos] == '\0') return false;

  int linha = lx->linha;
  int coluna = lx->coluna;
  const char* resto = lx->entrada + lx->pos;

  /* processa o maximo de caracteres */
  int melhor_tam = 0;
  const char* melhor_tipo = NULL;

  /* Strings e caracteres seguem as regras STRING_LIT e CHAR_LIT da gramática. */
  if (resto[0] == '"' || resto[0] == '\'') {
    char delimitador = resto[0];
    size_t i = 1;
    int unidades = 0;
    while (resto[i] && resto[i] != '\n' && resto[i] != '\r') {
      if (resto[i] == '\\') {
        if (!resto[i + 1] || resto[i + 1] == '\n' || resto[i + 1] == '\r') break;
        i += 2;
        unidades++;
      } else if (resto[i] == delimitador) {
        if (delimitador == '\'' && unidades != 1) break;
        melhor_tam = (int)i + 1;
        melhor_tipo = delimitador == '"' ? "STRING_LIT" : "CHAR_LIT";
        break;
      } else {
        i++;
        unidades++;
      }
    }
    if (melhor_tam == 0) {
      fprintf(stderr, "Literal de texto invalido em L%d:C%d\n", linha, coluna);
      exit(1);
    }
  }

  if (melhor_tam == 0) {
  for (int i = 0; i < lx->n_regras; i++) {
    int tam = afd_maior_prefixo(&lx->regras[i].afd, resto);
    if (tam > melhor_tam) {
      melhor_tam = tam;
      melhor_tipo = lx->regras[i].tipo;
    }
  }
  }

  if (melhor_tam == 0) {
    fprintf(stderr, "Caractere inesperado '%c' em L%d:C%d\n", resto[0], linha,
            coluna);
    exit(1);
  }

  if ((size_t)melhor_tam >= sizeof out->valor) {
    fprintf(stderr, "Token muito longo em L%d:C%d (maximo: %zu caracteres)\n",
            linha, coluna, sizeof out->valor - 1);
    exit(1);
  }

  snprintf(out->tipo, sizeof out->tipo, "%s", melhor_tipo);
  memcpy(out->valor, resto, (size_t)melhor_tam);
  out->valor[melhor_tam] = '\0';
  out->linha = linha;
  out->coluna = coluna;
  lexer_avancar(lx, melhor_tam);

  /* O tipo original pode apontar para out->tipo; evite copiar sobre ele mesmo. */
  const char* tipo = reclassificar(melhor_tipo, out->valor);
  snprintf(out->tipo, sizeof out->tipo, "%s", tipo);

  return true;
}

/* =====================================================================
 * 8. PROGRAMA PRINCIPAL
 * ===================================================================== */

static void construir_regras(Lexer* lx) {
  AFD afd;

  afd_identificador(&afd);
  lexer_add_regra(lx, "ID", &afd);
  afd_float(&afd);
  lexer_add_regra(lx, "FLOAT_LIT", &afd);
  afd_numero(&afd);
  lexer_add_regra(lx, "INT_LIT", &afd);
  afd_conjunto_literais(&afd, "FIXO", FIXOS);
  lexer_add_regra(lx, "FIXO", &afd);
}

int main(int argc, char** argv) {
  const char* codigo = "";
  char* buffer = NULL;

  if (argc > 1) {
    FILE* f = fopen(argv[1], "rb");
    if (!f) {
      perror(argv[1]);
      return 1;
    }
    fseek(f, 0, SEEK_END);
    long n = ftell(f);
    rewind(f);
    buffer = malloc((size_t)n + 1);
    if (!buffer) {
      fclose(f);
      return 1;
    }
    fread(buffer, 1, (size_t)n, f);
    buffer[n] = '\0';
    fclose(f);
    codigo = buffer;
  }

  Lexer lx;
  lexer_init(&lx, codigo);
  construir_regras(&lx);

  const char* caminho_saida = argc > 2 ? argv[2] : "tokens.tsv";
  FILE* saida = fopen(caminho_saida, "w");
  if (!saida) {
    perror(caminho_saida);
    free(buffer);
    return 1;
  }
  /* Formato: nome simbólico, linha, coluna (1-based), texto escapado. */
  fprintf(saida, "#LEXER_TOKENS_V1\n");

  Token t;
  while (lexer_proximo(&lx, &t)) {
    fprintf(saida, "%s\t%d\t%d\t", t.tipo, t.linha, t.coluna);
    for (const unsigned char* p = (const unsigned char*)t.valor; *p; p++) {
      if (*p == '\\') fputs("\\\\", saida);
      else if (*p == '\t') fputs("\\t", saida);
      else if (*p == '\n') fputs("\\n", saida);
      else if (*p == '\r') fputs("\\r", saida);
      else fputc(*p, saida);
    }
    fputc('\n', saida);
  }

  fclose(saida);

  free(buffer);
  return 0;
}
