import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Clone extends JFrame {


    // ─── Componentes ──────────────────────────────────────────────────────
    private JTextArea editorArea;
    private LineNumberPanel lineNumberPanel;
    private JLabel statusLabel;
    private JLabel posLabel;
    private JLabel fileLabel;
    private JLabel themeLabel;
    private JScrollPane editorScroll;

    // ─── Estado ───────────────────────────────────────────────────────────
    private File currentFile = null;
    private boolean modified = false;
    private UndoManager undoManager = new UndoManager();
    private Theme currentTheme;
    private String currentFontName = "Monospaced";
    private int currentFontSize = 14;

    // ═══════════════════════════════════════════════════════════════════════
    // TEMAS
    // ═══════════════════════════════════════════════════════════════════════
    static class Theme {
        final String name;
        final Color bgEditor, bgLineNum, bgPanel, bgBar, bgToolbar;
        final Color fgText, fgLineNum, fgDim, fgAccent, fgAccent2;
        final Color selection, caret, border;

        Theme(String name,
              Color bgEditor, Color bgLineNum, Color bgPanel, Color bgBar, Color bgToolbar,
              Color fgText, Color fgLineNum, Color fgDim, Color fgAccent, Color fgAccent2,
              Color selection, Color caret, Color border) {
            this.name = name;
            this.bgEditor = bgEditor; this.bgLineNum = bgLineNum;
            this.bgPanel = bgPanel;   this.bgBar = bgBar; this.bgToolbar = bgToolbar;
            this.fgText = fgText;     this.fgLineNum = fgLineNum;
            this.fgDim = fgDim;       this.fgAccent = fgAccent; this.fgAccent2 = fgAccent2;
            this.selection = selection; this.caret = caret; this.border = border;
        }
    }

    // Catálogo de temas
    private static final Map<String, Theme> THEMES = new LinkedHashMap<>();
    static {
        // ── 1. Dark (padrão) ──────────────────────────────────────────────
        THEMES.put("Dark", new Theme("Dark",
                new Color(22, 22, 34),      
                new Color(15, 15, 24),      // bgLineNum
                new Color(26, 26, 42),      
                new Color(18, 18, 30),      // bgBar
                new Color(30, 30, 50),      // bgToolbar
                new Color(220, 220, 240),   
                new Color(80, 100, 140),    // fgLineNum
                new Color(120, 120, 160),   
                new Color(99, 179, 237),    // fgAccent (azul)
                new Color(154, 117, 255),   // fgAccent2 (roxo)
                new Color(99, 179, 237, 55),// selection
                new Color(99, 179, 237),    
                new Color(45, 45, 75)       
        ));

        // ** ligth
        THEMES.put("Light", new Theme("Light",
                new Color(252, 252, 255),
                new Color(238, 238, 248),
                new Color(245, 245, 255),
                new Color(235, 235, 248),
                new Color(240, 240, 252),
                new Color(30, 30, 50),
                new Color(140, 140, 180),
                new Color(140, 140, 180),
                new Color(55, 120, 200),
                new Color(130, 60, 200),
                new Color(55, 120, 200, 50),
                new Color(55, 120, 200),
                new Color(200, 200, 220)
        ));

        //** mokaino */
        THEMES.put("Monokai", new Theme("Monokai",
                new Color(39, 40, 34),
                new Color(30, 31, 26),
                new Color(45, 46, 40),
                new Color(35, 36, 30),
                new Color(50, 51, 44),
                new Color(248, 248, 242),
                new Color(117, 113, 94),
                new Color(117, 113, 94),
                new Color(166, 226, 46),    // verde Monokai
                new Color(253, 151, 31),    // laranja Monokai
                new Color(73, 72, 62),
                new Color(166, 226, 46),
                new Color(73, 72, 62)
        ));

        // * Sola
        THEMES.put("Solarized", new Theme("Solarized",
                new Color(0, 43, 54),
                new Color(7, 54, 66),
                new Color(0, 43, 54),
                new Color(7, 54, 66),
                new Color(7, 54, 66),
                new Color(131, 148, 150),
                new Color(88, 110, 117),
                new Color(88, 110, 117),
                new Color(38, 139, 210),    // azul Solarized
                new Color(211, 54, 130),    // magenta Solarized
                new Color(38, 139, 210, 60),
                new Color(38, 139, 210),
                new Color(7, 54, 66)
        ));

        // ── 5. Midni
        THEMES.put("Midnight", new Theme("Midnight",
                new Color(5, 5, 15),
                new Color(3, 3, 10),
                new Color(8, 8, 20),
                new Color(5, 5, 15),
                new Color(10, 10, 25),
                new Color(180, 200, 255),
                new Color(50, 60, 110),
                new Color(60, 70, 120),
                new Color(100, 160, 255),
                new Color(200, 100, 255),
                new Color(80, 120, 255, 55),
                new Color(100, 160, 255),
                new Color(20, 20, 50)
        ));
    }

    // Fontesmonospace
    private static final String[] MONO_FONTS = {
            "Consolas", "JetBrains Mono", "Fira Code", "Source Code Pro",
            "Courier New", "Lucida Console", "Monospaced", "DejaVu Sans Mono"
    };


    public Clone() {
        super("KSS");
        currentTheme = THEMES.get("Dark");

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { acaoSair(); }
        });

        setSize(1100, 740);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);

        buildUI();
        applyTheme(currentTheme);
        updateTitle();
        setVisible(true);
    }

    //  Layut principal
    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        setJMenuBar(buildMenuBar());
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildEditorPane(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MENU BAR
    // ═══════════════════════════════════════════════════════════════════════
    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();

        // ── Arquivo ───────────────────────────────────────────────────────
        JMenu mArquivo = menu("Arquivo");
        mArquivo.add(menuItem("Novo",          "ctrl N",           e -> acaoNovo()));
        mArquivo.add(menuItem("Abrir...",       "ctrl O",           e -> acaoAbrir()));
        mArquivo.addSeparator();
        mArquivo.add(menuItem("Salvar",         "ctrl S",           e -> acaoSalvar()));
        mArquivo.add(menuItem("Salvar Como...", "ctrl shift S",     e -> acaoSalvarComo()));
        mArquivo.addSeparator();
        mArquivo.add(menuItem("Sair",           "alt F4",           e -> acaoSair()));

        // ── Editar ────────────────────────────────────────────────────────
        JMenu mEditar = menu("Editar");
        mEditar.add(menuItem("Desfazer",        "ctrl Z",           e -> { if (undoManager.canUndo()) undoManager.undo(); }));
        mEditar.add(menuItem("Refazer",         "ctrl Y",           e -> { if (undoManager.canRedo()) undoManager.redo(); }));
        mEditar.addSeparator();
        mEditar.add(menuItem("Copiar",          "ctrl C",           e -> editorArea.copy()));
        mEditar.add(menuItem("Recortar",        "ctrl X",           e -> editorArea.cut()));
        mEditar.add(menuItem("Colar",           "ctrl V",           e -> editorArea.paste()));
        mEditar.addSeparator();
        mEditar.add(menuItem("Selecionar Tudo", "ctrl A",           e -> editorArea.selectAll()));

        // ── Visualizar
        JMenu mVisualizar = menu("Visualizar");

        // Submenu Temas
        JMenu mTemas = new JMenu("Tema");
        styleMenu(mTemas);
        ButtonGroup bgTema = new ButtonGroup();
        for (String nomeTema : THEMES.keySet()) {
            JRadioButtonMenuItem rb = new JRadioButtonMenuItem(nomeTema, nomeTema.equals("Dark"));
            styleMenuItem(rb);
            bgTema.add(rb);
            rb.addActionListener(e -> applyTheme(THEMES.get(nomeTema)));
            mTemas.add(rb);
        }
        mVisualizar.add(mTemas);
        mVisualizar.addSeparator();

        // Submenu Fonte
        JMenu mFonte = new JMenu("Fonte");
        styleMenu(mFonte);
        ButtonGroup bgFonte = new ButtonGroup();
        for (String fontName : MONO_FONTS) {
            // Verificar se a fonte está disponível no sistema
            Font testFont = new Font(fontName, Font.PLAIN, 12);
            if (!testFont.getFamily().equals("Dialog") || fontName.equals("Monospaced")) {
                JRadioButtonMenuItem rb = new JRadioButtonMenuItem(fontName, fontName.equals("Monospaced"));
                rb.setFont(new Font(fontName, Font.PLAIN, 12));
                styleMenuItem(rb);
                bgFonte.add(rb);
                rb.addActionListener(e -> { currentFontName = fontName; updateEditorFont(); });
                mFonte.add(rb);
            }
        }
        mVisualizar.add(mFonte);
        mVisualizar.addSeparator();

        // Tamanho de fonte
        mVisualizar.add(menuItem("Aumentar Fonte  (+)",  "ctrl EQUALS", e -> changeFontSize(2)));
        mVisualizar.add(menuItem("Diminuir Fonte  (−)",  "ctrl MINUS",  e -> changeFontSize(-2)));
        mVisualizar.add(menuItem("Tamanho Personalizado...", null,       e -> dialogFontSize()));
        mVisualizar.add(menuItem("Repor Fonte Padrão",    "ctrl 0",     e -> { currentFontSize = 14; currentFontName = "Monospaced"; updateEditorFont(); }));

        mb.add(mArquivo);
        mb.add(mEditar);
        mb.add(mVisualizar);
        return mb;
    }

    private static final Font MENU_FONT = new Font("Segoe UI", Font.PLAIN, 15);

    private JMenu menu(String title) {
        JMenu m = new JMenu(title);
        styleMenu(m);
        return m;
    }

    private void styleMenu(JMenu m) {
        m.setFont(MENU_FONT);
    }

    private JMenuItem menuItem(String label, String accel, ActionListener al) {
        JMenuItem mi = new JMenuItem(label);
        mi.setFont(MENU_FONT);
        if (accel != null) mi.setAccelerator(KeyStroke.getKeyStroke(accel));
        mi.addActionListener(al);
        return mi;
    }

    private void styleMenuItem(JMenuItem mi) {
        mi.setFont(MENU_FONT);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TOOLBAR
    // ═══════════════════════════════════════════════════════════════════════
    private JPanel buildToolbar() {
        JPanel tb = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 6));
        tb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.GRAY));

        // Grupo Arquivo
        tb.add(tbBtn("Novo", "Novo  (Ctrl+N)",       e -> acaoNovo()));
        tb.add(tbBtn("Abrir", "Abrir  (Ctrl+O)",      e -> acaoAbrir()));
        tb.add(tbBtn("Salvar", "Salvar  (Ctrl+S)",     e -> acaoSalvar()));
        tb.add(tbSep());
        // Grupo Editar
        tb.add(tbBtn("<-", "Desfazer  (Ctrl+Z)",   e -> { if (undoManager.canUndo()) undoManager.undo(); }));
        tb.add(tbBtn("->", "Refazer  (Ctrl+Y)",    e -> { if (undoManager.canRedo()) undoManager.redo(); }));
        tb.add(tbSep());
        tb.add(tbBtn("Copiar", "Copiar  (Ctrl+C)",     e -> editorArea.copy()));
        tb.add(tbBtn("Recortar", "Recortar  (Ctrl+X)",   e -> editorArea.cut()));
        tb.add(tbBtn("Colar", "Colar  (Ctrl+V)",      e -> editorArea.paste()));
        tb.add(tbSep());
        // Grupo Fonte
        tb.add(tbBtn("A+", "Aumentar Fonte  (Ctrl++)", e -> changeFontSize(2)));
        tb.add(tbBtn("A−", "Diminuir Fonte  (Ctrl+−)", e -> changeFontSize(-2)));
        tb.add(tbSep());
        // Seletor de tema rápido
        themeLabel = new JLabel("  Tema: ");
        themeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tb.add(themeLabel);
        JComboBox<String> themeCombo = new JComboBox<>(THEMES.keySet().toArray(new String[0]));
        themeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        themeCombo.setPreferredSize(new Dimension(120, 28));
        themeCombo.addActionListener(e -> {
            String sel = (String) themeCombo.getSelectedItem();
            if (sel != null) applyTheme(THEMES.get(sel));
        });
        tb.add(themeCombo);

        // Botão Executar — separado à direita com destaque visual
        JButton btnExecutar = new JButton("Run");  // Não sei se é importante colocar esse botão de correr Aqui
        btnExecutar.setToolTipText("Executar (F5)");
        btnExecutar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnExecutar.setForeground(Color.WHITE);
        btnExecutar.setBackground(new Color(39, 160, 90));
        btnExecutar.setOpaque(true);
        btnExecutar.setBorderPainted(false);
        btnExecutar.setFocusPainted(false);
        btnExecutar.setMargin(new Insets(4, 14, 4, 14));
        btnExecutar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExecutar.addActionListener(e -> acaoAnalisar());
        // Hover
        btnExecutar.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btnExecutar.setBackground(new Color(50, 190, 110)); }
            @Override public void mouseExited(MouseEvent e)  { btnExecutar.setBackground(new Color(39, 160, 90)); }
        });
        // Atalho F5
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "analisar");
        getRootPane().getActionMap().put("analisar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { acaoAnalisar(); }
        });

        tb.add(btnExecutar);

        return tb;
    }




    private JButton tbBtn(String icon, String tooltip, ActionListener al) {
        JButton b = new JButton(icon);
        b.setToolTipText(tooltip);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setMargin(new Insets(4, 10, 4, 10));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        return b;
    }

    private JSeparator tbSep() {
        JSeparator s = new JSeparator(JSeparator.VERTICAL);
        s.setPreferredSize(new Dimension(6, 28));
        return s;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PAINEL DO EDITOR
    // ═══════════════════════════════════════════════════════════════════════
    private JPanel buildEditorPane() {
        JPanel panel = new JPanel(new BorderLayout());

        // Área de texto
        editorArea = new JTextArea();
        editorArea.setTabSize(4);
        editorArea.setLineWrap(false);
        editorArea.setMargin(new Insets(6, 10, 6, 6));
        editorArea.setFont(resolveFont(currentFontName, currentFontSize));

        // Undo/Redo
        editorArea.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));

        // Listeners
        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onChange(); }
            @Override public void removeUpdate(DocumentEvent e) { onChange(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange(); }
        });
        editorArea.addCaretListener(e -> updatePosition());

        // Painel numeração de linhas
        lineNumberPanel = new LineNumberPanel(editorArea);

        // Scroll
        editorScroll = new JScrollPane(editorArea);
        editorScroll.setRowHeaderView(lineNumberPanel);
        editorScroll.setBorder(null);
        editorScroll.getVerticalScrollBar().setUnitIncrement(16);

        // Sincronizar scroll do número de linhas
        editorScroll.getViewport().addChangeListener(e -> lineNumberPanel.repaint());

        panel.add(editorScroll, BorderLayout.CENTER);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PAINEL DE NÚMEROS DE LINHA (componente personalizado) * ajuda nessa parte meus Manos
    // ═══════════════════════════════════════════════════════════════════════
    class LineNumberPanel extends JPanel {
        private final JTextArea target;
        private static final int PADDING = 10;

        LineNumberPanel(JTextArea target) {
            this.target = target;
            setPreferredSize(new Dimension(52, 0));
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));

            // Atualizar quando o texto muda
            target.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { repaint(); updateWidth(); }
                @Override public void removeUpdate(DocumentEvent e) { repaint(); updateWidth(); }
                @Override public void changedUpdate(DocumentEvent e) { repaint(); }
            });
        }

        private void updateWidth() {
            int lines = target.getLineCount();
            int digits = String.valueOf(lines).length();
            int width = PADDING * 2 + getFontMetrics(target.getFont()).stringWidth("0".repeat(Math.max(digits, 3)));
            setPreferredSize(new Dimension(width, 0));
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Usar fonte e métricas do editor
            g2.setFont(target.getFont());
            FontMetrics fm = g2.getFontMetrics();
            int lineHeight = target.getFont().getSize() + 4;

            // Posição vertical do viewport
            Rectangle viewRect = editorScroll.getViewport().getViewRect();
            int firstLine = viewRect.y / lineHeight;
            int lastLine = Math.min(target.getLineCount() - 1, (viewRect.y + viewRect.height) / lineHeight + 1);

            int startY = (firstLine * lineHeight) - viewRect.y + fm.getAscent() + target.getMargin().top + 6;
            int panelW = getWidth();

            for (int i = firstLine; i <= lastLine; i++) {
                String num = String.valueOf(i + 1);

                // Destaque na linha do cursor
                try {
                    int caretLine = target.getLineOfOffset(target.getCaretPosition());
                    if (i == caretLine) {
                        g2.setColor(currentTheme.fgAccent);
                        g2.setFont(target.getFont().deriveFont(Font.BOLD));
                    } else {
                        g2.setColor(currentTheme.fgLineNum);
                        g2.setFont(target.getFont());
                    }
                } catch (BadLocationException ex) {
                    g2.setColor(currentTheme.fgLineNum);
                }

                int textW = fm.stringWidth(num);
                g2.drawString(num, panelW - textW - PADDING, startY + (i - firstLine) * lineHeight);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATUS BAR
    // ═══════════════════════════════════════════════════════════════════════
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setPreferredSize(new Dimension(0, 26));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));

        statusLabel = new JLabel("  Pronto");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightPanel.setOpaque(false);

        fileLabel = new JLabel("Vendo tudo ");
        fileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        posLabel = new JLabel("Ln 1, Col 1");
        posLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JSeparator sepBar = new JSeparator(JSeparator.VERTICAL);
        sepBar.setPreferredSize(new Dimension(1, 14));

        rightPanel.add(fileLabel);
        rightPanel.add(sepBar);
        rightPanel.add(posLabel);

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(rightPanel, BorderLayout.EAST);
        return bar;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // APLICAR TEMA
    // ═══════════════════════════════════════════════════════════════════════
    private void applyTheme(Theme t) {
        currentTheme = t;

        // Editor
        editorArea.setBackground(t.bgEditor);
        editorArea.setForeground(t.fgText);
        editorArea.setCaretColor(t.caret);
        editorArea.setSelectionColor(t.selection);
        editorArea.setSelectedTextColor(t.fgText);

        // Numeração
        lineNumberPanel.setBackground(t.bgLineNum);
        lineNumberPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, t.border));

        // Scroll
        editorScroll.setBackground(t.bgEditor);
        editorScroll.getViewport().setBackground(t.bgEditor);
        editorScroll.getVerticalScrollBar().setBackground(t.bgLineNum);
        editorScroll.getHorizontalScrollBar().setBackground(t.bgLineNum);

        // Status bar
        Container statusBar = (Container) ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        if (statusBar instanceof JPanel) {
            statusBar.setBackground(t.bgBar);
            statusLabel.setForeground(t.fgAccent);
            fileLabel.setForeground(t.fgDim);
            posLabel.setForeground(t.fgDim);
        }

        // Toolbar
        Container toolbar = (Container) ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.NORTH);
        if (toolbar instanceof JPanel) {
            toolbar.setBackground(t.bgToolbar);
            //toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, t.border));
            styleToolbarChildren((JPanel) toolbar, t);
            if (themeLabel != null) themeLabel.setForeground(t.fgDim);
        }

        // Frame
        getContentPane().setBackground(t.bgPanel);

        // Menu bar
        JMenuBar mb = getJMenuBar();
        if (mb != null) {
            mb.setBackground(t.bgPanel);
            mb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, t.border));
            for (int i = 0; i < mb.getMenuCount(); i++) {
                styleMenuColors(mb.getMenu(i), t);
            }
        }

        lineNumberPanel.repaint();
        repaint();
    }

    private void styleToolbarChildren(JPanel tb, Theme t) {
        for (Component c : tb.getComponents()) {
            if (c instanceof JButton b) {
                b.setBackground(t.bgToolbar);
                b.setForeground(t.fgText);
            } else if (c instanceof JLabel l) {
                l.setForeground(t.fgDim);
            } else if (c instanceof JComboBox<?> cb) {
                cb.setBackground(t.bgPanel);
                cb.setForeground(t.fgText);
            }
        }
    }

    private void styleMenuColors(JMenu menu, Theme t) {
        if (menu == null) return;
        menu.setForeground(t.fgText);
        menu.setBackground(t.bgPanel);
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem mi = menu.getItem(i);
            if (mi == null) continue;
            mi.setBackground(t.bgPanel);
            mi.setForeground(t.fgText);
            if (mi instanceof JMenu subMenu) styleMenuColors(subMenu, t);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FONTES
    // ═══════════════════════════════════════════════════════════════════════
    private Font resolveFont(String name, int size) {
        Font f = new Font(name, Font.PLAIN, size);
        // Fallback se a fonte não existir (família retorna "Dialog")
        if (f.getFamily().equals("Dialog") && !name.equals("Monospaced")) {
            return new Font(Font.MONOSPACED, Font.PLAIN, size);
        }
        return f;
    }

    private void updateEditorFont() {
        Font f = resolveFont(currentFontName, currentFontSize);
        editorArea.setFont(f);
        lineNumberPanel.updateWidth();
        lineNumberPanel.repaint();
        setStatus("Fonte: " + currentFontName + "  " + currentFontSize + "pt");
    }

    private void changeFontSize(int delta) {
        currentFontSize = Math.max(8, Math.min(48, currentFontSize + delta));
        updateEditorFont();
    }

    private void dialogFontSize() {
        String input = JOptionPane.showInputDialog(
                this,
                "Digite o tamanho da fonte (8 – 48):",
                "Tamanho da Fonte",
                JOptionPane.PLAIN_MESSAGE
        );
        if (input == null) return;
        try {
            int size = Integer.parseInt(input.trim());
            if (size < 8 || size > 48) throw new NumberFormatException();
            currentFontSize = size;
            updateEditorFont();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Valor inválido. Insira um número entre 8 e 48.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AÇÕES DE ARQUIVO
    // ═══════════════════════════════════════════════════════════════════════
    private void acaoNovo() {
        if (!confirmarDescarte()) return;
        editorArea.setText("");
        currentFile = null;
        modified = false;
        undoManager.discardAllEdits();
        updateTitle();
        setStatus("Novo arquivo criado.");
    }

    private void acaoAbrir() {
        if (!confirmarDescarte()) return;
        JFileChooser fc = buildFileChooser();
        fc.setDialogTitle("Abrir Arquivo");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                editorArea.setText(new String(Files.readAllBytes(f.toPath())));
                editorArea.setCaretPosition(0);
                currentFile = f;
                modified = false;
                undoManager.discardAllEdits();
                updateTitle();
                setStatus("Aberto: " + f.getName());
            } catch (IOException ex) {
                erro("Erro ao abrir arquivo:\n" + ex.getMessage());
            }
        }
    }

    private void acaoSalvar() {
        if (currentFile == null) { acaoSalvarComo(); return; }
        try {
            Files.write(currentFile.toPath(), editorArea.getText().getBytes());
            modified = false;
            updateTitle();
            setStatus("Salvo: " + currentFile.getName());
        } catch (IOException ex) {
            erro("Erro ao salvar:\n" + ex.getMessage());
        }
    }

    private void acaoSalvarComo() {
        JFileChooser fc = buildFileChooser();
        fc.setDialogTitle("Salvar Como");
        if (currentFile != null) fc.setSelectedFile(currentFile);
        else fc.setSelectedFile(new File("programa.pas"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                // Garantir extensão
                if (!f.getName().contains(".")) f = new File(f.getPath() + ".pas");
                Files.write(f.toPath(), editorArea.getText().getBytes());
                currentFile = f;
                modified = false;
                updateTitle();
                setStatus("Salvo como: " + f.getName());
            } catch (IOException ex) {
                erro("Erro ao salvar:\n" + ex.getMessage());
            }
        }
    }

    private void acaoSair() {
        if (confirmarDescarte()) System.exit(0);
    }

    private JFileChooser buildFileChooser() {
        File startDir = (currentFile != null)
                ? currentFile.getParentFile()
                : new File(System.getProperty("user.home"));
        JFileChooser fc = new JFileChooser(startDir);
        fc.addChoosableFileFilter(new FileNameExtensionFilter(
                "Arquivos Pascal (*.pas, *.p)", "pas", "p"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter(
                "Arquivos de texto (*.txt)", "txt"));
        fc.setAcceptAllFileFilterUsed(true);
        return fc;
    }

    private void acaoAnalisar() {
        String codigo = editorArea.getText().trim();
        if (codigo.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "KKS.", // tens que tirar isso depois
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // UTILITÁRIOS ******
    // ═══════════════════════════════════════════════════════════════════════
    private void onChange() {
        if (!modified) {
            modified = true;
            updateTitle();
        }
        lineNumberPanel.repaint();
    }

    private void updatePosition() {
        try {
            int pos = editorArea.getCaretPosition();
            int line = editorArea.getLineOfOffset(pos) + 1;
            int col  = pos - editorArea.getLineStartOffset(line - 1) + 1;
            posLabel.setText("Ln " + line + ", Col " + col + "  ");
            lineNumberPanel.repaint();
        } catch (BadLocationException ignored) {}
    }

    private void updateTitle() {
        String nome = (currentFile != null) ? currentFile.getName() : "Sem título";
        String mod  = modified ? " " : "";  // deve Conter um Exato de mini Log para a Imagem
        setTitle("KKS — " + nome + mod);
        fileLabel.setText((currentFile != null
                ? currentFile.getAbsolutePath()
                : "Sem título") + "  ");
    }

    private void setStatus(String msg) {
        statusLabel.setText("  " + msg);
    }

    private boolean confirmarDescarte() {
        if (!modified) return true;
        int r = JOptionPane.showConfirmDialog(this,
                "O arquivo foi modificado. Deseja salvar antes de continuar?",
                "Alterações não salvas",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) { acaoSalvar(); return !modified; }
        return r == JOptionPane.NO_OPTION;
    }

    private void erro(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN
    // ═══════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        // Anti-aliasing de texto
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Forçar fonte dos menus — o L&F do sistema pode sobrepor setFont() *****
        Font menuFont = new Font("Segoe UI", Font.PLAIN, 15);
        UIManager.put("Menu.font",               menuFont);
        UIManager.put("MenuItem.font",           menuFont);
        UIManager.put("RadioButtonMenuItem.font", menuFont);
        UIManager.put("CheckBoxMenuItem.font",   menuFont);
        UIManager.put("PopupMenu.font",          menuFont);
        UIManager.put("MenuBar.font",            menuFont);

        SwingUtilities.invokeLater(Clone::new);
    }
}