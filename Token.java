public class Token {
    private String lexema;
    private String classe;
    private int linha;

    public Token(String lexema, String classe, int linha) {
        this.lexema = lexema;
        this.classe = classe;
        this.linha = linha;
    }

    public String getLexema() {
        return lexema;
    }

    public String getClasse() {
        return classe;
    }

    public int getLinha() {


        return linha;
    }
}