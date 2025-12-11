import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.swing.*;

import Texture.TextureReader;
import com.sun.opengl.util.j2d.TextRenderer;

import java.awt.event.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;

public class eventListener extends AnimListener implements MouseMotionListener, MouseListener {

    // Game state constants
    private static final int STATE_MENU = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_PAUSED = 2;
    private static final int STATE_GAMEOVER = 3;
    private static final int STATE_WIN = 4;
    private static final int STATE_INSTRUCTIONS = 5;
    private static final int STATE_LEVEL_SELECT = 6;
    private static final int STATE_ENTER_NAME = 7;
    private static final int STATE_HIGH_SCORES = 8;
    private static final int STATE_EDIT_NAME = 9;
    
    // Menu button rectangles (normalized coordinates)
    private Rectangle startButton = new Rectangle(-0.3, 0.05, 0.6, 0.18);
    private Rectangle levelSelectButton = new Rectangle(-0.3, -0.18, 0.6, 0.18);
    private Rectangle instructionsButton = new Rectangle(-0.3, -0.41, 0.6, 0.18);
    private Rectangle exitButton = new Rectangle(-0.3, -0.64, 0.6, 0.18);
    private Rectangle backButton = new Rectangle(-0.3, -0.8, 0.6, 0.15);
    
    // Pause screen buttons
    private Rectangle resumeButton = new Rectangle(-0.3, 0.0, 0.6, 0.18);
    private Rectangle returnToMenuButton = new Rectangle(-0.3, -0.25, 0.6, 0.18);
    
    // Level selection buttons (arranged in a grid)
    private Rectangle[] levelButtons = new Rectangle[MAX_LEVELS];
    
    private int mouseX = 0;
    private int mouseY = 0;
    private int windowWidth = 1000;
    private int windowHeight = 700;
    
    // Map dimensions for classic Pac-Man (28 x 31) - will be updated per level
    static int maxWidth = 28;
    static int maxHeight = 31;
    static int[][] currentMap;
    static Food[][] foodMap; // Matrix to track food at each position

    static GL gl;
    private int gameState = STATE_MENU;
    private boolean needsGameInit = false;
    private int score = 0;
    private int highScore = 0;
    private static final String HIGH_SCORE_FILE = "highscore.txt";
    private static final int MAX_HIGH_SCORES = 10;
    private ArrayList<HighScoreEntry> highScoreList = new ArrayList<>();
    
    // Player name input
    private String playerName = "";
    private String currentPlayerName = "PLAYER"; // Current player's name
    private static final int MAX_NAME_LENGTH = 10;
    private boolean enteringName = false;
    private int pendingScore = 0; // Score waiting to be saved
    private Rectangle submitButton = new Rectangle(-0.2, -0.3, 0.4, 0.15);
    private Rectangle highScoresButton = new Rectangle(-0.3, -0.87, 0.6, 0.15);
    private Rectangle setNameButton = new Rectangle(-0.3, 0.28, 0.6, 0.12);
    
    private int currentLevel = 1;
    private static final int MAX_LEVELS = 4;
    private int foodCount = 0;
    private int foodCollected = 0;
    
    // Frame rate control
    private int frameCount = 0;
    private static final int UPDATE_INTERVAL = 3; // Update every 3 frames (slows down movement)
    
    // Timer
    private int timeRemaining = 180; // 180 seconds = 3 minutes
    private static final int STARTING_TIME = 180; // Starting time in seconds
    private int timerFrameCount = 0;
    private static final int FRAMES_PER_SECOND = 18; // Based on FPSAnimator(18)
    
    // Pause key toggle tracking
    private boolean escapeWasPressed = false;
    
    // Ghost house position (calculated per level)
    private int ghostHouseX = 14;
    private int ghostHouseY = 14;

    // Textures
    String[] textureNames = {"Pacman-01.png", "wall.png", "cherry.png", "powerFood.png",
                            "pacmanRight.png", "pacmanLeft.png", "pacmanUp.png", "pacmanDown.png",
                            "redGhost.png", "pinkGhost.png", "blueGhost.png", "orangeGhost.png", "scaredGhost.png"};
    TextureReader.Texture[] texture = new TextureReader.Texture[textureNames.length];
    int[] textures = new int[textureNames.length];
    
    background back;
    Pacman pacman;
    Ghost[] ghosts = new Ghost[4];
    ArrayList<Food> foods = new ArrayList<>();
    
