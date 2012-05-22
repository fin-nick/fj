/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/SplashCanvas.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Vladimir Kryukov
 *******************************************************************************/


package jimm;

import DrawControls.*;
import DrawControls.icons.*;
import DrawControls.text.FormatedText;
import java.io.IOException;
import javax.microedition.lcdui.*;
import java.util.*;
import jimm.comm.*;
import jimm.chat.message.Message;
import jimm.cl.*;
import jimm.modules.*;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.timers.*;
import jimm.util.*;
import protocol.*;
// #sijapp cond.if modules_PASSWORD is "true" # //password
import jimm.modules.*;                          //password
// #sijapp cond.end#                            //password

public final class SplashCanvas extends CanvasEx {

    // True if keylock has been enabled
    static private final short KEY_LOCK_MSG_TIME = 2000 / NativeCanvas.UIUPDATE_TIME;
    private short keyLock = -1;
    static private final short UPDATE_INTERVAL = 20000 / NativeCanvas.UIUPDATE_TIME;
    private short updateTime = UPDATE_INTERVAL;
    static private final short RESET_INTERVAL = 3000 / NativeCanvas.UIUPDATE_TIME;
    private short resetTime = -1;

	// #sijapp cond.if target is "SIEMENS2"#
	private final Image battImg = ImageList.loadImage("/batt.png");
	// #sijapp cond.end#
    private final Image splash = ImageList.loadImage("/logo.png");

