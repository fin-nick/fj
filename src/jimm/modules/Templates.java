/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-05  Jimm Project
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
 * File: src/jimm/Templates.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Igor Palkin
 *******************************************************************************/


package jimm.modules;

import java.io.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
import DrawControls.*;
import jimm.*;
import jimm.comm.*;
import jimm.io.Storage;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import jimm.util.*;

public final class Templates extends TextList implements CommandListener, SelectListener {
    /**
     * 
     * @deprecated 
     */
    private final Command menuCommand = new Command("menu", Command.OK, 1);
    
    //private final Command addCommand    = new Command(ResourceBundle.getString("add_new"), Command.OK, 1);
    private final Command addCommand    = new Command(ResourceBundle.getString("ok"), Command.OK, 1);
    private final Command editCommand   = new Command(ResourceBundle.getString("ok"), Command.OK, 1);
    private final Command cancelCommand = new Command(ResourceBundle.getString("back"), Command.BACK, 2);
    
    private CommandListener selectionListener;
    private Vector templates = new Vector();
    private String selectedTemplate;
    private TextBox templateTextbox;
    
    private static final int MENU_SELECT = 0;
    private static final int MENU_BACK   = 1;
    private static final int MENU_ADD    = 2;
    private static final int MENU_CLEAR  = 3;
    private static final int MENU_DELETE = 4;
    private static final int MENU_EDIT   = 5;
    
    protected MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        menu.clean();
        if (templates.size() > 0) {
            menu.addItem("select", MENU_SELECT);
            menu.addItem("delete", MENU_DELETE);
            menu.addItem("edit",   MENU_EDIT);
        }
        menu.addItem("add_new", MENU_ADD);
        menu.addItem("delete_all",   MENU_CLEAR);
        menu.addItem("back",    MENU_BACK);
        menu.setDefaultItemCode(MENU_ADD);
        menu.setActionListener(this);
        return menu;
    }
    
    private Templates() {
        super(ResourceBundle.getString("templates"));
        setMenuCodes(MENU_BACK, MENU_SELECT);
    }
    
    private static final Templates instance = new Templates();
    public static Templates getInstance() {
        return instance;
    }
    public void selectTemplate(CommandListener selectionListener_) {
        selectionListener = selectionListener_;
        refreshList();
        show();
    }
    
    public final String getSelectedTemplate() {
        return selectedTemplate;
    }
    
    public final boolean isMyOkCommand(Command c) {
        return menuCommand == c;
    }
    
    protected void itemSelected() {
        getMenu().exec(null, MENU_SELECT);
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == addCommand) {
            templates.addElement(templateTextbox.getString());
            save();
            refreshList();
            restore();
            templateTextbox = null;
            
        } else if (c == editCommand) {
            templates.setElementAt(templateTextbox.getString(), getCurrTextIndex());
            save();
            refreshList();
            restore();
            templateTextbox = null;

        } else if (c == cancelCommand) {
            restore();
            
        } else if (JimmUI.isYesCommand(c)) {
            templates.removeAllElements();
            save();
            back();
        }
    }
    
    private void refreshList() {
        lock();
        clear();
        int count = templates.size();
        for ( int i = 0; i < count; i++) {
            addBigText((String)templates.elementAt(i),
                    CanvasEx.THEME_TEXT, Font.STYLE_PLAIN, i).doCRLF(i);
        }
        unlock();
    }
    
    public void load() {
        Storage storage = new Storage("rms-templates");
        try {
            storage.open(false);
            templates = storage.loadListOfString();
        } catch (Exception e) {
        }
        storage.close();
    }

    private void save() {
        try {
            Storage storage = new Storage("rms-templates");
            storage.delete();
            if (0 == templates.size()) {
                return;
            }
            storage.open(true);
            storage.saveListOfString(templates);
            storage.close();
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.println("template: " + e.getMessage());
            // #sijapp cond.end#
        }
    }
    
    private String getTemlate() {
        return (getSize() == 0)
                ? ""
                : (String)templates.elementAt(getCurrTextIndex());
    }
    public void select(Select select, MenuModel model, int cmd) {
        switch (cmd) {
            case MENU_SELECT:
                if (null != selectionListener) {
                    selectedTemplate = getTemlate();
                    back();
    	            selectionListener.commandAction(menuCommand, null);
                }
                selectionListener = null;
                break;
                
            case MENU_ADD:
                templateTextbox = new TextBox(ResourceBundle.getString("new_template"), null, 1000, TextField.ANY);
                templateTextbox.addCommand(addCommand);
                templateTextbox.addCommand(cancelCommand);
                templateTextbox.setCommandListener(this);
                Jimm.setDisplay(templateTextbox);
                break;
                
            case MENU_EDIT:
                templateTextbox = new TextBox(ResourceBundle.getString("new_template"), getTemlate(), 1000, TextField.ANY);
                templateTextbox.addCommand(editCommand);
                templateTextbox.addCommand(cancelCommand);
                templateTextbox.setCommandListener(this);
                Jimm.setDisplay(templateTextbox);
                break;
                
            case MENU_BACK:
                back();
                break;
                
            case MENU_DELETE:
                templates.removeElementAt(getCurrTextIndex());
                save();
                refreshList();
                restore();
                break;
                
            case MENU_CLEAR:
                JimmUI.attentionBox(ResourceBundle.getString("clear") + "?", getInstance());
                break;
        }
    }
}