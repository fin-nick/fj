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
File: src/jimm/DebugLog.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Artyomov Denis
*******************************************************************************/

// #sijapp cond.if modules_MAGIC_EYE is "true" #
package jimm.modules;

import DrawControls.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.*;
import jimm.comm.*;
import jimm.ui.*;
import jimm.util.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.*;
import jimm.comm.*;
import jimm.ui.*;
import jimm.ui.base.*; //add
import jimm.ui.menu.*;
import jimm.util.*;
import protocol.*;

public class MagicEye extends TextListEx implements SelectListener {
    private static MagicEye instance = new MagicEye();
    private Vector uins = new Vector();
    private Vector protocols = new Vector();
    
    private MagicEye() {
        super(ResourceBundle.getString("magic eye"));

        MenuModel menu = new MenuModel();
        menu.addItem("user_menu",     MENU_USER_MENU);
        menu.addItem("copy_text",     MENU_COPY);
        menu.addItem("copy_all_text", MENU_COPY_ALL);
        menu.addItem("clear",         MENU_CLEAN);
        menu.addItem("back",          MENU_BACK);
        menu.setActionListener(this);
        menu.setDefaultItemCode(MENU_COPY);
        setMenu(menu, MENU_BACK, MENU_USER_MENU);
    }
    
    public static void activate() {
        instance.setCurrentItem(instance.getSize());
        instance.show();
    }

    private synchronized void registerAction(Protocol protocol, String uin, String action, String msg) {
        uins.addElement(uin);
        protocols.addElement(protocol);

        int counter = uiBigTextIndex++;
        String date = Util.getDateString(false);
        action = ResourceBundle.getString(action);
        Contact contact = protocol.getItemByUIN(uin);

        lock();
        addBigText(date + ": ", THEME_MAGIC_EYE_NUMBER, Font.STYLE_PLAIN, counter);
        if (null == contact) {
            addBigText(uin + "\n", THEME_MAGIC_EYE_NL_USER, Font.STYLE_PLAIN, counter);
        } else {
            addBigText(contact.getName() + "\n",
                    THEME_MAGIC_EYE_USER, Font.STYLE_PLAIN, counter);
        }
        addBigText(action, THEME_MAGIC_EYE_ACTION, Font.STYLE_PLAIN, counter);
        if (null != msg) {
            doCRLF(counter);
            addBigText(msg, THEME_MAGIC_EYE_TEXT, Font.STYLE_PLAIN, counter);
        }
        doCRLF(counter);
        removeOldRecords();
        unlock();
    }
    
    private void removeOldRecords() {
        final int maxRecordCount = 50;
        while (maxRecordCount < uins.size()) {
            protocols.removeElementAt(0);
            uins.removeElementAt(0);
            removeFirstText();
        }
    }

//    public static final int ACTTYPE_SYSNOTICE   = 0;
//    public static final int ACT_ANTISPAM        = 1;
//    public static final int ACTTYPE_READ_STATUS = 2;
//    public static final int ACT_INVISIBLE_CHECK = 4;
//
    public static void addAction(Protocol protocol, String uin, String action, String msg) {
        instance.registerAction(protocol, uin, action, msg);
    }

    public static void addAction(Protocol protocol, String uin, String action) {
        instance.registerAction(protocol, uin, action, null);
    }

    private static final int MENU_COPY      = 0;
    private static final int MENU_COPY_ALL  = 1;
    private static final int MENU_CLEAN     = 2;
    private static final int MENU_USER_MENU = 3;
    private static final int MENU_BACK      = 4;
    
    public void select(Select select, MenuModel model, int cmd) {
        switch (cmd) {
            case MENU_COPY:
            case MENU_COPY_ALL:
                copy(MENU_COPY_ALL == cmd);
                break;

            case MENU_CLEAN:
                synchronized (instance) {
                    uins.removeAllElements();
                    protocols.removeAllElements();
                    clear();
                }
                break;

            case MENU_USER_MENU:
                try {
                    int item = getCurrTextIndex();
                    String uin = (String)uins.elementAt(item);
                    Protocol protocol = (Protocol)protocols.elementAt(item);
                    MenuModel m = protocol.createTempContact(uin).getContextMenu();
                    if (null != m) {
                        new Select(m).show();
                    }
                } catch (Exception e) {
                }
                return;

            case MENU_BACK:
                back();
                return;
        }
        select.back();
    }
    
    protected final void doKeyReaction(int keyCode, int actionCode, int type) { //clear eye clear key
        if (CanvasEx.KEY_PRESSED == type) {                                     //clear eye clear key
        switch(keyCode) {                                                       //clear eye clear key
        case NativeCanvas.CLEAR_KEY:                                            //clear eye clear key
        synchronized(instance) {                                                //clear eye clear key
        uins.removeAllElements();                                               //clear eye clear key
        protocols.removeAllElements();                                          //clear eye clear key
        clear();                                                                //clear eye clear key
            }                                                                   //clear eye clear key
        }                                                                       //clear eye clear key
    }                                                                           //clear eye clear key
	if (!JimmUI.execHotKey(null, keyCode, type)) {                          //clear eye clear key
        super.doKeyReaction(keyCode, actionCode, type);                         //clear eye clear key
        }                                                                       //clear eye clear key
    }                                                                           //clear eye clear key
}
// #sijapp cond.end#