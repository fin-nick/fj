/*
 * NativeCanvas.java
 *
 * Midp Canvas wrapper.
 *
 * @author Vladimir Kryukov
 */

package jimm.ui.base;

import DrawControls.*;
import java.util.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.Util;
import jimm.modules.*;
import jimm.ui.timers.*;
import jimm.util.ResourceBundle;

/**
 *
 * @author Vladimir Kryukov
 */
public class NativeCanvas extends Canvas {
    public static final int UIUPDATE_TIME = 250;
    public static final Object setDisplaySemaphore = new Object();

    private static CanvasEx canvas = null;
    private static Image bDIimage = null;

    private static String leftButton = null;
    private static String rightButton = null;
    private static NativeCanvas instance = new NativeCanvas();
    private static boolean fullScreen = false;
    private GraphicsEx graphicsEx = new GraphicsEx();
    
    /** Creates a new instance of NativeCanvas */
    private NativeCanvas() {
    }
    
    
    /**
     * 
     * 
     * @see paint
     */
    protected void paint(Graphics g) {
        if (isDoubleBuffered()) {
            paintAllOnGraphics(g);
        } else {
            try {
                if ((null == bDIimage) || (bDIimage.getHeight() != getHeight())) {
                    bDIimage = Image.createImage(getWidth(), getHeight());
                }
                paintAllOnGraphics(bDIimage.getGraphics());
                g.drawImage(bDIimage, 0, 0, Graphics.TOP | Graphics.LEFT);
            } catch (Exception e) {
                paintAllOnGraphics(g);
            }
        }
    }
    
    // #sijapp cond.if target="MIDP2" #
    protected void showNotify() {
        if (Jimm.isPaused()) {
            Jimm.setMinimized(false);
        }
        updateMetrix();
    }
    // #sijapp cond.end #
//    protected void hideNotify() {
//    }
    
    private void paintAllOnGraphics(Graphics graphics) {

        CanvasEx c = canvas;
        if (null != c) {
            graphicsEx.setGraphics(graphics);

            // #sijapp cond.if target="MIDP2" | target="MOTOROLA" | target="SIEMENS2"#
            if (!NativeCanvas.isFullScreen()) {
                if (c instanceof VirtualList) {
                    setTitle(((VirtualList)c).getCaption());
                } else {
                    setTitle(null);
                }
            }
            // #sijapp cond.end #
            try {
                if (graphicsEx.getClipY() < getScreenHeight()) {
                    c.paint(graphicsEx);
                }
            } catch(Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.panic("native", e);
                // #sijapp cond.end #
            }
            // #sijapp cond.if target="MIDP2" | target="MOTOROLA" | target="SIEMENS2"#
            graphicsEx.setStrokeStyle(Graphics.SOLID);
            int h = graphicsEx.getSoftBarSize(rightButton, time, leftButton);
            if (0 < h) {
                if (Options.getBoolean(Options.OPTION_SWAP_SOFT_KEY)) {
                    graphicsEx.drawSoftBar(rightButton, time, leftButton, h);
                } else {
                    graphicsEx.drawSoftBar(leftButton, time, rightButton, h);
                }
            }
            // #sijapp cond.end #
            graphicsEx.reset();
        }
    }

