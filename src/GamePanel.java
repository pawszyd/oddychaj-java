import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.IOException;

/**
 * Główny panel gry obsługujący logikę, minigry oraz wyświetlanie scen.
 */
public class GamePanel extends JPanel {

    public static final String MENU_VIEW = "Menu View";
    public static final String GAME_VIEW = "Game View";

    // Zarządzanie widokami
    private CardLayout mainCardLayout = new CardLayout();
    private JPanel menuPanel;
    private JPanel gameRootPanel;

    // Warstwa tła i fabuły
    private CardLayout backgroundCardLayout = new CardLayout();
    private JPanel backgroundPanel;
    private JTextArea storyTextArea;
    private FadingImagePanel fadingImagePanel;

    // UI i komponenty pomocnicze
    private JPanel uiPanel;
    private JPanel buttonContainer;
    private JPanel statsPanel;
    private Timer timer;
    private javax.sound.sampled.Clip musicClip;

    // Stan gry i statystyki
    private boolean statsUnlocked = false;
    private int statMental = 100;
    private int statEnergy = 100;
    private int statSocial = 100;

    private JProgressBar barMental;
    private JProgressBar barEnergy;
    private JProgressBar barSocial;

    // Minigra 1: QTE (Kodowanie)
    private java.awt.event.KeyListener qteKeyListener;
    private javax.swing.Timer qteTimer;
    private int qteClicks = 0;
    private int savedQteScore = 0;

    // Minigra 2: Focus (Egzaminy/Leki)
    private javax.swing.Timer focusTimer;
    private java.awt.event.KeyListener focusKeyListener;
    private int focusPosition = 0;
    private int focusDirection = 1;
    private int focusSpeed = 2;
    private boolean savedFocusSuccess = false;

    /**
     * Inicjalizuje widoki i uruchamia muzykę w tle.
     */
    public GamePanel() {
        this.setLayout(mainCardLayout);

        menuPanel = createMenuPanel();
        gameRootPanel = createGameRootPanel();

        this.add(menuPanel, MENU_VIEW);
        this.add(gameRootPanel, GAME_VIEW);

        mainCardLayout.show(this, MENU_VIEW);
    }

    /**
     * Przełącza widok między menu a grą.
     */
    public void showPanel(String viewName) {
        mainCardLayout.show(this, viewName);
    }

    private JPanel createGameRootPanel() {
        JPanel root = new JPanel();
        root.setLayout(new OverlayLayout(root));

        // Warstwa interfejsu
        uiPanel = new JPanel(new BorderLayout());
        uiPanel.setOpaque(false);

        statsPanel = createStatsPanel();
        statsPanel.setVisible(false);
        uiPanel.add(statsPanel, BorderLayout.NORTH);

        uiPanel.add(createTextComponent(), BorderLayout.CENTER);

        buttonContainer = new JPanel();
        buttonContainer.setOpaque(false);
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        buttonContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        buttonContainer.setPreferredSize(new Dimension(100, 80));
        uiPanel.add(buttonContainer, BorderLayout.SOUTH);

        root.add(uiPanel);

        // Warstwa tła
        backgroundPanel = new JPanel(backgroundCardLayout);

        JPanel whitePanel = new JPanel();
        whitePanel.setBackground(Color.WHITE);
        backgroundPanel.add(whitePanel, "TEXT");

        fadingImagePanel = new FadingImagePanel();
        backgroundPanel.add(fadingImagePanel, "IMAGE");

        root.add(backgroundPanel);

        playBackgroundMusic();

        return root;
    }

    private JScrollPane createTextComponent() {
        storyTextArea = new JTextArea();
        storyTextArea.setOpaque(false);
        storyTextArea.setBackground(new Color(255, 255, 255, 200));
        storyTextArea.setForeground(Color.BLACK);
        storyTextArea.setFont(new Font("Arial", Font.PLAIN, 22));
        storyTextArea.setLineWrap(true);
        storyTextArea.setWrapStyleWord(true);
        storyTextArea.setEditable(false);
        storyTextArea.setCaretColor(new Color(0, 0, 0, 0));
        storyTextArea.setHighlighter(null);
        storyTextArea.setMargin(new Insets(50, 100, 50, 100));

        JScrollPane scroll = new JScrollPane(storyTextArea);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        return scroll;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        panel.setOpaque(false);

        JPanel verticalBox = new JPanel();
        verticalBox.setLayout(new BoxLayout(verticalBox, BoxLayout.Y_AXIS));
        verticalBox.setOpaque(false);

        barMental = createSingleBar("Zdrowie psychiczne", new Color(136, 0, 21));
        barSocial = createSingleBar("Relacje społeczne",  new Color(191, 166, 162));
        barEnergy = createSingleBar("Energia",  new Color(243, 238, 217));

        verticalBox.add(barMental);
        verticalBox.add(Box.createVerticalStrut(5));
        verticalBox.add(barSocial);
        verticalBox.add(Box.createVerticalStrut(5));
        verticalBox.add(barEnergy);

        panel.add(verticalBox);
        return panel;
    }

