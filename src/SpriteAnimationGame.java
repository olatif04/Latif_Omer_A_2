import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;

public class SpriteAnimationGame extends JPanel implements KeyListener, Runnable {

    // Constants
    private static final int SPRITE_SIZE = 64;
    private static final int SPRITE_SHEET_WIDTH = 8;
    private static final int SPRITE_SHEET_HEIGHT = 8;
    private static final int WINDOW_WIDTH = 512;
    private static final int WINDOW_HEIGHT = 512;

    // Animation
    private BufferedImage spriteSheet;
    private BufferedImage[][] animations;
    private int currentAnimation;
    private int animationFrame;
    private int frameDelay;
    private final int IDLE = 0;
    private final int RUNNING_LEFT = 1;
    private final int RUNNING_UP = 2;
    private final int RUNNING_RIGHT = 3;
    private final int RUNNING_DOWN = 4;

    // Background and Power-up
    private BufferedImage backgroundTile;
    private BufferedImage powerUpSprite;
    private ArrayList<Point> powerUpLocations;

    // Character properties
    private Rectangle character;
    private int dx;
    private int dy;
    private int characterSpeed = 4;
    private int score = 0;

    // Game Thread
    private Thread gameThread;
    private boolean running;

    public SpriteAnimationGame() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        loadSpriteSheet();  // Ensure this is the first call in the constructor
        if (spriteSheet == null) {
            throw new RuntimeException("Sprite sheet could not be loaded.");
        }
        initializeGameObjects();
        startGame();
    }

    private void loadSpriteSheet() {
        try {
            URL spriteSheetURL = this.getClass().getResource("/SpriteSheet.png");
            if (spriteSheetURL == null) {
                throw new IOException("The sprite sheet could not be found.");
            }
            spriteSheet = ImageIO.read(spriteSheetURL);
            animations = new BufferedImage[5][];
            for (int i = 0; i < 5; i++) {
                animations[i] = new BufferedImage[SPRITE_SHEET_WIDTH];
                for (int j = 0; j < SPRITE_SHEET_WIDTH; j++) {
                    animations[i][j] = spriteSheet.getSubimage(j * SPRITE_SIZE, i * SPRITE_SIZE, SPRITE_SIZE, SPRITE_SIZE);
                }
            }
            backgroundTile = spriteSheet.getSubimage(7 * SPRITE_SIZE, 0, SPRITE_SIZE, SPRITE_SIZE);
            powerUpSprite = spriteSheet.getSubimage(0, 6 * SPRITE_SIZE, SPRITE_SIZE, SPRITE_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void initializeGameObjects() {
        // Initialize character properties
        character = new Rectangle(100, 100, SPRITE_SIZE, SPRITE_SIZE);
        currentAnimation = IDLE;
        animationFrame = 0;
        frameDelay = 0;

        // Initialize power-up locations
        powerUpLocations = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int x = (int) (Math.random() * (WINDOW_WIDTH - SPRITE_SIZE));
            int y = (int) (Math.random() * (WINDOW_HEIGHT - SPRITE_SIZE));
            powerUpLocations.add(new Point(x, y));
        }
    }

    private void startGame() {
        if (gameThread == null) {
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    @Override
    public void run() {
        while (running) {
            long startTime = System.currentTimeMillis();

            updateGame();
            repaint();

            long timeTaken = System.currentTimeMillis() - startTime;
            long timeLeft = (1000 / 60) - timeTaken; // Target 60 FPS
            if (timeLeft < 5) timeLeft = 5; // Minimum delay
            try {
                Thread.sleep(timeLeft);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateGame() {
        // Update the character's position
        character.x += dx * characterSpeed;
        character.y += dy * characterSpeed;

        // Keep the character within bounds
        if (character.x < 0) character.x = 0;
        if (character.y < 0) character.y = 0;
        if (character.x > WINDOW_WIDTH - SPRITE_SIZE) character.x = WINDOW_WIDTH - SPRITE_SIZE;
        if (character.y > WINDOW_HEIGHT - SPRITE_SIZE) character.y = WINDOW_HEIGHT - SPRITE_SIZE;

        // Update the animation frame
        frameDelay++;
        if (frameDelay > 5) {
            if (currentAnimation == IDLE) {
                // Assuming the idle animation uses the first 7 frames (0-6)
                animationFrame = (animationFrame + 1) % 7; // Only cycle through the first 7 frames
            } else {
                animationFrame = (animationFrame + 1) % SPRITE_SHEET_WIDTH;
            }
            frameDelay = 0;
        }

        // Check for collision with power-ups
        Rectangle charRect = new Rectangle(character.x, character.y, SPRITE_SIZE, SPRITE_SIZE);
        ArrayList<Point> toRemove = new ArrayList<>();
        for (Point powerUp : powerUpLocations) {
            Rectangle powerUpRect = new Rectangle(powerUp.x, powerUp.y, SPRITE_SIZE, SPRITE_SIZE);
            if (charRect.intersects(powerUpRect)) {
                score++;
                toRemove.add(powerUp);
            }
        }
        powerUpLocations.removeAll(toRemove);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw background tiles
        for (int y = 0; y < getHeight(); y += SPRITE_SIZE) {
            for (int x = 0; x < getWidth(); x += SPRITE_SIZE) {
                g.drawImage(backgroundTile, x, y, this);
            }
        }

        // Draw power-ups
        for (Point powerUp : powerUpLocations) {
            g.drawImage(powerUpSprite, powerUp.x, powerUp.y, this);
        }

        // Draw the character
        g.drawImage(animations[currentAnimation][animationFrame], character.x, character.y, SPRITE_SIZE, SPRITE_SIZE, this);

        // Draw the score
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT) {
            dx = -1;
            dy = 0;
            currentAnimation = RUNNING_LEFT;
        } else if (key == KeyEvent.VK_RIGHT) {
            dx = 1;
            dy = 0;
            currentAnimation = RUNNING_RIGHT;
        } else if (key == KeyEvent.VK_UP) {
            dx = 0;
            dy = -1;
            currentAnimation = RUNNING_UP;
        } else if (key == KeyEvent.VK_DOWN) {
            dx = 0;
            dy = 1;
            currentAnimation = RUNNING_DOWN;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
            dx = 0;
        }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) {
            dy = 0;
        }

        if (dx == 0 && dy == 0) {
            currentAnimation = IDLE;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used, but required by KeyListener
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Sprite Animation Game");
        SpriteAnimationGame gamePanel = new SpriteAnimationGame();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(gamePanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
