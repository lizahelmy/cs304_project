import javax.media.opengl.GL;

import java.util.ArrayList;
import java.util.Arrays;
import Texture.TextureReader;

public abstract class Entity {
    int x = -1, y = -1; // place on the field ;
    int prevX = -1, prevY = -1; // previous position for collision detection
    int width = 0; // width of the entity on the screen
    boolean render = false, destroyed = false;
    ArrayList<String> Textures = new ArrayList<>();
    TextureReader.Texture texture[] ;
    int textures[] ;

    Entity(int x, int y, boolean render, String[] texturesStrings) {
        this.x = x;
        this.y = y;
        this.prevX = x;
        this.prevY = y;
        this.render = render;
        Textures.addAll(Arrays.asList(texturesStrings));
        texture = new TextureReader.Texture[Textures.size()];
        textures = new int [Textures.size()];
        if (eventListener.gl != null) {
            eventListener.gl.glGenTextures(texturesStrings.length, textures, 0);
        }
    }
    
    // Call this before updating position
    public void savePreviousPosition() {
        this.prevX = this.x;
        this.prevY = this.y;
    }

    abstract public void update();


    abstract public void render(GL gl); // a function to render entity on the screen

    abstract public void destroy(); // to destroy an entity at the end of its life span ;
}