	// Font used to display the logo (if image is not available)
	private final Font logoFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);

	// Font used to display the version nr
	private final Font versionFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

	// Font used to display informational messages
	private final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

        private final Font date = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL); //font date

	/*****************************************************************************/


    // Message to display beneath the splash image
    private Icon statusImg;
    private String message;
    private Progress process;

    // Progress in percent
    private volatile int progress;

    // Number of available messages
    private int availableMessages;

    // Time since last key # pressed
    // #sijapp cond.if modules_PASSWORD is "true" # //password
    private static long poundPressTime;             //password
    // #sijapp cond.else#                           //password
    private long poundPressTime;                    //password
    // #sijapp cond.end#                            //password

	public static final SplashCanvas instance = new SplashCanvas();

    // Constructor
	private SplashCanvas() {
	}
	// Sets the informational message
	static public void setMessage(String message) {
		instance.message = message;
        instance.statusImg = null;
        instance.progress = 0;
        instance.invalidate();
	}

	public static void setStatusToDraw(Icon img) {
		instance.statusImg = img;
	}

	public static void setNotifyMessage(Icon img, String msg) {
		instance.statusImg = img;
		instance.message = msg;
        instance.resetTime = RESET_INTERVAL;
        instance.invalidate();
    }
    public static void showSplash() {
        instance.show();
    }

    // Sets the current progress in percent (and request screen refresh)
    static public void setProgress(int progress) {
        if (progress == instance.progress) return;
        instance.progress = progress;
        NativeCanvas.getInstance().updateMetrix();
        instance.invalidate();
    }

    private void setLockMessage() {
        setMessage(ResourceBundle.getString("keylock_enabled"));
        setStatusToDraw(ContactList.getInstance().getCurrentProtocol().getCurrentStatusIcon());
    }
    public void lockJimm() {
        keyLock = 0;
        process = null;
        setLockMessage();
        messageAvailable();
        show();
    }


    // Called when message has been received
    public static void messageAvailable() {
        if (Jimm.isLocked()) {
            int unread = ContactList.getInstance().getUnreadMessCount();
            if (unread != instance.availableMessages) {
                instance.availableMessages = unread;
                instance.invalidate();
            }
        }
    }

    // #sijapp cond.if modules_STYLUS is "true"#
    protected void stylusTap(int x, int y, boolean isLongTap) {
        int height = getProgressHeight() + 20;
        int minY = NativeCanvas.getScreenHeight() - height;
        if (minY < y) {
            int region = NativeCanvas.getScreenWidth() * 2 / 3;
            if (region < x) {
                closeAnyAction();
            }
        }
    }
    protected void stylusMoved(int fromX, int fromY, int toX, int toY) {
        int region = getProgressHeight() + 20;
        int minY = NativeCanvas.getScreenHeight() - region;
        if ((fromY < minY) || (toY < minY)) {
            poundPressTime = 0;
            keyLock = KEY_LOCK_MSG_TIME;
            invalidate();
            return;
        }
        int x1 = Math.min(fromX, toX);
        int x2 = Math.max(fromX, toX);
        if ((x1 < region) && (NativeCanvas.getScreenWidth() - region < x2)) {
            if (Jimm.isLocked()) {
                Jimm.unlockJimm();
                return;
            }
            ContactList.activate();
        }
    }
    // #sijapp cond.end#
    private void closeAnyAction() {
        Progress p = process;
        if (null != p) {
            p.closeAnyAction();
        }
    }
    // Called when a key is pressed
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if (KEY_PRESSED == type) {
            if (Jimm.isLocked()) {
                if (Canvas.KEY_POUND == keyCode) {
                    if (0 == poundPressTime) {
                        poundPressTime = System.currentTimeMillis();
                    }

                } else {
                    poundPressTime = 0;
                    keyLock = KEY_LOCK_MSG_TIME;
                    invalidate();
                }

            } else {
                if (NativeCanvas.RIGHT_SOFT == keyCode) {
                    closeAnyAction();

                // #sijapp cond.if modules_DEBUGLOG is "true" #
                } else if ((Canvas.KEY_POUND == keyCode)) {
                    ContactList.activate();
                // #sijapp cond.end#
                }
            }

        } else {
            if (!Jimm.isLocked()) return;
            if (Canvas.KEY_POUND != keyCode) return;
            if ((0 != poundPressTime)
                    && ((System.currentTimeMillis() - poundPressTime) > 900)) {
    // #sijapp cond.if modules_PASSWORD is "true" #   //password
                if (PasswordEnter.init()) {           //password
                    PasswordEnter.show(1);            //password
                } else {                              //password
                    continueJimm();                   //password
                }                                     //password
            }                                         //password
        }                                             //password
    }                                                 //password
                public static void continueJimm() {   //password
    // #sijapp cond.end#                              //password
                Jimm.unlockJimm();                    //password
                poundPressTime = 0;                   //password
   // #sijapp cond.if modules_PASSWORD isnot "true" # //password
             }                                        //password
         }                                            //password
   // #sijapp cond.end#                               //password
    }

    protected void updateTask() {
        boolean repaintIt = false;
        // icq action
        if (0 <= resetTime) {
            if (0 == resetTime) {
                setLockMessage();
                repaintIt = true;
            }

            resetTime--;
        }

        // key lock
        if (0 <= keyLock) {
            if (0 == keyLock) {
                repaintIt = true;
            }
            keyLock--;
        }

        // clock
        if (0 <= updateTime) {
            updateTime--;
            if (0 > updateTime) {
                updateTime = UPDATE_INTERVAL;
                repaintIt = true;
            }
        }
        if (repaintIt) {
            invalidate();
        }
    }


    private void showMessage(GraphicsEx g, String msg, int width, int height) {
        final int size_x = width / 10 * 8;
        final int textWidth = size_x - 8;

        Font[] fontSet = GraphicsEx.chatFontSet;
        FormatedText formatedText = new FormatedText();
        formatedText.setWidth(textWidth);
        formatedText.addBigText(fontSet, msg, THEME_SPLASH_LOCK_TEXT, (byte)Font.STYLE_PLAIN, -1);

        final int textHeight = formatedText.getHeight();
        final int size_y = textHeight + 8;
        final int x = width / 2 - (width / 10 * 4);
        final int y = height / 2 - (size_y / 2);
        g.setThemeColor(THEME_SPLASH_LOCK_BACK);
        g.fillRect(x, y, size_x, size_y);
        g.setThemeColor(THEME_SPLASH_LOCK_TEXT);
        g.drawRect(x + 2, y + 2, size_x - 5, size_y - 5);
        g.setThemeColor(THEME_SPLASH_LOCK_TEXT);
        formatedText.paint(fontSet, g, x + 4, y + 4, size_x - 8, textHeight);
    }

    // Render the splash image
    protected void paint(GraphicsEx g) {
        final int height = NativeCanvas.getScreenHeight();
        final int width  = NativeCanvas.getScreenWidth();
        final int fontHeight = font.getHeight();
        // Do we need to draw the splash image?
        if (g.getClipY() < height - fontHeight - 2) {
            // Draw background
            g.setThemeColor(THEME_SPLASH_BACKGROUND);
            g.fillRect(0, 0, width, height);

            // Display splash image (or text)
            if (null != splash) {
                g.drawImage(splash, width / 2, height / 2, Graphics.HCENTER | Graphics.VCENTER);
            } else {
                g.setThemeColor(THEME_SPLASH_LOGO_TEXT);
                g.setFont(logoFont);
                g.drawString("jimm", width / 2, height / 2 + 5, Graphics.HCENTER | Graphics.BASELINE);
                g.setFont(font);
            }

            // Draw the date
            if (Options.getBoolean(Options.OPTION_DISPLAY_DATE)) {
                g.setThemeColor(THEME_SPLASH_DATE);
                g.setFont(date); //font date
                g.drawString(Util.getDateString(false), width / 2, 12, Graphics.TOP | Graphics.HCENTER);
                g.drawString(Util.getCurrentDay(), width / 2, 13 + font.getHeight(),
                        Graphics.TOP | Graphics.HCENTER);
            }

            // Display message icon, if keylock is enabled
            if (Jimm.isLocked()) {
                if (0 < availableMessages) {
                    Icon icon = Message.msgIcons.iconAt(Message.ICON_MSG_NONE);
                    if (null != icon) {
                        g.drawByLeftTop(icon, 1, height - (2 * fontHeight) - 6); //it is changed
                    }
                    g.setThemeColor(THEME_SPLASH_MESSAGES);
                    g.setFont(font);
                    int x = Message.msgIcons.getWidth() + 4;
                    int y = height-(2 * fontHeight) - 5;
                    g.drawString("# " + availableMessages, x, y, Graphics.LEFT | Graphics.TOP);
                }

                // #sijapp cond.if target is "SIEMENS2"#
                String accuLevel = System.getProperty("MPJC_CAP");
                if (null != accuLevel) {
                    accuLevel += "%";
                    int fontX = width -  font.stringWidth(accuLevel) - 1;
                    if (null != battImg) {
                        g.drawImage(battImg, fontX - battImg.getWidth() - 1, height-(2 * fontHeight) - 9,
                                Graphics.LEFT | Graphics.TOP);
                    }
                    g.setThemeColor(THEME_SPLASH_DATE);
                    g.setFont(font);
                    g.drawString(accuLevel, fontX, height - (2 * fontHeight) - 5, Graphics.LEFT | Graphics.TOP);
                }
                // #sijapp cond.end#

                // Display the keylock message if someone hit the wrong key
                if (0 < keyLock) {
                    // Init the dimensions
                    String lockMsg = ResourceBundle.getString("keylock_message");
                    showMessage(g, lockMsg, width, height);
                }
            }
        }

        final int im_width = (null == statusImg) ? 0 : statusImg.getWidth();
        int progressHeight = getProgressHeight();
        int stringWidth = font.stringWidth(message);
        g.setFont(font);

        // Draw white bottom bar
        g.setThemeColor(THEME_SPLASH_PROGRESS_BACK);
        //g.setStrokeStyle(Graphics.DOTTED);
        g.drawLine(0, height - progressHeight - 2, width, height - progressHeight - 2); //it is changed progressbar

        g.setThemeColor(THEME_SPLASH_PROGRESS_BACK);
        g.drawString(message, (width / 2) + (im_width / 2), height, Graphics.BOTTOM | Graphics.HCENTER);
        if (null != statusImg) {
            g.drawInCenter(statusImg, (width / 2) - (stringWidth / 2),
                    height - (progressHeight / 2));
        }

        // Draw current progress
        int progressPx = width * progress / 100;
        if (progressPx < 1) return;
        g.setClip(0, height - progressHeight - 1, progressPx, progressHeight + 1); //it is changed progressbar

        g.setThemeColor(THEME_SPLASH_PROGRESS_BACK);
        g.fillRect(0, height - progressHeight - 1, progressPx, progressHeight + 1); //it is changed progressbar

        g.setThemeColor(THEME_SPLASH_PROGRESS_TEXT);
        // Draw the progressbar message
        g.drawString(message, (width / 2) + (im_width / 2), height, Graphics.BOTTOM | Graphics.HCENTER);
        if (null != statusImg) {
            g.drawInCenter(statusImg, (width / 2) - (stringWidth / 2),
                    height - (progressHeight / 2));
        }
    }
    private int getProgressHeight() {
        final int fontHeight = font.getHeight();
        if (null != statusImg) {
            return Math.max(fontHeight, statusImg.getHeight());
        }
        return fontHeight;
    }

    protected void restoring() {
        boolean isCancellable = (null != process);
        String command = (!NativeCanvas.isFullScreen() && isCancellable) ? "cancel" : null;
        NativeCanvas.setCommands(null, command);
    }

    public static void setProgressBar(Progress p) {
        instance.process = p;
    }
}
