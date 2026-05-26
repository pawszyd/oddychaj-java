/**
 * Główna klasa inicjalizująca strukturę gry.
 * Odpowiada za utworzenie panelu logiki oraz osadzenie go w oknie aplikacji.
 */

public class Game {

    private GameWindow gameWindow;
    private GamePanel gamePanel;

    public Game() {
        gamePanel = new GamePanel();
        gameWindow = new GameWindow(gamePanel);

    }
}
