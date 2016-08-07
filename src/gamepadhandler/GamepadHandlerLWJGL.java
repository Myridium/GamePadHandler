/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gamepadhandler;

import org.lwjgl.Version;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 *
 * @author murdock
 */
public class GamepadHandlerLWJGL {
    //The window handle
    private long window;
    
    private final long minFrameWaitNanoTime = Math.round(1000*1000*1000/60);
    private final int WIDTH = 1000;
    private final int HEIGHT = 600;
    private final float SCALE = 1.0f;
    
    
    private final boolean FPS_CAPPED = true;
    
    private GamepadHandlerFrameLooper frameLooper;
    
    private int displayWidth() {
        return Math.round(WIDTH*SCALE);
    }
    private int displayHeight() {
        return Math.round(HEIGHT*SCALE);
    }
    public void run() throws InterruptedException {
        System.out.println("LWJGL Version: " + Version.getVersion());
        
        try {
            init();
            loop();
            
            //Free the window callbacks and destroy the window
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        }  finally {
            glfwTerminate();
            glfwSetErrorCallback(null).free();
        }
    }
    
    private void init() {
        
        
        // Setup an error callback. The default implementation
	// will print the error message in System.err.
	GLFWErrorCallback.createPrint(System.err).set();
        
        // Initialise GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Failed to initialise GLFW.");
        
        // Configure the settings with which a new window will be created.
        // It will later be created and the handle assigned to gameWindow.
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // The window should be invisible until we're ready to display it.
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        
        // Create the window and assign the handle to gameWindow
        // Note that the NULL object is a constant defined in the LWJGL libraries.
        window = glfwCreateWindow(displayWidth(),displayHeight(), "Gamepad Configuration", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window!");
        
        // Setup a key callback. It will be called every time a key is presed, repeated or released.
        glfwSetKeyCallback(window, (gw, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(gw,true);
        });
        
        // Get the resolution of the primary monitor
        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidmode = glfwGetVideoMode(monitor);
        // Get the position of the primary monitor
        int[] xbuff = new int[1], ybuff = new int[1];
        glfwGetMonitorPos(monitor,xbuff,ybuff);
        
        // Center the window
        glfwSetWindowPos(window,
                (vidmode.width() - displayWidth()) / 2 + xbuff[0],
                (vidmode.height() - displayHeight()) / 2 + ybuff[0]
        );
        
        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);
        
        // Finally, make our configured window visible to the user
        glfwShowWindow(window);
        
        frameLooper = new GamepadHandlerFrameLooper();
        
    }
    
    public void loop() throws InterruptedException {
        
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
        GL11.glMatrixMode(GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, WIDTH, 0, HEIGHT, -1, 1);
        GL11.glMatrixMode(GL_MODELVIEW);
        
        GL11.glDisable(GL_DEPTH_TEST);
        GL11.glEnable(GL_BLEND);
        GL11.glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);

        frameLooper.init(window, WIDTH, HEIGHT);
        
        boolean keepRunning = true;
        
        long currentTime = System.nanoTime();
        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) && keepRunning) {
                
                long elapsedNanoTime;
            
                // Putting on a frame cap (of ~120fps when this comment was made)
                if (FPS_CAPPED) {
                    while( System.nanoTime() - currentTime < minFrameWaitNanoTime) {
                        Thread.sleep(2);
                    }
                }
                
                elapsedNanoTime = System.nanoTime() - currentTime;
                currentTime = System.nanoTime();
            
                glClear(GL_COLOR_BUFFER_BIT); // clear the framebuffer

                // Poll for window events. The key callback above will only be
                // invoked during this call.
                glfwPollEvents();

                
                // Where all the game logic is handled:
                keepRunning = frameLooper.frame();
                
                
                glfwSwapBuffers(window); // swap the color buffers
                
                
        }
        
        
        
        
    }
}
