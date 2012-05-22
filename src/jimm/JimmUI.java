/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-06  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: src/jimm/JimmUI.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Artyomov Denis, Igor Palkin, Andreas Rossbacher, Vladimir Kryukov
 *******************************************************************************/

package jimm;

import java.io.*;
import java.util.*;

import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import jimm.chat.*;
import jimm.comm.*;
import jimm.history.*;
import jimm.modules.*;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.cl.*;
import jimm.util.*;
import DrawControls.*;
import protocol.Contact;

public final class JimmUI implements CommandListener {
    
    /////////////////////////
    //                     //
    //     Message Box     //
    //                     //
    /////////////////////////
    private FormEx msgForm;
    private CommandListener listener;

    private JimmUI() {
    }

    private static JimmUI _this = new JimmUI();

    public void commandAction(Command c, Displayable d) {
        // Message box
        listener.commandAction(c, d);
        msgForm.clearForm();
        if (msgForm.backCommand == c) {
            msgForm.back();
        }
        msgForm = null;
    }

    public static boolean isYesCommand(Command testCommand) {
        return (null != _this.msgForm) && (testCommand == _this.msgForm.saveCommand);
    }
    
    public static void attentionBox(String text, CommandListener listener) {
        _this.messageBox_(ResourceBundle.getString("attention"), text, listener);
    }
    private void messageBox_(String cap, String text, CommandListener listener) {
        msgForm = new FormEx(cap, "yes", "no", this);
        msgForm.addString(text);
        this.listener = listener;
        msgForm.show();
    }
    
    //////////////////////
    //                  //
    //    Clipboard     //
    //                  //
    //////////////////////
    
    private static String clipBoardText;
    private static String clipBoardHeader;
    private static boolean clipBoardIncoming;
    
    private static void insertQuotingChars(StringBuffer out, String text, char qChars) {
        int size = text.length();
        boolean wasNewLine = true;
        for (int i = 0; i < size; ++i) {
            char chr = text.charAt(i);
            if (wasNewLine) out.append(qChars).append(' ');
            out.append(chr);
            wasNewLine = (chr == '\n');
        }
    }
    
    public static boolean clipBoardIsEmpty() {
        return null == clipBoardText;
    }
    
    public static String getClipBoardText(boolean quote) {
        if (clipBoardIsEmpty()) {
            return "";
        }
        if (!quote || (null == clipBoardHeader)) {
            return clipBoardText + " ";
        }
        StringBuffer sb = new StringBuffer();
        sb.append('[').append(clipBoardHeader).append(']').append('\n');
        insertQuotingChars(sb, clipBoardText, clipBoardIncoming ? '\u00bb' : '\u00ab');//'»' : '«');
        sb.append("\n\n");
        return sb.toString();
    }
    
    public static void setClipBoardText(String header, String text) {
        clipBoardText     = text;
        clipBoardHeader   = header;
        clipBoardIncoming = true;
    }
    
    public static void setClipBoardText(boolean incoming, String date, String from, String text) {
        clipBoardText     = text;
        clipBoardHeader   = from + ' ' + date;
        clipBoardIncoming = incoming;
    }
    
    public static void clearClipBoardText() {
        clipBoardText = null;
    }
    
    
    /************************************************************************/
    /************************************************************************/
    /************************************************************************/
    
    ///////////////////
    //               //
    //    Hotkeys    //
    //               //
    ///////////////////
    
    private static int getHotKeyOpCode(int keyCode, int type) {
        int action = Options.HOTKEY_NONE;
        switch (keyCode) {
            case Canvas.KEY_NUM0:
                action = Options.getInt(Options.OPTION_EXT_CLKEY0);
                break;
            case Canvas.KEY_NUM4:
                action = Options.getInt(Options.OPTION_EXT_CLKEY4);
                break;
                
            case Canvas.KEY_NUM6:
                action = Options.getInt(Options.OPTION_EXT_CLKEY6);
                break;
                
            case Canvas.KEY_STAR:
                action = Options.getInt(Options.OPTION_EXT_CLKEYSTAR);
                break;
                
            case Canvas.KEY_POUND:
                action = Options.getInt(Options.OPTION_EXT_CLKEYPOUND);
                break;
                
            case NativeCanvas.CAMERA_KEY:
            case NativeCanvas.CALL_KEY:
                action = Options.getInt(Options.OPTION_EXT_CLKEYCALL);
                break;
        }
        return action;
    }
    public static boolean isHotKey(int keyCode, int type) {
        return (Options.HOTKEY_NONE != getHotKeyOpCode(keyCode, type));
    }
    public static boolean execHotKey(Contact contact, int keyCode, int type) {
        int action = getHotKeyOpCode(keyCode, type);
        return (Options.HOTKEY_NONE != action) && execHotKeyAction(contact, action, type);
    }
    
    private static boolean execHotKeyAction(Contact contact, int actionNum, int keyType) {
        if ((CanvasEx.KEY_REPEATED == keyType)
                || (CanvasEx.KEY_RELEASED == keyType)) {
            return false;
        }
        if (Options.HOTKEY_LOCK == actionNum) {
            Jimm.lockJimm();
            return true;
        }
        ContactList cl = ContactList.getInstance();
        if (null != contact) {
            switch (actionNum) {
                // #sijapp cond.if modules_HISTORY is "true" #
                case Options.HOTKEY_HISTORY:
                    contact.showHistory();
                    return true;
                // #sijapp cond.end#
                    
                case Options.HOTKEY_INFO:
                    contact.showUserInfo();
                    return true;
                    
                case Options.HOTKEY_STATUSES:
                    contact.showStatus();
                    return true;
            }
        }
        switch (actionNum) {
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            case Options.HOTKEY_MAGIC_EYE:
                MagicEye.activate();
                return true;
            // #sijapp cond.end#                    
                    
            case Options.HOTKEY_OPEN_CHATS:
                ChatHistory.instance.showChatList();
                return true;
                
            case Options.HOTKEY_ONOFF:
                boolean hide = !Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
                Options.setBoolean(Options.OPTION_CL_HIDE_OFFLINE, hide);
                Options.safeSave();
                cl.update();
                ContactList.activate();
                return true;

                // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
            case Options.HOTKEY_MINIMIZE:
                Jimm.setMinimized(true);
                return true;
                // #sijapp cond.end#
                
                // #sijapp cond.if modules_SOUND is "true" #
            case Options.HOTKEY_SOUNDOFF:
                Notify.changeSoundMode(true);
                return true;
                // #sijapp cond.end#

            // #sijapp cond.if modules_LIGHT is "true" #
            case Options.HOTKEY_LIGHT:
                CustomLight.setLightMode(CustomLight.ACTION_USER);
                return true;
            // #sijapp cond.end#
            
            case Options.HOTKEY_COLLAPSE_ALL:
                ContactList.getInstance().collapseAll();
                return true;
        }
        return false;
    }
}