    // #sijapp cond.if modules_STYLUS is "true"#
    public TouchControl touchControl = new TouchControl();
    protected void pointerReleased(int x, int y) {
        // #sijapp cond.if modules_ABSENCE is "true" #
        jimm.modules.AutoAbsence.instance.userActivity();
        // #sijapp cond.end #
        touchControl.pointerReleased(x, y);
    }
    protected void pointerPressed(int x, int y) {
        // #sijapp cond.if modules_ABSENCE is "true" #
        jimm.modules.AutoAbsence.instance.userActivity();
        // #sijapp cond.end #
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
        touchControl.pointerPressed(x, y);
    }
    protected void pointerDragged(int x, int y) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
        touchControl.pointerDragged(x, y);
    }
    // #sijapp cond.end#
    

    private String addCommand(Hashtable hash, String prev, String cur, int type) {
        cur = ResourceBundle.getString(cur);
        if (cur == prev) {
            return cur;
        }
        if (null != prev) {
            removeCommand((Command)hash.get(prev));
        }
        if (null != cur) {
            Command cmd = (Command)hash.get(cur);
            if (null == cmd) {
                cmd = new Command(cur, type, 0);
                hash.put(cur, cmd);
            }
            addCommand(cmd);
        }
        return cur;
    }

    static public void setCommands(String left, String right) {
        leftButton   = ResourceBundle.getString(left);
        rightButton  = ResourceBundle.getString(right);
        instance.updateMetrix();
    }


    public static void setCanvas(CanvasEx canvasEx) {
        canvas = canvasEx;
        stopKeyRepeating();
        // #sijapp cond.if modules_STYLUS is "true"#
        instance.touchControl.setCanvas(canvas);
        // #sijapp cond.end#
    }
    public static void stopKeyRepeating() {
        // #sijapp cond.if target="MIDP2" | target="MOTOROLA"#
        KeyRepeatTimer.stop();
        // #sijapp cond.end#
    }

    public static boolean isFullScreen() {
        return fullScreen;
    }

    // #sijapp cond.if target="MIDP2" | target="MOTOROLA" | target="SIEMENS2"#
    public static void setCaption(String capt) {
        instance.setTitle(capt);
    }
    // #sijapp cond.end#
    
    /**
     * Set fullscreen mode
     */
    public static void setFullScreen(boolean value) {
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        if (fullScreen == value) return;
        fullScreen = value;

        instance.setFullScreenMode(fullScreen);
        instance.resetMetrix();
        instance.updateMetrix();
        CanvasEx c = canvas;
        if (null != c) {
            c.setDisplayableToDisplay();
        }
        // #sijapp cond.end#
    }
    
    
    protected void sizeChanged(int w, int h) {
        boolean oldIsVertical = (width < height);
        boolean newIsVertical = (w < h);
        if ((oldIsVertical != newIsVertical) && isShown()) {
            resetMetrix();
            updateMetrix();
            invalidate(canvas);
        }
    }
    
    public static final int LEFT_SOFT  = 0x00100000;

    public static final int RIGHT_SOFT = 0x00100001;
    public static final int CLEAR_KEY  = 0x00100002;
    public static final int CLOSE_KEY  = 0x00100003;
    public static final int CALL_KEY   = 0x00100004;
    public static final int CAMERA_KEY = 0x00100005;
    public static final int ABC_KEY    = 0x00100006;
    public static final int VOLPLUS_KEY  = 0x00100007;
    public static final int VOLMINUS_KEY = 0x00100008;
    public static final int NAVIKEY_RIGHT = 0x00100009;
    public static final int NAVIKEY_LEFT  = 0x0010000A;
    public static final int NAVIKEY_UP    = 0x0010000B;
    public static final int NAVIKEY_DOWN  = 0x0010000C;
    public static final int NAVIKEY_FIRE  = 0x0010000D;
    private int getKey(int code) {
        int leftSoft  = LEFT_SOFT;
        int rightSoft = RIGHT_SOFT;
        if (Options.getBoolean(Options.OPTION_SWAP_SOFT_KEY)) {
            leftSoft  = RIGHT_SOFT;
            rightSoft = LEFT_SOFT;
        }
        // #sijapp cond.if modules_ANDROID is "true" #
        if (Jimm.isPhone(Jimm.PHONE_ANDROID)) {
            if (-4 == code) {
                return CLOSE_KEY;
            }
            if (-84 == code) {
                return CALL_KEY;
            }
        }
        // #sijapp cond.end #
        String strCode = null;
        try {
            strCode = instance.getKeyName(code);
            if (null != strCode) {
                strCode = strCode.replace('_', ' ').toLowerCase();
            }
        } catch(IllegalArgumentException e) {
        }

        if (null != strCode) {
            if ("soft1".equals(strCode)
                    || "soft 1".equals(strCode)
                    || "softkey 1".equals(strCode)
                    || strCode.startsWith("left soft")) {
                return leftSoft;
            }
            if ("soft2".equals(strCode)
                    || "soft 2".equals(strCode)
                    || "softkey 4".equals(strCode)
                    || strCode.startsWith("right soft")) {
                return rightSoft;
            }
            if ("on/off".equals(strCode) || ("ba" + "ck").equals(strCode)) {
                return CLOSE_KEY;
            }
            if (("clea" + "r").equals(strCode)) {
                return CLEAR_KEY;
            }
//            if ("soft3".equals(strCode)) {
//                return MIDDLE_SOFT;
//            }
            if (("se" + "nd").equals(strCode)) {
                return CALL_KEY;
            }
            if (("sele" + "ct").equals(strCode) || ("o" + "k").equals(strCode)
                    || "fire".equals(strCode) || "navi-center".equals(strCode)
                    || "enter".equals(strCode)) {
                return NAVIKEY_FIRE;
            }
            if ("start".equals(strCode)) {
                return CALL_KEY;
            }
            if ("up".equals(strCode) || "navi-up".equals(strCode)
                    || "up arrow".equals(strCode)) {
                return NAVIKEY_UP;
            }
            if ("down".equals(strCode) || "navi-down".equals(strCode)
                    || "down arrow".equals(strCode)) {
                return NAVIKEY_DOWN;
            }
            if ("left".equals(strCode) || "navi-left".equals(strCode)
                    || "left arrow".equals(strCode) || "sideup".equals(strCode)) {
                return NAVIKEY_LEFT;
            }
            if ("right".equals(strCode) || "navi-right".equals(strCode)
                    || "right arrow".equals(strCode) || "sidedown".equals(strCode)) {
                return NAVIKEY_RIGHT;
            }
        }
        if(code == -6 || code == -21 || code == 21 || code == 105
                || code == -202 || code == 113 || code == 57345
                || code == 0xFFBD) {
            return leftSoft;
        }
        if (code == -7 || code == -22 || code == 22 || code == 106
                || code == -203 || code == 112 || code == 57346
                || code == 0xFFBB) {
            return rightSoft;
        }
        if (-41 == code) { // Alcatel-OT-800/1.0
            return NAVIKEY_FIRE;
        }
        if (-5 == code) {
            return NAVIKEY_FIRE;
        }
        if (63557 == code) { // nokia e63
            return NAVIKEY_FIRE;
        }
        if (code == -8) {
            return CLEAR_KEY;
        }
        if ((-11 == code) || (-12 == code)) {
            return CLOSE_KEY;
        }
        if ((-24 == code) || (-25 == code)|| (-26 == code)) {      //camera key
            return CAMERA_KEY;
        }
        if (code == -10) {
            return CALL_KEY;
        }
        if (code == -50 || code == 1048582) {
            return ABC_KEY;
        }
        if (code == -36) {
            return VOLPLUS_KEY;
        }
        if (code == -37) {
            return VOLMINUS_KEY;
        }
        return code;
    }

    protected void keyPressed(int keyCode) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
        doKeyReaction(keyCode, CanvasEx.KEY_PRESSED);
    }
    
    protected void keyRepeated(int keyCode) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
        // #sijapp cond.if target="MIDP2" | target="MOTOROLA"#
        // #sijapp cond.else#
        doKeyReaction(keyCode, CanvasEx.KEY_REPEATED);
        // #sijapp cond.end#
        // #sijapp cond.if modules_ANDROID is "true" #
        doKeyReaction(keyCode, CanvasEx.KEY_REPEATED);
        // #sijapp cond.end #
    }
    
    protected void keyReleased(int keyCode) {
        doKeyReaction(keyCode, CanvasEx.KEY_RELEASED);
    }

    public static int getJimmKey(int code) {
        return instance.getKey(code);
    }
    public static int getJimmAction(int key, int keyCode) {
        return instance.getAction(key, keyCode);
    }
    private int getAction(int key, int keyCode) {
        if (key != keyCode) {
            return key;
        }
        try {// getGameAction can raise exception
            int action = instance.getGameAction(keyCode);
            switch (action) {
                case Canvas.RIGHT: return NAVIKEY_RIGHT;
                case Canvas.LEFT:  return NAVIKEY_LEFT;
                case Canvas.UP:    return NAVIKEY_UP;
                case Canvas.DOWN:  return NAVIKEY_DOWN;
                case Canvas.FIRE:  return NAVIKEY_FIRE;
            }
        } catch(Exception e) {
        }
        return key;
    }
    private int qwerty2phone(int key) {
        switch (key) {
            case 'm': return KEY_NUM0;
            case 'r': return KEY_NUM1;
            case 't': return KEY_NUM2;
            case 'z': return KEY_NUM3;
            case 'f': return KEY_NUM4;
            case 'g': return KEY_NUM5;
            case 'h': return KEY_NUM6;
            case 'v': return KEY_NUM7;
            case 'b': return KEY_NUM8;
            case 'n': return KEY_NUM9;
            case 'j': return KEY_POUND;
            case 'u': return KEY_STAR;
        }
        return key;
    }
    private int qwerty2action(int key) {
        switch (key) {
            case KEY_NUM0: return 0;
            case KEY_NUM1: return 0;
            case KEY_NUM2: return NAVIKEY_UP;
            case KEY_NUM3: return NAVIKEY_RIGHT;
            case KEY_NUM4: return NAVIKEY_LEFT;
            case KEY_NUM5: return NAVIKEY_FIRE;
            case KEY_NUM6: return NAVIKEY_RIGHT;
            case KEY_NUM7: return 0;
            case KEY_NUM8: return NAVIKEY_DOWN;
            case KEY_NUM9: return 0;
            case KEY_POUND: return 0;
            case KEY_STAR: return 0;
        }
        return key;
    }
    private void doKeyReaction(int keyCode, int type) {
        CanvasEx c = canvas;
        if (null != c) {
            int key = getKey(keyCode);
            int action = getAction(key, keyCode);
            if (1 == Options.getInt(Options.OPTION_KEYBOARD)) {
                int qwertyKeyCode = qwerty2phone(keyCode);
                if (qwertyKeyCode != keyCode) {
                    key = qwertyKeyCode;
                    action = qwerty2action(key);
                }
            }
            try {
                // #sijapp cond.if modules_ABSENCE is "true" #
                jimm.modules.AutoAbsence.instance.userActivity();
                // #sijapp cond.end #
                c.doKeyReaction(key, action, type);
            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.panic("Key error", e);
                // #sijapp cond.end #
            }
            // #sijapp cond.if modules_ANDROID is "true" #
            if (true) {
                return;
            }
            // #sijapp cond.end #
            
            // #sijapp cond.if target="MIDP2" | target="MOTOROLA"#
            if (CanvasEx.KEY_PRESSED == type) { // navigation keys only
                switch (action) {
                    case NAVIKEY_RIGHT:
                    case NAVIKEY_LEFT:
                    case NAVIKEY_UP:
                    case NAVIKEY_DOWN:
                    case NAVIKEY_FIRE:
                    case KEY_NUM1:
                    case KEY_NUM3:
                    case KEY_NUM7:
                    case KEY_NUM9:
                        KeyRepeatTimer.start(key, action, c);
                        break;
                }

            } else {
                KeyRepeatTimer.stop();
            }
            // #sijapp cond.end#
        }
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        //try {
        //    String strCode = NativeCanvas.getInstance().getKeyName(keyCode);
        //    DebugLog.println("key = '" + strCode + "'(" + keyCode + ") = " + type);
        //} catch(IllegalArgumentException e) {
        //}
        // #sijapp cond.end #
    }

    void emulateKey(int key) {
        doKeyReaction(key, CanvasEx.KEY_PRESSED);
        doKeyReaction(key, CanvasEx.KEY_RELEASED);
    }

    public static void invalidate(CanvasEx canvasEx) {
        if (canvas == canvasEx) {
            instance.repaint();
        }
    }

    public static NativeCanvas getInstance() {
        return instance;
    }
    
    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
    private String time = Util.getDateString(true);
    private long timestamp = 0;
    private int getSoftBarHeight() {
        return graphicsEx.getSoftBarSize(leftButton, time, rightButton);
    }
    // #sijapp cond.end #
    private int width = 0;
    private int height = 0;
    private int windowHeight = 0;
    private void resetMetrix() {
        height = 0;
        width = 0;
    }
    public void updateMetrix() {
        final int h = getHeight();
        final int w = getWidth();
        if (height < h) {
            width = getWidth();
            height = getHeight();
        }
        windowHeight = height;
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        windowHeight -= getSoftBarHeight();
        // #sijapp cond.end #
        // #sijapp cond.if modules_STYLUS is "true"#
        instance.touchControl.setView(width, windowHeight);
        // #sijapp cond.end#
    }
    public static int getScreenWidth() {
        return instance.width;
    }
    public int getMinScreenMetrics() {
        return Math.min(getWidth(), getHeight());
    }

    public static int getScreenHeight() {
        return instance.windowHeight;
    }

    public void refreshClock() {
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        int h = getSoftBarHeight();
        if (0 < h) {
            time = Util.getDateString(true);
            repaint(0, instance.getHeight() - h, getScreenWidth(), h);
        }
        // #sijapp cond.end #
    }

    public void updateTask() {
        try {
            CanvasEx c = canvas;
            if (null != c) {
                c.updateTask();
            }
        } catch (Exception e) {
        }

        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        // Time update task
        final long newTime = System.currentTimeMillis() / 60000;
        if (timestamp < newTime) {
            timestamp = newTime;
            refreshClock();
        }
        // #sijapp cond.end #
    }
    
    private static class KeyRepeatTimer extends TimerTask {
        private static Timer timer = new Timer();
        private int key;
        private int action;
        private CanvasEx canvas;
        private int slowlyIterations = 8;
        
        
        public static void start(int key, int action, CanvasEx c) {
            stop();
            timer = new Timer();
            KeyRepeatTimer repeater = new KeyRepeatTimer(key, action, c);
            int delay = (Util.strToIntDef(Options.getString(Options.OPTION_REPEATER_KEY), 150));     //key repeat
            int interval  = (Util.strToIntDef(Options.getString(Options.OPTION_REPEATER_TIME), 60)); //key repeat
                delay = Math.max(1, Math.min(delay, 1000));                                          //key repeat
                interval = Math.max(1, Math.min(interval, 1000));                                    //key repeat
            timer.schedule(repeater, delay, interval);                                               //key repeat
        }
        public static void stop() {
            Timer t = timer;
            if (null != t) {
                t.cancel();
                t = null;
            }
        }
        
        private KeyRepeatTimer(int keyCode, int actionCode, CanvasEx c) {
            key = keyCode;
            action = actionCode;
            canvas = c;
        }

        public void run() {
            if (0 < slowlyIterations) {
                slowlyIterations--;
                if (0 != slowlyIterations % 2) {
                    return;
                }
            }
            if (!NativeCanvas.getInstance().isShown()) {
                KeyRepeatTimer.stop();
                return;
            }
            canvas.doKeyReaction(key, action, CanvasEx.KEY_REPEATED);
        }
    }
}

