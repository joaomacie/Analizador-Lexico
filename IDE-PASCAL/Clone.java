import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.undo.*;

public class Clone extends JFrame {

    // ─── Componentes ──────
    private JTextPane editorArea;
    private LineNumberPanel lineNumberPanel;
    private JLabel statusLabel;
    private JLabel posLabel;
    private JLabel fileLabel;
    private JLabel tokenCountLabel;
    private JLabel errorCountLabel;
    private JScrollPane editorScroll;
    private DefaultTableModel tokenTableModel;
    private DefaultTableModel errorTableModel;
    private JTable tokenTable;
    private JTable errorTable;
    private java.util.List<Token> ultimosTokens = new ArrayList<>();
    private java.util.List<String> ultimosErros = new ArrayList<>();

    // ─── Componentes de busca ──────
    private JTextField searchTokenField;
    private JTextField searchErrorField;
    private JButton aiCorrigirBtn;
    private JProgressBar progressBar;

    // ─── Estado ─────
    private File currentFile = null;
    private boolean modified = false;
    private UndoManager undoManager = new UndoManager();
    private Theme currentTheme;
    private boolean aplicandoDestaque = false;
    private String currentFontName = "Monospaced";
    private int currentFontSize = 14;

    // Configuração da LLM
    private static final String LLM_API_URL = "http://localhost:11434/api/generate";
    private static final String LLM_MODEL = "codellama:7b";

    // ─── PALETA DE CORES CENTRALIZADA ──────────────────────────────────────
    // Dark theme colors
    private static final Color D_BG_EDITOR   = new Color(22, 22, 34);
    private static final Color D_BG_LINENUM  = new Color(15, 15, 24);
    private static final Color D_BG_PANEL    = new Color(26, 26, 42);
    private static final Color D_BG_BAR      = new Color(18, 18, 30);
    private static final Color D_BG_TOOLBAR  = new Color(30, 30, 50);
    private static final Color D_FG_TEXT     = new Color(220, 220, 240);
    private static final Color D_FG_DIM      = new Color(80, 100, 140);
    private static final Color D_ACCENT      = new Color(99, 179, 237);
    private static final Color D_ACCENT2     = new Color(154, 117, 255);
    private static final Color D_SELECTION   = new Color(99, 179, 237, 55);
    private static final Color D_BORDER      = new Color(45, 45, 75);
    private static final Color D_KW          = new Color(99, 179, 237);
    private static final Color D_NUM         = new Color(230, 200, 100);
    private static final Color D_STR         = new Color(150, 220, 150);
    private static final Color D_CMT         = new Color(130, 130, 170);

    // ─── NOVAS CORES PARA TEMAS ADICIONAIS ─────────────────────────────────
    // Dracula Theme
    private static final Color DRACULA_BG      = new Color(40, 42, 54);
    private static final Color DRACULA_CURRENT = new Color(68, 71, 90);
    private static final Color DRACULA_SELECT  = new Color(68, 71, 90);
    private static final Color DRACULA_FG      = new Color(248, 248, 242);
    private static final Color DRACULA_COMMENT = new Color(98, 114, 164);
    private static final Color DRACULA_CYAN    = new Color(139, 233, 253);
    private static final Color DRACULA_GREEN   = new Color(80, 250, 123);
    private static final Color DRACULA_ORANGE  = new Color(255, 184, 108);
    private static final Color DRACULA_PINK    = new Color(255, 121, 198);
    private static final Color DRACULA_PURPLE  = new Color(189, 147, 249);
    private static final Color DRACULA_YELLOW  = new Color(241, 250, 140);

    // Nord Theme
    private static final Color NORD_BG         = new Color(46, 52, 64);
    private static final Color NORD_PANEL      = new Color(59, 66, 82);
    private static final Color NORD_FG         = new Color(216, 222, 233);
    private static final Color NORD_COMMENT    = new Color(94, 103, 116);
    private static final Color NORD_BLUE       = new Color(129, 161, 193);
    private static final Color NORD_AURORA_GREEN = new Color(163, 190, 140);
    private static final Color NORD_AURORA_RED = new Color(191, 97, 106);
    private static final Color NORD_AURORA_YELLOW = new Color(235, 203, 139);
    private static final Color NORD_AURORA_PURPLE = new Color(180, 142, 173);

    // GitHub Dark Theme
    private static final Color GHD_BG          = new Color(13, 17, 23);
    private static final Color GHD_PANEL       = new Color(22, 27, 34);
    private static final Color GHD_FG          = new Color(201, 209, 217);
    private static final Color GHD_BLUE        = new Color(88, 166, 255);
    private static final Color GHD_GREEN       = new Color(63, 185, 80);
    private static final Color GHD_ORANGE      = new Color(255, 123, 75);
    private static final Color GHD_PURPLE      = new Color(188, 140, 255);
    private static final Color GHD_CYAN        = new Color(86, 199, 214);

    // Catppuccin Mocha
    private static final Color CAT_BG          = new Color(30, 30, 46);
    private static final Color CAT_SURFACE     = new Color(49, 50, 68);
    private static final Color CAT_FG          = new Color(205, 214, 244);
    private static final Color CAT_ROSEWATER   = new Color(245, 224, 220);
    private static final Color CAT_FLAMINGO    = new Color(242, 205, 205);
    private static final Color CAT_PINK        = new Color(245, 194, 231);
    private static final Color CAT_MAUVE       = new Color(203, 166, 247);
    private static final Color CAT_RED         = new Color(243, 139, 168);
    private static final Color CAT_MAROON      = new Color(235, 160, 172);
    private static final Color CAT_PEACH       = new Color(250, 179, 135);
    private static final Color CAT_YELLOW      = new Color(249, 226, 175);
    private static final Color CAT_GREEN       = new Color(166, 218, 149);
    private static final Color CAT_TEAL        = new Color(148, 226, 213);
    private static final Color CAT_BLUE        = new Color(137, 180, 250);

    // Gruvbox Dark
    private static final Color GRUV_BG         = new Color(40, 40, 40);
    private static final Color GRUV_RED        = new Color(204, 102, 102);
    private static final Color GRUV_GREEN      = new Color(153, 204, 102);
    private static final Color GRUV_YELLOW     = new Color(255, 204, 102);
    private static final Color GRUV_BLUE       = new Color(102, 153, 204);
    private static final Color GRUV_PURPLE     = new Color(181, 137, 0);
    private static final Color GRUV_AQUA       = new Color(102, 204, 153);
    private static final Color GRUV_ORANGE     = new Color(255, 136, 102);
    private static final Color GRUV_FG         = new Color(235, 219, 178);
    private static final Color GRUV_COMMENT    = new Color(124, 111, 100);

