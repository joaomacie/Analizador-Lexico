public class Token {
    private final String lexema;
    private final String classe;
    private final String padrao;
    private final String descricao;
    private final int linha;

    public Token(String lexema, String classe, int linha) {
        this(lexema, classe, "", "", linha);
    }

    public Token(String lexema, String classe, String padrao, int linha) {
        this(lexema, classe, padrao, "", linha);
    }

    public Token(String lexema, String classe, String padrao, String descricao, int linha) {
        this.lexema = lexema;
        this.classe = classe;
        this.padrao = padrao;
        this.descricao = descricao;
        this.linha = linha;
    }

    public String getLexema() { return lexema; }
    public String getClasse() { return classe; }
    public String getPadrao() { return padrao; }
    public String getDescricao() { return descricao; }
    public int getLinha() { return linha; }

    @Override
    public String toString() {
        return String.format("Token[%s, %s, linha=%d]", lexema, classe, linha);
    }
}