import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;

public class GamePanel extends JPanel implements ActionListener {
    private final String backgroundFolder = "C:/Snake Game/assets/"; // Background folder path
    private String[] backgrounds;
    private BufferedImage backgroundImage;
    private Random random;
    private Timer backgroundTimer;

    private Clip backgroundMusic;
    private Clip gameOverSound;
    private Clip appleSound;
    private Clip moveSound;

    private List<ScorePopup> scorePopups = new ArrayList<>();

    static final int SCREEN_WIDTH = 600;
    static final int SCREEN_HEIGHT = 600;
    static final int UNIT_SIZE = 25;
    static int DELAY = 150;
    static final int EASY = 150, NORMAL = 100, HARD = 50;

    final int[] x = new int[(SCREEN_WIDTH * SCREEN_HEIGHT) / UNIT_SIZE];
    final int[] y = new int[(SCREEN_WIDTH * SCREEN_HEIGHT) / UNIT_SIZE];

    int bodyParts = 6;
    int applesEaten;
    int appleX, appleY;
    boolean running = false;
    boolean showStartMenu = true;
    boolean showGameOver = false;

    char direction = 'R';
    Timer timer;
    Timer gameTimer;
    long startTime;
    long elapsedTime;
    int highScore = 0;

    JButton startButton, easyButton, normalButton, hardButton, restartButton;

    private boolean isGameOverAnimation = false;
    private int zoomLevel = 100;  // Starts with normal size (100%) and will decrease during animation
    private int snakeOpacity = 255;  // Opacity of the snake, starts at 255 (fully visible)
    private int fadeSpeed = 1; // How fast the fade/zoom happens
    private int zoomSpeed = 1; // Speed of shrinking
    private int fadeInterval = 30;// Increase the interval between fade steps for slower fading
    private int fadeSteps = 255; // Set to 255 for full fade out

    public GamePanel() {
        random = new Random();
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.black);
        this.setFocusable(true);
        this.addKeyListener(new MyKeyAdapter());

        // Load high score from file or initialize to 0
        loadHighScore();
        
        loadBackgroundImages();
        loadRandomBackground();
        loadSounds();
        // Timer to change background every 5 seconds
        backgroundTimer = new Timer(5000, e -> {
            loadRandomBackground();
            repaint();
        });
        backgroundTimer.start();

        setLayout(null);

        // Start Button
        startButton = createButton("Start Game", 200, 220);

        // Mode Selection Buttons
        easyButton = createButton("Easy", 200, 270);
        normalButton = createButton("Normal", 200, 320);
        hardButton = createButton("Hard", 200, 370);
        restartButton = createButton("Restart", 200, 420);

