import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

public class Timex extends JFrame {

    private static final Color C_TIMER     = Color.WHITE;
    private static final Color C_SUB       = new Color(185, 195, 210);
    private static final Color C_DIM       = new Color(130, 145, 165);

    private static final Color C_TAB_A_BG  = new Color(255, 255, 255, 230);
    private static final Color C_TAB_A_FG  = new Color(14,  24,  46);
    private static final Color C_TAB_I_BG  = new Color(255, 255, 255,  30);
    private static final Color C_TAB_I_BD  = new Color(255, 255, 255,  90);
    private static final Color C_TAB_I_FG  = Color.WHITE;

    private static final Color C_PILL_A_BG = new Color(255, 255, 255, 235);
    private static final Color C_PILL_A_FG = new Color(14,  24,  46);
    private static final Color C_PILL_I_BG = new Color(255, 255, 255,  28);
    private static final Color C_PILL_I_BD = new Color(255, 255, 255,  80);
    private static final Color C_PILL_I_FG = Color.WHITE;

    private static final Color C_START_BG  = Color.WHITE;
    private static final Color C_START_FG  = new Color(14,  24,  46);
    private static final Color C_ICO_BG    = new Color(255, 255, 255,  30);
    private static final Color C_ICO_BD    = new Color(255, 255, 255,  80);
    private static final Color C_ICO_FG    = Color.WHITE;

    private static final Color C_ADJ_BG    = new Color(255, 255, 255,  26);
    private static final Color C_ADJ_BD    = new Color(255, 255, 255,  75);
    private static final Color C_ADJ_FG    = Color.WHITE;

    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FMT_DAY  = DateTimeFormatter.ofPattern("EEEE");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    private static final int TAB_CLOCK = 0, TAB_POMO = 1;
    private int activeTab = TAB_CLOCK;

    private static final String[] MODE_NAMES = {"pomodoro", "short break", "long break"};
    private final int[] modeSecs = {25 * 60, 5 * 60, 15 * 60};
    private int     activeMode = 0;
    private long    secsLeft   = modeSecs[0];
    private boolean running    = false;
    private Timer   countdown;

    private JLabel clockTime, clockDay, clockDate;
    private Timer  clockTimer;

    // Updated Pomodoro Timer Components
    private JPanel     pomoTimeContainer;
    private JTextField hhField, mmField, ssField;
    private JLabel     colon1, colon2;
    
    private TimexBtn  startBtn;
    private PillBtn[] modePills;

    private TabBtn   tabClock, tabPomo;
    private TimexBtn closeBtn;

    private JPanel root, clockPanel, pomoPanel;

    private static final int RZ = 7;
    private Point     pressPoint;
    private Rectangle pressWinBounds;
    private int       resizeDir = -1;

    public Timex() {
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        setType(Window.Type.UTILITY);
        setResizable(false);
        setAlwaysOnTop(true);

        setSize(350, 220);
        setMinimumSize(new Dimension(280, 180));
        setBackground(new Color(44, 50, 60));

        buildUI();
        setupWindowInteractions();

        setLocationRelativeTo(null);
        setVisible(true);

        clockTimer = new Timer(1000, e -> refreshClock());
        clockTimer.setInitialDelay(0);
        clockTimer.start();
    }

