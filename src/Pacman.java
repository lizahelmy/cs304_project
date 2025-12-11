import javax.media.opengl.GL;
import java.awt.event.KeyEvent;

public class Pacman extends Entity {
    private int dirX = 0; // current direction X
    private int dirY = 0; // current direction Y
    private int nextDirX = 0; // next desired direction X
    private int nextDirY = 0; // next desired direction Y
    private int lives = 3;
    private int textureIndex = 0; // 0=right, 1=left, 2=up, 3=down
    private boolean powered = false;
    private int powerTimer = 0;

    public Pacman(int x, int y, boolean render, String[] texturesStrings) {
        super(x, y, render, texturesStrings);
        this.width = 3;
    }

    public void setDirection(int dx, int dy) {
        this.nextDirX = dx;
        this.nextDirY = dy;
    }

    public int getDirX() {
        return dirX;
    }

    public int getDirY() {
        return dirY;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
        if (powered) {
            this.powerTimer = 180; // about 10 seconds at 18 FPS
        }
    }

    public boolean isPowered() {
        return powered;
    }

    public int getLives() {
        return lives;
    }

    public void loseLife() {
        lives--;
    }

    public void resetPosition(int x, int y) {
        this.x = x;
        this.y = y;
        this.dirX = 0;
        this.dirY = 0;
        this.nextDirX = 0;
        this.nextDirY = 0;
    }

    @Override
    public void update() {
        if (powered) {
            powerTimer--;
            if (powerTimer <= 0) {
                powered = false;
            }
        }

        // Try to turn in the desired direction
        int testX = x + nextDirX;
        int testY = y + nextDirY;
        if (canMove(testX, testY)) {
            dirX = nextDirX;
            dirY = nextDirY;
        }

        // Move in current direction
        int newX = x + dirX;
        int newY = y + dirY;
        
        // Handle tunnel wrapping (tunnels are at the edges of the map)
        if (newX < 0) {
            newX = eventListener.maxWidth - 1;
        } else if (newX >= eventListener.maxWidth) {
            newX = 0;
        }
        
        if (canMove(newX, newY)) {
            x = newX;
            y = newY;
        }

        // Update texture based on direction
        if (dirX == 1) textureIndex = 0; // right
        else if (dirX == -1) textureIndex = 1; // left
        else if (dirY == -1) textureIndex =3 ;// up texture when dirY is -1 (UP key)
        else if (dirY == 1) textureIndex = 2; // down texture when dirY is 1 (DOWN key)
    }

    private boolean canMove(int x, int y) {
        if (eventListener.currentMap == null) return false;
        if (x < 0 || y < 0 || y >= eventListener.currentMap.length || x >= eventListener.currentMap[0].length)
            return false;
        int tile = eventListener.currentMap[y][x];
        // Pacman can move on paths (0) and tunnels (3), but NOT ghost house (2) or walls (1)
        return tile == 0 || tile == 3;
    }

    @Override
    public void render(GL gl) {
        if (!render || textures == null || textures.length == 0) return;
        
        gl.glEnable(GL.GL_BLEND);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[textureIndex % textures.length]);
        gl.glPushMatrix();
        
        gl.glTranslated(x * (2.0 / eventListener.maxWidth) - 1.0 + (1.0 / eventListener.maxWidth),
                       y * (2.0 / eventListener.maxHeight) - 1.0 + (1.0 / eventListener.maxHeight), 0);
        
        // Dynamic scale based on map size
        double tileScale = Math.min(1.0 / eventListener.maxWidth, 1.0 / eventListener.maxHeight) * 0.9;
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
        gl.glDisable(GL.GL_BLEND);
    }

    @Override
    public void destroy() {
        // cleanup if needed
    }
}
