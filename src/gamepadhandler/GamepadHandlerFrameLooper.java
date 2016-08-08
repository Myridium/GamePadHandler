/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gamepadhandler;

import LWJGLTools.GLDrawing.GLDrawHelper;
import LWJGLTools.input.ControllerReader;
import LWJGLTools.input.ControllerReader.*;
import LWJGLTools.input.KeyTracker;
import java.awt.Color;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.lwjgl.glfw.GLFW.*;

/**
 *
 * @author murdock
 */
public class GamepadHandlerFrameLooper {
    
    private int WIDTH;
    private int HEIGHT;
    private long WINDOW;
    
    private int stage;
    private KeyTracker spaceTracker;
    
    private ControllerReader cr;
    private HashMap<AxisID,Float[]> AxesMinMax;
    private HashMap<AxisID,Float> AxesNeutral;
    
    // The main axis being used:
    private AxisID axisID;
    private float min, max;
    
    // The button being pressed
    private ButtonID bid;
    
    boolean APressed;
    boolean canProceed;
    
    
    // Variables used to create a ControllerReader object.
    private Axis lxaxis, lyaxis, rxaxis, ryaxis, ltaxis, rtaxis;
    private boolean ltreverse, rtreverse;
    
    public void init(long window, int width, int height) {
        WINDOW = window;
        WIDTH = width;
        HEIGHT = height;
        
        stage = 0;
        spaceTracker = new KeyTracker(WINDOW,GLFW_KEY_SPACE);
        cr = new ControllerReader();
        AxesMinMax = new HashMap<>();
        AxesNeutral = new HashMap<>();
        
        axisID = null;
        bid = null;
        min = 0;
        max = 0;
        canProceed = true;
        APressed = false;
    }
    
    public boolean frame() {
        
        footer();
        try {
            updateAxes();
            
            boolean AFreshlyPressed;
            if (APressed) {
                AFreshlyPressed = false;
                updateAPress();
            } else {
                AFreshlyPressed = updateAPress();
            }
        
            switch(stage) {
                case 0:
                    message(instruction(stage));
                    break;
                case 1:
                    message(instruction(stage));
                    updateButton();
                    if (bid != null) {
                        canProceed = true;
                        AFreshlyPressed = true;
                    }
                    break;
                case 2:
                case 3:
                case 5:
                case 6:
                case 8:
                case 9:
                    axisID = findUserAxis();

                    if (axisID == null) {
                        message(instruction(stage));
                        break;
                    }

                    canProceed = true;
                    message("You're using Axis:" + String.valueOf(axisID.value()));
                    min = AxesMinMax.get(axisID)[0];
                    max = AxesMinMax.get(axisID)[1];
                    drawMinMax(min,max);

                    break;
                
                // This is when the user must hold one of the sticks to the top-right.
                // At this stage, the lxaxis, lyaxis, rxaxis and ryaxis object should already be defined.
                // Though they may have to be redefined to reverse their orientation.
                    
                // For reliability, it is assumed that each axis can vary independently across its full range,
                // And so the maximum magnitude is actually root 2. Checking for a magnitude greater than 1
                // Ensures that the user is not pointing the stick in one of the four cardinal directions.
                // I tried a threshold of 1.2 but this did not work.
                case 4:
                    if (canProceed) {
                        message("Release stick and continue.");
                    } else {
                        message(instruction(stage));
                        float mag = (float)Math.sqrt(Math.pow(lxaxis.value(-1, 1),2) + Math.pow(lyaxis.value(-1, 1), 2));
                        if(mag > 1.1)
                            canProceed = true;
                    }
                    break;
                case 7:
                    if (canProceed) {
                        message("Release stick and continue.");
                    } else {
                        message(instruction(stage));
                        float mag = (float)Math.sqrt(Math.pow(rxaxis.value(-1, 1),2) + Math.pow(ryaxis.value(-1, 1), 2));
                        if(mag > 1.1)
                            canProceed = true;
                    }
                    break;
                case 10:
                    message(instruction(stage));
                    canProceed = true;
                    break;
                default:
                    message("Done.");
                    canProceed = true;
                    break;
            }
        
        if (canProceed && 
                (spaceTracker.isFreshlyPressed() || AFreshlyPressed)
                ) {
            if (stage > 10)
                return false;
            
            
            updateControllerConfig(stage);
            resetAxesMinMax();
            stage++;
            
            canProceed = false;
            
        }
                
        } catch (NoControllerException ex) {
            message("No controller found.");
        } catch (NoSuchAxisException ex) {
            message("A problem occurred.");
            return false;
        }
        // Returns true if it should should keep running
        return true;
    }
    
