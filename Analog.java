import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

public class Analog extends JFrame {

    // Same color palette from Timex.java
    private static final Color C_TIMER     = Color.WHITE;
    private static final Color C_SUB       = new Color(185, 195, 210);
    private static final Color C_DIM       = new Color(130, 145, 165);
    private static final Color C_SEC_HAND  = new Color(235, 87, 87); // Soft red for dark mode

    private static final Color C_ICO_BG    = new Color(255, 255, 255,  30);
    private static final Color C_ICO_BD    = new Color(255, 255, 255,  80);
    private static final Color C_ICO_FG    = Color.WHITE;

    private static final DateTimeFormatter FMT_DAY  = DateTimeFormatter.ofPattern("EEEE");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    private JPanel root;
    private AnalogFace analogFace;
    private JLabel clockDay, clockDate;
    private TimexBtn closeBtn;

    private static final int RZ = 7;
    private Point     pressPoint;
    private Rectangle pressWinBounds;
    private int       resizeDir = -1;

    public Analog() {
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
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

        Timer timer = new Timer(1000, e -> refreshClock());
        timer.setInitialDelay(0);
        timer.start();
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

        closeBtn = new TimexBtn("✕", C_ICO_BG, C_ICO_BD, C_ICO_FG, true);
        closeBtn.setFont(new Font("Dialog", Font.PLAIN, 13));
        closeBtn.addActionListener(e -> System.exit(0));
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
        int dayFsz = Math.max(16, (int)(H * 0.06));
        int dateFsz = Math.max(12, (int)(H * 0.045));
        int textH = dayFsz + dateFsz + 20;

        // Make the analog clock perfectly square and centered
        int faceSz = Math.min(W - 40, H - textH - 30);
        int faceX = (W - faceSz) / 2;
        int faceY = 20;
        analogFace.setBounds(faceX, faceY, faceSz, faceSz);

        // Labels at the bottom
        int textStartY = faceY + faceSz + 10;
        clockDay.setFont(new Font("Helvetica Neue", Font.BOLD, dayFsz));
        clockDay.setBounds(0, textStartY, W, dayFsz + 4);

        clockDate.setFont(new Font("Helvetica Neue", Font.PLAIN, dateFsz));
        clockDate.setBounds(0, textStartY + dayFsz + 4, W, dateFsz + 4);

        root.repaint();
    }

    private void refreshClock() {
        LocalDateTime now = LocalDateTime.now();
        clockDay.setText(now.format(FMT_DAY));
        clockDate.setText(now.format(FMT_DATE));
        analogFace.repaint();
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

            // Draw outer dial
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(C_DIM);
            g2.draw(new Ellipse2D.Double(centerX - radius, centerY - radius, 2 * radius, 2 * radius));

            // TIMEX Label
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

            // Draw Hands (dynamically scaled)
            int hrThick = Math.max(3, radius / 15);
            int minThick = Math.max(2, radius / 25);
            
            drawHand(g2, centerX, centerY, hours * 30, radius * 0.55, C_TIMER, hrThick);   // Hour
            drawHand(g2, centerX, centerY, minutes * 6, radius * 0.8, C_SUB, minThick);    // Minute
            drawHand(g2, centerX, centerY, seconds * 6, radius * 0.85, C_SEC_HAND, 2);     // Second

            // Center pivot dot
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
        SwingUtilities.invokeLater(Analog::new);
    }
}import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

public class Analog extends JFrame {

    public Analog() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Timex Analog Clock");
        this.setResizable(false);
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);

        AnalogFace analogFace = new AnalogFace();
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 20, 10));

        JLabel dayLabel = new JLabel();
        dayLabel.setFont(new Font("Ink Free", Font.BOLD, 22));
        dayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel dateLabel = new JLabel();
        dateLabel.setFont(new Font("Ink Free", Font.BOLD, 18));
        dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        infoPanel.add(dayLabel);
        infoPanel.add(dateLabel);

        container.add(analogFace, BorderLayout.CENTER);
        container.add(infoPanel, BorderLayout.SOUTH);

        this.add(container);
        this.pack(); 
        this.setLocationRelativeTo(null);
        this.setVisible(true);

        Timer timer = new Timer(1000, e -> {
            analogFace.repaint();
            dayLabel.setText(LocalTime.now().atDate(java.time.LocalDate.now()).format(DateTimeFormatter.ofPattern("EEEE")));
            dateLabel.setText(LocalTime.now().atDate(java.time.LocalDate.now()).format(DateTimeFormatter.ofPattern("dd MMMM, yyyy")));
        });
        timer.start();
    }

    class AnalogFace extends JPanel {
        AnalogFace() {
            this.setPreferredSize(new Dimension(400, 400));
            this.setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int radius = 160;

            g2.setStroke(new BasicStroke(5));
            g2.setColor(Color.DARK_GRAY);
            g2.drawOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);



          //----label---------
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.setColor(Color.BLACK);
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString("TIMEX", centerX - (metrics.stringWidth("TIMEX") / 2), centerY - 60);
        //------added later onn---------
          
            LocalTime now = LocalTime.now();
            double seconds = now.getSecond();
            double minutes = now.getMinute() + seconds / 60.0;
            double hours = (now.getHour() % 12) + minutes / 60.0;

            drawHand(g2, centerX, centerY, hours * 30, radius * 0.5, Color.BLACK, 7); // Hr Hand
            drawHand(g2, centerX, centerY, minutes * 6, radius * 0.8, Color.BLACK, 4);  // Mins Hand
            drawHand(g2, centerX, centerY, seconds * 6, radius * 0.9, Color.RED, 2);    // Sec Hand

            g2.setColor(Color.DARK_GRAY);
            g2.fillOval(centerX - 6, centerY - 6, 12, 12);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Analog());
    }
}