    private JProgressBar createSingleBar(String title, Color color) {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(100);
        bar.setStringPainted(true);
        bar.setString(title + ": 100%");
        bar.setForeground(color);
        bar.setBackground(Color.DARK_GRAY);
        bar.setPreferredSize(new Dimension(250, 150));
        bar.setBorderPainted(false);

        // Nadpisanie UI dla płaskiego wyglądu
        bar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override protected Color getSelectionBackground() { return Color.BLACK; }
            @Override protected Color getSelectionForeground() { return Color.BLACK; }

            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g;
                int w = c.getWidth();
                int h = c.getHeight();

                g2d.setColor(c.getBackground());
                g2d.fillRect(0, 0, w, h);

                int amountFull = (int) Math.round(((double) progressBar.getValue() / progressBar.getMaximum()) * w);
                g2d.setColor(c.getForeground());
                g2d.fillRect(0, 0, amountFull, h);

                if (progressBar.isStringPainted()) {
                    paintString(g, 0, 0, w, h, amountFull, new Insets(0, 0, 0, 0));
                }
            }
        });
        return bar;
    }

    private void setButtons(JButton... buttons) {
        buttonContainer.removeAll();
        for (JButton btn : buttons) {
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(500, 60));
            btn.setPreferredSize(new Dimension(500, 60));
            buttonContainer.add(btn);
            buttonContainer.add(Box.createVerticalStrut(10));
        }
        buttonContainer.revalidate();
        buttonContainer.repaint();
        if (gameRootPanel != null) gameRootPanel.repaint();
    }

    private void showTextMode(String text, Runnable onTextComplete) {
        backgroundCardLayout.show(backgroundPanel, "TEXT");
        if (statsPanel != null) statsPanel.setVisible(false);

        buttonContainer.removeAll();
        buttonContainer.repaint();

        storyTextArea.setForeground(Color.BLACK);
        storyTextArea.setBackground(new Color(255, 255, 255, 255));

        typeText(text, onTextComplete);
    }

    private void showImageMode(String imagePath, Runnable onFadeComplete) {
        backgroundCardLayout.show(backgroundPanel, "IMAGE");
        if (statsPanel != null) statsPanel.setVisible(statsUnlocked);

        buttonContainer.removeAll();
        buttonContainer.repaint();

        storyTextArea.setText("");
        storyTextArea.setForeground(new Color(136, 0, 21));
        storyTextArea.setBackground(new Color(255, 255, 255, 180));

        fadingImagePanel.setImage(imagePath);
        fadingImagePanel.startFadeIn(onFadeComplete);
    }

    /**
     * Modyfikuje wartości statystyk i odświeża interfejs.
     */
    public void modifyStats(int mentalChange, int energyChange, int socialChange) {
        statMental = Math.max(0, Math.min(100, statMental + mentalChange));
        statEnergy = Math.max(0, Math.min(100, statEnergy + energyChange));
        statSocial = Math.max(0, Math.min(100, statSocial + socialChange));

        updateBar(barMental, "Zdrowie psychiczne", statMental);
        updateBar(barEnergy, "Energia", statEnergy);
        updateBar(barSocial, "Relacje społeczne", statSocial);
    }

    private void updateBar(JProgressBar bar, String name, int value) {
        bar.setValue(value);
        bar.setString(name + ": " + value + "%");
    }

    /**
     * Rozpoczyna nową grę i resetuje stan.
     */
    public void startNewGame() {
        statMental = 100;
        statEnergy = 100;
        statSocial = 100;
        modifyStats(0, 0, 0);
        statsUnlocked = false;

        showPanel(GAME_VIEW);

        showTextMode(
                "[1 PAŹDZIERNIKA]\n" +
                        "\nKiedy otworzyłeś list z wynikami rekrutacji na Politechnikę, serce zabiło Ci mocniej... \n " +
                        "\n...\n"+
                        "\nUdało się! Oficjalnie jesteś studentem.\n" +
                        "\nCzujesz niesamowitą dumę i radość – to początek nowego, ekscytującego rozdziału w Twoim życiu." +
                        "\nPrzed Tobą świat pełen możliwości, ambitnych projektów i nowych przyjaźni." +
                        "\nW tej chwili wydaje Ci się, że możesz przenosić góry. Pamiętaj jednak, że ta podróż to maraton, a nie sprint." +
                        "\n\n\n\nCiesz się tą chwilą, ale nie zapominaj... oddychać.",

                () -> {
                    JButton continueButton = createChoiceButton("Dalej");
                    continueButton.addActionListener(e -> playSceneTwo_Image());
                    setButtons(continueButton);
                }
        );
    }

    /** Scena 2: Wprowadzenie graficzne. */
    public void playSceneTwo_Image() {
        showImageMode("res/scene1.jpg", () -> {
            JButton continueButton = createChoiceButton("Dalej");
            continueButton.addActionListener(e -> playSceneThree_Text());
            setButtons(continueButton);
        });
    }

    /** Scena 3: Opis dylematu naukowego. */
    public void playSceneThree_Text() {
        showTextMode(
                "[9 LISTOPADA]\n" +
                        "\n" +
                        "Październikowy optymizm zniknął pod stosem notatek, a nadchodzące kolokwium staje się pierwszą poważną weryfikacją Twoich marzeń." +
                        "\n" +
                        "\nUcisk w klatce piersiowej to narastający stres, który podpowiada Ci, że Twoja wiedza może nie wystarczyć, by sprostać temu wyzwaniu.\n" +
                        "\n" +
                        "\nStoisz przed trudnym wyborem: kolejna godzina nauki kosztem energii, czy krótki sen, by przetrwać jutrzejszy dzień?" +
                        "\n\n\nKażda decyzja wpłynie na to, jak poradzisz sobie na sali egzaminacyjnej.",

                () -> {
                    JButton continueButton = createChoiceButton("Dalej");
                    continueButton.addActionListener(e -> playSceneFour_Choices());
                    setButtons(continueButton);
                }
        );
    }

    /** Scena 4: Wybór (Nauka vs Sen). */
    public void playSceneFour_Choices() {
        statsUnlocked = true;

        showImageMode("res/scene2.jpg", () -> {
            JButton choice1 = createChoiceButton("Ucz się do późna");
            choice1.addActionListener(e -> {
                modifyStats(-10, -30, 0);
                typeText("Siedzisz nad książkami do świtu. Oczy cię pieką, a głowa pęka.",
                        () -> {
                            JButton nextBtn = createChoiceButton("Dalej");
                            nextBtn.addActionListener(ev -> playSceneFive_Morning());
                            setButtons(nextBtn);
                        });
                buttonContainer.removeAll(); buttonContainer.repaint();
            });

            JButton choice2 = createChoiceButton("Pójdź spać");
            choice2.addActionListener(e -> {
                modifyStats(0, 0, 0);
                typeText("Odkładasz notatki. Sen jest teraz ważniejszy.\n" +
                                "Zasypiasz natychmiast, ignorując stertę książek na biurku.",
                        () -> {
                            JButton nextBtn = createChoiceButton("Dalej");
                            nextBtn.addActionListener(ev -> playSceneFive_Morning());
                            setButtons(nextBtn);
                        });
                buttonContainer.removeAll(); buttonContainer.repaint();
            });
            setButtons(choice1, choice2);
        });
    }

    public void playSceneFive_Morning() {
        showImageMode("res/scene3.jpg", () -> {
            typeText("", () -> {
                JButton nextBtn = createChoiceButton("Dalej");
                nextBtn.addActionListener(e -> playSceneSix_University());
                setButtons(nextBtn);
            });
        });
    }

    public void playSceneSix_University() {
        showTextMode(
                "[26 LISTOPADA]\n" +
                        "\n" +
                        "Tempo na politechnice nie zwalnia, a Twój organizm coraz głośniej domaga się przerwy." +
                        "\n" +
                        "\nTelefon wibruje od powiadomień, znajomi wyciągają Cię na imprezę, obiecując reset, którego tak bardzo potrzebujesz.\n" +
                        "\n" +
                        "\nMusisz zdecydować: wybierasz samotną naukę do kolokwium czy wyjście ze znajomymi?",

                () -> {
                    JButton nextBtn = createChoiceButton("Dalej");
                    nextBtn.addActionListener(e -> playSceneSeven_Image());
                    setButtons(nextBtn);
                }
        );
    }

    public void playSceneSeven_Image() {
        showImageMode("res/scene4.jpg", () -> {
            JButton nextBtn = createChoiceButton("Dalej");
            nextBtn.addActionListener(e -> playSceneEight_Choices());
            setButtons(nextBtn);
        });
    }

    /** Scena 8: Wybór (Impreza vs Nauka). */
    public void playSceneEight_Choices() {
        showImageMode("res/scene5.jpg", () -> {
            // Decyzja: Zostaję w domu
            JButton choice1 = createChoiceButton("Niestety nie mogę");
            choice1.addActionListener(e -> {
                modifyStats(-10, 0, -50);
                typeText("Zamykasz drzwi na klucz. Wybierasz samotność.\n",
                        () -> {
                            JButton nextBtn = createChoiceButton("Dalej");
                            nextBtn.addActionListener(ev -> playSceneNine_Happy());
                            setButtons(nextBtn);
                        });
                buttonContainer.removeAll(); buttonContainer.repaint();
            });

            // Decyzja: Wychodzę
            JButton choice2 = createChoiceButton("Tak");
            choice2.addActionListener(e -> {
                modifyStats(-20, -20, 10);
                typeText("Wychodzisz do ludzi. Śmiech, głośna muzyka, rozmowy.\n" +
                                "Baterie społeczne naładowane, ale w środku czujesz dziwny niepokój.",
                        () -> {
                            JButton nextBtn = createChoiceButton("Dalej");
                            nextBtn.addActionListener(ev -> playSceneNine_Sad());
                            setButtons(nextBtn);
                        });
                buttonContainer.removeAll(); buttonContainer.repaint();
            });
            setButtons(choice1, choice2);
        });
    }

    public void playSceneNine_Sad() {
        showImageMode("res/scene_sad.jpg", () -> {
            typeText("Twoi nowi przyjaciele polubili cie jeszcze bardziej ale zawaliłeś kolokwium :(",
                    () -> {
                        JButton nextBtn = createChoiceButton("Dalej");
                        nextBtn.addActionListener(e -> playSceneTen_ProjectsIntro());
                        setButtons(nextBtn);
                    });
        });
    }

    public void playSceneNine_Happy() {
        showImageMode("res/scene_happy.jpg", () -> {
            typeText("Mimo utraconych okazji, czujesz, że to była dobra decyzja. Kolokwium dobrze ci poszło.",
                    () -> {
                        JButton nextBtn = createChoiceButton("Dalej");
                        nextBtn.addActionListener(e -> playSceneTen_ProjectsIntro());
                        setButtons(nextBtn);
                    });
        });
    }

    public void playSceneTen_ProjectsIntro() {
        showTextMode(
                "[15 GRUDNIA]\n" +
                        "\n" +
                        "Terminy oddania projektów depczą Ci po piętach, a przed Tobą maraton pisania kodu." +
                        "\n\nMusisz pracować szybko i rytmicznie, by wygrać wyścig z czasem i narastającym zmęczeniem.",

                () -> {
                    JButton nextBtn = createChoiceButton("Zacznij kodować!");
                    nextBtn.addActionListener(e -> playSceneEleven_CodingQTE());
                    setButtons(nextBtn);
                }
        );
    }

    /**
     * Uruchamia minigrę QTE (wciskanie klawisza 'K').
     */
    public void playSceneEleven_CodingQTE() {
        qteClicks = 0;

        showImageMode("res/scene6.jpg", () -> {
            storyTextArea.setText("");
            storyTextArea.setBackground(new Color(0, 0, 0, 0));

            buttonContainer.removeAll();
            buttonContainer.setPreferredSize(null);

            JLabel timeLabel = new JLabel("CZAS: 10.0 s");
            timeLabel.setFont(new Font("Arial", Font.BOLD, 30));
            timeLabel.setForeground(new Color(136, 0, 21));
            timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JProgressBar codingBar = new JProgressBar(0, 100);
            codingBar.setValue(0);
            codingBar.setStringPainted(true);
            codingBar.setString("Postęp kodu: 0%");
            codingBar.setPreferredSize(new Dimension(600, 50));
            codingBar.setMaximumSize(new Dimension(600, 50));
            codingBar.setForeground(new Color(136, 0, 21));
            codingBar.setBorderPainted(false);

            buttonContainer.add(Box.createVerticalStrut(10));
            buttonContainer.add(timeLabel);
            buttonContainer.add(Box.createVerticalStrut(10));
            buttonContainer.add(codingBar);
            buttonContainer.add(Box.createVerticalStrut(20));

            buttonContainer.revalidate();
            buttonContainer.repaint();
            if (uiPanel != null) {
                uiPanel.revalidate();
                uiPanel.repaint();
            }

            this.setFocusable(true);
            this.requestFocusInWindow();

            qteKeyListener = new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent evt) {
                    if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_K) {
                        qteClicks++;
                        int clicksNeeded = 70; // Trudność
                        int progress = Math.min(100, (qteClicks * 100) / clicksNeeded);
                        codingBar.setValue(progress);
                        codingBar.setString("Postęp kodu: " + progress + "%");
                    }
                }
            };
            this.addKeyListener(qteKeyListener);

            long startTime = System.currentTimeMillis();
            qteTimer = new javax.swing.Timer(50, timeEvt -> {
                long now = System.currentTimeMillis();
                double elapsed = (now - startTime) / 1000.0;
                double remaining = 10.0 - elapsed;

                if (remaining <= 0) {
                    timeLabel.setText("KONIEC");
                    finishQTE(codingBar.getValue());
                } else {
                    timeLabel.setText(String.format("CZAS: %.1f s", remaining));
                }
            });
            qteTimer.start();
        });
    }

    private void finishQTE(int finalScore) {
        this.savedQteScore = finalScore;
        if (qteTimer != null) qteTimer.stop();
        this.removeKeyListener(qteKeyListener);

        buttonContainer.setPreferredSize(new Dimension(100, 180));

        javax.swing.Timer delay = new javax.swing.Timer(1500, e -> {
            String resultText;
            if (finalScore >= 90) {
                resultText = "NIESAMOWITE! Twój kod jest perfekcyjny. Grupa jest pod wrażeniem.";
                modifyStats(10, -20, 20);
            } else if (finalScore >= 50) {
                resultText = "Udało się napisać działający prototyp. Nie jest piękny, ale działa.";
                modifyStats(0, -20, 5);
            } else {
                resultText = "Panika wzięła górę. Kod się nie kompiluje, a deadline minął.";
                modifyStats(-20, -20, -10);
            }

            showTextMode(
                    "[WYNIK: " + finalScore + "%]\n\n" +
                            resultText + "\n\n" +
                            "\n\n\nCzas projektów się zakończył, trzeba zacząć się przygotowywać do sesji zimowej.",
                    () -> {
                        JButton nextBtn = createChoiceButton("Dalej");
                        nextBtn.addActionListener(ev -> playSceneTwelve_WinterImage());
                        setButtons(nextBtn);
                    }
            );
        });
        delay.setRepeats(false);
        delay.start();
    }

    public void playSceneTwelve_WinterImage() {
        showImageMode("res/scene7.jpg", () -> {
            storyTextArea.setText("");
            storyTextArea.setBackground(new Color(0, 0, 0, 0));
            JButton nextBtn = createChoiceButton("Dalej");
            nextBtn.addActionListener(ev -> playSceneThirteen_WinterText());
            setButtons(nextBtn);
        });
    }

    public void playSceneThirteen_WinterText() {
        showTextMode(
                "[28 STYCZNIA]\n\n" +
                        "Zimowa sesja egzaminacyjna to czas największej presji i niekończącej się nauki." +
                        "\nTwój wycieńczony organizm w końcu się poddaje, a Ty zapadasz na chorobę w samym środku najważniejszych egzaminów." +
                        "\n\n\nAby nie zaprzepaścić całego semestru, musisz teraz zadbać o siebie i przyjmować leki...",

                () -> {
                    JButton endBtn = createChoiceButton("Przyjmuj leki regularnie");
                    endBtn.addActionListener(e -> playSceneFourteen_FocusGame());
                    setButtons(endBtn);
                }
        );
    }

    /**
     * Uruchamia minigrę Focus (zręcznościowa z paskiem).
     */
    public void playSceneFourteen_FocusGame() {
        focusPosition = 0;
        focusDirection = 1;
        focusSpeed = 3;

        showImageMode("res/scene8.jpg", () -> {
            storyTextArea.setText("");
            storyTextArea.setBackground(new Color(0, 0, 0, 0));
            buttonContainer.removeAll();
            buttonContainer.setPreferredSize(null);

            JLabel infoLabel = new JLabel("");
            infoLabel.setFont(new Font("Arial", Font.BOLD, 20));
            infoLabel.setForeground(Color.RED);
            infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Niestandardowe rysowanie panelu
            JPanel focusBarPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    int w = getWidth();
                    int h = getHeight();

                    g2.setColor(Color.BLACK);
                    g2.fillRect(0, 0, w, h);

                    int targetX = (int)(w * 0.40);
                    int targetW = (int)(w * 0.20);
                    g2.setColor(new Color(136, 0, 21));
                    g2.fillRect(targetX, 0, targetW, h);

                    int cursorX = (int)((focusPosition / 100.0) * w);
                    g2.setColor(new Color(3, 133, 223));
                    g2.setStroke(new BasicStroke(5));
                    g2.drawLine(cursorX, 0, cursorX, h);
                }
            };

            focusBarPanel.setPreferredSize(new Dimension(600, 60));
            focusBarPanel.setMaximumSize(new Dimension(600, 60));
            focusBarPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

            buttonContainer.add(Box.createVerticalStrut(30));
            buttonContainer.add(infoLabel);
            buttonContainer.add(Box.createVerticalStrut(20));
            buttonContainer.add(focusBarPanel);

            buttonContainer.revalidate();
            buttonContainer.repaint();
            if (uiPanel != null) { uiPanel.revalidate(); uiPanel.repaint(); }

            this.setFocusable(true);
            this.requestFocusInWindow();

            focusTimer = new javax.swing.Timer(16, e -> {
                focusPosition += (focusSpeed * focusDirection);
                if (focusPosition >= 100) {
                    focusPosition = 100;
                    focusDirection = -1;
                } else if (focusPosition <= 0) {
                    focusPosition = 0;
                    focusDirection = 1;
                }
                focusBarPanel.repaint();
            });
            focusTimer.start();

            focusKeyListener = new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent evt) {
                    if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_X) {
                        checkFocusHit();
                    }
                }
            };
            this.addKeyListener(focusKeyListener);
        });
    }

    private void checkFocusHit() {
        focusTimer.stop();
        this.removeKeyListener(focusKeyListener);

        boolean success = (focusPosition >= 40 && focusPosition <= 60);
        this.savedFocusSuccess = success;

        buttonContainer.setPreferredSize(new Dimension(100, 180));

        javax.swing.Timer delay = new javax.swing.Timer(1000, e -> {
            String resultText;
            if (success) {
                resultText = "Przyjmowałeś leki regularnie. Sesja dobrze ci poszła.";
                modifyStats(10, -10, 5);
            } else {
                resultText = "Nie przyjmowałeś leków i choroba cię pokonała. Sesja słabo ci poszła.";
                modifyStats(-40, -20, -10);
            }

            showTextMode(
                    (success ? "[SUKCES]" : "[PORAŻKA]") + "\n\n" + resultText,
                    () -> {
                        JButton nextBtn = createChoiceButton("Dalej");
                        nextBtn.addActionListener(ev -> {
                            if (success) playSceneFifteen_Happy();
                            else playSceneFifteen_Sad();
                        });
                        setButtons(nextBtn);
                    }
            );
        });
        delay.setRepeats(false);
        delay.start();
    }

    public void playSceneFifteen_Happy() {
        showImageMode("res/scene_happy2.jpg", () -> {
            storyTextArea.setText("");
            storyTextArea.setBackground(new Color(0, 0, 0, 0));
            JButton nextBtn = createChoiceButton("Dalej");
            nextBtn.addActionListener(ev -> playSceneSixteen_NextChapter());
            setButtons(nextBtn);
        });
    }

    public void playSceneFifteen_Sad() {
        showImageMode("res/scene_sad2.jpg", () -> {
            storyTextArea.setText("");
            storyTextArea.setBackground(new Color(0, 0, 0, 0));
            JButton nextBtn = createChoiceButton("Dalej");
            nextBtn.addActionListener(ev -> playSceneSixteen_NextChapter());
            setButtons(nextBtn);
        });
    }

    /**
     * Finałowe podsumowanie semestru i wybór zakończenia.
     */
    public void playSceneSixteen_NextChapter() {
        int psychika = barMental.getValue();
        int energia = barEnergy.getValue();
        int relacje = barSocial.getValue();

        // Logika zakończeń
        boolean isBadEnding = (!savedFocusSuccess || psychika < 20 || energia < 10);
        boolean isGoodEnding = (!isBadEnding && psychika > 60 && relacje > 50 && savedQteScore >= 50);

        StringBuilder summary = new StringBuilder();

        if (isBadEnding) {
            summary.append("[ZAWIADOMIENIE O SKREŚLENIU Z LISTY STUDENTÓW]\n\n");
            summary.append("Niestety, ten semestr okazał się barierą nie do przejścia. Poniższy raport wskazuje przyczyny tej decyzji:\n\n");
        } else {
            summary.append("[RAPORT KOŃCOWY SEMESTRU]\n\n");
            summary.append("Dziekanat przeanalizował Twoje wyniki. Oto szczegółowy feedback:\n\n");
        }

        // Sekcja 1: Minigry
        summary.append(">> WYNIKI ZADAŃ:\n");

        if (savedQteScore >= 90) {
            summary.append("[+] PROJEKT GRUPOWY: CELUJĄCY (" + savedQteScore + "%).\n");
            summary.append("   Kod był czysty (Clean Code) i zoptymalizowany. Zespół był pod wrażeniem Twojej szybkości.\n");
        } else if (savedQteScore >= 50) {
            summary.append("[+] PROJEKT GRUPOWY: ZALICZONY (" + savedQteScore + "%).\n");
            summary.append("   Aplikacja działa, choć kod to trochę 'spaghetti'. Wymaga refaktoryzacji, ale dowieziono go na czas.\n");
        } else {
            summary.append("[-] PROJEKT GRUPOWY: NIEZALICZONY (" + savedQteScore + "%).\n");
            summary.append("   Deadline Cię pokonał. Kod się nie kompiluje. W IT niedowiezienie produktu to błąd krytyczny.\n");
        }

        if (savedFocusSuccess) {
            summary.append("[+] EGZAMINY TEORETYCZNE: ZALICZONE.\n");
            summary.append("   Dzięki regularnemu leczeniu zachowałeś jasność umysłu. Wiedza wchodziła do głowy bez problemu.\n\n");
        } else {
            summary.append("[-] EGZAMINY TEORETYCZNE: OBLANE (BRAK SKUPIENIA).\n");
            summary.append("   Zlekceważenie leków miało swoje skutki. 'Mgła mózgowa' i stres uniemożliwiły rozwiązanie zadań.\n");
            summary.append("   Choroba wygrała z ambicjami. To główny powód Twoich problemów na uczelni.\n\n");
        }

        // Sekcja 2: Statystyki
        summary.append(">> PROFIL PSYCHOFIZYCZNY:\n");

        if (psychika < 30) {
            summary.append("! STAN PSYCHICZNY: KRYTYCZNY.\n");
            summary.append("   Balansujesz na krawędzi załamania. Ignorowanie stresu to droga do wypalenia zawodowego.\n");
        } else {
            summary.append("+ STAN PSYCHICZNY: STABILNY. Potrafisz oddzielić stres uczelniany od życia prywatnego.\n");
        }

        if (energia < 20) {
            summary.append("! POZIOM ENERGII: WYCZERPANIE.\n");
            summary.append("   Funkcjonujesz na oparach. Pamiętaj: sen jest kluczowy dla konsolidacji pamięci. Zarywanie nocy się mści.\n");
        } else {
            summary.append("+ POZIOM ENERGII: W NORMIE. Dbałeś o regenerację, co procentuje lepszą nauką.\n");
        }

        if (relacje < 30) {
            summary.append("! RELACJE: IZOLACJA.\n");
            summary.append("   Jesteś 'samotną wyspą'. W branży IT networking jest kluczowy - często ważniejszy niż same oceny.\n");
        } else if (relacje > 60) {
            summary.append("+ RELACJE: ROZBUDOWANE. Masz silną grupę wsparcia, na którą możesz liczyć w kryzysie.\n");
        }

        showTextMode(summary.toString(), () -> {
            String btnText = isBadEnding ? "Przyjmij decyzję o skreśleniu" : "Odbierz dyplom semestralny";
            JButton finalBtn = createChoiceButton(btnText);

            finalBtn.addActionListener(e -> {
                if (isBadEnding) playEnding_Dropout();
                else if (isGoodEnding) playEnding_Success();
                else playEnding_Exhausted();
            });
            setButtons(finalBtn);
        });
    }

    public void playEnding_Dropout() {
        showImageMode("res/ending_bad.jpg", () -> {
            storyTextArea.setText("");
            storyTextArea.setBackground(new Color(0,0,0,0));
            JButton exitBtn = createChoiceButton("Koniec Gry (Porażka)");
            exitBtn.addActionListener(ev -> System.exit(0));
            setButtons(exitBtn);
        });
    }

    public void playEnding_Exhausted() {
        showImageMode("res/ending_neutral.jpg", () -> {
            storyTextArea.setText("");
            storyTextArea.setBackground(new Color(0,0,0,0));
            JButton exitBtn = createChoiceButton("Koniec Gry (Przetrwanie)");
            exitBtn.addActionListener(ev -> System.exit(0));
            setButtons(exitBtn);
        });
    }

    public void playEnding_Success() {
        showImageMode("res/ending_good.jpg", () -> {
            storyTextArea.setText("");
            storyTextArea.setBackground(new Color(0,0,0,0));
            JButton exitBtn = createChoiceButton("Koniec Gry (Sukces!)");
            exitBtn.addActionListener(ev -> System.exit(0));
            setButtons(exitBtn);
        });
    }

    private JButton createChoiceButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.PLAIN, 20));
        btn.setFocusPainted(false);
        btn.setBackground(Color.DARK_GRAY);
        btn.setForeground(Color.WHITE);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorderPainted(false);
        btn.setOpaque(true);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                playSound("res/hover.wav");
                btn.setBackground(Color.GRAY);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(Color.DARK_GRAY);
            }
        });
        return btn;
    }

    private void typeText(String text, Runnable onComplete) {
        storyTextArea.setText("");
        if (timer != null && timer.isRunning()) timer.stop();

        timer = new Timer(10, new ActionListener() {
            int charIndex = 0;
            public void actionPerformed(ActionEvent e) {
                if (charIndex < text.length()) {
                    storyTextArea.append(String.valueOf(text.charAt(charIndex)));
                    charIndex++;
                } else {
                    ((Timer)e.getSource()).stop();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        });
        timer.start();
    }

    /**
     * Odtwarza efekt dźwiękowy.
     */
    public void playSound(String soundFile) {
        try {
            File f = new File(soundFile);
            if (!f.exists()) return;
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(f);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Odtwarza muzykę w tle (pętla).
     */
    public void playBackgroundMusic() {
        try {
            File musicPath = new File("res/ost.wav");
            if (musicPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
                musicClip = AudioSystem.getClip();
                musicClip.open(audioInput);

                try {
                    FloatControl gainControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
                    gainControl.setValue(-10.0f);
                } catch (Exception e) { /* Ignoruj brak obsługi głośności */ }

                musicClip.loop(Clip.LOOP_CONTINUOUSLY);
                musicClip.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JPanel createMenuPanel() {
        BackgroundPanel panel = new BackgroundPanel("res/menu_background.jpg");
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("ODDYCHAJ");
        title.setFont(new Font("Arial", Font.PLAIN, 68));
        title.setForeground(Color.BLACK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(title);
        panel.add(Box.createVerticalStrut(100));

        JButton newGameButton = createMenuButton("Start");
        newGameButton.setForeground(Color.RED);
        newGameButton.addActionListener(e -> startNewGame());

        JButton settingsButton = createMenuButton("Ustawienia");
        settingsButton.addActionListener(e -> {
            if (musicClip != null) {
                if (musicClip.isRunning()) {
                    musicClip.stop();
                    JOptionPane.showMessageDialog(this, "Muzyka została WYŁĄCZONA.");
                } else {
                    musicClip.start();
                    musicClip.loop(Clip.LOOP_CONTINUOUSLY);
                    JOptionPane.showMessageDialog(this, "Muzyka została WŁĄCZONA.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Brak pliku muzycznego lub błąd ładowania.");
            }
        });

        JButton exitButton = createMenuButton("Wyjdź");
        exitButton.addActionListener(e -> System.exit(0));

        panel.add(Box.createVerticalStrut(100));
        panel.add(newGameButton);
        panel.add(Box.createVerticalStrut(20));
        panel.add(settingsButton);
        panel.add(Box.createVerticalStrut(20));
        panel.add(exitButton);

        return panel;
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 24));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(150, 48));
        button.setPreferredSize(new Dimension(150, 48));
        button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBorderPainted(true);
                playSound("res/hover.wav");
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBorderPainted(false);
            }
        });
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        return button;
    }

    /**
     * Klasa pomocnicza do płynnego wyświetlania obrazków.
     */
    private class FadingImagePanel extends JPanel {
        private Image img;
        private float alpha = 0.0f;
        private Timer fadeTimer;

        public FadingImagePanel() {
            setBackground(Color.WHITE);
            setLayout(new BorderLayout());
        }

        public void setImage(String path) {
            try { img = ImageIO.read(new File(path)); }
            catch (Exception e) {
                System.err.println("Błąd: " + path);
                e.printStackTrace();
            }
        }

        public void startFadeIn(Runnable onComplete) {
            alpha = 0.0f;
            if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();

            fadeTimer = new Timer(50, e -> {
                alpha += 0.05f;
                if (alpha >= 1.0f) {
                    alpha = 1.0f;
                    fadeTimer.stop();
                    repaint();
                    if (onComplete != null) onComplete.run();
                } else {
                    repaint();
                }
            });
            fadeTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img != null) {
                ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }
}