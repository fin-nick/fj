/*
 * UIUpadter.java
 *
 * Created on 22 ���� 2007 �., 23:43
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.base;

import DrawControls.VirtualList;
import javax.microedition.lcdui.*;
import java.util.*;
import jimm.*;
import jimm.comm.Util;
import jimm.ui.*;
import jimm.ui.timers.*;
import jimm.util.ResourceBundle;

/**
 *
 * @author vladimir
 */
public class UIUpdater extends TimerTask {
    private int gcCount = 0;
    private boolean gcNotified = false;
    private boolean gcEnabled = Options.getBoolean(Options.OPTION_CUSTOM_GC);
    private static final int FLASH_COUNTER = 16;
    private static final int SYS_FLASH_COUNTER = 16*3;

    private static UIUpdater uiUpdater;
    private static Timer uiTimer;
    public static void startUIUpdater() {
        if (null != uiTimer) {
            uiTimer.cancel();
        }
        uiTimer = new Timer();
        uiUpdater = new UIUpdater();
        uiTimer.schedule(uiUpdater, 0, NativeCanvas.UIUPDATE_TIME);
    }
    
    private Object displ = null;
    private String text = null;
    private int counter;
    
    public static void startFlashCaption(Object disp, String text) {
        if (null == disp) return;
        if (null == text) return;
        Object prevDisplay = uiUpdater.displ;
        if (null != prevDisplay) {
            uiUpdater.displ = null;
            uiUpdater.setTicker(prevDisplay, null);
        }
        uiUpdater.setTicker(disp, text);
        uiUpdater.text  = text;
        uiUpdater.counter = (disp instanceof InputTextBox) ? SYS_FLASH_COUNTER : FLASH_COUNTER;
        uiUpdater.flashCaptionInterval = FLASH_CAPTION_INTERVAL;
        uiUpdater.displ = disp;
    }

    private static final int FLASH_CAPTION_INTERVAL = 250;
    private int flashCaptionInterval;
    private void taskFlashCaption() {
        Object curDispay = displ;
        if (null == curDispay) {
            return;
        }
        flashCaptionInterval -= NativeCanvas.UIUPDATE_TIME;
        if (0 < flashCaptionInterval) {
            return;
        }
        flashCaptionInterval = FLASH_CAPTION_INTERVAL;
        if (0 < counter) {
            if (curDispay instanceof VirtualList) {
                setTicker(curDispay, ((counter & 1) == 0) ? text : " ");
            }
            counter--;

        } else {
            setTicker(curDispay, null);
            displ = null;
        }
    }
    private void setTicker(Object displ, String text) {
        if (displ instanceof InputTextBox) {
            ((InputTextBox)displ).setTicker(text);
        } else if (displ instanceof VirtualList) {
            ((VirtualList)displ).setTicker(text);
        }
    }

    public void run() {
        try {
            update();
        } catch (OutOfMemoryError out) {
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            jimm.modules.DebugLog.panic("UIUpdater", e);
            // #sijapp cond.end #
        }
    }
    // #sijapp cond.if modules_ANDROID is "true" #
    private long fact(long x) {
        long result = 1;
        while (1 < x) {
            result = (result * x--)  % 1000001 + x % 41;
        }
        return result;
    }
    private void delay() {
        // It is slowly and maybe can help the Earth.
        if (fact(100000) == fact(42)) {
            PopupWindow.showShadowPopup("Danger", "Universe is collapsing now!");
        }
    }
    // #sijapp cond.end#
    private void update() {
        // flash caption task
        taskFlashCaption();

        // #sijapp cond.if modules_ABSENCE is "true" #
        jimm.modules.AutoAbsence.instance.run();
        // #sijapp cond.end #

        if (!NativeCanvas.getInstance().isShown()) {
            // #sijapp cond.if modules_ANDROID is "true" #
            // It's so fast... We mast be slower.
            delay();
            // #sijapp cond.end#
            return;
        }

        // UI update task
        NativeCanvas.getInstance().updateTask();
        
        // Memory Control task
        if (gcEnabled) {
            long mem = Runtime.getRuntime().freeMemory();
            final long CRICAL_MEMORY = 20 * 1024;
            final long CRICAL_COUNT = 2;
            if (CRICAL_MEMORY < mem) {
                gcNotified = false;
                gcCount = 0;

            } else if (gcCount < CRICAL_COUNT) {
                System.gc();
                gcCount++;

            } else if (!gcNotified) {
                new jimm.ui.PopupWindow("warning", ResourceBundle.getString("critical_heap_level")).show();
                gcNotified = true;
            }
        }
    }
}
