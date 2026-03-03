import java.awt.*;
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
