import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Tabela de Símbolos com suporte a escopos aninhados (pilha de escopos).
 *
 * <p>Cada escopo é um {@code HashMap<String, SimboloInfo>} empilhado em uma
 * {@code ArrayDeque}. Ao entrar em um bloco (funcDecl / block) um novo escopo
 * é criado; ao sair, ele é descartado.
 *
 * <p>As informações armazenadas para cada símbolo são:
 * <ul>
 *   <li><b>nome</b>  — lexema do identificador
 *   <li><b>tipo</b>  — tipo primitivo declarado (int, float, bool, string, char)
 *   <li><b>linha</b> — linha da declaração no código-fonte
 * </ul>
 */
public final class TabelaDeSimbolos {

    // ---------------------------------------------------------------
    // Registro de um símbolo
    // ---------------------------------------------------------------
    public static final class SimboloInfo {
        public final String nome;
        public final String tipo;
        public final int    linha;

        public SimboloInfo(String nome, String tipo, int linha) {
            this.nome  = nome;
            this.tipo  = tipo;
            this.linha = linha;
        }

        @Override
        public String toString() {
            return String.format("SimboloInfo{nome='%s', tipo='%s', linha=%d}", nome, tipo, linha);
        }
    }

    // ---------------------------------------------------------------
    // Pilha de escopos: topo = escopo mais interno
    // ---------------------------------------------------------------
    private final Deque<Map<String, SimboloInfo>> escopos = new ArrayDeque<>();

    // Contador de erros semânticos encontrados
    private int totalErros = 0;

    // ---------------------------------------------------------------
    // Gestão de escopos
    // ---------------------------------------------------------------

    /** Abre um novo escopo (início de função ou bloco aninhado). */
    public void abrirEscopo() {
        escopos.push(new HashMap<>());
    }

    /** Fecha o escopo atual (fim de função ou bloco aninhado). */
    public void fecharEscopo() {
        if (!escopos.isEmpty()) {
            escopos.pop();
        }
    }

    // ---------------------------------------------------------------
    // Declaração
    // ---------------------------------------------------------------

    /**
     * Declara uma variável no escopo atual.
     *
     * @return {@code true} se declarada com sucesso;
     *         {@code false} se já existe no mesmo escopo (duplicada).
     */
    public boolean declarar(String nome, String tipo, int linha) {
        Map<String, SimboloInfo> escopoAtual = escopos.peek();
        if (escopoAtual == null) {
            throw new IllegalStateException("Nenhum escopo aberto. Chame abrirEscopo() primeiro.");
        }

        if (escopoAtual.containsKey(nome)) {
            // Declaração duplicada no mesmo escopo
            SimboloInfo anterior = escopoAtual.get(nome);
            reportarErro(linha,
                "ERRO: Declaração duplicada da variável '" + nome + "'. "
                + "Já declarada na linha " + anterior.linha + ".");
            return false;
        }

        escopoAtual.put(nome, new SimboloInfo(nome, tipo, linha));
        return true;
    }

    // ---------------------------------------------------------------
    // Busca
    // ---------------------------------------------------------------

    /**
     * Verifica se um identificador foi declarado em algum escopo visível.
     *
     * @return o {@link SimboloInfo} encontrado, ou {@code null} se não declarado.
     */
    public SimboloInfo buscar(String nome) {
        // Percorre da mais interna para a mais externa
        for (Map<String, SimboloInfo> escopo : escopos) {
            SimboloInfo info = escopo.get(nome);
            if (info != null) return info;
        }
        return null;
    }

    /**
     * Verifica o uso de uma variável; reporta erro se não declarada.
     *
     * @param nome  nome do identificador usado
     * @param linha linha onde o uso ocorre
     * @return o {@link SimboloInfo} se encontrado, {@code null} caso contrário.
     */
    public SimboloInfo verificarUso(String nome, int linha) {
        SimboloInfo info = buscar(nome);
        if (info == null) {
            reportarErro(linha,
                "ERRO: Uso de variável '" + nome + "' antes da declaração (linha " + linha + ").");
        }
        return info;
    }

    // ---------------------------------------------------------------
    // Relatório de erros
    // ---------------------------------------------------------------

    private void reportarErro(int linha, String mensagem) {
        totalErros++;
        System.err.println("[Semântico] Linha " + linha + ": " + mensagem);
    }

    public int getTotalErros() { return totalErros; }

    // ---------------------------------------------------------------
    // Debug: exibe todos os escopos ativos (da mais interna à global)
    // ---------------------------------------------------------------
    public void dump() {
        int nivel = 0;
        for (Map<String, SimboloInfo> escopo : escopos) {
            System.out.println("  Escopo nível " + nivel + ":");
            if (escopo.isEmpty()) {
                System.out.println("    (vazio)");
            } else {
                escopo.values().forEach(s ->
                    System.out.printf("    %-20s tipo=%-8s linha=%d%n",
                        s.nome, s.tipo, s.linha));
            }
            nivel++;
        }
    }
}
