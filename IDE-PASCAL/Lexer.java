import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Lexer {
    private final String codigo;
    private int pos = 0;
    private int linha = 1;

    private final List<Token> tokens = new ArrayList<>();
    private final List<String> erros = new ArrayList<>();

    public static final Set<String> PALAVRAS_RESERVADAS = Set.of(
            "program", "const", "type", "var", "array", "of", "record",
            "begin", "end", "integer", "real", "boolean", "char", "string",
            "if", "then", "else", "while", "do", "for", "to", "downto",
            "repeat", "until", "read", "readln", "write", "writeln",
            "div", "mod", "and", "or", "not", "true", "false",
            "function", "procedure"
    );

    public Lexer(String codigo) {
        this.codigo = codigo == null ? "" : codigo;
    }

    public List<Token> analisar() {
        tokens.clear();
        erros.clear();
        pos = 0;
        linha = 1;

        while (pos < codigo.length()) {
            char atual = codigo.charAt(pos);

            if (Character.isWhitespace(atual)) {
                consumirEspaco();
            } else if (atual == '{') {
                reconhecerComentarioChaves();
            } else if (atual == '(' && proximoEh('*')) {
                reconhecerComentarioParenteses();
            } else if (atual == '/' && proximoEh('/')) {
                reconhecerComentarioLinha();
            } else if (Character.isLetter(atual)) {
                reconhecerIdentificadorOuReservada();
            } else if (Character.isDigit(atual)) {
                reconhecerNumero();
            } else if (atual == '\'') {
                reconhecerString();
            } else {
                reconhecerSimbolo();
            }
        }

        validarSintaxeBasica();

        tokens.add(new Token("EOF", "FIM_ARQUIVO", "fim da entrada",
                "Marcador de fim do código", linha));

        return new ArrayList<>(tokens);
    }

    private void consumirEspaco() {
        if (codigo.charAt(pos) == '\n') linha++;
        pos++;
    }

    private void reconhecerIdentificadorOuReservada() {
        int linhaInicial = linha;
        StringBuilder sb = new StringBuilder();

        while (pos < codigo.length()) {
            char c = codigo.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
                pos++;
            } else {
                break;
            }
        }

        String lexema = sb.toString();
        String lexemaLower = lexema.toLowerCase();

        if (isPalavraReservada(lexemaLower)) {
            tokens.add(new Token(lexema, "PALAVRA_RESERVADA", lexemaLower,
                    "Palavra com significado fixo na linguagem", linhaInicial));
        } else {
            tokens.add(new Token(lexema, "IDENTIFICADOR", "letra (letra | digito | _)*",
                    "Nome definido pelo programador", linhaInicial));
        }
    }

    private void reconhecerNumero() {
        int linhaInicial = linha;
        StringBuilder sb = new StringBuilder();

        while (pos < codigo.length() && Character.isDigit(codigo.charAt(pos))) {
            sb.append(codigo.charAt(pos));
            pos++;
        }

        if (pos < codigo.length()
                && codigo.charAt(pos) == '.'
                && !proximoEh('.')
                && pos + 1 < codigo.length()
                && Character.isDigit(codigo.charAt(pos + 1))) {

            sb.append('.');
            pos++;

            while (pos < codigo.length() && Character.isDigit(codigo.charAt(pos))) {
                sb.append(codigo.charAt(pos));
                pos++;
            }

            tokens.add(new Token(sb.toString(), "NUMERO_REAL", "digito+ . digito+",
                    "Número com parte decimal", linhaInicial));
        } else {
            tokens.add(new Token(sb.toString(), "NUMERO_INTEIRO", "digito+",
                    "Número inteiro", linhaInicial));
        }
    }

    private void reconhecerString() {
        int linhaInicial = linha;
        StringBuilder sb = new StringBuilder();

        sb.append(codigo.charAt(pos));
        pos++;

        while (pos < codigo.length() && codigo.charAt(pos) != '\'') {
            if (codigo.charAt(pos) == '\n') {
                adicionarErro("Linha " + linhaInicial + ": constante literal não fechada.");
                linha++;
                pos++;
                return;
            }

            sb.append(codigo.charAt(pos));
            pos++;
        }

        if (pos < codigo.length() && codigo.charAt(pos) == '\'') {
            sb.append(codigo.charAt(pos));
            pos++;

            String classe = sb.length() == 3 ? "CONSTANTE_CARACTERE" : "STRING_LITERAL";
            String descricao = sb.length() == 3
                    ? "Constante de um caractere"
                    : "Texto delimitado por aspas simples";

            tokens.add(new Token(sb.toString(), classe, "' caractere* '",
                    descricao, linhaInicial));
        } else {
            adicionarErro("Linha " + linhaInicial + ": constante literal não fechada.");
        }
    }

    private void reconhecerComentarioChaves() {
        int linhaInicial = linha;
        StringBuilder sb = new StringBuilder();

        sb.append(codigo.charAt(pos));
        pos++;

        while (pos < codigo.length() && codigo.charAt(pos) != '}') {
            char c = codigo.charAt(pos);
            if (c == '\n') linha++;
            sb.append(c);
            pos++;
        }

        if (pos < codigo.length()) {
            sb.append('}');
            pos++;

            tokens.add(new Token(sb.toString(), "COMENTARIO", "{ caractere* }",
                    "Comentário ignorado pelo compilador", linhaInicial));
        } else {
            adicionarErro("Linha " + linhaInicial + ": comentário iniciado com { não foi fechado.");
        }
    }

    private void reconhecerComentarioParenteses() {
        int linhaInicial = linha;
        StringBuilder sb = new StringBuilder("(*");

        pos += 2;

        while (pos < codigo.length()) {
            char c = codigo.charAt(pos);

            if (c == '\n') linha++;

            if (c == '*' && proximoEh(')')) {
                sb.append("*)");
                pos += 2;

                tokens.add(new Token(sb.toString(), "COMENTARIO", "(* caractere* *)",
                        "Comentário ignorado pelo compilador", linhaInicial));
                return;
            }

            sb.append(c);
            pos++;
        }

        adicionarErro("Linha " + linhaInicial + ": comentário iniciado com (* não foi fechado.");
    }

    private void reconhecerComentarioLinha() {
        int linhaInicial = linha;
        StringBuilder sb = new StringBuilder("//");

        pos += 2;

        while (pos < codigo.length() && codigo.charAt(pos) != '\n') {
            sb.append(codigo.charAt(pos));
            pos++;
        }

        tokens.add(new Token(sb.toString(), "COMENTARIO", "// caractere*",
                "Comentário de uma linha", linhaInicial));
    }

    private void reconhecerSimbolo() {
        char c = codigo.charAt(pos);

        switch (c) {
            case '+':
                adicionarSimbolo("+", "OPERADOR_ADICAO", "+", "Operador de adição");
                break;

            case '-':
                adicionarSimbolo("-", "OPERADOR_SUBTRACAO", "-", "Operador de subtração");
                break;

            case '*':
                adicionarSimbolo("*", "OPERADOR_MULTIPLICACAO", "*", "Operador de multiplicação");
                break;

            case '/':
                adicionarSimbolo("/", "OPERADOR_DIVISAO", "/", "Operador de divisão real");
                break;

            case '=':
                adicionarSimbolo("=", "OPERADOR_RELACIONAL", "=", "Comparação de igualdade");
                break;

            case '<':
                if (proximoEh('=')) {
                    adicionarSimboloDuplo("<=", "OPERADOR_RELACIONAL", "<=", "Menor ou igual");
                } else if (proximoEh('>')) {
                    adicionarSimboloDuplo("<>", "OPERADOR_RELACIONAL", "<>", "Diferente");
                } else {
                    adicionarSimbolo("<", "OPERADOR_RELACIONAL", "<", "Menor que");
                }
                break;

            case '>':
                if (proximoEh('=')) {
                    adicionarSimboloDuplo(">=", "OPERADOR_RELACIONAL", ">=", "Maior ou igual");
                } else {
                    adicionarSimbolo(">", "OPERADOR_RELACIONAL", ">", "Maior que");
                }
                break;

            case ':':
                if (proximoEh('=')) {
                    adicionarSimboloDuplo(":=", "ATRIBUICAO", ":=", "Atribuição de valor");
                } else {
                    adicionarSimbolo(":", "DOIS_PONTOS", ":", "Separador de tipo");
                }
                break;

            case '.':
                if (proximoEh('.')) {
                    adicionarSimboloDuplo("..", "INTERVALO", "..", "Intervalo");
                } else {
                    adicionarSimbolo(".", "PONTO", ".", "Fim do programa ou acesso");
                }
                break;

            case ',':
                adicionarSimbolo(",", "VIRGULA", ",", "Separador de itens");
                break;

            case ';':
                adicionarSimbolo(";", "PONTO_E_VIRGULA", ";", "Fim de comando/declaração");
                break;

            case '(':
                adicionarSimbolo("(", "PARENTESE_ESQUERDO", "(", "Abertura de parênteses");
                break;

            case ')':
                adicionarSimbolo(")", "PARENTESE_DIREITO", ")", "Fecho de parênteses");
                break;

            case '[':
                adicionarSimbolo("[", "COLCHETE_ESQUERDO", "[", "Abertura de colchete");
                break;

            case ']':
                adicionarSimbolo("]", "COLCHETE_DIREITO", "]", "Fecho de colchete");
                break;

            default:
                adicionarErro("Linha " + linha + ": símbolo inválido '" + c + "'.");
                pos++;
                break;
        }
    }

    private void validarSintaxeBasica() {
        List<Token> ts = tokens.stream()
                .filter(t -> !"COMENTARIO".equals(t.getClasse()))
                .filter(t -> !"FIM_ARQUIVO".equals(t.getClasse()))
                .toList();

        if (ts.isEmpty()) return;

        validarDelimitadores(ts);
        validarEstruturaPrograma(ts);

        Set<String> declarados = coletarDeclaracoes(ts);
        validarComandos(ts, declarados);
    }

    private void validarDelimitadores(List<Token> ts) {
        List<Token> parenteses = new ArrayList<>();
        List<Token> colchetes = new ArrayList<>();

        for (Token t : ts) {
            switch (t.getClasse()) {
                case "PARENTESE_ESQUERDO" -> parenteses.add(t);

                case "PARENTESE_DIREITO" -> {
                    if (parenteses.isEmpty()) {
                        adicionarErroUnico("Linha " + t.getLinha() + ": parêntese ')' sem abertura.");
                    } else {
                        parenteses.remove(parenteses.size() - 1);
                    }
                }

                case "COLCHETE_ESQUERDO" -> colchetes.add(t);

                case "COLCHETE_DIREITO" -> {
                    if (colchetes.isEmpty()) {
                        adicionarErroUnico("Linha " + t.getLinha() + ": colchete ']' sem abertura.");
                    } else {
                        colchetes.remove(colchetes.size() - 1);
                    }
                }
            }
        }

        for (Token t : parenteses) {
            adicionarErroUnico("Linha " + t.getLinha() + ": parêntese '(' não foi fechado.");
        }

        for (Token t : colchetes) {
            adicionarErroUnico("Linha " + t.getLinha() + ": colchete '[' não foi fechado.");
        }
    }

    private void validarEstruturaPrograma(List<Token> ts) {
        Token primeiro = ts.get(0);

        if (lexemaEh(primeiro, "program")) {
            if (ts.size() < 2 || !"IDENTIFICADOR".equals(ts.get(1).getClasse())) {
                adicionarErroUnico("Linha " + primeiro.getLinha() + ": esperado nome do programa após 'program'.");
            }

            if (ts.size() < 3 || !"PONTO_E_VIRGULA".equals(ts.get(2).getClasse())) {
                adicionarErroUnico("Linha " + primeiro.getLinha() + ": esperado ';' após o cabeçalho do programa.");
            }
        } else {
            adicionarErroUnico("Linha " + primeiro.getLinha() + ": programa deve iniciar com 'program'.");
        }

        Token ultimo = ts.get(ts.size() - 1);

        if (!"PONTO".equals(ultimo.getClasse())) {
            adicionarErroUnico("Linha " + ultimo.getLinha() + ": programa deve terminar com ponto final '.'.");
        }

        List<Token> begins = new ArrayList<>();
        boolean encontrouBegin = false;

        for (Token t : ts) {
            if (lexemaEh(t, "begin")) {
                begins.add(t);
                encontrouBegin = true;
            } else if (lexemaEh(t, "end")) {
                if (begins.isEmpty()) {
                    adicionarErroUnico("Linha " + t.getLinha() + ": 'end' sem 'begin' correspondente.");
                } else {
                    begins.remove(begins.size() - 1);
                }
            }
        }

        if (!encontrouBegin) {
            adicionarErroUnico("Linha " + primeiro.getLinha() + ": bloco principal deve conter 'begin'.");
        }

        for (Token t : begins) {
            adicionarErroUnico("Linha " + t.getLinha() + ": 'begin' sem 'end' correspondente.");
        }
    }

    private Set<String> coletarDeclaracoes(List<Token> ts) {
        Set<String> declarados = new HashSet<>();

        for (int i = 0; i < ts.size(); i++) {
            if (!lexemaEh(ts.get(i), "var")) continue;

            i++;

            while (i < ts.size() && !lexemaEh(ts.get(i), "begin")) {
                List<Token> nomes = new ArrayList<>();

                while (i < ts.size() && "IDENTIFICADOR".equals(ts.get(i).getClasse())) {
                    nomes.add(ts.get(i));
                    i++;

                    if (i < ts.size() && "VIRGULA".equals(ts.get(i).getClasse())) {
                        i++;
                    } else {
                        break;
                    }
                }

                if (nomes.isEmpty()) {
                    i++;
                    continue;
                }

                if (i >= ts.size() || !"DOIS_PONTOS".equals(ts.get(i).getClasse())) {
                    adicionarErroUnico("Linha " + nomes.get(0).getLinha() + ": declaração de variável sem ':'.");
                    continue;
                }

                i++;

                if (i >= ts.size() || !tipoValido(ts.get(i))) {
                    adicionarErroUnico("Linha " + nomes.get(0).getLinha() + ": tipo de variável inválido ou ausente.");
                }

                for (Token nome : nomes) {
                    String id = nome.getLexema().toLowerCase();

                    if (!declarados.add(id)) {
                        adicionarErroUnico("Linha " + nome.getLinha() + ": variável '" + nome.getLexema() + "' já foi declarada.");
                    }
                }

                while (i < ts.size()
                        && !"PONTO_E_VIRGULA".equals(ts.get(i).getClasse())
                        && !lexemaEh(ts.get(i), "begin")) {
                    i++;
                }

                if (i < ts.size() && "PONTO_E_VIRGULA".equals(ts.get(i).getClasse())) {
                    i++;
                } else {
                    adicionarErroUnico("Linha " + nomes.get(0).getLinha() + ": declaração de variável deve terminar com ';'.");
                }
            }
        }

        return declarados;
    }

    private void validarComandos(List<Token> ts, Set<String> declarados) {
        boolean dentroBlocoPrincipal = false;
        Set<String> idsReportados = new HashSet<>();

        for (int i = 0; i < ts.size(); i++) {
            Token t = ts.get(i);

            if (lexemaEh(t, "begin")) {
                dentroBlocoPrincipal = true;
                continue;
            }

            if (lexemaEh(t, "end")) {
                dentroBlocoPrincipal = false;
            }

            if (lexemaEh(t, "if")
                    && !existeAntes(ts, i + 1, Set.of("then"), Set.of("PONTO_E_VIRGULA"))) {
                adicionarErroUnico("Linha " + t.getLinha() + ": comando 'if' sem 'then'.");
            } else if (lexemaEh(t, "while")
                    && !existeAntes(ts, i + 1, Set.of("do"), Set.of("PONTO_E_VIRGULA"))) {
                adicionarErroUnico("Linha " + t.getLinha() + ": comando 'while' sem 'do'.");
            } else if (lexemaEh(t, "for")
                    && !existeAntes(ts, i + 1, Set.of("do"), Set.of("PONTO_E_VIRGULA"))) {
                adicionarErroUnico("Linha " + t.getLinha() + ": comando 'for' sem 'do'.");
            } else if (lexemaEh(t, "repeat")
                    && !existeAntes(ts, i + 1, Set.of("until"), Set.of("PONTO"))) {
                adicionarErroUnico("Linha " + t.getLinha() + ": comando 'repeat' sem 'until'.");
            }

            if (!dentroBlocoPrincipal || !"IDENTIFICADOR".equals(t.getClasse())) continue;

            if (i > 0 && "PONTO".equals(ts.get(i - 1).getClasse())) continue;

            String id = t.getLexema().toLowerCase();

            if (!declarados.contains(id) && idsReportados.add(id)) {
                adicionarErroUnico("Linha " + t.getLinha() + ": identificador '" + t.getLexema() + "' não foi declarado.");
            }

            if (i + 1 < ts.size()
                    && "ATRIBUICAO".equals(ts.get(i + 1).getClasse())
                    && !comandoTerminaCorretamente(ts, i + 2, t.getLinha())) {
                adicionarErroUnico("Linha " + t.getLinha() + ": comando de atribuição deve terminar com ';'.");
            }
        }
    }

    private boolean comandoTerminaCorretamente(List<Token> ts, int inicio, int linhaInicial) {
        for (int i = inicio; i < ts.size(); i++) {
            Token t = ts.get(i);

            if ("PONTO_E_VIRGULA".equals(t.getClasse())) return true;

            if (t.getLinha() > linhaInicial && iniciaComando(ts, i)) return false;

            if (lexemaEh(t, "else")
                    || lexemaEh(t, "until")
                    || lexemaEh(t, "end")) {
                return false;
            }
        }

        return false;
    }

    private boolean iniciaComando(List<Token> ts, int indice) {
        Token t = ts.get(indice);

        if (lexemaEh(t, "if")
                || lexemaEh(t, "while")
                || lexemaEh(t, "for")
                || lexemaEh(t, "repeat")
                || lexemaEh(t, "read")
                || lexemaEh(t, "readln")
                || lexemaEh(t, "write")
                || lexemaEh(t, "writeln")) {
            return true;
        }

        return "IDENTIFICADOR".equals(t.getClasse())
                && indice + 1 < ts.size()
                && "ATRIBUICAO".equals(ts.get(indice + 1).getClasse());
    }

    private boolean existeAntes(List<Token> ts, int inicio, Set<String> procurados, Set<String> limites) {
        for (int i = inicio; i < ts.size(); i++) {
            Token t = ts.get(i);

            if (procurados.contains(t.getLexema().toLowerCase())) return true;

            if (limites.contains(t.getClasse())
                    || lexemaEh(t, "begin")
                    || lexemaEh(t, "end")) {
                return false;
            }
        }

        return false;
    }

    private boolean tipoValido(Token t) {
        return lexemaEh(t, "integer")
                || lexemaEh(t, "real")
                || lexemaEh(t, "boolean")
                || lexemaEh(t, "char")
                || lexemaEh(t, "string");
    }

    private boolean lexemaEh(Token token, String esperado) {
        return token != null && token.getLexema().equalsIgnoreCase(esperado);
    }

    private void adicionarErro(String erro) {
        if (!erros.contains(erro)) {
            erros.add(erro);
        }
    }

    private void adicionarErroUnico(String erro) {
        adicionarErro(erro);
    }

    private void adicionarSimbolo(String lexema, String classe, String padrao, String descricao) {
        tokens.add(new Token(lexema, classe, padrao, descricao, linha));
        pos++;
    }

    private void adicionarSimboloDuplo(String lexema, String classe, String padrao, String descricao) {
        tokens.add(new Token(lexema, classe, padrao, descricao, linha));
        pos += 2;
    }

    private boolean proximoEh(char esperado) {
        return pos + 1 < codigo.length() && codigo.charAt(pos + 1) == esperado;
    }

    public List<String> getErros() {
        return new ArrayList<>(erros);
    }

    public static boolean isPalavraReservada(String palavra) {
        return palavra != null && PALAVRAS_RESERVADAS.contains(palavra.toLowerCase());
    }

    public static Set<String> getPalavrasReservadas() {
        return Collections.unmodifiableSet(PALAVRAS_RESERVADAS);
    }
}