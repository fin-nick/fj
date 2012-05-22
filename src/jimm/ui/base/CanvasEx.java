/*
 * CanvasEx.java
 *
 * @author Vladimir Krukov
 */

package jimm.ui.base;

import DrawControls.icons.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.comm.Util;
import jimm.modules.*;

/**
 * Basic class for UI-controls.
 *
 * @author Vladimir Kryukov
 */
abstract public class CanvasEx extends DisplayableEx {
    public static final byte THEME_BACKGROUND           = 0;
    public static final byte THEME_TEXT                 = 1;
    public static final byte THEME_CAP_BACKGROUND       = 2;
    public static final byte THEME_CAP_TEXT             = 3;
    public static final byte THEME_PARAM_VALUE          = 4;
    
    public static final byte THEME_CHAT_INMSG           = 5;
    public static final byte THEME_CHAT_OUTMSG          = 6;
    public static final byte THEME_CHAT_FROM_HISTORY    = 7;
    
    public static final byte THEME_CONTACT_ONLINE       = 8;
    public static final byte THEME_CONTACT_WITH_CHAT    = 9;
    public static final byte THEME_CONTACT_OFFLINE      = 10;
    public static final byte THEME_CONTACT_TEMP         = 11;
    
    public static final byte THEME_SCROLL_BACK          = 12;
    
    public static final byte THEME_SELECTION_RECT       = 13;
    public static final byte THEME_BACK                 = 14;
    
    public static final byte THEME_SPLASH_BACKGROUND    = 15;
    public static final byte THEME_SPLASH_LOGO_TEXT     = 16;
    public static final byte THEME_SPLASH_MESSAGES      = 17;
    public static final byte THEME_SPLASH_DATE          = 18;///
    public static final byte THEME_SPLASH_PROGRESS_BACK = 19;
    public static final byte THEME_SPLASH_PROGRESS_TEXT = 20;
    public static final byte THEME_SPLASH_LOCK_BACK     = 21;
    public static final byte THEME_SPLASH_LOCK_TEXT     = 22;
    
    public static final byte THEME_MAGIC_EYE_NUMBER     = 23;
    public static final byte THEME_MAGIC_EYE_ACTION     = 24;
    public static final byte THEME_MAGIC_EYE_NL_USER    = 25;
    public static final byte THEME_MAGIC_EYE_USER       = 26;
    public static final byte THEME_MAGIC_EYE_TEXT       = 27;
    
    //public static final byte THEME_MENU_SHADOW          = 28;
    public static final byte THEME_MENU_BACK            = 29;
    public static final byte THEME_MENU_BORDER          = 30;
    public static final byte THEME_MENU_TEXT            = 31;
    public static final byte THEME_MENU_SEL_BACK        = 32;
    public static final byte THEME_MENU_SEL_BORDER      = 33;
    public static final byte THEME_MENU_SEL_TEXT        = 34;
    
    //public static final byte THEME_POPUP_SHADOW         = 35;
    public static final byte THEME_POPUP_BORDER         = 36;
    public static final byte THEME_POPUP_BACK           = 37;
    public static final byte THEME_POPUP_TEXT           = 38;