    // Tokyo Night
    private static final Color TN_BG           = new Color(26, 27, 38);
    private static final Color TN_PANEL        = new Color(31, 33, 46);
    private static final Color TN_FG           = new Color(169, 177, 214);
    private static final Color TN_CYAN         = new Color(61, 199, 211);
    private static final Color TN_BLUE         = new Color(122, 162, 247);
    private static final Color TN_PURPLE       = new Color(187, 154, 247);
    private static final Color TN_GREEN        = new Color(158, 206, 106);
    private static final Color TN_ORANGE       = new Color(255, 158, 100);
    private static final Color TN_RED          = new Color(247, 118, 142);

    // ─── TEMAS ─────────────────────────────────────────────────────────────
    static class Theme {
        final String name;
        final Color bgEditor, bgLineNum, bgPanel, bgBar, bgToolbar;
        final Color fgText, fgDim, fgAccent, fgAccent2;
        final Color selection, caret, border;
        final Color keywordColor, numberColor, stringColor, commentColor;

        Theme(String name,
              Color bgEditor, Color bgLineNum, Color bgPanel, Color bgBar, Color bgToolbar,
              Color fgText, Color fgDim, Color fgAccent, Color fgAccent2,
              Color selection, Color caret, Color border,
              Color keywordColor, Color numberColor, Color stringColor, Color commentColor) {
            this.name = name;
            this.bgEditor = bgEditor; this.bgLineNum = bgLineNum;
            this.bgPanel = bgPanel; this.bgBar = bgBar; this.bgToolbar = bgToolbar;
            this.fgText = fgText; this.fgDim = fgDim;
            this.fgAccent = fgAccent; this.fgAccent2 = fgAccent2;
            this.selection = selection; this.caret = caret; this.border = border;
            this.keywordColor = keywordColor; this.numberColor = numberColor;
            this.stringColor = stringColor; this.commentColor = commentColor;
        }

    }

    private static final Map<String, Theme> THEMES = new LinkedHashMap<>();
    static {
        THEMES.put("Dark", new Theme("Dark",
                D_BG_EDITOR, D_BG_LINENUM, D_BG_PANEL, D_BG_BAR, D_BG_TOOLBAR,
                D_FG_TEXT, D_FG_DIM, D_ACCENT, D_ACCENT2,
                D_SELECTION, D_ACCENT, D_BORDER,
                D_KW, D_NUM, D_STR, D_CMT));

        THEMES.put("Light", new Theme("Light",
                new Color(252, 252, 255), new Color(238, 238, 248), new Color(245, 245, 255),
                new Color(235, 235, 248), new Color(240, 240, 252),
                new Color(30, 30, 50), new Color(140, 140, 180),
                new Color(55, 120, 200), new Color(130, 60, 200),
                new Color(55, 120, 200, 50), new Color(55, 120, 200), new Color(200, 200, 220),
                new Color(55, 120, 200), new Color(200, 100, 50), new Color(50, 150, 50), new Color(100, 100, 150)));

        THEMES.put("Monokai", new Theme("Monokai",
                new Color(39, 40, 34), new Color(30, 31, 26), new Color(45, 46, 40),
                new Color(35, 36, 30), new Color(50, 51, 44),
                new Color(248, 248, 242), new Color(117, 113, 94),
                new Color(166, 226, 46), new Color(253, 151, 31),
                new Color(73, 72, 62), new Color(166, 226, 46), new Color(73, 72, 62),
                new Color(166, 226, 46), new Color(230, 219, 116), new Color(230, 219, 116), new Color(117, 113, 94)));

        THEMES.put("Solarized", new Theme("Solarized",
                new Color(0, 43, 54), new Color(7, 54, 66), new Color(0, 43, 54),
                new Color(7, 54, 66), new Color(7, 54, 66),
                new Color(131, 148, 150), new Color(88, 110, 117),
                new Color(38, 139, 210), new Color(211, 54, 130),
                new Color(38, 139, 210, 60), new Color(38, 139, 210), new Color(7, 54, 66),
                new Color(38, 139, 210), new Color(133, 153, 0), new Color(42, 161, 152), new Color(88, 110, 117)));

        THEMES.put("Midnight", new Theme("Midnight",
                new Color(5, 5, 15), new Color(3, 3, 10), new Color(8, 8, 20),
                new Color(5, 5, 15), new Color(10, 10, 25),
                new Color(180, 200, 255), new Color(60, 70, 120),
                new Color(100, 160, 255), new Color(200, 100, 255),
                new Color(80, 120, 255, 55), new Color(100, 160, 255), new Color(20, 20, 50),
                new Color(100, 160, 255), new Color(255, 200, 100), new Color(150, 255, 150), new Color(80, 80, 140)));

        // ─── NOVOS TEMAS ─────────────────────────────────────────────────────

        // Dracula Theme
        THEMES.put("Dracula", new Theme("Dracula",
                DRACULA_BG, DRACULA_CURRENT, DRACULA_BG, DRACULA_CURRENT, DRACULA_CURRENT,
                DRACULA_FG, DRACULA_COMMENT, DRACULA_CYAN, DRACULA_PURPLE,
                DRACULA_SELECT, DRACULA_CYAN, DRACULA_CURRENT,
                DRACULA_CYAN, DRACULA_ORANGE, DRACULA_GREEN, DRACULA_COMMENT));

        // Nord Theme
        THEMES.put("Nord", new Theme("Nord",
                NORD_BG, NORD_PANEL, NORD_BG, NORD_PANEL, NORD_PANEL,
                NORD_FG, NORD_COMMENT, NORD_BLUE, NORD_AURORA_PURPLE,
                new Color(129, 161, 193, 55), NORD_BLUE, NORD_PANEL,
                NORD_BLUE, NORD_AURORA_YELLOW, NORD_AURORA_GREEN, NORD_COMMENT));

        // GitHub Dark
        THEMES.put("GitHub Dark", new Theme("GitHub Dark",
                GHD_BG, GHD_PANEL, GHD_BG, GHD_PANEL, GHD_PANEL,
                GHD_FG, new Color(139, 148, 158), GHD_BLUE, GHD_PURPLE,
                new Color(88, 166, 255, 40), GHD_BLUE, GHD_PANEL,
                GHD_BLUE, GHD_ORANGE, GHD_GREEN, new Color(139, 148, 158)));

        // Catppuccin Mocha
        THEMES.put("Catppuccin", new Theme("Catppuccin",
                CAT_BG, CAT_SURFACE, CAT_BG, CAT_SURFACE, CAT_SURFACE,
                CAT_FG, new Color(147, 153, 178), CAT_BLUE, CAT_MAUVE,
                new Color(137, 180, 250, 50), CAT_BLUE, CAT_SURFACE,
                CAT_BLUE, CAT_YELLOW, CAT_GREEN, new Color(147, 153, 178)));

        // Gruvbox Dark
        THEMES.put("Gruvbox", new Theme("Gruvbox",
                GRUV_BG, new Color(50, 48, 47), GRUV_BG, new Color(60, 56, 54), new Color(60, 56, 54),
                GRUV_FG, GRUV_COMMENT, GRUV_BLUE, GRUV_PURPLE,
                new Color(189, 174, 147, 50), GRUV_YELLOW, new Color(60, 56, 54),
                GRUV_BLUE, GRUV_YELLOW, GRUV_GREEN, GRUV_COMMENT));

        // Tokyo Night
        THEMES.put("Tokyo Night", new Theme("Tokyo Night",
                TN_BG, TN_PANEL, TN_BG, TN_PANEL, TN_PANEL,
                TN_FG, new Color(127, 135, 176), TN_BLUE, TN_PURPLE,
                new Color(122, 162, 247, 50), TN_BLUE, TN_PANEL,
                TN_BLUE, TN_ORANGE, TN_GREEN, new Color(127, 135, 176)));
    }

