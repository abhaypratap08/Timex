import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

public class Analog extends JFrame {
    private static final Color C_BG_TOP    = new Color(17, 23, 38);
    private static final Color C_BG_BOT    = new Color(31, 44, 68);
    private static final Color C_GLOW      = new Color(89, 135, 255, 76);
    private static final Color C_FACE_BG   = new Color(20, 29, 47);
    private static final Color C_FACE_RING = new Color(214, 225, 245, 90);
    private static final Color C_TIMER     = Color.WHITE;
    private static final Color C_SUB       = new Color(199, 212, 232);
    private static final Color C_DIM       = new Color(136, 154, 184);
    private static final Color C_SEC_HAND  = new Color(235, 87, 87); 

    private static final Color C_ICO_BG    = new Color(255, 255, 255,  30);
    private static final Color C_ICO_BD    = new Color(255, 255, 255,  80);
    private static final Color C_ICO_FG    = Color.WHITE;

    private static final DateTimeFormatter FMT_DAY  = DateTimeFormatter.ofPattern("EEEE");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    private JPanel root;
    private AnalogFace analogFace;
    private JLabel clockDay, clockDate;
    private TimexBtn closeBtn;
    private Timer clockTimer;

    private static final int RZ = 7;
    private Point     pressPoint;
    private Rectangle pressWinBounds;
    private int       resizeDir = -1;

