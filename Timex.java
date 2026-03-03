import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

public class Timex extends JFrame {
    
    private JLabel timeLabel;
    private JLabel dayLabel;
    private JLabel dateLabel;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM, yyyy");

    public Timex() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Digital Clock");
        this.setLayout(new FlowLayout());
        this.setSize(350, 220);
        this.setResizable(false);

        timeLabel = new JLabel();
        timeLabel.setFont(new Font("SANS_SERIF", Font.PLAIN, 59));
        timeLabel.setBackground(Color.BLACK);
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setOpaque(true);

        dayLabel = new JLabel();
        dayLabel.setFont(new Font("Ink Free", Font.BOLD, 34));

        dateLabel = new JLabel();
        dateLabel.setFont(new Font("Ink Free", Font.BOLD, 30));

        this.add(timeLabel);
        this.add(dayLabel);
        this.add(dateLabel);
        startClock();

        this.setLocationRelativeTo(null); 
        this.setVisible(true);
    }

    private void startClock() {
        Timer timer = new Timer(1000, e -> updateTime());
        timer.start();
        updateTime(); 
    }

    private void updateTime() {
        LocalDateTime now = LocalDateTime.now();
        timeLabel.setText(now.format(timeFormatter));
        dayLabel.setText(now.format(dayFormatter));
        dateLabel.setText(now.format(dateFormatter));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Timex());
    }
}