    // Text renderer for proper text display
    private TextRenderer textRenderer;

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        gl = glAutoDrawable.getGL();
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(-1.0, 1.0, -1.0, 1.0, -1, 1);
        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        
        // Initialize text renderer with a bold font
        textRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 32), true, true);
        
        // Initialize level selection buttons
        initLevelButtons();
        
        // Load high score from file
        loadHighScore();
        
        // Load textures
        gl.glGenTextures(textureNames.length, textures, 0);
        for (int i = 0; i < textureNames.length; i++) {
            try {
                texture[i] = TextureReader.readTexture(assetsFolderName + "//" + textureNames[i], true);
                gl.glBindTexture(GL.GL_TEXTURE_2D, textures[i]);
                new GLU().gluBuild2DMipmaps(
                        GL.GL_TEXTURE_2D,
                        GL.GL_RGBA,
                        texture[i].getWidth(), texture[i].getHeight(),
                        GL.GL_RGBA,
                        GL.GL_UNSIGNED_BYTE,
                        texture[i].getPixels()
                );
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void initGame() {
        // Load map for current level
        int levelIndex = currentLevel - 1;
        if (levelIndex >= 0 && levelIndex < Map.levels.length) {
            currentMap = Map.levels[levelIndex];
            maxHeight = Map.levelDimensions[levelIndex][1];
            maxWidth = Map.levelDimensions[levelIndex][0];
        } else {
            currentMap = Map.classicMap;
            maxHeight = 31;
            maxWidth = 28;
        }
        
        // Find ghost house center (value 2 in map)
        ghostHouseX = maxWidth / 2;
        ghostHouseY = maxHeight / 2;
        int ghostHouseCount = 0;
        int sumX = 0, sumY = 0;
        
        for (int y = 0; y < currentMap.length; y++) {
            for (int x = 0; x < currentMap[y].length; x++) {
                if (currentMap[y][x] == 2) { // Ghost house
                    sumX += x;
                    sumY += y;
                    ghostHouseCount++;
                }
            }
        }
        
        if (ghostHouseCount > 0) {
            ghostHouseX = sumX / ghostHouseCount;
            ghostHouseY = sumY / ghostHouseCount;
        }
        
        // Ghost starting positions (around ghost house)
        int[][] ghostStarts = {
            {ghostHouseX, ghostHouseY},
            {ghostHouseX + 1, ghostHouseY},
            {ghostHouseX, ghostHouseY + 1},
            {ghostHouseX - 1, ghostHouseY}
        };
        
        // Find Pacman starting position - look for open space away from ghost house
        int pacmanStartX = ghostHouseX;
        int pacmanStartY = ghostHouseY;
        
        // Search for a good spawn point - try multiple strategies
        boolean foundSpawn = false;
        int minDistance = 5;
        
        // Strategy 1: Search bottom half of map first (traditional Pacman spawn)
        for (int y = maxHeight - 3; y >= maxHeight / 2 && !foundSpawn; y--) {
            for (int x = 1; x < maxWidth - 1; x++) {
                if (y >= 0 && y < currentMap.length && x >= 0 && x < currentMap[y].length) {
                    if (currentMap[y][x] == 0) { // Open path
                        int distanceFromGhosts = Math.abs(x - ghostHouseX) + Math.abs(y - ghostHouseY);
                        if (distanceFromGhosts >= minDistance) {
                            pacmanStartX = x;
                            pacmanStartY = y;
                            foundSpawn = true;
                            break;
                        }
                    }
                }
            }
        }
        
        // Strategy 2: If not found, search top half
        if (!foundSpawn) {
            for (int y = 2; y < maxHeight / 2 && !foundSpawn; y++) {
                for (int x = 1; x < maxWidth - 1; x++) {
                    if (y >= 0 && y < currentMap.length && x >= 0 && x < currentMap[y].length) {
                        if (currentMap[y][x] == 0) {
                            int distanceFromGhosts = Math.abs(x - ghostHouseX) + Math.abs(y - ghostHouseY);
                            if (distanceFromGhosts >= minDistance) {
                                pacmanStartX = x;
                                pacmanStartY = y;
                                foundSpawn = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        // Strategy 3: If still not found, reduce distance requirement
        if (!foundSpawn) {
            minDistance = 3;
            for (int y = 1; y < maxHeight - 1 && !foundSpawn; y++) {
                for (int x = 1; x < maxWidth - 1; x++) {
                    if (y >= 0 && y < currentMap.length && x >= 0 && x < currentMap[y].length) {
                        if (currentMap[y][x] == 0) {
                            int distanceFromGhosts = Math.abs(x - ghostHouseX) + Math.abs(y - ghostHouseY);
                            if (distanceFromGhosts >= minDistance) {
                                pacmanStartX = x;
                                pacmanStartY = y;
                                foundSpawn = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        // If no good spawn found, use fallback
        if (!foundSpawn) {
            pacmanStartX = ghostHouseX;
            pacmanStartY = Math.min(ghostHouseY + 9, maxHeight - 2);
            // Make sure it's on a valid path
            if (pacmanStartY >= 0 && pacmanStartY < currentMap.length && 
                pacmanStartX >= 0 && pacmanStartX < currentMap[pacmanStartY].length &&
                currentMap[pacmanStartY][pacmanStartX] != 0) {
                // Search nearby for a valid path
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dx = -3; dx <= 3; dx++) {
                        int ny = pacmanStartY + dy;
                        int nx = pacmanStartX + dx;
                        if (ny >= 0 && ny < currentMap.length && nx >= 0 && nx < currentMap[ny].length && 
                            currentMap[ny][nx] == 0) {
                            pacmanStartX = nx;
                            pacmanStartY = ny;
                            dy = 4; // break outer loop
                            break;
                        }
                    }
                }
            }
        }
        
        // Initialize food map
        foodMap = new Food[maxHeight][maxWidth];

        // Don't render a full-screen background - let the black clear color show
        // back = new background(0, 0, false, new String[]{"Pacman-01.png"});
        // entityManager.addEntity(back);

        // Init Pacman at starting position
        pacman = new Pacman(pacmanStartX, pacmanStartY, true, new String[]{"pacmanRight.png", "pacmanLeft.png", "pacmanUp.png", "pacmanDown.png"});
        entityManager.addEntity(pacman);
        
        // Init ghosts with both normal and frightened textures
        String[] ghostNames = {"blinky", "pinky", "inky", "clyde"};
        String[] ghostTextures = {"redGhost.png", "pinkGhost.png", "blueGhost.png", "orangeGhost.png"};
        
        for (int i = 0; i < 4; i++) {
            int ghostX = ghostStarts[i][0];
            int ghostY = ghostStarts[i][1];
            
            // Safety check: if ghost spawns at same position as Pacman, offset it
            if (ghostX == pacmanStartX && ghostY == pacmanStartY) {
                // Try to find nearby valid position
                boolean foundAlt = false;
                for (int dy = -2; dy <= 2 && !foundAlt; dy++) {
                    for (int dx = -2; dx <= 2 && !foundAlt; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        int ny = ghostY + dy;
                        int nx = ghostX + dx;
                        if (ny >= 0 && ny < currentMap.length && nx >= 0 && nx < currentMap[ny].length &&
                            currentMap[ny][nx] != 1 && !(nx == pacmanStartX && ny == pacmanStartY)) {
                            ghostX = nx;
                            ghostY = ny;
                            foundAlt = true;
                        }
                    }
                }
            }
            
            ghosts[i] = new Ghost(ghostX, ghostY, true, 
                                  new String[]{ghostTextures[i], "scaredGhost.png"}, ghostNames[i]);
            entityManager.addEntity(ghosts[i]);
        }
        
        // Init AI with chase mode
        GhostAI.map = currentMap;
        GhostAI.updateScatterCorners();
        GhostAI.pacmanX = pacman.x;
        GhostAI.pacmanY = pacman.y;
        GhostAI.currentMode = GhostAI.MODE_CHASE;
        GhostAI.modeTimer = 200; // Start with chase mode
        
        // Init food
        foodCount = 0;
        foodCollected = 0;
        for (int y = 0; y < currentMap.length; y++) {
            for (int x = 0; x < currentMap[y].length; x++) {
                // Don't place food on pacman start or ghost starts or ghost house (value 2) or tunnels (value 3)
                boolean isGhostStart = false;
                for (int[] ghostStart : ghostStarts) {
                    if (x == ghostStart[0] && y == ghostStart[1]) {
                        isGhostStart = true;
                        break;
                    }
                }
                // Only place food on paths (0), not walls (1), ghost house (2), or tunnels (3)
                if (currentMap[y][x] == 0 && !(x == pacmanStartX && y == pacmanStartY) && !isGhostStart) {
                    // Power pellets at the 4 corners of the classic Pac-Man map
                    boolean isPower = (x == 1 && y == 3) || (x == 26 && y == 3) || 
                                     (x == 1 && y == 23) || (x == 26 && y == 23);
                    Food food = new Food(x, y, true, new String[]{isPower ? "powerFood.png" : "cherry.png"}, isPower);
                    foods.add(food);
                    foodMap[y][x] = food; // Store food in the matrix
                    entityManager.addEntity(food);
                    foodCount++;
                }
            }
        }
        
        // Load entity textures
        for (Entity entity : entityManager.entities) {
            for (int i = 0; i < entity.texture.length; i++) {
                try {
                    entity.texture[i] = TextureReader.readTexture(assetsFolderName + "//" + entity.Textures.get(i), true);
                    gl.glBindTexture(GL.GL_TEXTURE_2D, entity.textures[i]);
                    new GLU().gluBuild2DMipmaps(
                            GL.GL_TEXTURE_2D,
                            GL.GL_RGBA,
                            entity.texture[i].getWidth(), entity.texture[i].getHeight(),
                            GL.GL_RGBA,
                            GL.GL_UNSIGNED_BYTE,
                            entity.texture[i].getPixels()
                    );
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glLoadIdentity();
        
        // Initialize game if requested (must happen in OpenGL thread)
        if (needsGameInit) {
            needsGameInit = false;
            entityManager.entities.clear();
            foods.clear();
            score = 0;
            // Only reset to level 1 if not already set (from level selection)
            // currentLevel will be set by level selection or stay at current value
            timeRemaining = STARTING_TIME;
            timerFrameCount = 0;
            initGame();
            gameState = STATE_PLAYING;
            Game.mainMusic.playMusic();
        }
        
        // Handle global key presses
        handleGlobalKeys();
        
        // Show menu if in menu state
        if (gameState == STATE_MENU) {
            renderMenu();
            return;
        }
        
        // Show instructions if in instructions state
        if (gameState == STATE_INSTRUCTIONS) {
            renderInstructions();
            return;
        }
        
        // Show level select if in level select state
        if (gameState == STATE_LEVEL_SELECT) {
            renderLevelSelect();
            return;
        }
        
        // Show name entry screen
        if (gameState == STATE_ENTER_NAME) {
            renderEnterName();
            return;
        }
        
        // Show edit name screen
        if (gameState == STATE_EDIT_NAME) {
            renderEditName();
            return;
        }
        
        // Show high scores screen
        if (gameState == STATE_HIGH_SCORES) {
            renderHighScores();
            return;
        }
        
        // Handle global keys (pause, restart, etc.)
        handleGlobalKeys();
        
        // Frame rate control - only update game logic every UPDATE_INTERVAL frames
        frameCount++;
        boolean shouldUpdate = (frameCount % UPDATE_INTERVAL == 0);

        if (gameState == STATE_PLAYING && pacman != null && shouldUpdate) { // Playing
            handleKeyPress();
            
            // Update timer (countdown every second)
            timerFrameCount++;
            if (timerFrameCount >= FRAMES_PER_SECOND) {
                timerFrameCount = 0;
                timeRemaining--;
                
                // Check if time ran out
                if (timeRemaining <= 0) {
                    timeRemaining = 0;
                    gameState = STATE_GAMEOVER;
                    updateHighScore();
                    Game.Dclick.playMusic();
                }
            }
            
            // Update ghost AI mode (scatter/chase/frightened)
            GhostAI.updateMode();
            
            // Update AI with pacman position
            GhostAI.pacmanX = pacman.x;
            GhostAI.pacmanY = pacman.y;
            GhostAI.pacDirX = pacman.getDirX();
            GhostAI.pacDirY = pacman.getDirY();
            
            // Update entities
            entityManager.update();
            
            // Check food collision using grid position
            int pacX = pacman.x;
            int pacY = pacman.y;
            if (pacY >= 0 && pacY < foodMap.length && pacX >= 0 && pacX < foodMap[0].length) {
                Food food = foodMap[pacY][pacX];
                if (food != null && !food.isCollected()) {
                    food.collect();
                    foodMap[pacY][pacX] = null; // Remove from food map
                    foodCollected++;
                    score += food.isPowerFood() ? 50 : 10;
                    
                    if (food.isPowerFood()) {
                        pacman.setPowered(true);
                        GhostAI.setFrightened(); // Make ghosts run away!
                        Game.Mclick.playMusic();
                    } else {
                        Game.Eclick.playMusic();
                    }
                }
            }
            
            // Check ghost collision
            for (Ghost ghost : ghosts) {
                if (checkCollisionSafely(pacman, ghost)) {
                    if (GhostAI.isFrightened()) {
                        // Eat ghost - respawn in ghost house
                        score += 200;
                        ghost.x = ghostHouseX;
                        ghost.y = ghostHouseY;
                        ghost.prevX = ghostHouseX;
                        ghost.prevY = ghostHouseY;
                        Game.Eclick.playMusic();
                    } else {
                        // Lose life
                        pacman.loseLife();
                        Game.Dclick.playMusic();
                        if (pacman.getLives() <= 0) {
                            gameState = STATE_GAMEOVER;
                            updateHighScore();
                        } else {
                            resetPositions();
                        }
                    }
                }
            }
            
            // Check win condition
            if (foodCollected >= foodCount) {
                if (currentLevel < MAX_LEVELS) {
                    // Advance to next level
                    currentLevel++;
                    entityManager.entities.clear();
                    foods.clear();
                    timeRemaining = STARTING_TIME;
                    timerFrameCount = 0;
                    initGame();
                    // Keep score and continue playing
                } else {
                    // Completed all levels!
                    gameState = STATE_WIN;
                    updateHighScore();
                }
            }
        }
        
        // Always render the game scene (only if not in menu)
        if (currentMap != null) {
            renderMap();
        }
        if (pacman != null) {
            entityManager.render(gl);
            drawScore();
            drawLevel();
            drawTimer();
            drawLives();
        }
        
        // Overlay game state messages
        if (gameState == STATE_PAUSED) {
            drawPaused();
        } else if (gameState == STATE_GAMEOVER) {
            drawGameOver();
        } else if (gameState == STATE_WIN) {
            drawWin();
        }
    }
    
    private boolean checkCollisionSafely(Entity entity1, Entity entity2) {
        // Matrix-based collision detection with pass-through check
        if (entity1 == null || entity2 == null) return false;
        
        // Check 1: Same position collision
        if (entity1.x == entity2.x && entity1.y == entity2.y) {
            return true;
        }
        
        // Check 2: Pass-through collision (entities swapped positions)
        // This happens when entity1 moves from A to B while entity2 moves from B to A
        boolean passThrough = (entity1.x == entity2.prevX && entity1.y == entity2.prevY &&
                               entity2.x == entity1.prevX && entity2.y == entity1.prevY);
        if (passThrough) {
            return true;
        }
        
        // Check 3: Near-miss collision for when ghosts skip frames due to moveDelay
        // If they were adjacent and are now adjacent in opposite direction, they crossed
        int dx = Math.abs(entity1.x - entity2.x);
        int dy = Math.abs(entity1.y - entity2.y);
        
        // Handle tunnel wrapping for x distance
        if (dx > maxWidth / 2) {
            dx = maxWidth - dx;
        }
        
        // If entities are adjacent (Manhattan distance == 1), check if they crossed paths
        if (dx + dy == 1) {
            // Check if entity1's previous position was entity2's current position or vice versa
            if ((entity1.prevX == entity2.x && entity1.prevY == entity2.y) ||
                (entity2.prevX == entity1.x && entity2.prevY == entity1.y)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void resetPositions() {
        // Find ghost house center
        int ghostHouseX = maxWidth / 2;
        int ghostHouseY = maxHeight / 2;
        int ghostHouseCount = 0;
        int sumX = 0, sumY = 0;
        
        for (int y = 0; y < currentMap.length; y++) {
            for (int x = 0; x < currentMap[y].length; x++) {
                if (currentMap[y][x] == 2) {
                    sumX += x;
                    sumY += y;
                    ghostHouseCount++;
                }
            }
        }
        
        if (ghostHouseCount > 0) {
            ghostHouseX = sumX / ghostHouseCount;
            ghostHouseY = sumY / ghostHouseCount;
        }
        
        // Find Pacman spawn - away from ghosts
        int pacmanStartX = ghostHouseX;
        int pacmanStartY = ghostHouseY;
        boolean foundSpawn = false;
        int minDistance = 5;
        
        // Search bottom half first
        for (int y = maxHeight - 3; y >= maxHeight / 2 && !foundSpawn; y--) {
            for (int x = 1; x < maxWidth - 1; x++) {
                if (y >= 0 && y < currentMap.length && x >= 0 && x < currentMap[y].length) {
                    if (currentMap[y][x] == 0) {
                        int distance = Math.abs(x - ghostHouseX) + Math.abs(y - ghostHouseY);
                        if (distance >= minDistance) {
                            pacmanStartX = x;
                            pacmanStartY = y;
                            foundSpawn = true;
                            break;
                        }
                    }
                }
            }
        }
        
        // Search top half if needed
        if (!foundSpawn) {
            for (int y = 2; y < maxHeight / 2 && !foundSpawn; y++) {
                for (int x = 1; x < maxWidth - 1; x++) {
                    if (y >= 0 && y < currentMap.length && x >= 0 && x < currentMap[y].length) {
                        if (currentMap[y][x] == 0) {
                            int distance = Math.abs(x - ghostHouseX) + Math.abs(y - ghostHouseY);
                            if (distance >= minDistance) {
                                pacmanStartX = x;
                                pacmanStartY = y;
                                foundSpawn = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        // Reduce distance if needed
        if (!foundSpawn) {
            minDistance = 3;
            for (int y = 1; y < maxHeight - 1 && !foundSpawn; y++) {
                for (int x = 1; x < maxWidth - 1; x++) {
                    if (y >= 0 && y < currentMap.length && x >= 0 && x < currentMap[y].length) {
                        if (currentMap[y][x] == 0) {
                            int distance = Math.abs(x - ghostHouseX) + Math.abs(y - ghostHouseY);
                            if (distance >= minDistance) {
                                pacmanStartX = x;
                                pacmanStartY = y;
                                foundSpawn = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        if (!foundSpawn) {
            pacmanStartX = ghostHouseX;
            pacmanStartY = Math.min(ghostHouseY + 9, maxHeight - 2);
        }
        
        pacman.resetPosition(pacmanStartX, pacmanStartY);
        
        // Reset ghosts to ghost house
        int[][] ghostStarts = {
            {ghostHouseX, ghostHouseY},
            {ghostHouseX - 1, ghostHouseY},
            {ghostHouseX, ghostHouseY - 1},
            {ghostHouseX + 1, ghostHouseY}
        };
        
        for (int i = 0; i < ghosts.length && i < ghostStarts.length; i++) {
            if (ghosts[i] != null) {
                ghosts[i].x = ghostStarts[i][0];
                ghosts[i].y = ghostStarts[i][1];
            }
        }
    }
    
    private void renderMap() {
        gl.glEnable(GL.GL_BLEND);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[1]); // wall texture
        
        // Calculate tile size based on map dimensions
        double tileScaleX = 1.0 / maxWidth;
        double tileScaleY = 1.0 / maxHeight;
        double tileScale = Math.min(tileScaleX, tileScaleY) * 0.95;
        
        for (int y = 0; y < currentMap.length; y++) {
            for (int x = 0; x < currentMap[y].length; x++) {
                if (currentMap[y][x] == 1) { // Wall
                    gl.glPushMatrix();
                    gl.glTranslated(x * (2.0 / maxWidth) - 1.0 + (1.0 / maxWidth),
                                   y * (2.0 / maxHeight) - 1.0 + (1.0 / maxHeight), 0);
                    gl.glScaled(tileScale, tileScale, 1);

                    gl.glBegin(GL.GL_QUADS);
                    gl.glTexCoord2f(0.0f, 0.0f);
                    gl.glVertex3f(-1.0f, -1.0f, -1.0f);
                    gl.glTexCoord2f(1.0f, 0.0f);
                    gl.glVertex3f(1.0f, -1.0f, -1.0f);
                    gl.glTexCoord2f(1.0f, 1.0f);
                    gl.glVertex3f(1.0f, 1.0f, -1.0f);
                    gl.glTexCoord2f(0.0f, 1.0f);
                    gl.glVertex3f(-1.0f, 1.0f, -1.0f);
                    gl.glEnd();
                    
                    gl.glPopMatrix();
                }
            }
        }
        gl.glDisable(GL.GL_BLEND);
    }
    
    private void drawScore() {
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        drawText("SCORE: " + score, -0.95f, 0.9f, 0.05f);
    }
    
    private void drawLevel() {
        gl.glColor3f(0.0f, 1.0f, 1.0f); // Cyan
        drawText("LEVEL: " + currentLevel + "/" + MAX_LEVELS, -0.2f, 0.9f, 0.05f);
    }
    
    private void drawTimer() {
        // Format time as MM:SS
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        String timeStr = String.format("TIME: %d:%02d", minutes, seconds);
        
        // Change color based on time remaining
        if (timeRemaining <= 10) {
            gl.glColor3f(1.0f, 0.0f, 0.0f); // Red when low
        } else if (timeRemaining <= 30) {
            gl.glColor3f(1.0f, 0.65f, 0.0f); // Orange when getting low
        } else {
            gl.glColor3f(1.0f, 1.0f, 1.0f); // White
        }
        drawText(timeStr, 0.55f, 0.9f, 0.05f);
    }
    
    private void drawLives() {
        // Draw health indicator using Pacman icons at the bottom of the screen
        if (pacman == null) return;
        
        int lives = pacman.getLives();
        double iconSize = 0.05; // Size of each life icon
        double startX = -0.95; // Start position X
        double startY = -0.95; // Bottom of screen
        double spacing = 0.12; // Space between icons
        
        gl.glEnable(GL.GL_BLEND);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[4]); // pacmanRight.png texture
        
        for (int i = 0; i < lives; i++) {
            gl.glPushMatrix();
            gl.glTranslated(startX + (i * spacing), startY, 0);
            gl.glScaled(iconSize, iconSize, 1);
            
            gl.glBegin(GL.GL_QUADS);
            gl.glTexCoord2f(0.0f, 0.0f);
            gl.glVertex3f(-1.0f, -1.0f, 0.0f);
            gl.glTexCoord2f(1.0f, 0.0f);
            gl.glVertex3f(1.0f, -1.0f, 0.0f);
            gl.glTexCoord2f(1.0f, 1.0f);
            gl.glVertex3f(1.0f, 1.0f, 0.0f);
            gl.glTexCoord2f(0.0f, 1.0f);
            gl.glVertex3f(-1.0f, 1.0f, 0.0f);
            gl.glEnd();
            
            gl.glPopMatrix();
        }
        
        gl.glDisable(GL.GL_BLEND);
    }
    
    private void drawGameOver() {
        gl.glColor3f(1.0f, 0.0f, 0.0f);
        gl.glRasterPos2f(-0.2f, 0.0f);
        // Note: Text rendering would need additional implementation
    }
    
    private void drawWin() {
        gl.glColor3f(0.0f, 1.0f, 0.0f);
        gl.glRasterPos2f(-0.2f, 0.0f);
        // Note: Text rendering would need additional implementation
    }
    
    private void drawPaused() {
        // Draw semi-transparent overlay
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glEnable(GL.GL_BLEND);
        gl.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex2f(-1.0f, -1.0f);
        gl.glVertex2f(1.0f, -1.0f);
        gl.glVertex2f(1.0f, 1.0f);
        gl.glVertex2f(-1.0f, 1.0f);
        gl.glEnd();
        
        // Draw pause title
        gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow
        drawText("PAUSED", -0.25f, 0.4f, 0.12f);
        
        // Get normalized mouse coordinates for hover effects
        double normX = (mouseX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (mouseY / (double) windowHeight) * 2.0;
        
        // Resume button
        boolean resumeHover = resumeButton.contains(normX, normY);
        if (resumeHover) {
            gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow hover
        } else {
            gl.glColor3f(0.0f, 0.5f, 1.0f); // Blue
        }
        drawButton(resumeButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f); // White text
        drawText("RESUME", -0.18f, 0.06f, 0.08f);
        
        // Return to Menu button
        boolean menuHover = returnToMenuButton.contains(normX, normY);
        if (menuHover) {
            gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow hover
        } else {
            gl.glColor3f(0.0f, 0.5f, 1.0f); // Blue
        }
        drawButton(returnToMenuButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f); // White text
        drawText("RETURN TO MENU", -0.35f, -0.19f, 0.08f);
        
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL.GL_TEXTURE_2D);
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) {
        windowWidth = width;
        windowHeight = height;
    }

    @Override
    public void displayChanged(GLAutoDrawable glAutoDrawable, boolean b, boolean b1) {

    }

    public BitSet keyBits = new BitSet(256);

    @Override
    public void keyPressed(final KeyEvent event) {
        int keyCode = event.getKeyCode();
        keyBits.set(keyCode);
    }

    @Override
    public void keyReleased(final KeyEvent event) {
        int keyCode = event.getKeyCode();
        keyBits.clear(keyCode);
    }

    @Override
    public void keyTyped(final KeyEvent event) {
        // Handle name input when in name entry or edit name state
        if ((gameState == STATE_ENTER_NAME || gameState == STATE_EDIT_NAME) && enteringName) {
            char c = event.getKeyChar();
            
            // Enter key submits the name
            if (c == '\n' || c == '\r') {
                if (gameState == STATE_ENTER_NAME) {
                    submitHighScore();
                } else {
                    submitEditedName();
                }
                return;
            }
            
            // Escape key cancels editing
            if (c == 27) { // ESC character
                if (gameState == STATE_EDIT_NAME) {
                    enteringName = false;
                    gameState = STATE_MENU;
                }
                return;
            }
            
            // Backspace removes last character
            if (c == '\b') {
                if (playerName.length() > 0) {
                    playerName = playerName.substring(0, playerName.length() - 1);
                }
                return;
            }
            
            // Only allow alphanumeric characters and some special chars
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-') {
                if (playerName.length() < MAX_NAME_LENGTH) {
                    playerName += Character.toUpperCase(c);
                }
            }
        }
    }

    public void handleKeyPress() {
        if (pacman == null) return;
        
        if (isKeyPressed(KeyEvent.VK_UP) || isKeyPressed(KeyEvent.VK_W)) {
            pacman.setDirection(0, 1);
        } else if (isKeyPressed(KeyEvent.VK_DOWN) || isKeyPressed(KeyEvent.VK_S)) {
            pacman.setDirection(0, -1);
        } else if (isKeyPressed(KeyEvent.VK_LEFT) || isKeyPressed(KeyEvent.VK_A)) {
            pacman.setDirection(-1, 0);
        } else if (isKeyPressed(KeyEvent.VK_RIGHT) || isKeyPressed(KeyEvent.VK_D)) {
            pacman.setDirection(1, 0);
        }
    }
    
    public void handleGlobalKeys() {
        // Toggle pause with ESC key (using toggle logic to prevent rapid toggling)
        boolean escapeIsPressed = isKeyPressed(KeyEvent.VK_ESCAPE);
        if (escapeIsPressed && !escapeWasPressed) {
            if (gameState == STATE_PLAYING) {
                gameState = STATE_PAUSED;
            } else if (gameState == STATE_PAUSED) {
                gameState = STATE_PLAYING;
            }
        }
        escapeWasPressed = escapeIsPressed;
        
        if (isKeyPressed(KeyEvent.VK_R)) {
            if (gameState == STATE_GAMEOVER || gameState == STATE_WIN) {
                // Request game restart
                needsGameInit = true;
            }
        }
    }


    public boolean isKeyPressed(final int keyCode) {
        return keyBits.get(keyCode);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (gameState == STATE_MENU) {
            handleMenuClick(e.getX(), e.getY());
        } else if (gameState == STATE_INSTRUCTIONS) {
            handleInstructionsClick(e.getX(), e.getY());
        } else if (gameState == STATE_LEVEL_SELECT) {
            handleLevelSelectClick(e.getX(), e.getY());
        } else if (gameState == STATE_PAUSED) {
            handlePauseClick(e.getX(), e.getY());
        } else if (gameState == STATE_ENTER_NAME) {
            handleEnterNameClick(e.getX(), e.getY());
        } else if (gameState == STATE_HIGH_SCORES) {
            handleHighScoresClick(e.getX(), e.getY());
        } else if (gameState == STATE_EDIT_NAME) {
            handleEditNameClick(e.getX(), e.getY());
        }
    }


    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }


    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

    }
    
    // Menu rendering and interaction
    private void renderMenu() {
        // Draw title
        gl.glDisable(GL.GL_TEXTURE_2D);
        
        // PAC-MAN title
        gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow
        drawText("PAC-MAN", -0.35f, 0.6f, 0.15f);
        
        // Convert mouse coordinates to OpenGL normalized coordinates
        double normX = (mouseX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (mouseY / (double) windowHeight) * 2.0;
        
        // Display current player name with Set Name button
        gl.glColor3f(0.0f, 1.0f, 1.0f); // Cyan
        drawText("PLAYER: " + currentPlayerName, -0.35f, 0.45f, 0.055f);
        
        // Draw Set Name button (small)
        boolean setNameHover = setNameButton.contains(normX, normY);
        if (setNameHover) {
            gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow hover
        } else {
            gl.glColor3f(0.4f, 0.4f, 0.6f); // Dark blue-gray
        }
        drawButton(setNameButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        drawText("CHANGE NAME", -0.27f, 0.31f, 0.05f);
        
        
        // Draw Start button
        boolean startHover = startButton.contains(normX, normY);
        if (startHover) {
            gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow hover
        } else {
            gl.glColor3f(0.0f, 0.5f, 1.0f); // Blue
        }
        drawButton(startButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f); // White text
        drawText("START GAME", -0.25f, 0.1f, 0.065f);
        
        // Draw Level Select button
        boolean levelSelectHover = levelSelectButton.contains(normX, normY);
        if (levelSelectHover) {
            gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow hover
        } else {
            gl.glColor3f(0.8f, 0.4f, 0.0f); // Orange
        }
        drawButton(levelSelectButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f); // White text
        drawText("SELECT LEVEL", -0.27f, -0.13f, 0.065f);
        
        // Draw Instructions button
        boolean instructionsHover = instructionsButton.contains(normX, normY);
        if (instructionsHover) {
            gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow hover
        } else {
            gl.glColor3f(0.0f, 0.8f, 0.5f); // Green
        }
        drawButton(instructionsButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f); // White text
        drawText("INSTRUCTIONS", -0.3f, -0.36f, 0.065f);
        
        // Draw Exit button
        boolean exitHover = exitButton.contains(normX, normY);
        if (exitHover) {
            gl.glColor3f(1.0f, 0.0f, 0.0f); // Red hover
        } else {
            gl.glColor3f(0.5f, 0.5f, 0.5f); // Gray
        }
        drawButton(exitButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f); // White text
        drawText("EXIT", -0.1f, -0.59f, 0.065f);
        
        // Draw High Scores button
        boolean highScoresHover = highScoresButton.contains(normX, normY);
        if (highScoresHover) {
            gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow hover
        } else {
            gl.glColor3f(0.6f, 0.0f, 0.8f); // Purple
        }
        drawButton(highScoresButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f); // White text
        drawText("HIGH SCORES", -0.27f, -0.83f, 0.065f);
        
        gl.glEnable(GL.GL_TEXTURE_2D);
    }
    
    private void drawButton(Rectangle rect) {
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex2d(rect.x, rect.y);
        gl.glVertex2d(rect.x + rect.width, rect.y);
        gl.glVertex2d(rect.x + rect.width, rect.y + rect.height);
        gl.glVertex2d(rect.x, rect.y + rect.height);
        gl.glEnd();
        
        // Draw border
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        gl.glLineWidth(2.0f);
        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex2d(rect.x, rect.y);
        gl.glVertex2d(rect.x + rect.width, rect.y);
        gl.glVertex2d(rect.x + rect.width, rect.y + rect.height);
        gl.glVertex2d(rect.x, rect.y + rect.height);
        gl.glEnd();
    }
    
    private void loadHighScore() {
        highScoreList.clear();
        try {
            java.io.File file = new java.io.File(HIGH_SCORE_FILE);
            if (file.exists()) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
                String line;
                while ((line = reader.readLine()) != null && highScoreList.size() < MAX_HIGH_SCORES) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        String name = parts[0].trim();
                        int scoreValue = Integer.parseInt(parts[1].trim());
                        highScoreList.add(new HighScoreEntry(name, scoreValue));
                    } else if (parts.length == 1) {
                        // Legacy format: just a score, assign default name
                        int scoreValue = Integer.parseInt(parts[0].trim());
                        highScoreList.add(new HighScoreEntry("PLAYER", scoreValue));
                    }
                }
                reader.close();
                // Sort by score descending
                highScoreList.sort((a, b) -> Integer.compare(b.score, a.score));
                // Update highScore to be the top score
                if (!highScoreList.isEmpty()) {
                    highScore = highScoreList.get(0).score;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load high scores: " + e.getMessage());
            highScore = 0;
        }
    }
    
    private void saveHighScore() {
        try {
            java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(HIGH_SCORE_FILE));
            for (HighScoreEntry entry : highScoreList) {
                writer.write(entry.name + "," + entry.score);
                writer.newLine();
            }
            writer.close();
        } catch (Exception e) {
            System.out.println("Could not save high scores: " + e.getMessage());
        }
    }
    
    private void updateHighScore() {
        // Check if score qualifies for top 10
        if (highScoreList.size() < MAX_HIGH_SCORES || score > highScoreList.get(highScoreList.size() - 1).score) {
            // Score qualifies - use current player name directly
            addHighScoreEntry(currentPlayerName, score);
        }
        // Update the top score
        if (score > highScore) {
            highScore = score;
        }
    }
    
    private void addHighScoreEntry(String name, int scoreValue) {
        // Add the new entry
        highScoreList.add(new HighScoreEntry(name, scoreValue));
        // Sort by score descending
        highScoreList.sort((a, b) -> Integer.compare(b.score, a.score));
        // Keep only top 10
        while (highScoreList.size() > MAX_HIGH_SCORES) {
            highScoreList.remove(highScoreList.size() - 1);
        }
        // Update top score
        if (!highScoreList.isEmpty()) {
            highScore = highScoreList.get(0).score;
        }
        saveHighScore();
    }
    
    private void drawText(String text, float x, float y, float scale) {
        if (textRenderer == null) return;
        
        // Convert normalized coordinates to screen coordinates
        // TextRenderer uses bottom-left origin, so y needs to be converted properly
        int screenX = (int)((x + 1.0f) * windowWidth / 2.0f);
        int screenY = (int)((y + 1.0f) * windowHeight / 2.0f);
        
        // Scale font size based on scale parameter
        int fontSize = (int)(scale * 400);
        
        // Create a new text renderer with the appropriate size if needed
        TextRenderer renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, fontSize), true, true);
        
        renderer.beginRendering(windowWidth, windowHeight);
        renderer.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        renderer.draw(text, screenX, screenY);
        renderer.endRendering();
    }
    
    private void handleMenuClick(int clickX, int clickY) {
        // Convert to normalized coordinates
        double normX = (clickX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (clickY / (double) windowHeight) * 2.0;
        
        if (startButton.contains(normX, normY)) {
            // Request game initialization (will happen in OpenGL thread)
            currentLevel = 1; // Start from level 1
            needsGameInit = true;
        } else if (levelSelectButton.contains(normX, normY)) {
            // Go to level selection screen
            gameState = STATE_LEVEL_SELECT;
        } else if (instructionsButton.contains(normX, normY)) {
            // Go to instructions screen
            gameState = STATE_INSTRUCTIONS;
        } else if (exitButton.contains(normX, normY)) {
            // Exit the application
            System.exit(0);
        } else if (highScoresButton.contains(normX, normY)) {
            // Go to high scores screen
            gameState = STATE_HIGH_SCORES;
        } else if (setNameButton.contains(normX, normY)) {
            // Go to edit name screen
            playerName = currentPlayerName;
            enteringName = true;
            gameState = STATE_EDIT_NAME;
        }
    }
    
    private void renderInstructions() {
        gl.glDisable(GL.GL_TEXTURE_2D);
        
        // Title
        gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow
        drawText("HOW TO PLAY", -0.35f, 0.75f, 0.12f);
        
        // Instructions content
        gl.glColor3f(1.0f, 1.0f, 1.0f); // White
        
        drawText("OBJECTIVE:", -0.85f, 0.55f, 0.06f);
        drawText("Collect all the food before time runs out!", -0.85f, 0.45f, 0.045f);
        drawText("Avoid the ghosts or eat power pellets to hunt them.", -0.85f, 0.37f, 0.045f);
        
        drawText("CONTROLS:", -0.85f, 0.2f, 0.06f);
        drawText("Arrow Keys / WASD - Move Pacman", -0.85f, 0.1f, 0.045f);
        drawText("ESC - Pause / Resume Game", -0.85f, 0.02f, 0.045f);
        drawText("R - Restart (when game over)", -0.85f, -0.06f, 0.045f);
        
        drawText("SCORING:", -0.85f, -0.22f, 0.06f);
        drawText("Small Food: 10 points", -0.85f, -0.32f, 0.045f);
        drawText("Power Pellet: 50 points", -0.85f, -0.40f, 0.045f);
        drawText("Eating Ghost: 200 points", -0.85f, -0.48f, 0.045f);
        
        // Back button
        double normX = (mouseX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (mouseY / (double) windowHeight) * 2.0;
        
        boolean backHover = backButton.contains(normX, normY);
        if (backHover) {
            gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow hover
        } else {
            gl.glColor3f(0.0f, 0.5f, 1.0f); // Blue
        }
        drawButton(backButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f); // White text
        drawText("BACK TO MENU", -0.3f, -0.65f, 0.07f);
        
        gl.glEnable(GL.GL_TEXTURE_2D);
    }
    
    private void handleInstructionsClick(int clickX, int clickY) {
        // Convert to normalized coordinates
        double normX = (clickX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (clickY / (double) windowHeight) * 2.0;
        
        if (backButton.contains(normX, normY)) {
            // Go back to menu
            gameState = STATE_MENU;
        }
    }
    
    private void handlePauseClick(int clickX, int clickY) {
        // Convert to normalized coordinates
        double normX = (clickX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (clickY / (double) windowHeight) * 2.0;
        
        if (resumeButton.contains(normX, normY)) {
            // Resume the game
            gameState = STATE_PLAYING;
        } else if (returnToMenuButton.contains(normX, normY)) {
            // Return to main menu
            gameState = STATE_MENU;
            // Reset music
            Game.mainMusic.stopMusic();
        }
    }
    
    private void initLevelButtons() {
        // Arrange level buttons in a grid (3 columns, 2 rows)
        double buttonWidth = 0.25;
        double buttonHeight = 0.2;
        double startX = -0.55;
        double startY = 0.3;
        double spacingX = 0.35;
        double spacingY = 0.35;
        
        for (int i = 0; i < MAX_LEVELS; i++) {
            int row = i / 3;
            int col = i % 3;
            double x = startX + col * spacingX;
            double y = startY - row * spacingY;
            levelButtons[i] = new Rectangle(x, y, buttonWidth, buttonHeight);
        }
    }
    
    private void renderLevelSelect() {
        gl.glDisable(GL.GL_TEXTURE_2D);
        
        // Title
        gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow
        drawText("SELECT LEVEL", -0.35f, 0.7f, 0.12f);
        
        // Convert mouse coordinates
        double normX = (mouseX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (mouseY / (double) windowHeight) * 2.0;
        
        // Draw level buttons
        for (int i = 0; i < MAX_LEVELS; i++) {
            boolean hover = levelButtons[i].contains(normX, normY);
            
            if (hover) {
                gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow hover
            } else {
                // Different colors for different levels
                switch (i) {
                    case 0: gl.glColor3f(0.0f, 0.8f, 0.2f); break; // Green
                    case 1: gl.glColor3f(0.0f, 0.6f, 1.0f); break; // Blue
                    case 2: gl.glColor3f(0.8f, 0.4f, 0.0f); break; // Orange
                    case 3: gl.glColor3f(0.8f, 0.0f, 0.4f); break; // Pink
                    case 4: gl.glColor3f(0.6f, 0.0f, 0.8f); break; // Purple
                }
            }
            
            drawButton(levelButtons[i]);
            
            // Draw level number and info
            gl.glColor3f(1.0f, 1.0f, 1.0f);
            float textX = (float)(levelButtons[i].x + 0.05);
            float textY = (float)(levelButtons[i].y + 0.1);
            drawText("LEVEL " + (i + 1), textX, textY, 0.06f);
            
            // Draw map size info
            gl.glColor3f(0.8f, 0.8f, 0.8f);
            String mapSize = Map.levelDimensions[i][0] + "x" + Map.levelDimensions[i][1];
            drawText(mapSize, textX + 0.02f, textY - 0.08f, 0.04f);
        }
        
        // Back button
        boolean backHover = backButton.contains(normX, normY);
        if (backHover) {
            gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow hover
        } else {
            gl.glColor3f(0.0f, 0.5f, 1.0f); // Blue
        }
        drawButton(backButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f); // White text
        drawText("BACK TO MENU", -0.3f, -0.75f, 0.06f);
        
        gl.glEnable(GL.GL_TEXTURE_2D);
    }
    
    private void handleLevelSelectClick(int clickX, int clickY) {
        // Convert to normalized coordinates
        double normX = (clickX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (clickY / (double) windowHeight) * 2.0;
        
        // Check if a level button was clicked
        for (int i = 0; i < MAX_LEVELS; i++) {
            if (levelButtons[i].contains(normX, normY)) {
                // Set the selected level and start game
                currentLevel = i + 1;
                needsGameInit = true;
                return;
            }
        }
        
        // Check back button
        if (backButton.contains(normX, normY)) {
            gameState = STATE_MENU;
        }
    }
    
    private void renderEnterName() {
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glEnable(GL.GL_BLEND);
        
        // Semi-transparent background overlay
        gl.glColor4f(0.0f, 0.0f, 0.0f, 0.85f);
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex2f(-1.0f, -1.0f);
        gl.glVertex2f(1.0f, -1.0f);
        gl.glVertex2f(1.0f, 1.0f);
        gl.glVertex2f(-1.0f, 1.0f);
        gl.glEnd();
        
        // Title
        gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow
        drawText("NEW HIGH SCORE!", -0.45f, 0.6f, 0.1f);
        
        // Display score
        gl.glColor3f(1.0f, 1.0f, 1.0f); // White
        drawText("SCORE: " + pendingScore, -0.25f, 0.4f, 0.08f);
        
        // Prompt for name
        gl.glColor3f(0.0f, 1.0f, 1.0f); // Cyan
        drawText("ENTER YOUR NAME:", -0.38f, 0.2f, 0.065f);
        
        // Name input box
        gl.glColor3f(0.2f, 0.2f, 0.4f);
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex2f(-0.4f, 0.0f);
        gl.glVertex2f(0.4f, 0.0f);
        gl.glVertex2f(0.4f, 0.15f);
        gl.glVertex2f(-0.4f, 0.15f);
        gl.glEnd();
        
        // Name input border
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        gl.glLineWidth(2.0f);
        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex2f(-0.4f, 0.0f);
        gl.glVertex2f(0.4f, 0.0f);
        gl.glVertex2f(0.4f, 0.15f);
        gl.glVertex2f(-0.4f, 0.15f);
        gl.glEnd();
        
        // Display entered name with blinking cursor
        String displayName = playerName + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        drawText(displayName, -0.35f, 0.03f, 0.08f);
        
        // Get mouse position for hover effects
        double normX = (mouseX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (mouseY / (double) windowHeight) * 2.0;
        
        // Submit button
        boolean submitHover = submitButton.contains(normX, normY);
        if (submitHover) {
            gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow hover
        } else {
            gl.glColor3f(0.0f, 0.7f, 0.0f); // Green
        }
        drawButton(submitButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        drawText("SUBMIT", -0.13f, -0.26f, 0.065f);
        
        // Instructions
        gl.glColor3f(0.7f, 0.7f, 0.7f);
        drawText("Type your name and click SUBMIT or press ENTER", -0.6f, -0.55f, 0.04f);
        
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL.GL_TEXTURE_2D);
    }
    
    private void handleEnterNameClick(int clickX, int clickY) {
        double normX = (clickX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (clickY / (double) windowHeight) * 2.0;
        
        if (submitButton.contains(normX, normY)) {
            submitHighScore();
        }
    }
    
    private void submitHighScore() {
        if (playerName.isEmpty()) {
            playerName = "PLAYER";
        }
        addHighScoreEntry(playerName, pendingScore);
        enteringName = false;
        playerName = "";
        gameState = STATE_HIGH_SCORES;
    }
    
    private void renderHighScores() {
        gl.glDisable(GL.GL_TEXTURE_2D);
        
        // Title
        gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow
        drawText("HIGH SCORES", -0.35f, 0.75f, 0.12f);
        
        // Table header
        gl.glColor3f(0.0f, 1.0f, 1.0f); // Cyan
        drawText("RANK", -0.75f, 0.55f, 0.05f);
        drawText("NAME", -0.35f, 0.55f, 0.05f);
        drawText("SCORE", 0.3f, 0.55f, 0.05f);
        
        // Draw separator line
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        gl.glLineWidth(1.0f);
        gl.glBegin(GL.GL_LINES);
        gl.glVertex2f(-0.8f, 0.5f);
        gl.glVertex2f(0.8f, 0.5f);
        gl.glEnd();
        
        // Draw high scores list
        float yPos = 0.4f;
        for (int i = 0; i < highScoreList.size() && i < MAX_HIGH_SCORES; i++) {
            HighScoreEntry entry = highScoreList.get(i);
            
            // Alternate colors for readability
            if (i % 2 == 0) {
                gl.glColor3f(1.0f, 1.0f, 1.0f); // White
            } else {
                gl.glColor3f(0.8f, 0.8f, 0.8f); // Light gray
            }
            
            // Highlight top 3
            if (i == 0) gl.glColor3f(1.0f, 0.84f, 0.0f); // Gold
            else if (i == 1) gl.glColor3f(0.75f, 0.75f, 0.75f); // Silver
            else if (i == 2) gl.glColor3f(0.8f, 0.5f, 0.2f); // Bronze
            
            drawText(String.format("%2d.", i + 1), -0.75f, yPos, 0.045f);
            drawText(entry.name, -0.35f, yPos, 0.045f);
            drawText(String.valueOf(entry.score), 0.3f, yPos, 0.045f);
            
            yPos -= 0.09f;
        }
        
        // If no scores yet
        if (highScoreList.isEmpty()) {
            gl.glColor3f(0.7f, 0.7f, 0.7f);
            drawText("No high scores yet!", -0.3f, 0.2f, 0.06f);
        }
        
        // Get mouse position for hover effects
        double normX = (mouseX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (mouseY / (double) windowHeight) * 2.0;
        
        // Back button
        boolean backHover = backButton.contains(normX, normY);
        if (backHover) {
            gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow hover
        } else {
            gl.glColor3f(0.0f, 0.5f, 1.0f); // Blue
        }
        drawButton(backButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        drawText("BACK TO MENU", -0.3f, -0.75f, 0.06f);
        
        gl.glEnable(GL.GL_TEXTURE_2D);
    }
    
    private void handleHighScoresClick(int clickX, int clickY) {
        double normX = (clickX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (clickY / (double) windowHeight) * 2.0;
        
        if (backButton.contains(normX, normY)) {
            gameState = STATE_MENU;
        }
    }
    
    private void renderEditName() {
        gl.glDisable(GL.GL_TEXTURE_2D);
        
        // Title
        gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow
        drawText("CHANGE PLAYER NAME", -0.5f, 0.6f, 0.1f);
        
        // Instructions
        gl.glColor3f(0.8f, 0.8f, 0.8f);
        drawText("Current name: " + currentPlayerName, -0.35f, 0.4f, 0.055f);
        
        // Prompt for new name
        gl.glColor3f(0.0f, 1.0f, 1.0f); // Cyan
        drawText("ENTER NEW NAME:", -0.35f, 0.2f, 0.065f);
        
        // Name input box
        gl.glColor3f(0.2f, 0.2f, 0.4f);
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex2f(-0.4f, 0.0f);
        gl.glVertex2f(0.4f, 0.0f);
        gl.glVertex2f(0.4f, 0.15f);
        gl.glVertex2f(-0.4f, 0.15f);
        gl.glEnd();
        
        // Name input border
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        gl.glLineWidth(2.0f);
        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex2f(-0.4f, 0.0f);
        gl.glVertex2f(0.4f, 0.0f);
        gl.glVertex2f(0.4f, 0.15f);
        gl.glVertex2f(-0.4f, 0.15f);
        gl.glEnd();
        
        // Display entered name with blinking cursor
        String displayName = playerName + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        drawText(displayName, -0.35f, 0.03f, 0.08f);
        
        // Get mouse position for hover effects
        double normX = (mouseX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (mouseY / (double) windowHeight) * 2.0;
        
        // Save button
        boolean submitHover = submitButton.contains(normX, normY);
        if (submitHover) {
            gl.glColor3f(1.0f, 1.0f, 0.0f); // Yellow hover
        } else {
            gl.glColor3f(0.0f, 0.7f, 0.0f); // Green
        }
        drawButton(submitButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        drawText("SAVE", -0.08f, -0.26f, 0.065f);
        
        // Cancel button (use back button position)
        boolean backHover = backButton.contains(normX, normY);
        if (backHover) {
            gl.glColor3f(1.0f, 0.0f, 0.0f); // Red hover
        } else {
            gl.glColor3f(0.5f, 0.5f, 0.5f); // Gray
        }
        drawButton(backButton);
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        drawText("CANCEL", -0.15f, -0.76f, 0.065f);
        
        // Instructions
        gl.glColor3f(0.7f, 0.7f, 0.7f);
        drawText("Type your name and click SAVE or press ENTER", -0.55f, -0.5f, 0.04f);
        drawText("Press ESC or click CANCEL to go back", -0.45f, -0.58f, 0.04f);
        
        gl.glEnable(GL.GL_TEXTURE_2D);
    }
    
    private void handleEditNameClick(int clickX, int clickY) {
        double normX = (clickX / (double) windowWidth) * 2.0 - 1.0;
        double normY = 1.0 - (clickY / (double) windowHeight) * 2.0;
        
        if (submitButton.contains(normX, normY)) {
            submitEditedName();
        } else if (backButton.contains(normX, normY)) {
            // Cancel - go back to menu
            enteringName = false;
            gameState = STATE_MENU;
        }
    }
    
    private void submitEditedName() {
        if (playerName.isEmpty()) {
            playerName = "PLAYER";
        }
        currentPlayerName = playerName;
        enteringName = false;
        playerName = "";
        gameState = STATE_MENU;
    }
    
    // Simple Rectangle class for button bounds
    private static class Rectangle {
        double x, y, width, height;
        
        Rectangle(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        boolean contains(double px, double py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }
    
    // High score entry class to store player name and score
    private static class HighScoreEntry {
        String name;
        int score;
        
        HighScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }
}