    public Analog() {
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                shutdown();
            }
        });
        
        // --- Cross-Platform Floating & Tool Hints ---
        setType(Window.Type.UTILITY);
        setResizable(false);          
        setAlwaysOnTop(true);         
        // --------------------------------------------

        setSize(280, 350); // Slightly taller to fit the analog face + text
        setMinimumSize(new Dimension(220, 280));
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
                paintShell(g2, W, H);
                g2.dispose();
            }
        };
        root.setOpaque(true);

        closeBtn = new TimexBtn("✕", C_ICO_BG, C_ICO_BD, C_ICO_FG, true);
        closeBtn.setFont(new Font("Dialog", Font.PLAIN, 13));
        closeBtn.addActionListener(e -> dispose());
        root.add(closeBtn);

        analogFace = new AnalogFace();
        root.add(analogFace);

        clockDay = transparentLabel("", SwingConstants.CENTER);
        clockDay.setForeground(C_TIMER);
        root.add(clockDay);

        clockDate = transparentLabel("", SwingConstants.CENTER);
        clockDate.setForeground(C_DIM);
        root.add(clockDate);

        root.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { relayout(); }
        });

        setContentPane(root);
    }

    private void relayout() {
        int W = root.getWidth(), H = root.getHeight();
        if (W < 1 || H < 1) return;

        // Close button top right
        int cbSz = Math.max(24, (int)(Math.min(W, H) * 0.08));
        closeBtn.setFont(new Font("Dialog", Font.PLAIN, Math.max(10, cbSz / 3)));
        closeBtn.setBounds(W - cbSz - 12, 12, cbSz, cbSz);

        // Calculate space for labels
        int dayFsz = Math.max(16, (int)(H * 0.058));
        int dateFsz = Math.max(12, (int)(H * 0.042));
        int textH = dayFsz + dateFsz + 24;

        // Make the analog clock perfectly square and centered
        int faceSz = Math.min(W - 40, H - textH - 30);
        int faceX = (W - faceSz) / 2;
        int faceY = 24;
        analogFace.setBounds(faceX, faceY, faceSz, faceSz);

        int textStartY = faceY + faceSz + 14;
        clockDay.setFont(new Font("Helvetica Neue", Font.BOLD, dayFsz));
        clockDay.setBounds(0, textStartY, W, dayFsz + 4);

        clockDate.setFont(new Font("Helvetica Neue", Font.PLAIN, dateFsz));
        clockDate.setBounds(0, textStartY + dayFsz + 6, W, dateFsz + 4);

        root.repaint();
    }

    private void paintShell(Graphics2D g2, int width, int height) {
        g2.setPaint(new GradientPaint(0, 0, C_BG_TOP, 0, height, C_BG_BOT));
        g2.fillRect(0, 0, width, height);

        g2.setColor(C_GLOW);
        g2.fill(new Ellipse2D.Double(-width * 0.22, -height * 0.20, width * 0.92, height * 0.72));
        g2.fill(new Ellipse2D.Double(width * 0.48, height * 0.28, width * 0.55, height * 0.44));

        Shape card = new RoundRectangle2D.Double(10, 10, width - 20, height - 20, 24, 24);
        g2.setColor(new Color(255, 255, 255, 14));
        g2.fill(card);
        g2.setColor(new Color(255, 255, 255, 42));
        g2.setStroke(new BasicStroke(1.1f));
        g2.draw(card);
    }

    private void refreshClock() {
        LocalDateTime now = LocalDateTime.now();
        clockDay.setText(now.format(FMT_DAY));
        clockDate.setText(now.format(FMT_DATE));
        analogFace.repaint();
    }

    private void shutdown() {
        if (clockTimer != null) {
            clockTimer.stop();
        }
        System.exit(0);
    }

    class AnalogFace extends JPanel {
        AnalogFace() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = aa(g);
            
            int W = getWidth();
            int H = getHeight();
            int centerX = W / 2;
            int centerY = H / 2;
            int radius = (Math.min(W, H) / 2) - 5; // Slight padding

            g2.setColor(new Color(0, 0, 0, 34));
            g2.fill(new Ellipse2D.Double(centerX - radius + 3, centerY - radius + 5, 2 * radius, 2 * radius));

            g2.setColor(C_FACE_BG);
            g2.fill(new Ellipse2D.Double(centerX - radius, centerY - radius, 2 * radius, 2 * radius));

            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(C_FACE_RING);
            g2.draw(new Ellipse2D.Double(centerX - radius, centerY - radius, 2 * radius, 2 * radius));
            g2.setStroke(new BasicStroke(1.4f));
            g2.setColor(new Color(255, 255, 255, 38));
            g2.draw(new Ellipse2D.Double(centerX - radius + 7, centerY - radius + 7, 2 * radius - 14, 2 * radius - 14));

            for (int i = 0; i < 60; i++) {
                double radians = Math.toRadians(i * 6 - 90);
                int outer = radius - 9;
                int inner = outer - (i % 5 == 0 ? Math.max(12, radius / 8) : Math.max(6, radius / 15));
                int x1 = (int) (centerX + Math.cos(radians) * outer);
                int y1 = (int) (centerY + Math.sin(radians) * outer);
                int x2 = (int) (centerX + Math.cos(radians) * inner);
                int y2 = (int) (centerY + Math.sin(radians) * inner);
                g2.setColor(i % 5 == 0 ? new Color(255, 255, 255, 120) : new Color(255, 255, 255, 44));
                g2.setStroke(new BasicStroke(i % 5 == 0 ? 2.2f : 1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x1, y1, x2, y2);
            }

            int fontSz = Math.max(10, radius / 5);
            g2.setFont(new Font("Helvetica Neue", Font.BOLD, fontSz));
            g2.setColor(C_SUB);
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString("TIMEX", centerX - (metrics.stringWidth("TIMEX") / 2), centerY - (radius / 3));

            // Calculate angles
            LocalTime now = LocalTime.now();
            double seconds = now.getSecond();
            double minutes = now.getMinute() + seconds / 60.0;
            double hours = (now.getHour() % 12) + minutes / 60.0;

            int hrThick = Math.max(3, radius / 15);
            int minThick = Math.max(2, radius / 25);
            
            drawHand(g2, centerX, centerY, hours * 30, radius * 0.55, C_TIMER, hrThick);   // Hour
            drawHand(g2, centerX, centerY, minutes * 6, radius * 0.8, C_SUB, minThick);    // Minute
            drawHand(g2, centerX, centerY, seconds * 6, radius * 0.85, C_SEC_HAND, 2);     // Second

            g2.setColor(C_TIMER);
            int dotSz = Math.max(6, radius / 12);
            g2.fillOval(centerX - (dotSz/2), centerY - (dotSz/2), dotSz, dotSz);
            
            g2.dispose();
        }

        private void drawHand(Graphics2D g2, int x, int y, double angle, double length, Color color, int thickness) {
            double radians = Math.toRadians(angle - 90); 
            int endX = (int) (x + Math.cos(radians) * length);
            int endY = (int) (y + Math.sin(radians) * length);

            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x, y, endX, endY);
        }
    }

    // --- Timex Custom UI Components & Helpers ---

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

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int W = getWidth(), H = getHeight();
            Color fill = getModel().isPressed()  ? shift(bg, -30)
                       : getModel().isRollover() ? shift(bg,  20) : bg;
            Shape s = circle
                    ? new Ellipse2D.Double(1, 1, W - 2, H - 2)
                    : new RoundRectangle2D.Double(1, 1, W - 2, H - 2, H - 2, H - 2);
            g2.setColor(new Color(0, 0, 0, 26));
            g2.fill(circle
                    ? new Ellipse2D.Double(1, 3, W - 2, H - 2)
                    : new RoundRectangle2D.Double(1, 3, W - 2, H - 2, H - 2, H - 2));
            g2.setColor(fill); g2.fill(s);
            if (!bg.equals(bd)) { g2.setColor(bd); g2.setStroke(new BasicStroke(1.2f)); g2.draw(s); }
            drawCenteredText(g2, getText(), getFont(), fg, W, H);
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
                    if (r.width < mn.width) {
                        if (resizeDir == Cursor.W_RESIZE_CURSOR || resizeDir == Cursor.NW_RESIZE_CURSOR || resizeDir == Cursor.SW_RESIZE_CURSOR) {
                            r.x += (r.width - mn.width);
                        }
                        r.width = mn.width;
                    }
                    if (r.height < mn.height) {
                        if (resizeDir == Cursor.N_RESIZE_CURSOR || resizeDir == Cursor.NW_RESIZE_CURSOR || resizeDir == Cursor.NE_RESIZE_CURSOR) {
                            r.y += (r.height - mn.height);
                        }
                        r.height = mn.height;
                    }
                    setBounds(r);
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                pressPoint = null;
                setCursor(Cursor.getDefaultCursor());
            }
        };
        attachWindowControls(root, ma);
    }

    private void attachWindowControls(Component component, MouseAdapter adapter) {
        component.addMouseListener(adapter);
        component.addMouseMotionListener(adapter);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                attachWindowControls(child, adapter);
            }
        }
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
        SwingUtilities.invokeLater(Analog::new);
        System.setProperty("sun.java2d.opengl", "true");
    }
}