    private void footer() {
        GLDrawHelper.setColor(Color.WHITE);
        /*
        if (stage < 1) {
            GLDrawHelper.drawString(WIDTH/2-250, 50, "Press SPACE to advance.", 4);
        } else {
            GLDrawHelper.drawString(WIDTH/2-250, 50, "Press SPACE or A button to advance.", 4);
        }
        */
        
        if (stage < 1) {
            GLDrawHelper.drawString(WIDTH/2, 50, "Press SPACE to advance.", 4, GLDrawHelper.TextAlignment.MIDDLE_TOP);
        } else {
            GLDrawHelper.drawString(WIDTH/2, 50, "Press SPACE or A button to advance.", 4, GLDrawHelper.TextAlignment.MIDDLE_TOP);
        }
    }
    
    private void message(String s) {
        int size = 3;
        GLDrawHelper.setColor(Color.YELLOW);
        GLDrawHelper.drawString(WIDTH/2, HEIGHT/2+100, s, size,GLDrawHelper.TextAlignment.MIDDLE_TOP);
    }
    
    private String instruction(int st) {
        switch(st) {
            case 0:
                return "Release all controls.";
            case 1:
                return "Press the A button.";
            case 2:
                return "Move the left stick through its full horizontal range.";
            case 3:
                return "Move the left stick through its full vertical range.";
            case 4:
                return "Move the left stick diagonally to the top-right.";
            case 5:
                return "Move the right stick through its full horizontal range.";
            case 6:
                return "Move the right stick through its full vertical range.";
            case 7:
                return "Move the right stick diagonally to the top-right.";
            case 8:
                return "Fully press and release the left trigger several times.";
            case 9:
                return "Fully press and release the right trigger several times.";
            case 10:
                return "Continue to create the configuration file.";
            default:
                return "Done.";
        }
    }
    
    private void drawMinMax(float min, float max) {
        int size = 5;
        GLDrawHelper.setColor(Color.CYAN);
        GLDrawHelper.drawString(WIDTH/2-200, HEIGHT/2, "Min:", size, GLDrawHelper.TextAlignment.MIDDLE_TOP);
        GLDrawHelper.drawString(WIDTH/2+200, HEIGHT/2, "Max:", size, GLDrawHelper.TextAlignment.MIDDLE_TOP);
        GLDrawHelper.drawString(WIDTH/2-200, HEIGHT/2-100, String.valueOf(min), size, GLDrawHelper.TextAlignment.MIDDLE_TOP);
        GLDrawHelper.drawString(WIDTH/2+200, HEIGHT/2-100, String.valueOf(max), size, GLDrawHelper.TextAlignment.MIDDLE_TOP);
    }
    
    private boolean updateAPress() {
        try {
            APressed = cr.isButtonPressed(Button.A);
            } catch (NotConfiguredException | NoControllerException | NoSuchButtonException ex) {
            APressed = false;
        }
        
        return APressed;
        
    }
    
    private void updateAxes() throws NoControllerException {
        for (AxisID aid : AxisID.values()) {
            float rawValue, existingMin, existingMax;
            Float[] minMax;
            
            try {
                rawValue = ControllerReader.rawAxisValue(ControllerReader.ControllerID.ONE, aid);
            } catch (NoSuchAxisException ex) {
                continue;
            }
            
            try {
                existingMin = AxesMinMax.get(aid)[0];
                existingMax = AxesMinMax.get(aid)[1];
                minMax = new Float[]{
                    Math.min(rawValue, existingMin),
                    Math.max(rawValue, existingMax)
                };
            } catch (NullPointerException e) {
                // No recorded data for this axis yet.
                minMax = new Float[]{rawValue,rawValue};
            }
            
            AxesMinMax.put(aid, minMax);
            
        }
    }
    
    private void resetAxesMinMax() {
        AxesMinMax = new HashMap<AxisID, Float[]>();
    }
    
    private void updateButton() throws NoControllerException {
        ControllerID cid = ControllerReader.ControllerID.ONE;
        
        for (ButtonID BID : ControllerReader.ButtonID.values()) {
            try {
                if (ControllerReader.isRawButtonPressed(cid, BID)) {
                    bid = BID;
                }
            } catch (NoSuchButtonException ex) {
                continue;
            }
        }
    }
    
    private AxisID findUserAxis() {
        AxisID axis = null;
        
        float range, nextRange;
        range = 0;
        
        for (Entry<AxisID,Float[]> entry : AxesMinMax.entrySet()) {
            nextRange = entry.getValue()[1] - entry.getValue()[0];
            if (nextRange > range) {
                range = nextRange;
                if (range > 0.15)
                    axis = entry.getKey();
            }
        }
        
        return axis;
    }
    
