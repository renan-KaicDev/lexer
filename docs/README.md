# Lexer C + ANTLR `ListTokenSource`

O lexer da aula continua reconhecendo os lexemas em C. Agora ele grava `tokens.tsv`, e `TokenFileParser.java` converte esse arquivo em `CommonToken` e os entrega ao parser por meio de `ListTokenSource`.

## Formato do arquivo

O arquivo começa com `#LEXER_TOKENS_V1`. Cada linha seguinte contém quatro campos separados por TAB:

```text
TIPO_ANTLR<TAB>linha<TAB>coluna<TAB>texto
```

Linha e coluna começam em 1 no arquivo do lexer. O adaptador converte a coluna para a convenção ANTLR (iniciada em 0), decodifica `\\t`, `\\n`, `\\r` e `\\\\`, e acrescenta EOF.

Os tokens nomeados usam os símbolos de `LexScript.g4` (`KW_INT`, `ID`, `INT_LIT` etc.). Os nomes internos do lexer C para pontuação e operadores são associados aos literais da gramática. O adaptador lê `LexScript.tokens`, gerado pelo ANTLR, para obter os números de tipo corretos; assim, não depende de números codificados à mão.

## Gerar e consumir tokens

1. Crie as pastas de saída e gere as classes Java e o vocabulário ANTLR:

   ```sh
   mkdir -p build/antlr build/classes resultado
   java -jar antlr/antlr-4.13.2-complete.jar -Dlanguage=Java -o build/antlr LexScript.g4
   ```

2. Compile e execute o lexer C. O TSV fica em `resultado/tokens.tsv`:

   ```sh
   gcc -std=c11 -Wall -Wextra -pedantic lexer_aluno.c -o build/lexer_aluno
   build/lexer_aluno codigo_entrada.txt resultado/tokens.tsv
   ```

3. Compile o adaptador junto com as classes geradas, usando o mesmo JAR do runtime ANTLR:

   ```sh
   javac -cp antlr/antlr-4.13.2-complete.jar -d build/classes build/antlr/*.java TokenFileParser.java
   ```

4. Passe o TSV e o vocabulário para o adaptador. A árvore sintática é gravada em `resultado/arvore.txt`:

   ```sh
   java -cp build/classes:antlr/antlr-4.13.2-complete.jar TokenFileParser \
     resultado/tokens.tsv build/antlr/LexScript.tokens resultado/arvore.txt
   ```

`TokenFileParser` chama `parser.program()`. Em caso de erro sintático, o ANTLR reporta o problema no terminal e o adaptador termina com erro, sem gravar uma árvore como resultado válido.

## Cobertura léxica

O lexer C reconhece as palavras-chave, identificadores, inteiros, decimais, booleanos, strings, caracteres, operadores e pontuação definidos em `LexScript.g4`. Espaços em branco e comentários de linha/bloco são ignorados. Comentários de bloco sem fechamento e literais de texto inválidos geram erro com posição.

Os operadores lógicos definidos pela gramática são as palavras `and` e `or`; `&&` e `||` não são aceitos. Atribuições compostas (`+=`, `-=`, `*=`, `/=`) e operadores restantes são traduzidos pelo adaptador a partir dos literais presentes em `LexScript.tokens`.
