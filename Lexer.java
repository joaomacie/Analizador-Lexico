import java.util.*;

public class Lexer {

    private final String codigo;
    private int pos = 0;
    private int linha = 1;

    private final List<Token> tokens = new ArrayList<>();
    private final List<String> erros = new ArrayList<>();

    private static final Set<String> PALAVRAS_RESERVADAS = Set.of(
            "program", "var", "begin", "end", "integer", "boolean", "char",
            "if", "then", "else", "while", "do", "read", "write",
            "array", "of", "div", "or", "and", "not", "true", "false",
            "function", "procedure"
    );

    public Lexer(String codigo) {
        this.codigo = codigo;
    }

    public List<Token> analisar() {
        while (pos < codigo.length()) {
            char atual = codigo.charAt(pos);

            if (Character.isWhitespace(atual)) {
                if (atual == '\n') linha++;
                pos++;
            }

            else if (Character.isLetter(atual)) {
                reconhecerIdentificadorOuReservada();
            }

            else if (Character.isDigit(atual)) {
                reconhecerNumero();
            }

            else if (atual == '\'') {
                reconhecerCharOuString();
            }

            else {
                reconhecerSimbolo();
            }
        }

        tokens.add(new Token("EOF", "FIM_ARQUIVO", linha));
        return tokens;
    }

    private void reconhecerIdentificadorOuReservada() {
        StringBuilder sb = new StringBuilder();

        while (pos < codigo.length()) {
            char c = codigo.charAt(pos);

            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
                pos++;
            } else {
                break;
            }
        }

        String lexema = sb.toString();

        if (PALAVRAS_RESERVADAS.contains(lexema.toLowerCase())) {
            tokens.add(new Token(lexema, "PALAVRA_RESERVADA", linha));
        } else {
            tokens.add(new Token(lexema, "IDENTIFICADOR", linha));
        }
    }

    private void reconhecerNumero() {
        StringBuilder sb = new StringBuilder();

        while (pos < codigo.length() && Character.isDigit(codigo.charAt(pos))) {
            sb.append(codigo.charAt(pos));
            pos++;
        }

        tokens.add(new Token(sb.toString(), "NUMERO_INTEIRO", linha));
    }

    private void reconhecerCharOuString() {
        StringBuilder sb = new StringBuilder();
        sb.append(codigo.charAt(pos));
        pos++;

        while (pos < codigo.length() && codigo.charAt(pos) != '\'') {
            if (codigo.charAt(pos) == '\n') {
                erros.add("Erro léxico na linha " + linha + ": constante de caractere não fechada.");
                linha++;
                return;
            }

            sb.append(codigo.charAt(pos));
            pos++;
        }

        if (pos < codigo.length() && codigo.charAt(pos) == '\'') {
            sb.append(codigo.charAt(pos));
            pos++;

            tokens.add(new Token(sb.toString(), "CONSTANTE_CARACTERE", linha));
        } else {
            erros.add("Erro léxico na linha " + linha + ": constante de caractere não fechada.");
        }
    }

    private void reconhecerSimbolo() {
        char c = codigo.charAt(pos);

        switch (c) {
            case '+':
                tokens.add(new Token("+", "OPERADOR_ADICAO", linha));
                pos++;
                break;

            case '-':
                tokens.add(new Token("-", "OPERADOR_SUBTRACAO", linha));
                pos++;
                break;

            case '*':
                tokens.add(new Token("*", "OPERADOR_MULTIPLICACAO", linha));
                pos++;
                break;

            case '=':
                tokens.add(new Token("=", "OPERADOR_RELACIONAL", linha));
                pos++;
                break;

            case '<':
                if (proximoEh('=')) {
                    tokens.add(new Token("<=", "OPERADOR_RELACIONAL", linha));
                    pos += 2;
                } else if (proximoEh('>')) {
                    tokens.add(new Token("<>", "OPERADOR_RELACIONAL", linha));
                    pos += 2;
                } else {
                    tokens.add(new Token("<", "OPERADOR_RELACIONAL", linha));
                    pos++;
                }
                break;

            case '>':
                if (proximoEh('=')) {
                    tokens.add(new Token(">=", "OPERADOR_RELACIONAL", linha));
                    pos += 2;
                } else {
                    tokens.add(new Token(">", "OPERADOR_RELACIONAL", linha));
                    pos++;
                }
                break;

            case ':':
                if (proximoEh('=')) {
                    tokens.add(new Token(":=", "ATRIBUICAO", linha));
                    pos += 2;
                } else {
                    tokens.add(new Token(":", "DOIS_PONTOS", linha));
                    pos++;
                }
                break;

            case '.':
                if (proximoEh('.')) {
                    tokens.add(new Token("..", "INTERVALO", linha));
                    pos += 2;
                } else {
                    tokens.add(new Token(".", "PONTO", linha));
                    pos++;
                }
                break;

            case ',':
                tokens.add(new Token(",", "VIRGULA", linha));
                pos++;
                break;

            case ';':
                tokens.add(new Token(";", "PONTO_E_VIRGULA", linha));
                pos++;
                break;

            case '(':
                tokens.add(new Token("(", "PARENTESE_ESQUERDO", linha));
                pos++;
                break;

            case ')':
                tokens.add(new Token(")", "PARENTESE_DIREITO", linha));
                  pos++;
                break;

            case '[':
                tokens.add(new Token("[", "COLCHETE_ESQUERDO", linha));
                pos++;
                break;

            case ']':
                tokens.add(new Token("]", "COLCHETE_DIREITO", linha));
                pos++;
                  break;

            default:
                erros.add("Erro léxico na linha " + linha + ": símbolo inválido '" + c + "'");
                pos++;
                break;
        }
    }

    private boolean proximoEh(char esperado) {
        return pos + 1 < codigo.length() && codigo.charAt(pos + 1) == esperado;
    }

    public List<String> getErros() {
        return erros;
    }
}