        // Initially hide mode selection & restart button
        easyButton.setVisible(false);
        normalButton.setVisible(false);
        hardButton.setVisible(false);
        restartButton.setVisible(false);
    }

    private void loadHighScore() {
        File scoreFile = new File("C:/Snake Game/assets/highscore.dat");
        if (scoreFile.exists()) {
            try {
                highScore = Integer.parseInt(new String(java.nio.file.Files.readAllBytes(scoreFile.toPath())).trim());
            } catch (Exception e) {
                System.out.println("Error reading high score file, starting with 0");
                highScore = 0;
            }
        }
    }

    private void saveHighScore() {
        if (applesEaten > highScore) {
            highScore = applesEaten;
            try {
                java.nio.file.Files.write(new File("C:/Snake Game/assets/highscore.dat").toPath(), 
                                         String.valueOf(highScore).getBytes());
            } catch (Exception e) {
                System.out.println("Error saving high score");
            }
        }
    }

    private void loadBackgroundImages() {
        File folder = new File(backgroundFolder);
        if (folder.exists() && folder.isDirectory()) {
            backgrounds = folder.list((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (backgrounds == null || backgrounds.length == 0) {
                System.out.println("No background images found in: " + backgroundFolder);
                backgrounds = new String[0];
            }
        } else {
            System.out.println("Background folder not found: " + backgroundFolder);
            backgrounds = new String[0];
        }
    }

    private void loadRandomBackground() {
        if (backgrounds.length > 0) {
            String randomBackground = backgrounds[random.nextInt(backgrounds.length)];
            try {
                backgroundImage = ImageIO.read(new File(backgroundFolder + randomBackground));
            } catch (IOException e) {
                System.out.println("Error loading background: " + randomBackground);
                e.printStackTrace();
            }
        }
    }

    private void loadSounds() {
        try {
            // Background Music
            File bgMusicFile = new File("C:\\Snake Game\\assets\\snake game bg music.wav");
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bgMusicFile);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioStream);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);  // Loop the background music
    
            // Apple Sound Effect
            File appleSoundFile = new File("C:\\Snake Game\\assets\\apple sound effect.wav");
            audioStream = AudioSystem.getAudioInputStream(appleSoundFile);
            appleSound = AudioSystem.getClip();
            appleSound.open(audioStream);
    
            // Game Over Sound Effect
            File gameOverSoundFile = new File("C:\\Snake Game\\assets\\game over effect.wav");
            audioStream = AudioSystem.getAudioInputStream(gameOverSoundFile);
            gameOverSound = AudioSystem.getClip();
            gameOverSound.open(audioStream);

            // Load move sound
            File moveSoundFile = new File("C:\\Snake Game\\assets\\move Sound effect.wav");
            AudioInputStream moveAudioStream = AudioSystem.getAudioInputStream(moveSoundFile);
            moveSound = AudioSystem.getClip();
            moveSound.open(moveAudioStream);
    
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("Error loading sound files.");
            e.printStackTrace();
        }
    }
    
    private JButton createButton(String text, int x, int y) {
        JButton button = new JButton(text);
        button.setBounds(x, y, 200, 40);
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setBackground(Color.DARK_GRAY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.addActionListener(this);
        add(button);
        return button;
    }

    public void startGame(int mode) {
        if (mode == 1) DELAY = EASY;
        else if (mode == 2) DELAY = NORMAL;
        else if (mode == 3) DELAY = HARD;

        // Reset game state
        bodyParts = 6;
        applesEaten = 0;
        direction = 'R';
        showStartMenu = false;
        showGameOver = false;
        running = true;

        // Place new apple and reset positions
        for (int i = 0; i < bodyParts; i++) {
            x[i] = 100 - (i * UNIT_SIZE);
            y[i] = 100;
        }
        newApple();

        // Start game timer
        startTime = System.currentTimeMillis();
        if (gameTimer != null) {
            gameTimer.stop();
        }
        gameTimer = new Timer(1000, e -> {
            elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
            repaint();
        });
        gameTimer.start();

        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(DELAY, this);
        timer.start();

        // Hide all buttons
        startButton.setVisible(false);
        easyButton.setVisible(false);
        normalButton.setVisible(false);
        hardButton.setVisible(false);
        restartButton.setVisible(false);

        // Play background music
        backgroundMusic.setFramePosition(0);  // Reset to the beginning of the music
        backgroundMusic.start();
        repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }

        if (showStartMenu) {
            drawStartMenu(g);
        } else if (showGameOver) {
            drawGameOverScreen(g);
        } else {
            draw(g);
        }

        // Update and draw score popups
        List<ScorePopup> toRemove = new ArrayList<>();
        for (ScorePopup popup : scorePopups) {
           popup.update();
           popup.draw(g);
           if (popup.isOffScreen()) {
              toRemove.add(popup);
           }
        }
        scorePopups.removeAll(toRemove);
    }

    public void draw(Graphics g) {
        if (running) {
            drawApple(g);

            for (int i = 0; i < bodyParts; i++) {
                if (i == 0) {
                    g.setColor(Color.green);
                } else {
                    g.setColor(new Color(30 + (i * 5), 150 - (i * 3), 50));
                }
                g.fillRoundRect(x[i], y[i], UNIT_SIZE, UNIT_SIZE, 10, 10);
            }

            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            
            // Draw score
            g.drawString("Score: " + applesEaten, 20, 40);
            
            // Draw high score
            g.drawString("High: " + highScore, 20, 80);
            
            // Draw timer
            String timeString = String.format("Time: %02d:%02d", elapsedTime / 60, elapsedTime % 60);
            g.drawString(timeString, SCREEN_WIDTH - 180, 40);
        }
        if (isGameOverAnimation) {
            drawGameOverScreen(g);  // Draw Game Over screen with animation
        }
    }

    private void drawApple(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        GradientPaint appleGradient = new GradientPaint(
            appleX, appleY, new Color(200, 0, 0),  
            appleX + UNIT_SIZE, appleY + UNIT_SIZE, new Color(255, 50, 50)
        );
        g2d.setPaint(appleGradient);
        g2d.fillOval(appleX, appleY, UNIT_SIZE, UNIT_SIZE);

        g.setColor(new Color(139, 69, 19));
        g.fillRect(appleX + UNIT_SIZE / 2 - 2, appleY - 5, 4, 10);

        g.setColor(new Color(255, 255, 255, 150));
        g.fillOval(appleX + 6, appleY + 6, 8, 8);
    }

    public void newApple() {
        appleX = random.nextInt(SCREEN_WIDTH / UNIT_SIZE) * UNIT_SIZE;
        appleY = random.nextInt(SCREEN_HEIGHT / UNIT_SIZE) * UNIT_SIZE;
    }

    public void move() {
        for (int i = bodyParts; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }
        switch (direction) {
            case 'U' -> y[0] -= UNIT_SIZE;
            case 'D' -> y[0] += UNIT_SIZE;
            case 'L' -> x[0] -= UNIT_SIZE;
            case 'R' -> x[0] += UNIT_SIZE;
        }
        if (moveSound != null) {
            moveSound.setFramePosition(0);  // Rewind the sound to the start
            moveSound.start();              // Play the move sound
        }
    }

    public void checkApple() {
        if (x[0] == appleX && y[0] == appleY) {
            bodyParts++;
            applesEaten++;
            newApple();

            // Play apple sound effect when the snake eats an apple
            appleSound.setFramePosition(0);  // Reset to the beginning of the sound clip
            appleSound.start();

            // Create and add a new score popup
            ScorePopup popup = new ScorePopup(x[0], y[0], 1); // 1 is the score increment
            scorePopups.add(popup);
        }
    }

    public void checkCollisions() {
        for (int i = bodyParts; i > 0; i--) {
            if (x[0] == x[i] && y[0] == y[i]) running = false;
        }
        if (x[0] < 0 || x[0] >= SCREEN_WIDTH || y[0] < 0 || y[0] >= SCREEN_HEIGHT) running = false;

        if (!running) {
            timer.stop();
            gameTimer.stop();
            saveHighScore();
            showGameOver = true;
            restartButton.setVisible(true);

            // Play game over sound effect
            gameOverSound.setFramePosition(0);  // Reset to the beginning of the sound clip
            gameOverSound.start();

            // Start the animation
            startGameOverAnimation();
            repaint();
        }
    }

    private void startGameOverAnimation() {
        Timer zoomOutTimer = new Timer(fadeInterval, e -> {
            if (zoomLevel > 0 || snakeOpacity > 0) {
                // Gradually reduce zoom level (shrinking snake)
                zoomLevel -= zoomSpeed;
                // Gradually reduce opacity (fade snake)
                snakeOpacity -= fadeSpeed;
                repaint();
            } else {
                // Once fading and zooming is complete, stop the animation
                ((Timer) e.getSource()).stop();
            }
        });
        zoomOutTimer.start();
    }
    
    public void drawGameOverScreen(Graphics g) {
        g.setColor(Color.red);
        g.setFont(new Font("Arial", Font.BOLD, 75));
        g.drawString("Game Over", SCREEN_WIDTH / 4, SCREEN_HEIGHT / 3);

        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Score: " + applesEaten, SCREEN_WIDTH / 3, SCREEN_HEIGHT / 2);
        
        g.drawString("High Score: " + highScore, SCREEN_WIDTH / 3, SCREEN_HEIGHT / 2 + 50);
        
        String timeString = String.format("Time: %02d:%02d", elapsedTime / 60, elapsedTime % 60);
        g.drawString(timeString, SCREEN_WIDTH / 3, SCREEN_HEIGHT / 2 + 100);
    }

    public void drawStartMenu(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        g.drawString("Snake Game", SCREEN_WIDTH / 4, SCREEN_HEIGHT / 4);
        
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("High Score: " + highScore, SCREEN_WIDTH / 3, SCREEN_HEIGHT / 3 + 50);
    }

    public void restartGame() {
        showGameOver = false;
        showStartMenu = true;
        startButton.setVisible(true);
        easyButton.setVisible(true);
        normalButton.setVisible(true);
        hardButton.setVisible(true);
        restartButton.setVisible(false);
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            move();
            checkApple();
            checkCollisions();
        }
        repaint();

        if (e.getSource() == startButton) {
            startButton.setVisible(false);
            easyButton.setVisible(true);
            normalButton.setVisible(true);
            hardButton.setVisible(true);
        } else if (e.getSource() == easyButton) startGame(1);
        else if (e.getSource() == normalButton) startGame(2);
        else if (e.getSource() == hardButton) startGame(3);
        else if (e.getSource() == restartButton) startGame(2);
    }

    private class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (running) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> { if (direction != 'R') direction = 'L'; }
                    case KeyEvent.VK_RIGHT -> { if (direction != 'L') direction = 'R'; }
                    case KeyEvent.VK_UP -> { if (direction != 'D') direction = 'U'; }
                    case KeyEvent.VK_DOWN -> { if (direction != 'U') direction = 'D'; }
                }
            }
        }
    }
}
class ScorePopup {
    private int x, y;
    private int value;
    private int opacity = 255;  // To handle fading effect
    private int speed = -2;  // Speed of the popup moving upwards

    public ScorePopup(int x, int y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public void update() {
        y += speed;  // Move upwards
        opacity -= 5;  // Fade the number as it flies up
        if (opacity < 0) {
            opacity = 0;
        }
    }

    public void draw(Graphics g) {
        if (opacity > 0) {
            g.setColor(new Color(255, 255, 255, opacity));  // Set color with fading opacity
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("+" + value, x, y);
        }
    }

    public boolean isOffScreen() {
        return y < 0 || opacity == 0;
    }
}
