import java.awt.Point;
import java.util.*;

public class GhostAI {

    // Game mode constants
    public static final int MODE_SCATTER = 0;
    public static final int MODE_CHASE = 1;
    public static final int MODE_FRIGHTENED = 2;
    
    // Current mode for all ghosts
    public static int currentMode = MODE_CHASE;
    public static int modeTimer = 0;
    public static int frightenedTimer = 0;
    
    // Mode durations (in frames at 18 FPS / UPDATE_INTERVAL)
    private static final int SCATTER_DURATION = 70;
    private static final int CHASE_DURATION = 200;
    private static final int FRIGHTENED_DURATION = 120;
    
    // Your game map (1 = wall, 0 = path, 2 = ghost house, 3 = tunnel)
    public static int[][] map;

    // Pac-Man position and direction
    public static int pacmanX;
    public static int pacmanY;
    public static int pacDirX;  // -1, 1, or 0
    public static int pacDirY;  // -1, 1, or 0

    // Blinky position (Inky needs this)
    public static int blinkyX;
    public static int blinkyY;

    // Corner targets for scatter mode - classic Pac-Man corners (28x31 map)
    public static Point blinkyCorner = new Point(26, 1);  // Top-right
    public static Point pinkyCorner = new Point(1, 1);    // Top-left
    public static Point inkyCorner = new Point(26, 29);   // Bottom-right
    public static Point clydeCorner = new Point(1, 29);   // Bottom-left
    
    // Update scatter corners based on map dimensions
    public static void updateScatterCorners() {
        if (map == null || map.length == 0 || map[0].length == 0) return;
        
        int mapWidth = map[0].length;
        int mapHeight = map.length;
        
        // Set corners based on actual map dimensions
        blinkyCorner = new Point(mapWidth - 2, 1);        // Top-right
        pinkyCorner = new Point(1, 1);                     // Top-left
        inkyCorner = new Point(mapWidth - 2, mapHeight - 2); // Bottom-right
        clydeCorner = new Point(1, mapHeight - 2);         // Bottom-left
    }
    
    // Random for frightened mode
    private static Random random = new Random();

    // Directions: UP, DOWN, LEFT, RIGHT
    private static final int[][] DIRS = {
            {0, -1},   // UP
            {0,  1},   // DOWN
            {-1, 0},   // LEFT
            {1,  0}    // RIGHT
    };

    // ───────────────────────────────────────────────
    // MODE MANAGEMENT
    // ───────────────────────────────────────────────
    public static void updateMode() {
        if (currentMode == MODE_FRIGHTENED) {
            frightenedTimer--;
            if (frightenedTimer <= 0) {
                // Return to chase mode after frightened, not scatter
                currentMode = MODE_CHASE;
                modeTimer = CHASE_DURATION;
            }
            return;
        }
        
        modeTimer--;
        if (modeTimer <= 0) {
            if (currentMode == MODE_SCATTER) {
                currentMode = MODE_CHASE;
                modeTimer = CHASE_DURATION;
            } else {
                currentMode = MODE_SCATTER;
                modeTimer = SCATTER_DURATION;
            }
        }
    }
    
    public static void setFrightened() {
        currentMode = MODE_FRIGHTENED;
        frightenedTimer = FRIGHTENED_DURATION;
    }
    
    public static boolean isFrightened() {
        return currentMode == MODE_FRIGHTENED;
    }

    // ───────────────────────────────────────────────
    // BASIC UTILITIES
    // ───────────────────────────────────────────────
    public static boolean canMove(int x, int y) {
        // Safety check
        if (map == null || map.length == 0 || map[0].length == 0) {
            return false;
        }
        
        // Handle tunnel wrapping
        if (x < 0) x = map[0].length - 1;
        else if (x >= map[0].length) x = 0;
        
        if (y < 0 || y >= map.length)
            return false;
        int tile = map[y][x];
        // 0 = path, 2 = ghost house, 3 = tunnel - all are walkable for ghosts
        return tile == 0 || tile == 2 || tile == 3;
    }
    
    // Wrap coordinates for tunnel
    private static int wrapX(int x) {
        if (map == null || map.length == 0 || map[0].length == 0) return x;
        if (x < 0) return map[0].length - 1;
        if (x >= map[0].length) return 0;
        return x;
    }

