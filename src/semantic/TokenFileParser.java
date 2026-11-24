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

/** Lê a saída TSV do lexer C e entrega os tokens ao parser ANTLR. */
public final class TokenFileParser {
  private TokenFileParser() {}

  public static void main(String[] args) throws IOException {
    if (args.length < 2 || args.length > 3) {
      System.err.println("Uso: TokenFileParser <tokens.tsv> <LexScript.tokens> [arvore.txt]");
      System.exit(2);
    }

    Map<String, Integer> vocabulary = readVocabulary(Path.of(args[1]));
    List<Token> tokens = readTokens(Path.of(args[0]), vocabulary);
    tokens.add(new CommonToken(Token.EOF, "<EOF>"));

    ListTokenSource source = new ListTokenSource(tokens, args[0]);
    CommonTokenStream stream = new CommonTokenStream(source);
    LexScriptParser parser = new LexScriptParser(stream);
    LexScriptParser.ProgramContext tree = parser.program();
    if (parser.getNumberOfSyntaxErrors() != 0) System.exit(1);

    String parseTree = tree.toStringTree(parser) + System.lineSeparator();
    if (args.length == 3) {
      Path output = Path.of(args[2]);
      if (output.getParent() != null) Files.createDirectories(output.getParent());
      Files.writeString(output, parseTree, StandardCharsets.UTF_8);
    } else {
      System.out.print(parseTree);
    }
  }

  private static Map<String, Integer> readVocabulary(Path file) throws IOException {
    Map<String, Integer> result = new HashMap<>();
    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("//")) continue;
      int equals = trimmed.lastIndexOf('=');
      if (equals < 1) continue;
      String name = trimmed.substring(0, equals);
      int type = Integer.parseInt(trimmed.substring(equals + 1));
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
      int line = Integer.parseInt(fields[1]);
      int column = Integer.parseInt(fields[2]);
      String text = unescape(fields[3]);

      CommonToken token = new CommonToken(type, text);
      token.setLine(line);
      token.setCharPositionInLine(column - 1); // C é 1-based; ANTLR é 0-based.
      result.add(token);
    }
    return result;
  }

  private static int tokenType(String name, Map<String, Integer> vocabulary) {
    Integer type = vocabulary.get(name);
    if (type == null) type = vocabulary.get(literalFor(name));
    if (type == null) throw new IllegalArgumentException(
        "Token '" + name + "' não existe em LexScript.tokens; regenere a gramática ou ajuste o lexer.");
    return type;
  }

  /* Tipos dos símbolos fixos emitidos pelo lexer C -> literais da gramática ANTLR. */
  private static String literalFor(String name) {
    switch (name) {
      case "ATRIB": return "=";
      case "MENOR": return "<";
      case "MAIOR": return ">";
      case "OP_EQ": return "==";
      case "OP_NE": return "!=";
      case "OP_LE": return "<=";
      case "OP_GE": return ">=";
      case "OP_ADD_ASSIGN": return "+=";
      case "OP_SUB_ASSIGN": return "-=";
      case "OP_MUL_ASSIGN": return "*=";
      case "OP_DIV_ASSIGN": return "/=";
      case "OP_PLUS": return "+";
      case "OP_MINUS": return "-";
      case "OP_MUL": return "*";
      case "OP_DIV": return "/";
      case "OP_MOD": return "%";
      case "OP_NOT": return "!";
      case "VIRGULA": return ",";
      case "PAR_ESQ": return "(";
      case "PAR_DIR": return ")";
      case "CHAVE_ESQ": return "{";
      case "CHAVE_DIR": return "}";
      case "PV": return ";";
      default: return "";
    }
  }

  private static String unescape(String text) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '\\' && i + 1 < text.length()) {
        char next = text.charAt(++i);
        switch (next) {
          case 't': out.append('\t'); break;
          case 'n': out.append('\n'); break;
          case 'r': out.append('\r'); break;
          case '\\': out.append('\\'); break;
          default: out.append(next);
        }
      } else out.append(c);
    }
    return out.toString();
  }

  private static String unquoteLiteral(String literal) {
    return unescape(literal.substring(1, literal.length() - 1));
  }
}
