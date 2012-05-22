/*
 * Light.java
 *
 * Light-control module.
 *
 * Usage:
 * <CODE>
 * // #sijapp cond.if modules_LIGHT is "true" #
 * CustomLight.setLightMode(CustomLight.ACTION_NONE);
 * // #sijapp cond.end#
 * </CODE>
 *
 * @author Vladimir Krukov
 */

package jimm.modules;


// #sijapp cond.if modules_LIGHT is "true" #


import javax.microedition.lcdui.*;
import java.util.*;
import jimm.*;
//import javax.microedition.midlet.*;
// #sijapp cond.if target is "MOTOROLA"#
import com.motorola.funlight.*;
// #sijapp cond.end#
// #sijapp cond.if target is "SIEMENS2" #
import com.siemens.mp.game.*;
// #sijapp cond.end #
// #sijapp cond.if target is "MIDP2" #
import com.nokia.mid.ui.*;
//import com.samsung.util.*;
// #sijapp cond.end #
import java.util.*;
import jimm.comm.Util;
import jimm.ui.FormEx;
import jimm.util.ResourceBundle;

/**
 * Class for platform-independent light control.
 * 
 * @author Vladimir Krukov
 */
public final class CustomLight extends TimerTask {
    private static CustomLight instance = new CustomLight(null);
    private Timer timer;
  
    public static final byte ACTION_NONE               = 0;
    public static final byte ACTION_ONLINE             = 1;
    public static final byte ACTION_KEY_PRESS          = 2;
    public static final byte ACTION_CONNECT            = 3;
    public static final byte ACTION_MESSAGE            = 4;
    public static final byte ACTION_ERROR              = 5;
    public static final byte ACTION_SYSTEM             = 6;
    
    public static final byte ACTION_COUNT              = 7;
    
    public static final byte ACTION_USER               = 10;
    public static final byte ACTION_SYSTEM_OFF         = 11;

    private static final byte ACTION_OFF               = 100;
    private static final byte ACTION_SLEEP             = 101;
    private static final byte ACTION_SYSTEM_SLEEP        = 102;

    
    private static final byte LIGHT_NONE               = 0;
    private static final byte LIGHT_SIEMENS            = 1;
    private static final byte LIGHT_NOKIA              = 2;
    private static final byte LIGHT_MOTOROLA_FUNLIGHT  = 3;
//    private static final byte LIGHT_SAMSUNG            = 4;
//    private static final byte LIGHT_LG                 = 5;
    private static final byte LIGHT_MIDP20             = 6;

    private static int light  = detectMode();
    private byte action = ACTION_NONE;
    private int tick = 0;
    private int prevLightLevel = 0;
    private boolean checkPrevState = true;
    private boolean systemLock = false;

    private static final int INTERVAL = 1000;
    
    public static void setLightMode(final byte m) {
        if (null != instance) {
            instance.setMode(m);
        }
    }
    
    private int getMaxTickCount() {
        return Math.max(1, Options.getInt(Options.OPTION_LIGHT_TICK));
    }
    private synchronized void setMode(final byte m) {
        if (ACTION_USER == m) {
            byte act = systemLock ? ACTION_NONE : ACTION_SYSTEM;
            processAction(act);
            action = nextAction(act);
            systemLock = (ACTION_SYSTEM == act);
            return;
        }
        if (!Options.getBoolean(Options.OPTION_LIGHT)) {
            return;
        }
        if (systemLock && (ACTION_SYSTEM_OFF != m)) {
            return;
        }
        systemLock = (ACTION_SYSTEM == m);
        tick = getMaxTickCount();
        processAction(m);
        action = nextAction(m);
    }

    public void run() {
        if (!Options.getBoolean(Options.OPTION_LIGHT)) {
            return;
        }
        final byte act = action;
        if (systemLock || (ACTION_OFF == act)) {
            return;
        }

        if (ACTION_SLEEP != act) {
            processAction(act);
        }
        
        if (0 < tick) {
            tick--;
            return;
        }
        tick = getMaxTickCount();
        action = nextAction(act);
    }

    private byte nextAction(byte action) {
        switch (action) {
            case ACTION_NONE:
            case ACTION_OFF:
                return ACTION_OFF;

            case ACTION_SYSTEM:
            case ACTION_SYSTEM_SLEEP:
                return ACTION_SYSTEM_SLEEP;

            case ACTION_SLEEP:
                return ACTION_NONE;

            case ACTION_SYSTEM_OFF:
            default:
                return ACTION_SLEEP;
        }
    }

    private synchronized void processAction(byte action) {
        setLight(getLightValue(action));
    }

