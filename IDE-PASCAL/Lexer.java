import java.util.ArrayList;
import java.util.Collections;
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
        this.codigo = codigo;
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

        tokens.add(new Token("EOF", "FIM_ARQUIVO", "fim da entrada", "Marcador de fim do código", linha));
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

        if (pos < codigo.length() && codigo.charAt(pos) == '.'
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
                erros.add("Linha " + linhaInicial + ": constante literal não fechada.");
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
            String descricao = sb.length() == 3 ? "Constante de um caractere" : "Texto delimitado por aspas simples";
            tokens.add(new Token(sb.toString(), classe, "' caractere* '", descricao, linhaInicial));
        } else {
            erros.add("Linha " + linhaInicial + ": constante literal não fechada.");
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
            erros.add("Linha " + linhaInicial + ": comentário iniciado com { não foi fechado.");
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

        erros.add("Linha " + linhaInicial + ": comentário iniciado com (* não foi fechado.");
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
            case '+': adicionarSimbolo("+", "OPERADOR_ADICAO", "+", "Operador de adição"); break;
            case '-': adicionarSimbolo("-", "OPERADOR_SUBTRACAO", "-", "Operador de subtração"); break;
            case '*': adicionarSimbolo("*", "OPERADOR_MULTIPLICACAO", "*", "Operador de multiplicação"); break;
            case '/': adicionarSimbolo("/", "OPERADOR_DIVISAO", "/", "Operador de divisão real"); break;
            case '=': adicionarSimbolo("=", "OPERADOR_RELACIONAL", "=", "Comparação de igualdade"); break;
            case '<':
                if (proximoEh('=')) adicionarSimboloDuplo("<=", "OPERADOR_RELACIONAL", "<=", "Menor ou igual");
                else if (proximoEh('>')) adicionarSimboloDuplo("<>", "OPERADOR_RELACIONAL", "<>", "Diferente");
                else adicionarSimbolo("<", "OPERADOR_RELACIONAL", "<", "Menor que");
                break;
            case '>':
                if (proximoEh('=')) adicionarSimboloDuplo(">=", "OPERADOR_RELACIONAL", ">=", "Maior ou igual");
                else adicionarSimbolo(">", "OPERADOR_RELACIONAL", ">", "Maior que");
                break;
            case ':':
                if (proximoEh('=')) adicionarSimboloDuplo(":=", "ATRIBUICAO", ":=", "Atribuição de valor");
                else adicionarSimbolo(":", "DOIS_PONTOS", ":", "Separador de tipo");
                break;
            case '.':
                if (proximoEh('.')) adicionarSimboloDuplo("..", "INTERVALO", "..", "Intervalo");
                else adicionarSimbolo(".", "PONTO", ".", "Fim do programa ou acesso");
                break;
            case ',': adicionarSimbolo(",", "VIRGULA", ",", "Separador de itens"); break;
            case ';': adicionarSimbolo(";", "PONTO_E_VIRGULA", ";", "Fim de comando/declaração"); break;
            case '(': adicionarSimbolo("(", "PARENTESE_ESQUERDO", "(", "Abertura de parênteses"); break;
            case ')': adicionarSimbolo(")", "PARENTESE_DIREITO", ")", "Fecho de parênteses"); break;
            case '[': adicionarSimbolo("[", "COLCHETE_ESQUERDO", "[", "Abertura de colchete"); break;
            case ']': adicionarSimbolo("]", "COLCHETE_DIREITO", "]", "Fecho de colchete"); break;
            default:
                erros.add("Linha " + linha + ": símbolo inválido '" + c + "'.");
                pos++;
                break;
        }
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
        return PALAVRAS_RESERVADAS.contains(palavra.toLowerCase());
    }

    public static Set<String> getPalavrasReservadas() {
        return Collections.unmodifiableSet(PALAVRAS_RESERVADAS);
    }
}