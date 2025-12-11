import javax.media.opengl.GL;
import java.awt.*;



public class Ghost extends Entity {

    private String name;
    private int textureIndex = 0; // 0=normal, 1=frightened
    private int moveCounter = 0;
    private int moveDelay = 0; // Each ghost has a different delay


    public Ghost(int x, int y, boolean render, String[] textureStrings, String name) {
        super(x, y, render, textureStrings);
        this.name = name.toLowerCase(); // normalize
        this.width = 3;
        
        // Set different delays for each ghost
        switch (this.name) {
            case "blinky": moveDelay = 0; break;  // Fastest
            case "pinky": moveDelay = 1; break;
            case "inky": moveDelay = 2; break;
            case "clyde": moveDelay = 3; break;  // Slowest
        }
        moveCounter = moveDelay; // Start with offset
    }

    @Override
    public void update() {
        // Only move every few frames based on ghost's delay
        moveCounter++;
        if (moveCounter <= moveDelay) {
            // Update texture but don't move yet
            textureIndex = GhostAI.isFrightened() ? 1 : 0;
            return;
        }
        moveCounter = 0;

        Point next;

        switch (name) {

            case "blinky":
                next = GhostAI.blinkyMove(this.x, this.y);
                this.x = next.x;
                this.y = next.y;
                break;

            case "pinky":
                next = GhostAI.pinkyMove(this.x, this.y);
                this.x = next.x;
                this.y = next.y;
                break;

            case "inky":
                next = GhostAI.inkyMove(this.x, this.y);
                this.x = next.x;
                this.y = next.y;
                break;

            case "clyde":
                next = GhostAI.clydeMove(this.x, this.y);
                this.x = next.x;
                this.y = next.y;
                break;
        }
        
        // Update texture based on frightened mode
        textureIndex = GhostAI.isFrightened() ? 1 : 0;
    }


    private void updateBlinky() {
        // Blinky directly targets Pac-Man
        Point next = GhostAI.blinkyMove(this.x, this.y);
        this.x = next.x;
        this.y = next.y;
    }

    private void updatePinky() {
        Point next = GhostAI.pinkyMove(this.x, this.y);
        this.x = next.x;
        this.y = next.y;
    }

    private void updateInky() {
        Point next = GhostAI.inkyMove(this.x, this.y);
        this.x = next.x;
        this.y = next.y;
    }

    private void updateClyde() {
        Point next = GhostAI.clydeMove(this.x, this.y);
        this.x = next.x;
        this.y = next.y;
    }


    @Override
    public void render(GL gl) {
        if (!render || textures == null || textures.length == 0) return;
        
        gl.glEnable(GL.GL_BLEND);
        
        // Use frightened texture if available (index 1), else use normal texture (index 0)
        int texIdx = (textureIndex < textures.length) ? textureIndex : 0;
        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[texIdx]);
        
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
        // add cleanup if needed
    }
}