    public static final byte THEME_GROUP                = 39;
    public static final byte THEME_CHAT_HIGHLIGHT_MSG   = 40;
    public static final byte THEME_SELECTION_BACK       = 41;
    public static final byte THEME_CONTACT_STATUS       = 42;
    public static final byte THEME_PROTOCOL             = 43;
    public static final byte THEME_PROTOCOL_BACK        = 44;
    
    
    // Width of scroller line
    protected final static int scrollerWidth = getScrollWidth();
    public static final int minItemHeight = getMinItemHeight();
    public static final int minItemWidth = getMinItemWidth();
    private static final int getMinItemHeight() {
        // #sijapp cond.if modules_STYLUS is "true"#
        NativeCanvas nc = NativeCanvas.getInstance();
        if (nc.hasPointerEvents()) {
            Font smallFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN,  Font.SIZE_SMALL);
            int height = nc.getMinScreenMetrics();

            // We use height between 2 and 3 times of small font
            int max = smallFont.getHeight() * 3;
            int min = smallFont.getHeight() * 2;
            // 640 ~ 40
            int optimal = (height <= 640) ? 40 : (height * 625 / 10000);
            return Math.max(min, Math.min(optimal, max));
        }
        // #sijapp cond.end #
        return 0;
    }
    private static final int getMinItemWidth() {
        NativeCanvas nc = NativeCanvas.getInstance();
        return Math.min(nc.getHeight(), nc.getWidth()) * 30 / 100;
    }
    private final static int getScrollWidth() {
        int zoom = 2;
        // #sijapp cond.if target is "MIDP2"#
        if (NativeCanvas.getInstance().hasPointerEvents()) {
            zoom = 5;
        }
        // #sijapp cond.end#
        int width = Math.min(NativeCanvas.getScreenWidth(), NativeCanvas.getScreenHeight());
        return Math.max(width * zoom / 100, 6);
    }

    /**
     * UI dinamic update
     */
    protected void updateTask() {
    }

    /**
     * Caclulate params
     */
    protected void beforShow() {
    }

    
    // #sijapp cond.if modules_STYLUS is "true"#
    protected void stylusGeneralMoved(int fromX, int fromY, int toX, int toY) {
    }
    protected void stylusMoving(int fromX, int fromY, int toX, int toY) {
        stylusGeneralMoved(fromX, fromY, toX, toY);
    }
    protected void stylusMoved(int fromX, int fromY, int toX, int toY) {
        stylusGeneralMoved(fromX, fromY, toX, toY);
    }
    protected void stylusKineticMoving(int fromX, int fromY, int toX, int toY) {
        stylusGeneralMoved(fromX, fromY, toX, toY);
    }
    protected void stylusTap(int x, int y, boolean longTap) {
    }
    protected void stylusPressed(int x, int y) {
    }
    // #sijapp cond.end#

    /**
     * paint procedure
     */
    protected abstract void paint(GraphicsEx g);
    
    protected void paintBack(GraphicsEx g, Object o) {
        try {
            if (o instanceof CanvasEx) {
                ((CanvasEx)o).paint(g);

            } else {
                g.setThemeColor(THEME_BACK);
                g.fillRect(g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("paint back", e);
            if (null != o) DebugLog.println("paint back o is " + o.getClass());
            // #sijapp cond.end #
        }
        g.clipRect(0, 0, NativeCanvas.getScreenWidth(), NativeCanvas.getScreenHeight());
    }

    
    protected void restoring() {
        NativeCanvas.setCommands(null, null);
    }
    public void restore() {
        repaintLocked = false;
        Jimm.setDisplay(this);
    }
    public void setDisplayableToDisplay() {
        restoring();
        beforShow();
        NativeCanvas.setCanvas(this);
        NativeCanvas instance = NativeCanvas.getInstance();

        if ((Jimm.getCurrentDisplay() instanceof CanvasEx) && instance.isShown()) {
            NativeCanvas.invalidate(this);

        } else {
            Jimm.setDisplayable(instance);
        }
    }
    
    
    // Used by "Invalidate" method to prevent invalidate when locked
    private boolean repaintLocked = false;
    
    // protected void invalidate()
    public void invalidate() {
        if (repaintLocked) return;
        NativeCanvas.invalidate(this);
    }
    
    public final void lock() {
        repaintLocked = true;
    }
    
    protected void afterUnlock() {
    }

    public final void unlock() {
        afterUnlock();
        repaintLocked = false;
        invalidate();
    }
    
    protected final boolean isLocked() {
        return repaintLocked;
    }
    
    // Key event type
    public final static int KEY_PRESSED = 1;
    public final static int KEY_REPEATED = 2;
    public final static int KEY_RELEASED = 3;
    
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
    }
}