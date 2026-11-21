# Execute a partir da raiz do projeto.

# Criar pastas separadas para ferramentas, classes e resultados:
mkdir -p build/antlr build/classes resultado

# Compilar o lexer C e gerar os tokens:
gcc -std=c11 -Wall -Wextra -pedantic lexer_aluno.c -o build/lexer_aluno
./build/lexer_aluno codigo_entrada.txt resultado/tokens.tsv

# Gerar e compilar o parser ANTLR usando o JAR local:
ANTLR_JAR=antlr/antlr-4.13.2-complete.jar
java -jar "$ANTLR_JAR" -Dlanguage=Java -o build/antlr LexScript.g4
javac -cp "$ANTLR_JAR" -d build/classes build/antlr/*.java TokenFileParser.java

# Analisar os tokens e gravar a árvore sintática:
java -cp "build/classes:$ANTLR_JAR" TokenFileParser \
  resultado/tokens.tsv build/antlr/LexScript.tokens resultado/arvore.txt

# Ver os resultados:
cat resultado/tokens.tsv
cat resultado/arvore.txt
