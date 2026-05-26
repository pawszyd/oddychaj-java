import javax.swing.*;

/**
 * Klasa zarządzająca głównym oknem aplikacji.
 * Konfiguruje ramkę (JFrame), ustawia jej wymiary i osadza w niej panel gry.
 */

public class GameWindow {
    private JFrame jframe;
    public GameWindow(GamePanel gamePanel) {
        jframe = new JFrame();

        jframe.setSize(1280, 720);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.setLocationRelativeTo(null);
        jframe.add(gamePanel);
        jframe.setVisible(true);
    }

}