    private void buildUI() {
        root = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                int W = getWidth(), H = getHeight();
                g2.setClip(new RoundRectangle2D.Double(0, 0, W, H, 18, 18));
                g2.setColor(new Color(44, 50, 60));
                g2.fillRect(0, 0, W, H);
                g2.dispose();
            }
        };
        root.setOpaque(true);
        root.setFocusable(true);
        root.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { root.requestFocusInWindow(); }
        });

        tabClock = new TabBtn("CLOCK", true);
        tabClock.addActionListener(e -> showTab(TAB_CLOCK));
        root.add(tabClock);

        tabPomo = new TabBtn("POMO", false);
        tabPomo.addActionListener(e -> showTab(TAB_POMO));
        root.add(tabPomo);

        closeBtn = new TimexBtn("✕", C_ICO_BG, C_ICO_BD, C_ICO_FG, true);
        closeBtn.setFont(new Font("Dialog", Font.PLAIN, 13));
        closeBtn.addActionListener(e -> System.exit(0));
        root.add(closeBtn);

        // clock panel ---->>> OG idea
        clockPanel = new JPanel(null);
        clockPanel.setOpaque(false);

        clockTime = transparentLabel("00:00:00", SwingConstants.CENTER);
        clockTime.setForeground(C_TIMER);
        clockPanel.add(clockTime);

        clockDay = transparentLabel("", SwingConstants.CENTER);
        clockDay.setForeground(C_SUB);
        clockPanel.add(clockDay);

        clockDate = transparentLabel("", SwingConstants.CENTER);
        clockDate.setForeground(C_DIM);
        clockPanel.add(clockDate);

        root.add(clockPanel);

        // pomodoro panel  ----->>> NEW ADDITION lololzzz
        pomoPanel = new JPanel(null);
        pomoPanel.setOpaque(false);

        modePills = new PillBtn[MODE_NAMES.length];
        for (int i = 0; i < MODE_NAMES.length; i++) {
            modePills[i] = new PillBtn(MODE_NAMES[i], i == 0);
            final int idx = i;
            modePills[i].addActionListener(e -> switchMode(idx));
            pomoPanel.add(modePills[i]);
        }

        // --- NEW EDITABLE TIME PANEL --- >>>>
        pomoTimeContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pomoTimeContainer.setOpaque(false);

        hhField = createTimeField();
        colon1  = transparentLabel(":", SwingConstants.CENTER);
        colon1.setForeground(C_TIMER);
        mmField = createTimeField();
        colon2  = transparentLabel(":", SwingConstants.CENTER);
        colon2.setForeground(C_TIMER);
        ssField = createTimeField();

        pomoTimeContainer.add(hhField);
        pomoTimeContainer.add(colon1);
        pomoTimeContainer.add(mmField);
        pomoTimeContainer.add(colon2);
        pomoTimeContainer.add(ssField);
        pomoPanel.add(pomoTimeContainer);
        updateTimeFields();
        // -------------------------------

        TimexBtn minusBtn = new TimexBtn("− 1 min", C_ADJ_BG, C_ADJ_BD, C_ADJ_FG, false);
        minusBtn.addActionListener(e -> adjustPomo(-60));
        minusBtn.setName("minus");
        pomoPanel.add(minusBtn);

        TimexBtn plusBtn = new TimexBtn("+ 1 min", C_ADJ_BG, C_ADJ_BD, C_ADJ_FG, false);
        plusBtn.addActionListener(e -> adjustPomo(+60));
        plusBtn.setName("plus");
        pomoPanel.add(plusBtn);

        startBtn = new TimexBtn("start", C_START_BG, C_START_BG, C_START_FG, false);
        startBtn.addActionListener(e -> toggleTimer());
        pomoPanel.add(startBtn);

        TimexBtn resetBtn = new TimexBtn("↺", C_ICO_BG, C_ICO_BD, C_ICO_FG, true);
        resetBtn.addActionListener(e -> resetTimer());
        resetBtn.setName("reset");
        pomoPanel.add(resetBtn);

        root.add(pomoPanel);

        root.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { relayout(); }
        });

        showTab(TAB_CLOCK);
        setContentPane(root);
    }

    private JTextField createTimeField() {
        JTextField tf = new JTextField(2) {
            @Override public Dimension getPreferredSize() {
                FontMetrics fm = getFontMetrics(getFont());
                return new Dimension(fm.stringWidth("00") + 4, fm.getHeight());
            }
        };
        tf.setOpaque(false);
        tf.setBorder(null);
        tf.setForeground(C_TIMER);
        tf.setCaretColor(C_TIMER);
        tf.setHorizontalAlignment(SwingConstants.CENTER);
        
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (!running) SwingUtilities.invokeLater(tf::selectAll);
            }
            @Override public void focusLost(FocusEvent e) {
                syncSecsFromFields();
            }
        });
        
        tf.addActionListener(e -> root.requestFocusInWindow()); 
        
        tf.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if(running || !Character.isDigit(e.getKeyChar())) {
                    e.consume();
                    return;
                }
                if(tf.getText().length() >= 2 && tf.getSelectedText() == null) {
                    e.consume(); 
                }
            }
        });
        return tf;
    }

    private void updateTimeFields() {
        long s = secsLeft;
        hhField.setText(String.format("%02d", s / 3600));
        mmField.setText(String.format("%02d", (s % 3600) / 60));
        ssField.setText(String.format("%02d", s % 60));
    }

    private void syncSecsFromFields() {
        try {
            int h = Integer.parseInt(hhField.getText().trim().isEmpty() ? "0" : hhField.getText().trim());
            int m = Integer.parseInt(mmField.getText().trim().isEmpty() ? "0" : mmField.getText().trim());
            int s = Integer.parseInt(ssField.getText().trim().isEmpty() ? "0" : ssField.getText().trim());
            secsLeft = h * 3600L + m * 60L + s;
            modeSecs[activeMode] = (int) secsLeft;
        } catch (Exception ex) {
            
        }
        updateTimeFields(); 
    }

    private void setFieldsEditable(boolean editable) {
        hhField.setEditable(editable);
        mmField.setEditable(editable);
        ssField.setEditable(editable);
        hhField.setFocusable(editable);
        mmField.setFocusable(editable);
        ssField.setFocusable(editable);
    }

    private void relayout() {
        int W = root.getWidth(), H = root.getHeight();
        if (W < 1 || H < 1) return;

        int tbH   = Math.max(28, (int)(H * 0.09));
        int tbW   = Math.max(70, (int)(W * 0.12));
        int tbY   = 10;
        int tbGap = Math.max(6, (int)(W * 0.012));
        Font tbFont = new Font("Helvetica Neue", Font.BOLD, Math.max(9, tbH / 3));
        tabClock.setScaledFont(tbFont);
        tabPomo.setScaledFont(tbFont);
        tabClock.setBounds(14, tbY, tbW, tbH);
        tabPomo.setBounds(14 + tbW + tbGap, tbY, tbW, tbH);

        int cbSz = Math.max(26, (int)(H * 0.075));
        closeBtn.setFont(new Font("Dialog", Font.PLAIN, Math.max(10, cbSz / 3)));
        closeBtn.setBounds(W - cbSz - 12, tbY + (tbH - cbSz) / 2, cbSz, cbSz);

        int panelY = tbY + tbH + 4;
        int panelH = H - panelY - 6;
        clockPanel.setBounds(0, panelY, W, panelH);
        pomoPanel.setBounds(0, panelY, W, panelH);

        layoutClockPanel(W, panelH);
        layoutPomoPanel(W, panelH);

        root.repaint();
    }

    private void layoutClockPanel(int W, int H) {
        int timeFsz = Math.max(26, (int)(H * 0.32));
        clockTime.setFont(new Font("Helvetica Neue", Font.BOLD, timeFsz));
        int tlH = timeFsz + 8;
        int tlY = (H - tlH) / 2 - (int)(H * 0.08);
        clockTime.setBounds(0, tlY, W, tlH);

        int dayFsz = Math.max(12, (int)(H * 0.11));
        clockDay.setFont(new Font("Helvetica Neue", Font.PLAIN, dayFsz));
        clockDay.setBounds(0, tlY + tlH + 4, W, dayFsz + 6);

        int dateFsz = Math.max(10, (int)(H * 0.08));
        clockDate.setFont(new Font("Helvetica Neue", Font.PLAIN, dateFsz));
        clockDate.setBounds(0, tlY + tlH + dayFsz + 14, W, dateFsz + 6);
    }

    private void layoutPomoPanel(int W, int H) {
        int pillH   = Math.max(24, (int)(H * 0.10));
        int pillW   = Math.max(76, (int)(W * 0.18));
        int pillGap = Math.max(6, (int)(W * 0.013));
        int pillX   = (W - (modePills.length * pillW + (modePills.length - 1) * pillGap)) / 2;
        int pillY   = (int)(H * 0.04);
        Font pillFont = new Font("Helvetica Neue", Font.PLAIN, Math.max(9, pillH / 3 + 1));
        for (PillBtn pb : modePills) {
            pb.setScaledFont(pillFont);
            pb.setBounds(pillX, pillY, pillW, pillH);
            pillX += pillW + pillGap;
        }

        int timerFsz = Math.max(22, (int)(H * 0.28));
        Font timerFont = new Font("Helvetica Neue", Font.BOLD, timerFsz);
        hhField.setFont(timerFont);
        mmField.setFont(timerFont);
        ssField.setFont(timerFont);
        colon1.setFont(timerFont);
        colon2.setFont(timerFont);
        
        int timerH = timerFsz + 10;
        int timerY = pillY + pillH + (int)(H * 0.05);
        pomoTimeContainer.setBounds(0, timerY, W, timerH);
        pomoTimeContainer.revalidate();

        int adjH    = Math.max(22, (int)(H * 0.09));
        int adjW    = Math.max(64, (int)(W * 0.13));
        int adjGap  = Math.max(8, (int)(W * 0.016));
        int adjY    = timerY + timerH + (int)(H * 0.03);
        int adjX    = (W - (adjW * 2 + adjGap)) / 2;
        Font adjFont = new Font("Helvetica Neue", Font.PLAIN, Math.max(9, adjH / 3));
        for (Component c : pomoPanel.getComponents()) {
            if ("minus".equals(c.getName())) {
                ((TimexBtn) c).setScaledFont(adjFont);
                c.setBounds(adjX, adjY, adjW, adjH);
            }
            if ("plus".equals(c.getName())) {
                ((TimexBtn) c).setScaledFont(adjFont);
                c.setBounds(adjX + adjW + adjGap, adjY, adjW, adjH);
            }
        }

        int ctrlH   = Math.max(34, (int)(H * 0.13));
        int startW  = Math.max(100, (int)(W * 0.22));
        int icoSz   = ctrlH;
        int ctrlGap = Math.max(10, (int)(W * 0.02));
        int ctrlX   = (W - (startW + icoSz + ctrlGap)) / 2;
        int ctrlY   = adjY + adjH + (int)(H * 0.04);
        startBtn.setScaledFont(new Font("Helvetica Neue", Font.BOLD, Math.max(10, ctrlH / 3 + 1)));
        startBtn.setBounds(ctrlX, ctrlY, startW, ctrlH);
        for (Component c : pomoPanel.getComponents()) {
            if ("reset".equals(c.getName())) {
                ((TimexBtn) c).setFont(new Font("Dialog", Font.PLAIN, Math.max(10, icoSz / 3) + 2));
                c.setBounds(ctrlX + startW + ctrlGap, ctrlY, icoSz, icoSz);
            }
        }
    }

    private void showTab(int tab) {
        activeTab = tab;
        clockPanel.setVisible(tab == TAB_CLOCK);
        pomoPanel.setVisible(tab == TAB_POMO);
        tabClock.setActive(tab == TAB_CLOCK);
        tabPomo.setActive(tab == TAB_POMO);
        root.repaint();
    }

    private void refreshClock() {
        LocalDateTime now = LocalDateTime.now();
        clockTime.setText(now.format(FMT_TIME));
        clockDay.setText(now.format(FMT_DAY));
        clockDate.setText(now.format(FMT_DATE));
    }

    private void switchMode(int idx) {
        if (running) { countdown.stop(); running = false; }
        activeMode = idx;
        secsLeft   = modeSecs[idx];
        setFieldsEditable(true);
        updateTimeFields();
        startBtn.setText("start");
        for (int i = 0; i < modePills.length; i++) modePills[i].setActive(i == idx);
    }

    private void adjustPomo(int delta) {
        if (running) return;
        secsLeft = Math.max(0, secsLeft + delta);
        modeSecs[activeMode] = (int) secsLeft;
        updateTimeFields();
    }

    private void toggleTimer() {
        if (running) {
            countdown.stop();
            startBtn.setText("start");
            setFieldsEditable(true);
        } else {
            setFieldsEditable(false);
            root.requestFocusInWindow(); 
            countdown = new Timer(1000, e -> {
                if (secsLeft > 0) {
                    secsLeft--;
                    updateTimeFields();
                } else {
                    countdown.stop();
                    running = false;
                    startBtn.setText("start");
                    setFieldsEditable(true);
                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(Timex.this,
                            "Session complete!", "Timex", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            countdown.setInitialDelay(1000);
            countdown.start();
            startBtn.setText("pause");
        }
        running = !running;
    }

    private void resetTimer() {
        if (countdown != null) countdown.stop();
        running  = false;
        secsLeft = modeSecs[activeMode];
        setFieldsEditable(true);
        updateTimeFields();
        startBtn.setText("start");
    }

    class TimexBtn extends JButton {
        private final Color bg, bd, fg;
        private final boolean circle;

        TimexBtn(String text, Color bg, Color bd, Color fg, boolean circle) {
            super(text);
            this.bg = bg; this.bd = bd; this.fg = fg; this.circle = circle;
            setUI(new BasicButtonUI());
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setScaledFont(Font f) { setFont(f); repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int W = getWidth(), H = getHeight();
            Color fill = getModel().isPressed()  ? shift(bg, -30)
                       : getModel().isRollover() ? shift(bg,  20) : bg;
            Shape s = circle
                    ? new Ellipse2D.Double(1, 1, W - 2, H - 2)
                    : new RoundRectangle2D.Double(1, 1, W - 2, H - 2, H - 2, H - 2);
            g2.setColor(fill); g2.fill(s);
            if (!bg.equals(bd)) { g2.setColor(bd); g2.setStroke(new BasicStroke(1.2f)); g2.draw(s); }
            drawCenteredText(g2, getText(), getFont(), fg, W, H);
            g2.dispose();
        }
    }

    class TabBtn extends JButton {
        private boolean active;
        private Font scaledFont = new Font("Helvetica Neue", Font.BOLD, 11);

        TabBtn(String text, boolean active) {
            super(text);
            this.active = active;
            setUI(new BasicButtonUI());
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setActive(boolean a) { this.active = a; repaint(); }
        void setScaledFont(Font f) { this.scaledFont = f; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int W = getWidth(), H = getHeight();
            Shape s = new RoundRectangle2D.Double(1, 1, W - 2, H - 2, H - 2, H - 2);
            if (active) {
                g2.setColor(getModel().isPressed() ? shift(C_TAB_A_BG, -20) : C_TAB_A_BG);
                g2.fill(s);
            } else {
                g2.setColor(getModel().isRollover() ? new Color(255, 255, 255, 46) : C_TAB_I_BG);
                g2.fill(s);
                g2.setColor(C_TAB_I_BD); g2.setStroke(new BasicStroke(1.1f)); g2.draw(s);
            }
            drawCenteredText(g2, getText(), scaledFont, active ? C_TAB_A_FG : C_TAB_I_FG, W, H);
            g2.dispose();
        }
    }

    class PillBtn extends JButton {
        private boolean active;
        private Font scaledFont = new Font("Helvetica Neue", Font.PLAIN, 11);

        PillBtn(String text, boolean active) {
            super(text);
            this.active = active;
            setUI(new BasicButtonUI());
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setActive(boolean a) { this.active = a; repaint(); }
        void setScaledFont(Font f) { this.scaledFont = f; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int W = getWidth(), H = getHeight();
            Shape s = new RoundRectangle2D.Double(1, 1, W - 2, H - 2, H - 2, H - 2);
            if (active) {
                g2.setColor(getModel().isPressed() ? shift(C_PILL_A_BG, -20) : C_PILL_A_BG);
                g2.fill(s);
            } else {
                g2.setColor(getModel().isRollover() ? new Color(255, 255, 255, 42) : C_PILL_I_BG);
                g2.fill(s);
                g2.setColor(C_PILL_I_BD); g2.setStroke(new BasicStroke(1.0f)); g2.draw(s);
            }
            drawCenteredText(g2, getText(), scaledFont, active ? C_PILL_A_FG : C_PILL_I_FG, W, H);
            g2.dispose();
        }
    }

    private static JLabel transparentLabel(String text, int align) {
        JLabel l = new JLabel(text, align);
        l.setOpaque(false);
        return l;
    }

    private static void drawCenteredText(Graphics2D g2, String text, Font f, Color c, int W, int H) {
        g2.setFont(f); g2.setColor(c);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (W - fm.stringWidth(text)) / 2, (H - fm.getHeight()) / 2 + fm.getAscent());
    }

    private static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }

    private static Color shift(Color c, int d) {
        return new Color(clamp(c.getRed() + d), clamp(c.getGreen() + d), clamp(c.getBlue() + d), c.getAlpha());
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private void setupWindowInteractions() {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                pressPoint     = e.getLocationOnScreen();
                pressWinBounds = getBounds();
                resizeDir      = edgeDir(e.getX(), e.getY());
            }
            @Override public void mouseMoved(MouseEvent e) {
                int d = edgeDir(e.getX(), e.getY());
                setCursor(Cursor.getPredefinedCursor(d == -1 ? Cursor.DEFAULT_CURSOR : d));
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (pressPoint == null) return;
                Point cur = e.getLocationOnScreen();
                int dx = cur.x - pressPoint.x, dy = cur.y - pressPoint.y;
                if (resizeDir == -1) {
                    setLocation(pressWinBounds.x + dx, pressWinBounds.y + dy);
                } else {
                    Rectangle r = new Rectangle(pressWinBounds);
                    if (resizeDir == Cursor.E_RESIZE_CURSOR  || resizeDir == Cursor.NE_RESIZE_CURSOR || resizeDir == Cursor.SE_RESIZE_CURSOR) r.width  += dx;
                    if (resizeDir == Cursor.S_RESIZE_CURSOR  || resizeDir == Cursor.SW_RESIZE_CURSOR || resizeDir == Cursor.SE_RESIZE_CURSOR) r.height += dy;
                    if (resizeDir == Cursor.W_RESIZE_CURSOR  || resizeDir == Cursor.NW_RESIZE_CURSOR || resizeDir == Cursor.SW_RESIZE_CURSOR) { r.width -= dx; r.x += dx; }
                    if (resizeDir == Cursor.N_RESIZE_CURSOR  || resizeDir == Cursor.NW_RESIZE_CURSOR || resizeDir == Cursor.NE_RESIZE_CURSOR) { r.height -= dy; r.y += dy; }
                    Dimension mn = getMinimumSize();
                    if (r.width >= mn.width && r.height >= mn.height) setBounds(r);
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                pressPoint = null;
                setCursor(Cursor.getDefaultCursor());
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private int edgeDir(int x, int y) {
        int W = getWidth(), H = getHeight();
        boolean l = x < RZ, r = x > W - RZ, t = y < RZ, b = y > H - RZ;
        if (l && t) return Cursor.NW_RESIZE_CURSOR;
        if (r && t) return Cursor.NE_RESIZE_CURSOR;
        if (l && b) return Cursor.SW_RESIZE_CURSOR;
        if (r && b) return Cursor.SE_RESIZE_CURSOR;
        if (l)      return Cursor.W_RESIZE_CURSOR;
        if (r)      return Cursor.E_RESIZE_CURSOR;
        if (t)      return Cursor.N_RESIZE_CURSOR;
        if (b)      return Cursor.S_RESIZE_CURSOR;
        return -1;
    }

    public static void main(String[] args) {
        System.setProperty("sun.awt.wmclass",             "Timex");
        System.setProperty("sun.java2d.opengl",           "true");
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext",                "true");
        SwingUtilities.invokeLater(Timex::new);
        System.setProperty("sun.java2d.opengl", "true");
    }
}