    private static final String[] MONO_FONTS = {
            "Consolas", "JetBrains Mono", "Fira Code", "Source Code Pro",
            "Courier New", "Lucida Console", "Monospaced", "DejaVu Sans Mono"
    };

    // ─── FONTS ─────────────────────────────────────────────────────────────
    private static final Font UI_FONT       = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font UI_FONT_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font UI_FONT_SM    = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font MENU_FONT     = new Font("Segoe UI", Font.PLAIN, 14);

    // ─── COLORS PRINCIPAIS DA UI ────────────────────────────────────────────
    private static final Color GREEN_RUN    = new Color(59, 109, 17);
    private static final Color GREEN_RUN_HV = new Color(39, 80, 10);
    private static final Color PURPLE_AI    = new Color(83, 74, 183);
    private static final Color PURPLE_AI_HV = new Color(60, 52, 140);
    private static final Color GREEN_BADGE  = new Color(59, 109, 17);
    private static final Color GREEN_BG     = new Color(234, 243, 222);
    private static final Color RED_BADGE    = new Color(163, 45, 45);
    private static final Color RED_BG       = new Color(252, 235, 235);
    private static final Color AMBER_DOT    = new Color(239, 159, 39);

    // ─── Construtor ──────
    public Clone() {
        super("IDE— Analisador Léxico Pascal");
        currentTheme = THEMES.get("Dark");

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { acaoSair(); }
        });

        setSize(1280, 820);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);

        buildUI();
        applyTheme(currentTheme);
        updateTitle();
        setVisible(true);
    }

    // ─── UI PRINCIPAL ──────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        setJMenuBar(buildMenuBar());
        add(buildTitleBar(),  BorderLayout.NORTH);
        add(buildWorkspace(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ─── TITLEBAR (logo + nome do arquivo + seletor de tema) ──────────────
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setPreferredSize(new Dimension(0, 44));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, currentTheme.border));

        // Esquerda: logo KSS
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        JLabel logo = new JLabel("IDE");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logo.setOpaque(true);
        logo.setBackground(new Color(59, 109, 17));
        logo.setForeground(new Color(192, 221, 151));
        logo.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        JLabel sub = new JLabel("Analisador Léxico");
        sub.setFont(UI_FONT);
        left.add(logo);
        left.add(sub);

        // Centro: chip do arquivo atual
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        center.setOpaque(false);
        fileLabel = new JLabel("Sem título");
        fileLabel.setFont(UI_FONT);
        fileLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        center.add(fileLabel);

        // Direita: combo de tema
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        JLabel themeLabel = new JLabel("Tema:");
        themeLabel.setFont(UI_FONT);
        JComboBox<String> themeCombo = new JComboBox<>(THEMES.keySet().toArray(new String[0]));
        themeCombo.setFont(UI_FONT);
        themeCombo.setPreferredSize(new Dimension(110, 28));
        themeCombo.addActionListener(e -> {
            String sel = (String) themeCombo.getSelectedItem();
            if (sel != null) applyTheme(THEMES.get(sel));
        });
        right.add(themeLabel);
        right.add(themeCombo);

        bar.add(left,   BorderLayout.WEST);
        bar.add(center, BorderLayout.CENTER);
        bar.add(right,  BorderLayout.EAST);
        return bar;
    }

    // ─── MENU ──────────────────────────────────────────────────────────────
    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu mArquivo = menu("Arquivo");
        mArquivo.add(menuItem("Novo",         "ctrl N",       e -> acaoNovo()));
        mArquivo.add(menuItem("Abrir...",      "ctrl O",       e -> acaoAbrir()));
        mArquivo.addSeparator();
        mArquivo.add(menuItem("Salvar",        "ctrl S",       e -> acaoSalvar()));
        mArquivo.add(menuItem("Salvar Como...", "ctrl shift S", e -> acaoSalvarComo()));
        mArquivo.addSeparator();
        mArquivo.add(menuItem("Sair",          "alt F4",       e -> acaoSair()));

        JMenu mEditar = menu("Editar");
        mEditar.add(menuItem("Desfazer",       "ctrl Z", e -> { if (undoManager.canUndo()) undoManager.undo(); }));
        mEditar.add(menuItem("Refazer",        "ctrl Y", e -> { if (undoManager.canRedo()) undoManager.redo(); }));
        mEditar.addSeparator();
        mEditar.add(menuItem("Copiar",         "ctrl C", e -> editorArea.copy()));
        mEditar.add(menuItem("Recortar",       "ctrl X", e -> editorArea.cut()));
        mEditar.add(menuItem("Colar",          "ctrl V", e -> editorArea.paste()));
        mEditar.addSeparator();
        mEditar.add(menuItem("Selecionar Tudo","ctrl A", e -> editorArea.selectAll()));

        JMenu mVisualizar = menu("Visualizar");
        JMenu mFonte = new JMenu("Fonte");
        styleMenu(mFonte);
        ButtonGroup bgFonte = new ButtonGroup();
        for (String fontName : MONO_FONTS) {
            Font testFont = new Font(fontName, Font.PLAIN, 12);
            if (!testFont.getFamily().equals("Dialog") || fontName.equals("Monospaced")) {
                JRadioButtonMenuItem rb = new JRadioButtonMenuItem(fontName, fontName.equals("Monospaced"));
                rb.setFont(new Font(fontName, Font.PLAIN, 13));
                bgFonte.add(rb);
                rb.addActionListener(e -> { currentFontName = fontName; updateEditorFont(); });
                mFonte.add(rb);
            }
        }
        mVisualizar.add(mFonte);
        mVisualizar.addSeparator();
        mVisualizar.add(menuItem("Aumentar Fonte (+)", "ctrl EQUALS", e -> changeFontSize(2)));
        mVisualizar.add(menuItem("Diminuir Fonte (-)",  "ctrl MINUS",  e -> changeFontSize(-2)));
        mVisualizar.add(menuItem("Tamanho Personalizado...", null,     e -> dialogFontSize()));
        mVisualizar.add(menuItem("Repor Fonte Padrão",  "ctrl 0",
                e -> { currentFontSize = 14; currentFontName = "Monospaced"; updateEditorFont(); }));

        mb.add(mArquivo);
        mb.add(mEditar);
        mb.add(mVisualizar);
        return mb;
    }

    private JMenu menu(String title) {
        JMenu m = new JMenu(title);
        styleMenu(m);
        return m;
    }
    private void styleMenu(JMenu m)       { m.setFont(MENU_FONT); }
    private void styleMenuItem(JMenuItem mi) { mi.setFont(MENU_FONT); }

    private JMenuItem menuItem(String label, String accel, ActionListener al) {
        JMenuItem mi = new JMenuItem(label);
        mi.setFont(MENU_FONT);
        if (accel != null) mi.setAccelerator(KeyStroke.getKeyStroke(accel));
        mi.addActionListener(al);
        return mi;
    }

    // ─── WORKSPACE: sidebar + editor + painel de resultados ───────────────
    private JPanel buildWorkspace() {
        JPanel ws = new JPanel(new BorderLayout(0, 0));
        ws.add(buildSidebar(),       BorderLayout.WEST);
        ws.add(buildEditorSection(), BorderLayout.CENTER);
        ws.add(buildResultsSection(),BorderLayout.EAST);
        return ws;
    }

    // ─── SIDEBAR ───────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sb = new JPanel();
        sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
        sb.setPreferredSize(new Dimension(44, 0));
        sb.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, currentTheme.border));

        sb.add(Box.createVerticalStrut(8));
        sb.add(sidebarBtn("N", "Novo arquivo (Ctrl+N)",     e -> acaoNovo()));
        sb.add(sidebarBtn("A", "Abrir arquivo (Ctrl+O)",    e -> acaoAbrir()));
        sb.add(sidebarBtn("S", "Salvar (Ctrl+S)",           e -> acaoSalvar()));
        sb.add(Box.createVerticalStrut(6));
        sb.add(sidebarSep());
        sb.add(Box.createVerticalStrut(6));
        sb.add(sidebarBtn("Z", "Desfazer (Ctrl+Z)",  e -> { if (undoManager.canUndo()) undoManager.undo(); }));
        sb.add(sidebarBtn("Y", "Refazer (Ctrl+Y)",   e -> { if (undoManager.canRedo()) undoManager.redo(); }));
        sb.add(Box.createVerticalStrut(6));
        sb.add(sidebarSep());
        sb.add(Box.createVerticalStrut(6));
        sb.add(sidebarBtn("+", "Aumentar fonte",     e -> changeFontSize(2)));
        sb.add(sidebarBtn("-", "Diminuir fonte",     e -> changeFontSize(-2)));
        sb.add(Box.createVerticalGlue());
        sb.add(sidebarBtn("?", "Exemplo Pascal",     e -> acaoCarregarExemplo()));
        sb.add(sidebarBtn("E", "Exportar resultados",e -> acaoExportarResultado()));
        sb.add(Box.createVerticalStrut(8));

        return sb;
    }

    private JButton sidebarBtn(String text, String tooltip, ActionListener al) {
        JButton b = new JButton(text);
        b.setToolTipText(tooltip);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setMaximumSize(new Dimension(44, 32));
        b.setPreferredSize(new Dimension(44, 32));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        return b;
    }

    private JSeparator sidebarSep() {
        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        sep.setMaximumSize(new Dimension(32, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        return sep;
    }

    // ─── EDITOR ────────────────────────────────────────────────────────────
    private JPanel buildEditorSection() {
        JPanel section = new JPanel(new BorderLayout(0, 0));

        // Aba do arquivo
        JPanel tabBar = buildEditorTabBar();

        // Toolbar compacta de ações principais
        JPanel toolbar = buildCompactToolbar();

        JPanel topArea = new JPanel(new BorderLayout(0, 0));
        topArea.add(tabBar,  BorderLayout.NORTH);
        topArea.add(toolbar, BorderLayout.SOUTH);

        section.add(topArea,        BorderLayout.NORTH);
        section.add(buildEditorPane(), BorderLayout.CENTER);
        return section;
    }

    private JPanel buildEditorTabBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setPreferredSize(new Dimension(0, 36));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, currentTheme.border));

        JPanel tab = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        tab.setOpaque(false);
        JLabel dot = new JLabel("●●");
        dot.setForeground(AMBER_DOT);
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        JLabel name = new JLabel("Menu da IDE");
        name.setFont(UI_FONT);
        tab.add(dot);
        tab.add(name);
        bar.add(tab);

        return bar;
    }

    private JPanel buildCompactToolbar() {
        JPanel tb = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 6));
        tb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, currentTheme.border));

        // Grupo arquivo
        tb.add(tbBtn("Novo",   "Ctrl+N",  e -> acaoNovo()));
        tb.add(tbBtn("Abrir",  "Ctrl+O",  e -> acaoAbrir()));
        tb.add(tbBtn("Salvar", "Ctrl+S",  e -> acaoSalvar()));
        tb.add(tbDiv());

        // Grupo editar
        tb.add(tbBtn("Copiar",    "Ctrl+C", e -> editorArea.copy()));
        tb.add(tbBtn("Recortar",  "Ctrl+X", e -> editorArea.cut()));
        tb.add(tbBtn("Colar",     "Ctrl+V", e -> editorArea.paste()));
        tb.add(tbDiv());

        // Grupo utilitários
        tb.add(tbBtn("Exemplo",  "Carrega código exemplo", e -> acaoCarregarExemplo()));
        tb.add(tbBtn("Exportar", "Exporta para CSV",       e -> acaoExportarResultado()));
        tb.add(tbDiv());

        // Botão principal: EXECUTAR
        JButton btnRun = new JButton("  Executar análise  (F5)");
        btnRun.setFont(UI_FONT_BOLD);
        btnRun.setForeground(new Color(234, 243, 222));
        btnRun.setBackground(GREEN_RUN);
        btnRun.setOpaque(true);
        btnRun.setBorderPainted(false);
        btnRun.setFocusPainted(false);
        btnRun.setMargin(new Insets(6, 14, 6, 14));
        btnRun.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRun.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btnRun.setBackground(GREEN_RUN_HV); }
            @Override public void mouseExited (MouseEvent e) { btnRun.setBackground(GREEN_RUN); }
        });
        btnRun.addActionListener(e -> acaoAnalisar());
        tb.add(btnRun);

        // F5 global
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "analisar");
        getRootPane().getActionMap().put("analisar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { acaoAnalisar(); }
        });

        // Botão IA
        aiCorrigirBtn = new JButton("  Corrigir com IA");
        aiCorrigirBtn.setFont(UI_FONT_BOLD);
        aiCorrigirBtn.setForeground(new Color(220, 210, 255));
        aiCorrigirBtn.setBackground(PURPLE_AI);
        aiCorrigirBtn.setOpaque(true);
        aiCorrigirBtn.setBorderPainted(false);
        aiCorrigirBtn.setFocusPainted(false);
        aiCorrigirBtn.setMargin(new Insets(6, 14, 6, 14));
        aiCorrigirBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        aiCorrigirBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { aiCorrigirBtn.setBackground(PURPLE_AI_HV); }
            @Override public void mouseExited (MouseEvent e) { aiCorrigirBtn.setBackground(PURPLE_AI); }
        });
        aiCorrigirBtn.addActionListener(e -> sugerirCorrecaoComIA());
        tb.add(aiCorrigirBtn);

        // ProgressBar
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(120, 18));
        progressBar.setVisible(false);
        tb.add(progressBar);

        return tb;
    }

    private JButton tbBtn(String text, String tooltip, ActionListener al) {
        JButton b = new JButton(text);
        b.setToolTipText(tooltip);
        b.setFont(UI_FONT);
        b.setMargin(new Insets(4, 10, 4, 10));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        return b;
    }

    private JSeparator tbDiv() {
        JSeparator s = new JSeparator(JSeparator.VERTICAL);
        s.setPreferredSize(new Dimension(6, 28));
        return s;
    }

    private JPanel buildEditorPane() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));

        editorArea = new JTextPane();
        editorArea.getDocument().putProperty(PlainDocument.tabSizeAttribute, 4);
        editorArea.setMargin(new Insets(10, 14, 10, 10));
        editorArea.setFont(resolveFont(currentFontName, currentFontSize));

        editorArea.getDocument().addUndoableEditListener(e -> {
            if (!aplicandoDestaque) undoManager.addEdit(e.getEdit());
        });
        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { if (!aplicandoDestaque) onChange(); }
            @Override public void removeUpdate(DocumentEvent e)  { if (!aplicandoDestaque) onChange(); }
            @Override public void changedUpdate(DocumentEvent e) { if (!aplicandoDestaque) onChange(); }
        });
        editorArea.addCaretListener(e -> updatePosition());

        lineNumberPanel = new LineNumberPanel(editorArea);

        editorScroll = new JScrollPane(editorArea);
        editorScroll.setRowHeaderView(lineNumberPanel);
        editorScroll.setBorder(BorderFactory.createEmptyBorder());
        editorScroll.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(editorScroll, BorderLayout.CENTER);
        return panel;
    }

    // ─── PAINEL DE RESULTADOS (lado direito) ────────────────────────────────
    private JPanel buildResultsSection() {
        JPanel section = new JPanel(new BorderLayout(0, 0));
        section.setPreferredSize(new Dimension(340, 0));
        section.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, currentTheme.border));

        // Abas tokens / erros
        JTabbedPane tabs = buildResultsTabs();
        section.add(tabs, BorderLayout.CENTER);

        // Rodapé com contadores
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 5));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, currentTheme.border));

        tokenCountLabel = new JLabel("0 tokens");
        tokenCountLabel.setFont(UI_FONT_SM);
        errorCountLabel = new JLabel("0 erros");
        errorCountLabel.setFont(UI_FONT_SM);

        footer.add(tokenCountLabel);
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 14));
        footer.add(sep);
        footer.add(errorCountLabel);
        section.add(footer, BorderLayout.SOUTH);

        return section;
    }

    private JTabbedPane buildResultsTabs() {
        // ── TOKEN TAB ──
        JPanel tokenSearchBar = new JPanel(new BorderLayout(6, 0));
        tokenSearchBar.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        searchTokenField = new JTextField();
        searchTokenField.setFont(UI_FONT);
        searchTokenField.setToolTipText("Filtrar tokens por lexema ou classe");
        searchTokenField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrarTokens(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrarTokens(); }
            @Override public void changedUpdate(DocumentEvent e){ filtrarTokens(); }
        });
        JLabel searchIcon = new JLabel("Pesquisa");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        tokenSearchBar.add(searchIcon, BorderLayout.WEST);
        tokenSearchBar.add(searchTokenField, BorderLayout.CENTER);

        tokenTableModel = new DefaultTableModel(new Object[]{"Lexema", "Classe", "Ln"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tokenTable = buildStyledTable(tokenTableModel);
        tokenTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        tokenTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        tokenTable.getColumnModel().getColumn(2).setPreferredWidth(35);
        tokenTable.getColumnModel().getColumn(2).setMaxWidth(40);

        // Renderer colorido para coluna Classe
        tokenTable.getColumnModel().getColumn(1).setCellRenderer(new ClassePillRenderer(currentTheme));

        tokenTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tokenTable.getSelectedRow();
                if (row != -1) {
                    Object val = tokenTable.getValueAt(row, 2);
                    if (val instanceof Integer) irParaLinha((Integer) val);
                }
            }
        });

        JPanel tokenPanel = new JPanel(new BorderLayout(0, 0));
        tokenPanel.add(tokenSearchBar, BorderLayout.NORTH);
        tokenPanel.add(new JScrollPane(tokenTable), BorderLayout.CENTER);

        // ── ERROR TAB ──
        JPanel errorSearchBar = new JPanel(new BorderLayout(6, 0));
        errorSearchBar.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        searchErrorField = new JTextField();
        searchErrorField.setFont(UI_FONT);
        searchErrorField.setToolTipText("Filtrar erros");
        searchErrorField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrarErros(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrarErros(); }
            @Override public void changedUpdate(DocumentEvent e){ filtrarErros(); }
        });
        JLabel errIcon = new JLabel("pesquisa");
        errIcon.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        errorSearchBar.add(errIcon, BorderLayout.WEST);
        errorSearchBar.add(searchErrorField, BorderLayout.CENTER);

        errorTableModel = new DefaultTableModel(new Object[]{"Erros léxicos"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        errorTable = buildStyledTable(errorTableModel);
        errorTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = errorTable.getSelectedRow();
                if (row != -1) extrairEIrParaLinhaDoErro((String) errorTable.getValueAt(row, 0));
            }
        });

        JPanel errorPanel = new JPanel(new BorderLayout(0, 0));
        errorPanel.add(errorSearchBar, BorderLayout.NORTH);
        errorPanel.add(new JScrollPane(errorTable), BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(UI_FONT_BOLD);
        tabs.addTab("Tokens", tokenPanel);
        tabs.addTab("Erros",  errorPanel);
        return tabs;
    }

    /** Renderer que pinta a classe do token com pílula colorida */
    static class ClassePillRenderer extends DefaultTableCellRenderer {
        private final Theme theme;
        ClassePillRenderer(Theme t) { this.theme = t; }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            String v = value == null ? "" : value.toString();
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

            if (!isSelected) {
                if (v.startsWith("PALAVRA_RESERVADA")) {
                    setBackground(new Color(238, 237, 254));
                    setForeground(new Color(83, 74, 183));
                } else if (v.startsWith("NUMERO")) {
                    setBackground(new Color(250, 238, 218));
                    setForeground(new Color(133, 79, 11));
                } else if (v.startsWith("STRING") || v.startsWith("CONSTANTE_CARACTERE")) {
                    setBackground(new Color(234, 243, 222));
                    setForeground(new Color(59, 109, 17));
                } else if (v.startsWith("COMENTARIO")) {
                    setBackground(new Color(238, 238, 248));
                    setForeground(new Color(100, 100, 160));
                } else if (v.startsWith("IDENTIFICADOR")) {
                    setBackground(new Color(240, 240, 245));
                    setForeground(new Color(80, 80, 120));
                } else {
                    setBackground(new Color(225, 245, 238));
                    setForeground(new Color(15, 110, 86));
                }
            }
            return this;
        }
    }

    private JTable buildStyledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setAutoCreateRowSorter(true);
        t.setFillsViewportHeight(true);
        t.setRowHeight(26);
        t.setFont(new Font("Monospaced", Font.PLAIN, 12));
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setFont(UI_FONT_BOLD);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    // ─── LINE NUMBER PANEL ─────────────────────────────────────────────────
    class LineNumberPanel extends JPanel {
        private final JTextPane target;
        private static final int PADDING = 10;

        LineNumberPanel(JTextPane target) {
            this.target = target;
            setPreferredSize(new Dimension(52, 0));
            target.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e)  { repaint(); updateWidth(); }
                @Override public void removeUpdate(DocumentEvent e)  { repaint(); updateWidth(); }
                @Override public void changedUpdate(DocumentEvent e) { repaint(); }
            });
        }

        void updateWidth() {
            int digits = String.valueOf(getLineCount()).length();
            int w = PADDING * 2 + getFontMetrics(target.getFont()).stringWidth("0".repeat(Math.max(digits, 4)));
            setPreferredSize(new Dimension(w, 0));
            revalidate();
        }

        private int getLineCount() { return target.getDocument().getDefaultRootElement().getElementCount(); }
        private int getLineOfOffset(int offset) { return target.getDocument().getDefaultRootElement().getElementIndex(offset); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(target.getFont());
            FontMetrics fm = g2.getFontMetrics();
            int lineH = target.getFont().getSize() + 5;

            Rectangle viewRect = editorScroll.getViewport().getViewRect();
            int firstLine = Math.max(0, viewRect.y / lineH);
            int lastLine  = Math.min(getLineCount() - 1, (viewRect.y + viewRect.height) / lineH + 1);
            int startY    = (firstLine * lineH) - viewRect.y + fm.getAscent() + target.getMargin().top + 8;
            int panelW    = getWidth();
            int caretLine = getLineOfOffset(target.getCaretPosition());

            for (int i = firstLine; i <= lastLine; i++) {
                String num = String.valueOf(i + 1);
                if (i == caretLine) {
                    g2.setColor(currentTheme.fgAccent);
                    g2.setFont(target.getFont().deriveFont(Font.BOLD));
                } else {
                    g2.setColor(currentTheme.fgDim);
                    g2.setFont(target.getFont());
                }
                g2.drawString(num, panelW - fm.stringWidth(num) - PADDING, startY + (i - firstLine) * lineH);
            }
        }
    }

    // ─── STATUS BAR ────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setPreferredSize(new Dimension(0, 28));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, currentTheme.border));

        statusLabel = new JLabel("  Pronto");
        statusLabel.setFont(UI_FONT_SM);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4));
        right.setOpaque(false);
        posLabel = new JLabel("Ln 1, Col 1");
        posLabel.setFont(UI_FONT_SM);
        JLabel lang = new JLabel("Pascal");
        lang.setFont(UI_FONT_SM);
        lang.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(currentTheme.border, 1),
                BorderFactory.createEmptyBorder(1, 6, 1, 6)));
        right.add(posLabel);
        right.add(new JSeparator(JSeparator.VERTICAL) {{ setPreferredSize(new Dimension(1, 14)); }});
        right.add(lang);

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(right,       BorderLayout.EAST);
        return bar;
    }

    // ─── BUSCA / FILTRO ────────────────────────────────────────────────────
    private void filtrarTokens() {
        String termo = searchTokenField.getText().toLowerCase().trim();
        tokenTableModel.setRowCount(0);
        for (Token tk : ultimosTokens) {
            if (termo.isEmpty()
                    || tk.getLexema().toLowerCase().contains(termo)
                    || tk.getClasse().toLowerCase().contains(termo)) {
                tokenTableModel.addRow(new Object[]{ tk.getLexema(), tk.getClasse(), tk.getLinha() });
            }
        }
    }

    private void filtrarErros() {
        String termo = searchErrorField.getText().toLowerCase().trim();
        errorTableModel.setRowCount(0);
        for (String err : ultimosErros) {
            if (termo.isEmpty() || err.toLowerCase().contains(termo))
                errorTableModel.addRow(new Object[]{ err });
        }
        if (errorTableModel.getRowCount() == 0 && ultimosErros.isEmpty())
            errorTableModel.addRow(new Object[]{ "Nenhum erro léxico encontrado." });
    }

    private void irParaErroSelecionado() {
        int row = errorTable.getSelectedRow();
        if (row != -1) extrairEIrParaLinhaDoErro((String) errorTable.getValueAt(row, 0));
        else setStatus("Selecione um erro na tabela primeiro");
    }

    private void extrairEIrParaLinhaDoErro(String erro) {
        Matcher m = Pattern.compile("Linha (\\d+)").matcher(erro);
        if (m.find()) irParaLinha(Integer.parseInt(m.group(1)));
        else setStatus("Não foi possível extrair o número da linha do erro");
    }

    private void irParaLinha(int linha) {
        try {
            Element root = editorArea.getDocument().getDefaultRootElement();
            if (linha >= 1 && linha <= root.getElementCount()) {
                editorArea.setCaretPosition(root.getElement(linha - 1).getStartOffset());
                editorArea.requestFocusInWindow();
                setStatus("Movido para a linha " + linha);
            }
        } catch (Exception ex) { setStatus("Erro ao ir para linha: " + ex.getMessage()); }
    }

    // ─── INTEGRAÇÃO COM IA ─────────────────────────────────────────────────
    private void sugerirCorrecaoComIA() {
        if (ultimosErros.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Nenhum erro encontrado. Execute a análise léxica primeiro (F5).",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String codigo = editorArea.getText();
        StringBuilder errosStr = new StringBuilder();
        for (String erro : ultimosErros) errosStr.append("- ").append(erro).append("\n");

        String prompt = String.format("""
            Você é um especialista em Pascal. O seguinte código tem estes erros léxicos:
            ERROS:
            %s
            CÓDIGO:
            %s
            Sugira a versão corrigida do código. Responda APENAS com o código corrigido, sem explicações.
            """, errosStr, codigo);

        setStatus("Consultando IA para correção...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        new Thread(() -> {
            try {
                String correcao = chamarLLM(prompt);
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    int r = JOptionPane.showConfirmDialog(this,
                            "Sugestão de correção gerada pela IA:\n\n" + correcao + "\n\nDeseja aplicar?",
                            "Correção com IA", JOptionPane.YES_NO_OPTION);
                    if (r == JOptionPane.YES_OPTION) {
                        editorArea.setText(correcao);
                        setStatus("Correção aplicada. Execute nova análise (F5) para verificar.");
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    JOptionPane.showMessageDialog(this,
                            "Erro ao consultar IA: " + ex.getMessage()
                                    + "\n\nCertifique-se que o Ollama está rodando.\nComando: ollama serve",
                            "Erro IA", JOptionPane.ERROR_MESSAGE);
                    setStatus("Erro na consulta IA — verifique se o Ollama está rodando");
                });
            }
        }).start();
    }

    private String chamarLLM(String prompt) throws Exception {
        String json = String.format("{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false}",
                LLM_MODEL, prompt.replace("\"", "\\\"").replace("\n", "\\n"));
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LLM_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Matcher m = Pattern.compile("\"response\":\"(.*?)\"").matcher(response.body());
        if (m.find()) return m.group(1).replace("\\n", "\n").replace("\\\"", "\"");
        return response.body();
    }

    // ─── AÇÕES DE ARQUIVO ──────────────────────────────────────────────────
    private void acaoNovo() {
        if (!confirmarDescarte()) return;
        editorArea.setText("");
        currentFile = null;
        modified = false;
        undoManager.discardAllEdits();
        limparResultados();
        updateTitle();
        setStatus("Novo arquivo criado.");
    }

    private void acaoAbrir() {
        if (!confirmarDescarte()) return;
        JFileChooser fc = buildFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                editorArea.setText(Files.readString(f.toPath()));
                editorArea.setCaretPosition(0);
                currentFile = f;
                modified = false;
                undoManager.discardAllEdits();
                limparResultados();
                updateTitle();
                setStatus("Aberto: " + f.getName());
            } catch (IOException ex) { erro("Erro ao abrir: " + ex.getMessage()); }
        }
    }

    private void acaoSalvar() {
        if (currentFile == null) { acaoSalvarComo(); return; }
        try {
            Files.writeString(currentFile.toPath(), editorArea.getText());
            modified = false;
            updateTitle();
            setStatus("Salvo: " + currentFile.getName());
        } catch (IOException ex) { erro("Erro ao salvar: " + ex.getMessage()); }
    }

    private void acaoSalvarComo() {
        JFileChooser fc = buildFileChooser();
        fc.setDialogTitle("Salvar Como");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                if (!f.getName().contains(".")) f = new File(f.getPath() + ".pas");
                Files.writeString(f.toPath(), editorArea.getText());
                currentFile = f;
                modified = false;
                updateTitle();
                setStatus("Salvo como: " + f.getName());
            } catch (IOException ex) { erro("Erro ao salvar: " + ex.getMessage()); }
        }
    }

    private void acaoSair() { if (confirmarDescarte()) System.exit(0); }

    private JFileChooser buildFileChooser() {
        File startDir = (currentFile != null) ? currentFile.getParentFile()
                : new File(System.getProperty("user.home"));
        JFileChooser fc = new JFileChooser(startDir);
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Arquivos Pascal (*.pas, *.p)", "pas", "p"));
        fc.setFileFilter(fc.getChoosableFileFilters()[1]);
        return fc;
    }

    // ─── ANÁLISE ────────────────────────────────────────────────────────────
    private void acaoAnalisar() {
        String codigo = editorArea.getText();
        if (codigo.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite algum código para analisar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        setStatus("Analisando código...");

        SwingUtilities.invokeLater(() -> {
            Lexer lexer = new Lexer(codigo);
            ultimosTokens = lexer.analisar();
            ultimosErros  = lexer.getErros();
            atualizarResultados();
            progressBar.setVisible(false);
            setStatus("Análise concluída: " + ultimosTokens.size() + " tokens, " + ultimosErros.size() + " erros.");
        });
    }

    private void acaoCarregarExemplo() {
        if (!confirmarDescarte()) return;
        String exemplo = """
                program Calculadora;
                var
                  numero1, numero2: integer;
                  resultado: integer;
                begin
                  { Este é um comentário de exemplo }
                  numero1 := 10;
                  numero2 := 20;
                  resultado := numero1 + numero2;
                  writeln('O resultado é: ', resultado);

                  if resultado > 25 then
                    writeln('Resultado maior que 25')
                  else
                    writeln('Resultado menor ou igual a 25');
                end.
                """;
        editorArea.setText(exemplo);
        editorArea.setCaretPosition(0);
        currentFile = null;
        modified = false;
        limparResultados();
        updateTitle();
        destacarPalavrasReservadas();
        setStatus("Exemplo Pascal carregado. Pressione F5 para analisar.");
    }

    private void acaoExportarResultado() {
        if (ultimosTokens.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Execute a análise léxica antes de exportar (F5).",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser(currentFile != null
                ? currentFile.getParentFile() : new File(System.getProperty("user.home")));
        fc.setSelectedFile(new File("resultado_analise.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File dest = fc.getSelectedFile();
                if (!dest.getName().contains(".")) dest = new File(dest.getPath() + ".csv");
                Files.writeString(dest.toPath(), gerarRelatorio());
                setStatus("Resultado exportado: " + dest.getName());
                JOptionPane.showMessageDialog(this,
                        "Arquivo exportado:\n" + dest.getAbsolutePath(),
                        "Exportação Concluída", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) { erro("Erro ao exportar: " + ex.getMessage()); }
        }
    }

    private String gerarRelatorio() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RELATÓRIO DE ANÁLISE LÉXICA ===\n")
                .append("Data/Hora: ").append(new java.util.Date()).append("\n\n")
                .append("--- TOKENS ---\nLexema;Classe;Padrão;Descrição;Linha\n");
        for (Token tk : ultimosTokens)
            sb.append(csv(tk.getLexema())).append(';')
                    .append(csv(tk.getClasse())).append(';')
                    .append(csv(tk.getPadrao())).append(';')
                    .append(csv(tk.getDescricao())).append(';')
                    .append(tk.getLinha()).append('\n');
        sb.append("\n--- ERROS ---\n");
        if (ultimosErros.isEmpty()) sb.append("Nenhum erro léxico.\n");
        else for (String e : ultimosErros) sb.append(e).append('\n');
        sb.append("\nTotal tokens: ").append(ultimosTokens.size())
                .append("\nTotal erros: ").append(ultimosErros.size()).append('\n');
        return sb.toString();
    }

    private String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private void atualizarResultados() {
        tokenTableModel.setRowCount(0);
        for (Token tk : ultimosTokens)
            tokenTableModel.addRow(new Object[]{ tk.getLexema(), tk.getClasse(), tk.getLinha() });

        errorTableModel.setRowCount(0);
        if (ultimosErros.isEmpty())
            errorTableModel.addRow(new Object[]{ "Nenhum erro léxico encontrado. Código válido!" });
        else
            for (String err : ultimosErros) errorTableModel.addRow(new Object[]{ err });

        // Atualiza contadores no rodapé
        tokenCountLabel.setText(ultimosTokens.size() + " tokens");
        errorCountLabel.setText(ultimosErros.size() + " erros");
        errorCountLabel.setForeground(ultimosErros.isEmpty() ? GREEN_BADGE : RED_BADGE);
    }

    private void limparResultados() {
        ultimosTokens.clear();
        ultimosErros.clear();
        if (tokenTableModel != null) tokenTableModel.setRowCount(0);
        if (errorTableModel != null) errorTableModel.setRowCount(0);
        if (tokenCountLabel != null) tokenCountLabel.setText("0 tokens");
        if (errorCountLabel != null) { errorCountLabel.setText("0 erros"); errorCountLabel.setForeground(GREEN_BADGE); }
    }

    // ─── FONTES ────────────────────────────────────────────────────────────
    private Font resolveFont(String name, int size) {
        Font f = new Font(name, Font.PLAIN, size);
        return f.getFamily().equals("Dialog") && !name.equals("Monospaced")
                ? new Font(Font.MONOSPACED, Font.PLAIN, size) : f;
    }

    private void updateEditorFont() {
        editorArea.setFont(resolveFont(currentFontName, currentFontSize));
        lineNumberPanel.updateWidth();
        setStatus("Fonte: " + currentFontName + " " + currentFontSize + "pt");
    }

    private void changeFontSize(int delta) {
        currentFontSize = Math.max(8, Math.min(48, currentFontSize + delta));
        updateEditorFont();
    }

    private void dialogFontSize() {
        String input = JOptionPane.showInputDialog(this,
                "Tamanho da fonte (8–48):\nAtual: " + currentFontSize + "pt",
                "Tamanho da Fonte", JOptionPane.QUESTION_MESSAGE);
        if (input != null) {
            try {
                int size = Integer.parseInt(input.trim());
                if (size >= 8 && size <= 48) { currentFontSize = size; updateEditorFont(); }
                else throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Use um número entre 8 e 48.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ─── TEMAS ─────────────────────────────────────────────────────────────
    private void applyTheme(Theme t) {
        currentTheme = t;

        // Editor
        editorArea.setBackground(t.bgEditor);
        editorArea.setForeground(t.fgText);
        editorArea.setCaretColor(t.caret);
        editorArea.setSelectionColor(t.selection);

        // Números de linha
        lineNumberPanel.setBackground(t.bgLineNum);
        lineNumberPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, t.border));

        // Painel principal
        getContentPane().setBackground(t.bgPanel);

        // Tabelas
        applyThemeToTable(tokenTable, t);
        applyThemeToTable(errorTable, t);

        // Renderer de classe precisa do tema atualizado
        if (tokenTable != null)
            tokenTable.getColumnModel().getColumn(1).setCellRenderer(new ClassePillRenderer(t));

        lineNumberPanel.repaint();
        destacarPalavrasReservadas();
        repaint();
    }

    private void applyThemeToTable(JTable table, Theme t) {
        if (table == null) return;
        table.setBackground(t.bgEditor);
        table.setForeground(t.fgText);
        table.setGridColor(t.border);
        table.setSelectionBackground(t.selection);
        table.setSelectionForeground(t.fgText);
        table.getTableHeader().setBackground(t.bgToolbar);
        table.getTableHeader().setForeground(t.fgText);
        table.getTableHeader().setFont(UI_FONT_BOLD);
    }

    // ─── REALCE DE SINTAXE ─────────────────────────────────────────────────
    private void destacarPalavrasReservadas() {
        if (editorArea == null || currentTheme == null || aplicandoDestaque) return;
        StyledDocument doc = editorArea.getStyledDocument();
        int caret   = editorArea.getCaretPosition();
        int tamanho = doc.getLength();

        SimpleAttributeSet normal  = attr(currentTheme.fgText, false, false);
        SimpleAttributeSet keyword = attr(currentTheme.keywordColor, true, false);
        SimpleAttributeSet number  = attr(currentTheme.numberColor, false, false);
        SimpleAttributeSet string  = attr(currentTheme.stringColor, false, false);
        SimpleAttributeSet comment = attr(currentTheme.commentColor, false, true);

        aplicandoDestaque = true;
        try {
            String code = doc.getText(0, tamanho);
            doc.setCharacterAttributes(0, tamanho, normal, true);

            highlight(doc, code, "\\b(program|var|begin|end|integer|real|boolean|char|string"
                            + "|if|then|else|while|do|for|to|downto|repeat|until"
                            + "|function|procedure|and|or|not|div|mod|writeln|readln|write|read"
                            + "|true|false|const|type|array|of|record)\\b",
                    Pattern.CASE_INSENSITIVE, keyword);
            highlight(doc, code, "\\b\\d+(\\.\\d+)?\\b", 0, number);
            highlight(doc, code, "'[^']*'", 0, string);
            highlight(doc, code, "\\{.*?\\}|\\(\\*.*?\\*\\)|//.*$", Pattern.MULTILINE, comment);

        } catch (BadLocationException ignored) {}
        finally {
            aplicandoDestaque = false;
            editorArea.setCaretPosition(Math.min(caret, doc.getLength()));
        }
    }

    private SimpleAttributeSet attr(Color fg, boolean bold, boolean italic) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, fg);
        StyleConstants.setBold(a, bold);
        StyleConstants.setItalic(a, italic);
        return a;
    }

    private void highlight(StyledDocument doc, String code, String regex, int flags, AttributeSet attr) {
        Matcher m = Pattern.compile(regex, flags).matcher(code);
        while (m.find()) doc.setCharacterAttributes(m.start(), m.end() - m.start(), attr, true);
    }

    // ─── ESTADO / POSIÇÃO ──────────────────────────────────────────────────
    private void onChange() {
        if (!modified) { modified = true; updateTitle(); }
        SwingUtilities.invokeLater(this::destacarPalavrasReservadas);
        lineNumberPanel.repaint();
    }

    private void updatePosition() {
        Element root = editorArea.getDocument().getDefaultRootElement();
        int pos  = editorArea.getCaretPosition();
        int line = root.getElementIndex(pos);
        int col  = pos - root.getElement(line).getStartOffset() + 1;
        posLabel.setText("Ln " + (line + 1) + ", Col " + col);
        lineNumberPanel.repaint();
    }

    private void updateTitle() {
        String nome = (currentFile != null) ? currentFile.getName() : "analisador";
        setTitle(" Analisador Léxico Pascal — " + nome + (modified ? " *" : ""));
        fileLabel.setText(currentFile != null ? currentFile.getAbsolutePath() : "lexico pascal");
    }

    private void setStatus(String msg) {
        statusLabel.setText("  " + msg);
    }

    private boolean confirmarDescarte() {
        if (!modified) return true;
        int r = JOptionPane.showConfirmDialog(this,
                "O arquivo foi modificado. Deseja salvar antes de continuar?",
                "Alterações não salvas", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) { acaoSalvar(); return !modified; }
        return r == JOptionPane.NO_OPTION;
    }

    private void erro(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
        setStatus("Erro: " + msg);
    }

    // ─── MAIN ──────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(Clone::new);
    }
}