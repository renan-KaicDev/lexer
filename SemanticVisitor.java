/**
 * SemanticVisitor — Visitante semântico para a gramática LexScript.
 *
 * <p>Percorre a AST gerada pelo ANTLR e realiza as seguintes verificações:
 * <ol>
 *   <li><b>Declaração duplicada</b>: mesma variável declarada mais de uma vez
 *       no mesmo escopo.
 *   <li><b>Uso antes da declaração</b>: identificador referenciado em expressão
 *       ou atribuição sem ter sido declarado em nenhum escopo visível.
 * </ol>
 *
 * <p>Estratégia de escopos:
 * <ul>
 *   <li>Um escopo global é aberto para o programa.
 *   <li>Cada {@code funcDecl} abre um escopo próprio (os parâmetros formais
 *       são registrados nele antes de visitar o corpo).
 *   <li>Cada {@code block} aninhado <em>dentro</em> de uma função abre um
 *       sub-escopo (suporte a variáveis locais de blocos).
 *   <li>O {@code forInit} abre um escopo dedicado para que a variável de
 *       controle fique visível apenas dentro do laço.
 * </ul>
 */
public class SemanticVisitor extends LexScriptBaseVisitor<Void> {

    private final TabelaDeSimbolos tabela;

    // Rastreia se estamos dentro de um bloco de função (para não abrir
    // escopo duplo: funcDecl abre o escopo, block interno NÃO abre outro
    // escopo imediatamente — apenas blocos aninhados dentro do corpo abrem).
    private boolean dentroDeCorpoDeFuncao = false;

    public SemanticVisitor(TabelaDeSimbolos tabela) {
        this.tabela = tabela;
    }

    // ===================================================================
    // Programa — escopo global
    // ===================================================================

    @Override
    public Void visitProgram(LexScriptParser.ProgramContext ctx) {
        tabela.abrirEscopo();           // escopo global
        visitChildren(ctx);
        tabela.fecharEscopo();
        return null;
    }

    // ===================================================================
    // Declaração de função
    // ===================================================================

    @Override
    public Void visitFuncDecl(LexScriptParser.FuncDeclContext ctx) {
        // Registra a função no escopo global (para chamadas recursivas ou cruzadas)
        String nomeFuncao = ctx.ID().getText();
        int linha = ctx.ID().getSymbol().getLine();
        String tipoRetorno = ctx.returnType().getText();
        tabela.declarar(nomeFuncao, "func:" + tipoRetorno, linha);

        // Abre escopo para o corpo da função
        tabela.abrirEscopo();
        dentroDeCorpoDeFuncao = true;

        // Declara os parâmetros formais no escopo da função
        if (ctx.paramList() != null) {
            visit(ctx.paramList());
        }

        // Visita o bloco do corpo (a flag evita dupla abertura de escopo)
        visit(ctx.block());
        dentroDeCorpoDeFuncao = false;

        tabela.fecharEscopo();
        return null;
    }

    // ===================================================================
    // Parâmetros formais — declarados no escopo da função
    // ===================================================================

    @Override
    public Void visitParam(LexScriptParser.ParamContext ctx) {
        String nome = ctx.ID().getText();
        String tipo = ctx.type().getText();
        int linha   = ctx.ID().getSymbol().getLine();
        tabela.declarar(nome, tipo, linha);
        return null;
    }

    // ===================================================================
    // Bloco { ... }
    // ===================================================================

    @Override
    public Void visitBlock(LexScriptParser.BlockContext ctx) {
        if (dentroDeCorpoDeFuncao) {
            // O bloco imediato do corpo da função já tem seu escopo aberto
            // em visitFuncDecl; apenas visitamos os filhos.
            dentroDeCorpoDeFuncao = false;
            visitChildren(ctx);
        } else {
            // Bloco aninhado (if/else/while/for) → novo sub-escopo
            tabela.abrirEscopo();
            visitChildren(ctx);
            tabela.fecharEscopo();
        }
        return null;
    }

    // ===================================================================
    // Declaração de variável:  var <tipo> <nome> [= <expr>] ;
    // ===================================================================

    @Override
    public Void visitVarDecl(LexScriptParser.VarDeclContext ctx) {
        String nome = ctx.ID().getText();
        String tipo = ctx.type().getText();
        int linha   = ctx.ID().getSymbol().getLine();

        // Se há inicializador, avalia a expressão ANTES de declarar a variável
        // (evita que "var int x = x + 1;" passe sem erro)
        if (ctx.expr() != null) {
            visit(ctx.expr());
        }

        tabela.declarar(nome, tipo, linha);
        return null;
    }

    // ===================================================================
    // Atribuição:  <nome> <op> <expr> ;
    // ===================================================================

    @Override
    public Void visitAssignStmt(LexScriptParser.AssignStmtContext ctx) {
        String nome = ctx.ID().getText();
        int linha   = ctx.ID().getSymbol().getLine();

        // O alvo da atribuição deve estar declarado
        tabela.verificarUso(nome, linha);

        // Verifica a expressão do lado direito
        visit(ctx.expr());
        return null;
    }

    // ===================================================================
    // For — a variável de init fica visível dentro do laço inteiro
    // ===================================================================

    @Override
    public Void visitForStmt(LexScriptParser.ForStmtContext ctx) {
        tabela.abrirEscopo();           // escopo do for

        visit(ctx.forInit());           // declara ou verifica variável de controle
        visit(ctx.expr());              // condição
        visit(ctx.forUpdate());         // atualização
        // O bloco não abre novo escopo (já estamos no escopo do for)
        dentroDeCorpoDeFuncao = true;   // reutiliza a flag para o bloco interno
        visit(ctx.block());

        tabela.fecharEscopo();
        return null;
    }

    // ===================================================================
    // ForUpdate:  <id> <op> <expr>  (sem ';')
    // ===================================================================

    @Override
    public Void visitForUpdate(LexScriptParser.ForUpdateContext ctx) {
        String nome = ctx.ID().getText();
        int linha   = ctx.ID().getSymbol().getLine();
        tabela.verificarUso(nome, linha);
        visit(ctx.expr());
        return null;
    }

    // ===================================================================
    // Identificador em expressão — verificar uso
    // ===================================================================

    @Override
    public Void visitIdentifier(LexScriptParser.IdentifierContext ctx) {
        String nome = ctx.ID().getText();
        int linha   = ctx.ID().getSymbol().getLine();
        tabela.verificarUso(nome, linha);
        return null;
    }

    // ===================================================================
    // Chamada de função em expressão — verifica o nome da função
    // ===================================================================

    @Override
    public Void visitFuncCall(LexScriptParser.FuncCallContext ctx) {
        String nome = ctx.ID().getText();
        int linha   = ctx.ID().getSymbol().getLine();
        tabela.verificarUso(nome, linha);

        // Visita os argumentos passados
        if (ctx.argList() != null) {
            visit(ctx.argList());
        }
        return null;
    }

    // ===================================================================
    // Nós folha que não exigem verificação extra — delega ao padrão
    // ===================================================================

    // visitLiteralExpr, visitAdditive, visitMultiplicative, etc.
    // já são cobertos por visitChildren via LexScriptBaseVisitor.
}
