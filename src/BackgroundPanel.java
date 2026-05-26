import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Niestandardowy panel (rozszerzenie JPanel), który umożliwia ustawienie
 * skalowalnego obrazu jako tła.
 * W przypadku błędu ładowania pliku graficznego, panel automatycznie
 * ustawia białe tło jako domyślne.
 */

public class BackgroundPanel extends JPanel {
    private Image backgroundImage;

    public BackgroundPanel(String imagePath) {
        try{
            backgroundImage = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            System.err.println("Błąd ładowania obrazu tła: " + e.getMessage());
            e.printStackTrace();
            this.setBackground(Color.WHITE);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            // Zmiana: dodajemy getWidth() i getHeight() do skalowania
            g.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), this);
        }
    }

}
