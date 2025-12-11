import javax.media.opengl.GL;

public class Food extends Entity {
    private boolean collected = false;
    private boolean isPower = false;

    public Food(int x, int y, boolean render, String[] texturesStrings, boolean isPower) {
        super(x, y, render, texturesStrings);
        this.width = 2;
        this.isPower = isPower;
    }

    public boolean isCollected() {
        return collected;
    }

    public void collect() {
        collected = true;
        render = false;
    }

    public boolean isPowerFood() {
        return isPower;
    }

    @Override
    public void update() {
        // Food is static, no update needed
    }

    @Override
    public void render(GL gl) {
        if (!render || collected || textures == null || textures.length == 0) return;
        
        gl.glEnable(GL.GL_BLEND);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[0]);
        gl.glPushMatrix();
        
        gl.glTranslated(x * (2.0 / eventListener.maxWidth) - 1.0 + (1.0 / eventListener.maxWidth),
                       y * (2.0 / eventListener.maxHeight) - 1.0 + (1.0 / eventListener.maxHeight), 0);
        
        // Dynamic scale based on map size
        double baseScale = Math.min(1.0 / eventListener.maxWidth, 1.0 / eventListener.maxHeight);
        double scale = isPower ? baseScale * 0.7 : baseScale * 0.4;
        gl.glScaled(scale, scale, 1);
        
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
