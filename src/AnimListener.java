/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import javax.media.opengl.GLEventListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;


public abstract class AnimListener implements GLEventListener, KeyListener , MouseListener, MouseMotionListener, MouseWheelListener {
 
    protected String assetsFolderName = "Assets";
    
}
