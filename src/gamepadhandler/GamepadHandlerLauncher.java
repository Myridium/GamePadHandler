/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gamepadhandler;

import java.io.File;
import java.net.URISyntaxException;

/**
 *
 * @author murdock
 */
public class GamepadHandlerLauncher {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        String s = new File(GamepadHandlerLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
        s = s + "/lib/native";
        System.out.println("Setting LWJGL library path to:");
        System.out.println(s);
        if (System.getProperty("Djava.library.path") == null && System.getProperty("org.lwjgl.librarypath") == null ) {
            System.setProperty("org.lwjgl.librarypath", s);
        }
        
        
        new GamepadHandlerLWJGL().run();
    }
    
}