    private void updateControllerConfig(int stageCompleted) throws NoControllerException {
        
            float avX, avY, avXExisting, avYExisting, avTrig;
                    
            switch(stageCompleted) {
                case 0:
                    for (AxisID aid : AxisID.values()) {
                        float rawValue;

                        try {
                            rawValue = ControllerReader.rawAxisValue(ControllerReader.ControllerID.ONE, aid);
                        } catch (NoSuchAxisException ex) {
                            continue;
                        }

                        AxesNeutral.put(aid, rawValue);

                    }
                    break;
                case 1:
                    ButtonContainer bc = new ButtonContainer(ControllerID.ONE, bid);
                    cr.setButton(Button.A, bc);
                    break;
                case 2:
                    lxaxis = new Axis(ControllerReader.ControllerID.ONE, axisID,min,max);
                    break;
                case 3:
                    lyaxis = new Axis(ControllerReader.ControllerID.ONE, axisID,min,max);
                    break;
                case 4:
                    // The user has just held the joystick to the top-right.
                    avX = AxesMinMax.get(lxaxis.getAxisID())[0] + AxesMinMax.get(lxaxis.getAxisID())[1];
                    avX/=2;
                    avXExisting = lxaxis.getRawMin() + lxaxis.getRawMax();
                    avXExisting/=2;
                    
                    avY = AxesMinMax.get(lyaxis.getAxisID())[0] + AxesMinMax.get(lyaxis.getAxisID())[1];
                    avY/=2;
                    avYExisting = lyaxis.getRawMin() + lyaxis.getRawMax();
                    avYExisting/=2;
                    
                    if (avX - avXExisting < 0) {
                        // Axis is reversed
                        lxaxis = new Axis(lxaxis.getControllerID(),lxaxis.getAxisID(),lxaxis.getRawMax(),lxaxis.getRawMin());
                    }
                    if (avY - avYExisting < 0) {
                        // Axis is reversed
                        lyaxis = new Axis(lyaxis.getControllerID(),lyaxis.getAxisID(),lyaxis.getRawMax(),lyaxis.getRawMin());
                    }
                    
                    break;
                case 5:
                    rxaxis = new Axis(ControllerReader.ControllerID.ONE, axisID,min,max);
                    break;
                case 6:
                    ryaxis = new Axis(ControllerReader.ControllerID.ONE, axisID,min,max);
                    break;
                case 7:
                    
                    // The user has just held the joystick to the top-right.
                    avX = AxesMinMax.get(rxaxis.getAxisID())[0] + AxesMinMax.get(rxaxis.getAxisID())[1];
                    avX/=2;
                    avXExisting = rxaxis.getRawMin() + rxaxis.getRawMax();
                    avXExisting/=2;
                    
                    avY = AxesMinMax.get(ryaxis.getAxisID())[0] + AxesMinMax.get(ryaxis.getAxisID())[1];
                    avY/=2;
                    avYExisting = ryaxis.getRawMin() + ryaxis.getRawMax();
                    avYExisting/=2;
                    
                    if (avX - avXExisting < 0) {
                        // Axis is reversed
                        rxaxis = new Axis(rxaxis.getControllerID(),rxaxis.getAxisID(),rxaxis.getRawMax(),rxaxis.getRawMin());
                    }
                    if (avY - avYExisting < 0) {
                        // Axis is reversed
                        ryaxis = new Axis(ryaxis.getControllerID(),ryaxis.getAxisID(),ryaxis.getRawMax(),ryaxis.getRawMin());
                    }
                    break;
                case 8:
                    avTrig = (min + max)/2;
                    if (avTrig > AxesNeutral.get(axisID)) {
                        ltaxis = new Axis(ControllerReader.ControllerID.ONE, axisID, min, max);
                    } else {
                        ltaxis = new Axis(ControllerReader.ControllerID.ONE, axisID, max, min);
                    }
                    break;
                case 9:
                    avTrig = (min + max)/2;
                    if (avTrig > AxesNeutral.get(axisID)) {
                        rtaxis = new Axis(ControllerReader.ControllerID.ONE, axisID, min, max);
                    } else {
                        rtaxis = new Axis(ControllerReader.ControllerID.ONE, axisID, max, min);
                    }
                    break;
                case 10:
                    // Now we must create the configuration file.
                    cr.setJoystickAxes(Joystick.LEFT, lxaxis, lyaxis);
                    cr.setJoystickAxes(Joystick.RIGHT, rxaxis, ryaxis);
                    cr.setJoystickDeadzone(Joystick.LEFT, 0.4f);
                    cr.setJoystickDeadzone(Joystick.RIGHT, 0.4f);
                    cr.setTriggerAxis(Trigger.LEFT, ltaxis);
                    cr.setTriggerAxis(Trigger.RIGHT, rtaxis);
            {
                try {
                    String myDir = new File(GamepadHandlerLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
                    String outDir = myDir + "/controllerConfig.xml";
                    System.out.println("Saving XML to: " + outDir);
                    cr.writeConfig(outDir);
                } catch (URISyntaxException ex) {
                    Logger.getLogger(GamepadHandlerFrameLooper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
                    break;
                default:
                    break;
            }
            
    }
}
