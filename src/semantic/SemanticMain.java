import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ListTokenSource;
import org.antlr.v4.runtime.Token;

/**
 * SemanticMain — ponto de entrada para a análise semântica.
 *
 * <p>Fluxo:
 * <ol>
 *   <li>Lê o TSV produzido pelo lexer C.
 *   <li>Monta o stream de tokens e roda o parser ANTLR (igual ao TokenFileParser).
 *   <li>Instancia a {@link TabelaDeSimbolos} e o {@link SemanticVisitor}.
 *   <li>Visita a árvore e imprime os erros semânticos encontrados.
 * </ol>
 *
 * <p>Uso:
 * <pre>
 *   java -cp "build/classes:antlr/antlr-4.13.2-complete.jar" SemanticMain \
 *        resultado/tokens.tsv build/antlr/LexScript.tokens
 * </pre>
 */
public final class SemanticMain {

    private SemanticMain() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Uso: SemanticMain <tokens.tsv> <LexScript.tokens>");
            System.exit(2);
        }

        // 1. Carrega vocabulário e tokens (reutiliza lógica do TokenFileParser)
        Map<String, Integer> vocabulary = readVocabulary(Path.of(args[1]));
        List<Token> tokens = readTokens(Path.of(args[0]), vocabulary);
        tokens.add(new CommonToken(Token.EOF, "<EOF>"));

        // 2. Parseia
        ListTokenSource source = new ListTokenSource(tokens, args[0]);
        CommonTokenStream stream = new CommonTokenStream(source);
        LexScriptParser parser = new LexScriptParser(stream);
        LexScriptParser.ProgramContext tree = parser.program();

        if (parser.getNumberOfSyntaxErrors() != 0) {
            System.err.println("Análise semântica abortada: erros sintáticos encontrados.");
            System.exit(1);
        }

        // 3. Análise semântica
        System.out.println("=== Árvore Sintática ===");
        System.out.println(tree.toStringTree(parser));
        System.out.println();
        System.out.println("=== Análise Semântica ===");

        TabelaDeSimbolos tabela = new TabelaDeSimbolos();
        SemanticVisitor  visitor = new SemanticVisitor(tabela);
        visitor.visit(tree);

        // 4. Resultado
        System.out.println();
        System.out.println("=== Tabela de Símbolos (estado final) ===");
        tabela.dump();
        System.out.println();

        int erros = tabela.getTotalErros();
        if (erros == 0) {
            System.out.println("Nenhum erro semântico encontrado. ✓");
        } else {
            System.out.println("Total de erros semânticos: " + erros);
            System.exit(3);
        }
    }

    // ------------------------------------------------------------------
    // Helpers (copiados/adaptados de TokenFileParser)
    // ------------------------------------------------------------------

    private static Map<String, Integer> readVocabulary(Path file) throws IOException {
        Map<String, Integer> result = new HashMap<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//")) continue;
            int equals = trimmed.lastIndexOf('=');
            if (equals < 1) continue;
            String name = trimmed.substring(0, equals);
            int type    = Integer.parseInt(trimmed.substring(equals + 1));
            result.put(name, type);
            if (name.startsWith("'") && name.endsWith("'")) {
                result.put(unquoteLiteral(name), type);
            }
        }
        return result;
    }

    private static List<Token> readTokens(Path file, Map<String, Integer> vocabulary)
            throws IOException {
        List<Token> result = new ArrayList<>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals("#LEXER_TOKENS_V1")) {
            throw new IllegalArgumentException("Cabeçalho/formato de tokens inválido: " + file);
        }
        for (int i = 1; i < lines.size(); i++) {
            String row = lines.get(i);
            if (row.isEmpty()) continue;
            String[] fields = row.split("\\t", 4);
            if (fields.length != 4) throw new IllegalArgumentException("Linha TSV inválida: " + (i + 1));
            int type = tokenType(fields[0], vocabulary);
            int lin  = Integer.parseInt(fields[1]);
            int col  = Integer.parseInt(fields[2]);
            String text = unescape(fields[3]);
            CommonToken token = new CommonToken(type, text);
            token.setLine(lin);
            token.setCharPositionInLine(col - 1);
            result.add(token);
        }
        return result;
    }

    private static int tokenType(String name, Map<String, Integer> vocabulary) {
        Integer type = vocabulary.get(name);
        if (type == null) type = vocabulary.get(literalFor(name));
        if (type == null) throw new IllegalArgumentException(
            "Token '" + name + "' não encontrado no vocabulário.");
        return type;
    }

    private static String literalFor(String name) {
        switch (name) {
            case "ATRIB":          return "=";
            case "MENOR":          return "<";
            case "MAIOR":          return ">";
            case "OP_EQ":          return "==";
            case "OP_NE":          return "!=";
            case "OP_LE":          return "<=";
            case "OP_GE":          return ">=";
            case "OP_ADD_ASSIGN":  return "+=";
            case "OP_SUB_ASSIGN":  return "-=";
            case "OP_MUL_ASSIGN":  return "*=";
            case "OP_DIV_ASSIGN":  return "/=";
            case "OP_PLUS":        return "+";
            case "OP_MINUS":       return "-";
            case "OP_MUL":         return "*";
            case "OP_DIV":         return "/";
            case "OP_MOD":         return "%";
            case "OP_NOT":         return "!";
            case "VIRGULA":        return ",";
            case "PAR_ESQ":        return "(";
            case "PAR_DIR":        return ")";
            case "CHAVE_ESQ":      return "{";
            case "CHAVE_DIR":      return "}";
            case "PV":             return ";";
            default:               return "";
        }
    }

    private static String unescape(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(++i);
                switch (next) {
                    case 't':  out.append('\t'); break;
                    case 'n':  out.append('\n'); break;
                    case 'r':  out.append('\r'); break;
                    case '\\': out.append('\\'); break;
                    default:   out.append(next);
                }
            } else out.append(c);
        }
        return out.toString();
    }

    private static String unquoteLiteral(String literal) {
        return unescape(literal.substring(1, literal.length() - 1));
    }
}