    // ───────────────────────────────────────────────
    // BFS SHORTEST PATH
    // ───────────────────────────────────────────────
    public static Point bfsNextStep(int startX, int startY, int targetX, int targetY) {
        // Safety check - if map is not initialized, return current position
        if (map == null || map.length == 0 || map[0].length == 0) {
            return new Point(startX, startY);
        }

        boolean[][] visited = new boolean[map.length][map[0].length];
        Queue<Point> q = new LinkedList<>();
        HashMap<Point, Point> parentMap = new HashMap<>();

        Point start = new Point(startX, startY);
        Point target = new Point(targetX, targetY);

        q.add(start);
        visited[startY][startX] = true;

        while (!q.isEmpty()) {
            Point cur = q.poll();

            if (cur.equals(target)) break;

            for (int[] d : DIRS) {
                int nx = wrapX(cur.x + d[0]); // Handle tunnel wrapping
                int ny = cur.y + d[1];

                if (canMove(nx, ny) && !visited[ny][nx]) {
                    visited[ny][nx] = true;
                    Point nxt = new Point(nx, ny);
                    parentMap.put(nxt, cur);
                    q.add(nxt);
                }
            }
        }

        // Backtrack one step from the target
        Point step = target;
        while (parentMap.containsKey(step) && !parentMap.get(step).equals(start)) {
            step = parentMap.get(step);
        }

        return step;
    }
    
    // ───────────────────────────────────────────────
    // FRIGHTENED MODE MOVEMENT (random but valid)
    // ───────────────────────────────────────────────
    private static Point frightenedMove(int x, int y) {
        ArrayList<Point> validMoves = new ArrayList<>();
        
        for (int[] d : DIRS) {
            int nx = wrapX(x + d[0]); // Handle tunnel wrapping
            int ny = y + d[1];
            if (canMove(nx, ny)) {
                validMoves.add(new Point(nx, ny));
            }
        }
        
        if (validMoves.isEmpty()) {
            return new Point(x, y);
        }
        
        return validMoves.get(random.nextInt(validMoves.size()));
    }

    // ───────────────────────────────────────────────
    // BLINKY (RED) – Direct chase or scatter to top-right
    // ───────────────────────────────────────────────
    public static Point blinkyMove(int x, int y) {
        if (currentMode == MODE_FRIGHTENED) {
            Point next = frightenedMove(x, y);
            blinkyX = next.x;
            blinkyY = next.y;
            return next;
        }
        
        Point target = (currentMode == MODE_SCATTER) ? blinkyCorner : new Point(pacmanX, pacmanY);
        Point next = bfsNextStep(x, y, target.x, target.y);
        blinkyX = next.x;
        blinkyY = next.y;
        return next;
    }

    // ───────────────────────────────────────────────
    // PINKY (PINK) – 4 tiles ahead of Pac-Man or scatter to top-left
    // ───────────────────────────────────────────────
    public static Point pinkyMove(int x, int y) {
        if (currentMode == MODE_FRIGHTENED) {
            return frightenedMove(x, y);
        }
        
        Point target;
        if (currentMode == MODE_SCATTER) {
            target = pinkyCorner;
        } else {
            int tx = pacmanX + pacDirX * 4;
            int ty = pacmanY + pacDirY * 4;

            // Clamp to valid map bounds
            tx = Math.max(0, Math.min(tx, map[0].length - 1));
            ty = Math.max(0, Math.min(ty, map.length - 1));

            if (!canMove(tx, ty)) {
                tx = pacmanX;
                ty = pacmanY;
            }
            target = new Point(tx, ty);
        }

        return bfsNextStep(x, y, target.x, target.y);
    }

    // ───────────────────────────────────────────────
    // INKY (BLUE) – Vector using Pac-Man + Blinky or scatter to bottom-right
    // ───────────────────────────────────────────────
    public static Point inkyMove(int x, int y) {
        if (currentMode == MODE_FRIGHTENED) {
            return frightenedMove(x, y);
        }
        
        Point target;
        if (currentMode == MODE_SCATTER) {
            target = inkyCorner;
        } else {
            int px = pacmanX + pacDirX * 2;
            int py = pacmanY + pacDirY * 2;

            int tx = px + (px - blinkyX);
            int ty = py + (py - blinkyY);

            // Clamp to valid map bounds
            tx = Math.max(0, Math.min(tx, map[0].length - 1));
            ty = Math.max(0, Math.min(ty, map.length - 1));

            if (!canMove(tx, ty)) {
                tx = pacmanX;
                ty = pacmanY;
            }
            target = new Point(tx, ty);
        }

        return bfsNextStep(x, y, target.x, target.y);
    }

    // ───────────────────────────────────────────────
    // CLYDE (ORANGE) – Chase if far; else scatter to bottom-left
    // ───────────────────────────────────────────────
    public static Point clydeMove(int x, int y) {
        if (currentMode == MODE_FRIGHTENED) {
            return frightenedMove(x, y);
        }
        
        int dx = x - pacmanX;
        int dy = y - pacmanY;
        int dist2 = dx * dx + dy * dy;

        Point target;
        if (currentMode == MODE_SCATTER || dist2 <= 64) {
            target = clydeCorner;
        } else {
            target = new Point(pacmanX, pacmanY);
        }

        return bfsNextStep(x, y, target.x, target.y);
    }
}