    private void setLight(int level) {
        if ((100 < level) || (level < 0)) {
            return;
        }
        if (checkPrevState && (level == prevLightLevel)) {
            return;
        }
        prevLightLevel = level;
        // #sijapp cond.if target is "MIDP2" #
        if ((0 < level) && Jimm.isPhone(Jimm.PHONE_NOKIA_S40)) {
            setHardwareLight(0);
        }
        // #sijapp cond.end #
        setHardwareLight(level);
    }
    private void setHardwareLight(int value) {
        try {
            switch (light) {                
                // #sijapp cond.if target is "MOTOROLA" #
                case LIGHT_MOTOROLA_FUNLIGHT:
                    int curBrightness = value * 255 / 100;
                    int c = curBrightness + (curBrightness << 8) + (curBrightness << 16);
                    // 1 - Display
                    // 2 - Keypad
                    // 3 - Sideband

                    FunLight.getRegion(1).setColor(c);
                    FunLight.getRegion(1).getControl();
//                    if (value > 0) {
//                        FunLight.getRegion(1).getControl();
//                    } else {
//                        FunLight.getRegion(1).releaseControl();
//                        Jimm.getJimm().getDisplay().flashBacklight(0x00000000);
//                    }
                    break;
                // #sijapp cond.end #

                // #sijapp cond.if target is "SIEMENS2" #
                case LIGHT_SIEMENS:
                    if (value > 0) {
                        Light.setLightOn();
                    } else {
                        Light.setLightOff();
                    }
                    break;
                // #sijapp cond.end #

                // #sijapp cond.if target is "MIDP2" #
                case LIGHT_NOKIA:
                    DeviceControl.setLights(0, value);
                    break;

//                case LIGHT_SAMSUNG:
//                    if (value > 0) {
//                        LCDLight.on(0x7FFFFFFF);
//                    } else {
//                        LCDLight.off();
//                    }
//                    break;
                // #sijapp cond.end #

                // #sijapp cond.if target is "SIEMENS2" | target is "MIDP2" | target is "MOTOROLA" #
                case LIGHT_MIDP20:
                    if (value > 0) {
                        Jimm.getJimm().getDisplay().flashBacklight(0x7FFFFFFF);
                    } else {
                        Jimm.getJimm().getDisplay().flashBacklight(0x00000000);
                    }
                    break;
                // #sijapp cond.end #
            }
        } catch (Exception e) {
        }
    }
    
    public static boolean isSupport() {
        return light != LIGHT_NONE;
    }
    
    public static boolean canControlBrightness() {
        return (light == LIGHT_NOKIA) || (light == LIGHT_MOTOROLA_FUNLIGHT);
    }
    
    private static int detectMode() {
        // #sijapp cond.if target is "MIDP2" #
        try {
            Class.forName("com.nokia.mid.ui.DeviceControl");
            return LIGHT_NOKIA;
        } catch (Exception e) {
        }
//        try {
//            Class.forName("com.samsung.util.LCDLight");
//            return LIGHT_SAMSUNG;
//        } catch (Exception e) {
//        }
        // #sijapp cond.end #
        // #sijapp cond.if target is "SIEMENS2" #
        try {
            Class.forName("com.siemens.mp.game.Light");
            return LIGHT_SIEMENS;
        } catch (Exception e) {
        }
        // #sijapp cond.end #
        // #sijapp cond.if target is "MOTOROLA" #
        try {
            Class.forName("com.motorola.funlight.FunLight");
            if (FunLight.getRegions() != null) {
                String prod = System.getProperty("funlights.product");
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.println("moto light product = " + prod);
                // #sijapp cond.end #
                if ("E380".equals(prod) || "V600".equals(prod) || "E390".equals(prod)) {
                    return LIGHT_MOTOROLA_FUNLIGHT;
                }
            }
        } catch (Exception e) {
        }
        // #sijapp cond.end #
        // #sijapp cond.if target is "SIEMENS2" | target is "MIDP2" | target is "MOTOROLA" #
        return LIGHT_MIDP20;
        // #sijapp cond.else #
        return LIGHT_NONE;
        // #sijapp cond.end #
    }
    
    private int getLightValue(int action) {
        int option = Options.OPTION_LIGHT_NONE;
        switch (action) {
            case ACTION_NONE:
                option = Options.OPTION_LIGHT_NONE;
                break;
            case ACTION_ONLINE:
            case ACTION_CONNECT:
                option = Options.OPTION_LIGHT_ONLINE;
                break;
            case ACTION_SYSTEM_OFF:
            case ACTION_KEY_PRESS:
                option = Options.OPTION_LIGHT_KEY_PRESS;
                break;
            case ACTION_MESSAGE:
            case ACTION_ERROR:
                option = Options.OPTION_LIGHT_MESSAGE;
                break;
            case ACTION_SYSTEM:
                option = Options.OPTION_LIGHT_SYSTEM;
                break;
            default:
                return -1;
        }
        return Options.getInt(option);
    }

    /** Creates a new instance of Light */
    private CustomLight(Timer timer) {
        this.timer = timer;
        if (null != timer) {
            timer.scheduleAtFixedRate(this, 0, INTERVAL);
        }
    }
//    private boolean isOff() {
//        return (null != timer) && checkPrevState && (0 == prevLightLevel);
//    }
//    public static boolean isTurnedOff() {
//        return instance.isOff();
//    }
    public static void switchOn(boolean on) {
        final boolean worked = (null != instance.timer);
        if (worked) {
            if (!on) {
                instance.timer.cancel();
                // #sijapp cond.if target is "MIDP2" #
                instance.setLight(Jimm.isPhone(Jimm.PHONE_NOKIA_S60) ? 40 : 0);
                // #sijapp cond.else #
                instance.setLight(0);
                // #sijapp cond.end #
                instance = new CustomLight(null);
            }
        } else {
            if (on) {
                instance = new CustomLight(new Timer());
            }
            instance.checkPrevState = (101 != instance.getLightValue(ACTION_NONE));
        }
    }
}
// #sijapp cond.end#
