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
 *******************************************************************************
 * File: src/jimm/Options.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis, Igor Palkin,
 * Vladimir Kryukov
 ******************************************************************************/
package jimm;

import java.util.*;
import javax.microedition.lcdui.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.modules.*;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import jimm.util.*;
import protocol.*;
import protocol.icq.*;
import DrawControls.icons.*; //add for a menu icons
// #sijapp cond.if modules_PASSWORD is "true" # //password
import jimm.modules.*;                          //password
// #sijapp cond.end#                            //password

/* Form for editing option values */
public class OptionsForm implements CommandListener, SelectListener
// #sijapp cond.if modules_MULTI is "true"#
        , ItemStateListener
// #sijapp cond.end#
        {
    private MenuModel optionsMenu = new MenuModel();
    private MenuModel colorScheme = null;
    private MenuModel hotkey = null;
    private MenuModel hotkeyAction = null;
    private MenuModel accountMenu = null;
    private FormEx form = new FormEx("options_lng", "save", "back", this);
    private int currentOptionsForm;
    private int editAccountNum;
    private TextListEx accountList = null;
    
    // Static constants for menu actios
    private static final int OPTIONS_ACCOUNT    = 0;
    private static final int OPTIONS_NETWORK    = 1;
    // #sijapp cond.if modules_PROXY is "true"#
    private static final int OPTIONS_PROXY      = 2;
    // #sijapp cond.end#
    private static final int OPTIONS_INTERFACE  = 3;
    private static final int OPTIONS_SCHEME     = 4;
    private static final int OPTIONS_HOTKEYS    = 5;
    private static final int OPTIONS_SIGNALING  = 6;
    // #sijapp cond.if modules_TRAFFIC is "true"#
    private static final int OPTIONS_TRAFFIC    = 7;
    // #sijapp cond.end#
    
    private static final int OPTIONS_TIMEZONE   = 8;
    private static final int OPTIONS_ANTISPAM   = 9;
    private static final int OPTIONS_LIGHT      = 10;
    private static final int OPTIONS_ABSENCE    = 11;
    
    // Exit has to be biggest element cause it also marks the size
    // #sijapp cond.if modules_PASSWORD is "true" #   //password
    private static final int OPTIONS_PASSWORD   = 12; //password
    // #sijapp cond.end#                              //password
    private static final int MENU_EXIT  = 13;  
    
    final private String[] hotkeyActionNames = Util.explode(
            "ext_hotkey_action_none"
            + "|" + "info"
            + "|" + "open_chats"
            // #sijapp cond.if modules_HISTORY is "true"#
            + "|" + "history"
            // #sijapp cond.end#
            + "|" + "ext_hotkey_action_onoff"
            + "|" + "keylock"
            // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
            + "|" + "minimize"
            // #sijapp cond.end#
            + "|" + "#sound_off"
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            + "|" + "magic eye"
            // #sijapp cond.end#
            // #sijapp cond.if modules_LIGHT is "true" #
            + "|" + "light_control"
            // #sijapp cond.end#
            + "|" + "user_statuses"
            + "|" + "collapse_all",
            '|'
            );
    
    final private int [] hotkeyActions = {
        Options.HOTKEY_NONE,
        Options.HOTKEY_INFO,
        Options.HOTKEY_OPEN_CHATS,
        // #sijapp cond.if modules_HISTORY is "true"#
        Options.HOTKEY_HISTORY,
        // #sijapp cond.end#
        Options.HOTKEY_ONOFF,
        Options.HOTKEY_LOCK,
        // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
        Options.HOTKEY_MINIMIZE,
        // #sijapp cond.end#
        Options.HOTKEY_SOUNDOFF,
        // #sijapp cond.if modules_MAGIC_EYE is "true" #
        Options.HOTKEY_MAGIC_EYE,
        // #sijapp cond.end#
        // #sijapp cond.if modules_LIGHT is "true" #
        Options.HOTKEY_LIGHT,
        // #sijapp cond.end#
        Options.HOTKEY_STATUSES,
        Options.HOTKEY_COLLAPSE_ALL
    };
    
    public OptionsForm() {
    }
    
    
    // Initialize the kist for the Options menu
    private static final ImageList menuIcons = ImageList.createImageList("/menuicons.png"); //add for a menu icons
    private void initOptionsList() {
        optionsMenu.clean();
        
        optionsMenu.addItem("options_account", menuIcons.iconAt(12), OPTIONS_ACCOUNT); //menu icons
        optionsMenu.addItem("options_network", menuIcons.iconAt(5), OPTIONS_NETWORK); //menu icons
        // #sijapp cond.if protocols_ICQ is "true" #
        // #sijapp cond.if modules_PROXY is "true"#
        if (IcqNetWorking.CONN_TYPE_PROXY == Options.getInt(Options.OPTION_CONN_TYPE)) {
            optionsMenu.addItem("proxy", OPTIONS_PROXY);
        }
        // #sijapp cond.end#
        // #sijapp cond.end#
        optionsMenu.addItem("options_interface", menuIcons.iconAt(16), OPTIONS_INTERFACE); //menu icons
        
        if (Scheme.getSchemeNames().length > 2) {
            optionsMenu.addItem("color_scheme", menuIcons.iconAt(17), OPTIONS_SCHEME); //menu icons
        }
        
        if (2 != Options.getInt(Options.OPTION_KEYBOARD)) {
			optionsMenu.addItem("options_hotkeys", menuIcons.iconAt(18), OPTIONS_HOTKEYS); //menu icons
        }
        optionsMenu.addItem("options_signaling", menuIcons.iconAt(3), OPTIONS_SIGNALING); //menu icons
        // #sijapp cond.if modules_TRAFFIC is "true"#
        optionsMenu.addItem("traffic_lng", menuIcons.iconAt(7), OPTIONS_TRAFFIC); //menu icons
        // #sijapp cond.end#
        // #sijapp cond.if modules_PASSWORD is "true" #                               //password
        optionsMenu.addItem("pass_password", menuIcons.iconAt(19), OPTIONS_PASSWORD); //password & menu icons
        // #sijapp cond.end#                                                          //password
        // #sijapp cond.if modules_ANTISPAM is "true"#
        optionsMenu.addItem("antispam", menuIcons.iconAt(20), OPTIONS_ANTISPAM); //menu icons
        // #sijapp cond.end#
        // #sijapp cond.if modules_LIGHT is "true" #
        if (CustomLight.isSupport()) {
            optionsMenu.addItem("light_control", menuIcons.iconAt(21), OPTIONS_LIGHT); //menu icons
        }
        // #sijapp cond.end#
        // #sijapp cond.if modules_ABSENCE is "true" #
        optionsMenu.addItem("absence", menuIcons.iconAt(22), OPTIONS_ABSENCE); //menu icons
        // #sijapp cond.end#
        
        optionsMenu.addItem("time_zone", menuIcons.iconAt(23), OPTIONS_TIMEZONE); //menu icons
        optionsMenu.setActionListener(this);
        optionsMenu.setDefaultItemCode(currentOptionsForm);
    }
    
    private void addHotKey(String keyName, int option) {
        int optionValue = Options.getInt(option);
        String name = null;
        for (int i = 0; i < hotkeyActionNames.length; i++) {
            if (hotkeyActions[i] == optionValue) {
                name = ResourceBundle.getString(keyName) + ": "
                        + ResourceBundle.getString(hotkeyActionNames[i]);
            }
        }
        if (null == name) {
            name = ResourceBundle.getString(keyName) + ": <???>";
        }
        hotkey.addRawItem(name, null, option);
    }
    
    private int currentHotkey = -1;
    private void initHotkeyMenuUI() {
        if (null == hotkey) {
            hotkey = new MenuModel();
        }
        hotkey.clean();
        addHotKey("ext_clhotkey0",     Options.OPTION_EXT_CLKEY0);
        addHotKey("ext_clhotkey4",     Options.OPTION_EXT_CLKEY4);
        addHotKey("ext_clhotkey6",     Options.OPTION_EXT_CLKEY6);
        addHotKey("ext_clhotkeystar",  Options.OPTION_EXT_CLKEYSTAR);
        addHotKey("ext_clhotkeypound", Options.OPTION_EXT_CLKEYPOUND);
        // #sijapp cond.if target is "SIEMENS2"#
        addHotKey("ext_clhotkeycall", Options.OPTION_EXT_CLKEYCALL);
        // #sijapp cond.elseif target is "MIDP2" #
        if (Jimm.isPhone(Jimm.PHONE_SE)) {
            addHotKey("camera", Options.OPTION_EXT_CLKEYCALL);
        } else {
            addHotKey("ext_clhotkeycall", Options.OPTION_EXT_CLKEYCALL);
        }
        // #sijapp cond.end#
        hotkey.setDefaultItemCode(currentHotkey);
        hotkey.setActionListener(this);
    }
    
    private void initHotkeyActionMenuUI() {
        if (null == hotkeyAction) {
            hotkeyAction = new MenuModel();
        }
        hotkeyAction.clean();
        for (int i=0; i < hotkeyActionNames.length; i++) {
            hotkeyAction.addItem(hotkeyActionNames[i], hotkeyActions[i]);
        }
        hotkeyAction.setDefaultItemCode(Options.getInt(currentHotkey));
        hotkeyAction.setActionListener(this);
    }
    
    ///////////////////////////////////////////////////////////////////////////
    
    private static final int MENU_ACCOUNT_EDIT        = 0;
    private static final int MENU_ACCOUNT_DELETE      = 1;
    private static final int MENU_ACCOUNT_NEW         = 2;
    private static final int MENU_ACCOUNT_SET_CURRENT = 3;
    private static final int MENU_ACCOUNT_SET_ACTIVE  = 4;
    private static final int MENU_ACCOUNT_CREATE      = 5;
    private static final int MENU_ACCOUNT_BACK        = 6;
    private void updateAccountList() {
        if (null == accountMenu) {
            accountMenu = new MenuModel();
        }
        if (null == accountList) {
            accountList = new TextListEx(ResourceBundle.getString("options_account"));
        }
        
        int curItem = accountList.getCurrItem();
        accountList.clear();
        int current = Options.getCurrentAccount();
        int accountCount = Options.getAccountCount();
        int maxAccount = Options.getMaxAccountCount();
        for (int i = 0; i < accountCount; ++i) {
            Profile account = Options.getAccount(i);
            boolean isCurrent = (current == i);
            // #sijapp cond.if modules_MULTI is "true" #
            isCurrent = account.isActive;
            // #sijapp cond.end #
            String text = account.userId;
            if (isCurrent) {
                text += "*";
            }
            accountList.addItem(text, i, isCurrent);
        }
        if (0 == accountCount) {
            accountList.addItem(ResourceBundle.getString("add_new"), maxAccount, false);
        }
        accountList.setCurrentItem(curItem);
        
        accountMenu.clean();
        boolean connected = ContactList.getInstance().isConnected();
        // #sijapp cond.if modules_MULTI is "true" #
        connected = false;
        // #sijapp cond.end #
        int defCount = MENU_ACCOUNT_EDIT;
        if ((0 < accountCount) && !connected) {
            // #sijapp cond.if modules_MULTI isnot "true" #
            accountMenu.addItem("set_current", MENU_ACCOUNT_SET_CURRENT);
            defCount = MENU_ACCOUNT_SET_CURRENT;
            // #sijapp cond.else #
            accountMenu.addItem("set_active", MENU_ACCOUNT_SET_ACTIVE);
            defCount = MENU_ACCOUNT_SET_ACTIVE;
            // #sijapp cond.end #
        }
        accountMenu.addItem("edit", MENU_ACCOUNT_EDIT);
        if ((0 < accountCount) && !connected) {
            if (accountCount < maxAccount) {
                accountMenu.addItem("add_new", MENU_ACCOUNT_NEW);
            }
            accountMenu.addItem("delete", MENU_ACCOUNT_DELETE);
        }
        // #sijapp cond.if protocols_JABBER is "true" #
        accountMenu.addItem("create_new", MENU_ACCOUNT_CREATE);
        // #sijapp cond.end #

        accountMenu.addItem("back", MENU_ACCOUNT_BACK);
        accountMenu.setActionListener(this);
        accountMenu.setDefaultItemCode(MENU_ACCOUNT_EDIT);
        accountList.setMenu(accountMenu, MENU_ACCOUNT_BACK, defCount);
    }
    
    ///////////////////////////////////////////////////////////////////////////
    
    /* Activate options menu */
    public static void activate() {
        OptionsForm instance = new OptionsForm();
        instance.initOptionsList();
        new Select(instance.optionsMenu).show();
    }
    
    private void showThemeSelector() {
        String[] strings = Scheme.getSchemeNames();
        colorScheme = null;
        if (strings.length > 1) {
            colorScheme = new MenuModel();
            colorScheme.setActionListener(this);
            for (int i = 0; i < strings.length; ++i) {
                colorScheme.addRawItem(strings[i], null, i);
            }
            colorScheme.setDefaultItemCode(Options.getInt(Options.OPTION_COLOR_SCHEME));
            new Select(colorScheme).show();
        }
    }
    
    private void setChecked(int contrilId, String lngStr, int optValue) {
        form.addChoiceItem(contrilId, lngStr, Options.getBoolean(optValue));
    }
    
    
    private void createNotifyControls(int modeOpt, int volumeOpt, int notifyType, String title, boolean typping) {
        // #sijapp cond.if modules_SOUND is "true" #
        Notify notify = Notify.getSound();
        // #sijapp cond.end #
        String list = "no";
        // #sijapp cond.if modules_SOUND is "true" #
        list += "|" + "beep";
        if (notify.hasSound(notifyType)) {
            list += "|" + "sound";
        }
        // #sijapp cond.end#
        createSelector(title, list, modeOpt);
        // #sijapp cond.if modules_SOUND is "true" #
        if (notify.hasSound(notifyType)) {
            loadOptionGauge(volumeOpt, "volume");
        }
        // #sijapp cond.end#
    }
    
    /* Helpers for options UI: */
    private void createSelector(String cap, String items, int opt) {
        form.addSelector(opt, cap, items, Options.getInt(opt));
    }
    private void loadOptionString(int opt, String label, int size, int type) {
        form.addTextField(opt, label, Options.getString(opt), size, type);
    }
    private void saveOptionString(int opt) {
        Options.setString(opt, form.getTextFieldValue(opt));
    }
    private void loadOptionInt(int opt, String label, int size) {
        form.addTextField(opt, label, String.valueOf(Options.getInt(opt)), size, TextField.NUMERIC);
    }
    private void saveOptionInt(int opt, int defval, int min, int max) {
        int val = Util.strToIntDef(form.getTextFieldValue(opt), defval);
        Options.setInt(opt, Math.max(min, Math.min(val, max)));
    }
    private void saveOptionBoolean(int opt, int controlId, int index) {
        Options.setBoolean(opt, form.getChoiceItemValue(controlId, index));
    }
    private void saveOptionSelector(int opt) {
        Options.setInt(opt, form.getSelectorValue(opt));
    }
    // #sijapp cond.if modules_SOUND is "true" #
    private void loadOptionGauge(int opt, String label) {
        form.addVolumeControl(opt, label, Options.getInt(opt));
    }
    private void saveOptionGauge(int opt) {
        Options.setInt(opt, form.getVolumeValue(opt));
    }
    // #sijapp cond.end#
    // #sijapp cond.if modules_TRAFFIC is "true"#
    private void loadOptionDecimal(int opt, String label) {
        form.addTextField(opt, label, Util.intToDecimal(Options.getInt(opt)),
                6, TextField.ANY);
    }
    private void saveOptionDecimal(int opt) {
        Options.setInt(opt, Util.decimalToInt(form.getTextFieldValue(opt)));
    }
    // #sijapp cond.end#
    // #sijapp cond.if modules_LIGHT is "true"#
    private void loadLightValue(String title, int id) {
        int value = Options.getInt(id);
        form.addTextField(id, title, String.valueOf(value), 3, TextField.DECIMAL);
    }
    private void saveLightValue(int opt, int min, int max, int def) {
        saveOptionInt(opt, def, min, max);
    }
    // #sijapp cond.end#
    
    private static final int keepConnAliveChoiceGroup = 1001;
    private static final int autoConnectChoiceGroup = 1002;
    private static final int connPropChoiceGroup = 1003;
    private static final int choiceInterfaceMisc = 1004;
    private static final int choiceContactList = 1005;
    private static final int chrgChat = 1006;
    private static final int chrgStatus = 1007;
    private static final int chrgMessages = 1008;
    private static final int chrgIcons = 1009;
    private static final int chsNotifyAct = 1010;
    private static final int protocolTypeField = 1011;
    private static final int uinField = 1012;
    private static final int passField = 1013;
    private static final int nickField = 1014;
    private static final int chrgAntispam = 1015;
    private static final int choiceRepeater = 1015; //key repeat
    
    public void addAccount(int num, Profile acc) {
        Options.setAccount(num, acc);
        setCurrentProtocol();
        updateAccountList();
    }
    /* Command listener */
    public void commandAction(Command c, Displayable d) {
        /* Look for back command */
        if (form.backCommand == c) {
            back();
            
            // Look for save command
        } else if (c == form.saveCommand) {
            // Save values, depending on selected option menu item
            switch (currentOptionsForm) {
                case OPTIONS_ACCOUNT:
                    Profile account = new Profile();
                    if (1 < Profile.protocolTypes.length) {
                        account.protocolType = Profile.protocolTypes[form.getSelectorValue(protocolTypeField)];
                    }
                    account.userId = form.getTextFieldValue(uinField);
                    if (StringConvertor.isEmpty(account.userId)) {
                        return;
                    }
                    account.password = form.getTextFieldValue(passField);
                    account.nick = form.getTextFieldValue(nickField);
                    // #sijapp cond.if modules_MULTI is "true" #
                    if (Options.getAccountCount() <= editAccountNum) {
                        account.isActive = true;
                    } else {
                        account.isActive = Options.getAccount(editAccountNum).isActive;
                    }
                    // #sijapp cond.end #
                    addAccount(editAccountNum, account);
                    break;
                    
                case OPTIONS_NETWORK:
                    // #sijapp cond.if protocols_ICQ is "true" #
                    saveOptionString(Options.OPTION_SRV_HOST);
                    saveOptionString(Options.OPTION_SRV_PORT);
                    saveOptionSelector(Options.OPTION_CONN_TYPE);
                    // #sijapp cond.end#
                    // #sijapp cond.if protocols_JABBER is "true" # //applications priority
                    saveOptionString(Options.OPTION_APL_PRIORITY);  //applications priority
                    // #sijapp cond.end#                            //applications priority
                    saveOptionBoolean(Options.OPTION_KEEP_CONN_ALIVE, keepConnAliveChoiceGroup, 0);
                    saveOptionString(Options.OPTION_CONN_ALIVE_INVTERV);
                    saveOptionBoolean(Options.OPTION_AUTO_CONNECT, autoConnectChoiceGroup, 0);
                    saveOptionBoolean(Options.OPTION_RECONNECT, connPropChoiceGroup, 0);
                    // #sijapp cond.if protocols_ICQ is "true" #
                    saveOptionBoolean(Options.OPTION_MD5_LOGIN, connPropChoiceGroup, 1);
                    // #sijapp cond.if target isnot "MOTOROLA"#
                    saveOptionBoolean(Options.OPTION_SHADOW_CON, connPropChoiceGroup, 2);
                    // #sijapp cond.end#
                    // #sijapp cond.end#
                    //saveOptionString(Options.OPTION_HTTP_USER_AGENT);
                    //saveOptionString(Options.OPTION_HTTP_WAP_PROFILE);
                    saveOptionInt(Options.OPTION_RECONNECT_NUMBER, 1, 0, 50);
                    initOptionsList();
                    break;
                    
                    // #sijapp cond.if protocols_ICQ is "true" #
                    // #sijapp cond.if modules_PROXY is "true"#
                case OPTIONS_PROXY:
                    saveOptionSelector(Options.OPTION_PRX_TYPE);
                    saveOptionString(Options.OPTION_PRX_SERV);
                    saveOptionString(Options.OPTION_PRX_PORT);
                    
                    saveOptionString(Options.OPTION_PRX_NAME);
                    saveOptionString(Options.OPTION_PRX_PASS);
                    
                    saveOptionString(Options.OPTION_AUTORETRY_COUNT);
                    break;
                    // #sijapp cond.end#
                    // #sijapp cond.end#
                    
                case OPTIONS_INTERFACE:
                    if (ResourceBundle.langAvailable.length > 1) {
                        int lang = form.getSelectorValue(Options.OPTION_UI_LANGUAGE);
                        Options.setString(Options.OPTION_UI_LANGUAGE, ResourceBundle.langAvailable[lang]);
                    }
                    
                    int idx = 0;
                    saveOptionBoolean(Options.OPTION_DISPLAY_DATE, choiceInterfaceMisc, idx++);
                    saveOptionBoolean(Options.OPTION_SWAP_SOFT_KEY, choiceInterfaceMisc, idx++);
                    saveOptionBoolean(Options.OPTION_SHOW_SOFTBAR,  choiceInterfaceMisc, idx++); //add
                    // #sijapp cond.if modules_UPDATES is "true" # //add updates modules
                    saveOptionBoolean(Options.OPTION_CHECK_UPDATES, choiceInterfaceMisc, idx++);
                    // #sijapp cond.end# //add updates modules
                    
                    idx = 0;
                    saveOptionBoolean(Options.OPTION_USER_GROUPS, choiceContactList, idx++);
                    saveOptionBoolean(Options.OPTION_CL_HIDE_OFFLINE, choiceContactList, idx++);
                    // #sijapp cond.if protocols_JABBER is "true" #                                   //hide conferences & transport
                    saveOptionBoolean(Options.OPTION_CL_HIDE_OFFLINE_ALL, choiceContactList, idx++);  //hide conferences & transport
                    // #sijapp cond.end#                                                              //hide conferences & transport
                    saveOptionBoolean(Options.OPTION_SAVE_TEMP_CONTACT, choiceContactList, idx++);
                    saveOptionBoolean(Options.OPTION_SORT_UP_WITH_MSG, choiceContactList, idx++);
                    saveOptionBoolean(Options.OPTION_SORT_UP_WITH_CHAT, choiceContactList, idx++); //contacts to a chat up
                    saveOptionBoolean(Options.OPTION_SHOW_STATUS_LINE, choiceContactList, idx++);
                    
                    saveOptionSelector(Options.OPTION_CL_SORT_BY);
                    
                    idx = 0;
                    // #sijapp cond.if modules_SMILES is "true"#                        //add
                    saveOptionBoolean(Options.OPTION_USE_SMILES,      chrgChat, idx++); //add
                    // #sijapp cond.end#                                                //add
                    // #sijapp cond.if modules_HISTORY is "true"#
                    saveOptionBoolean(Options.OPTION_HISTORY,         chrgChat, idx++);
                    saveOptionBoolean(Options.OPTION_SHOW_LAST_MESS,  chrgChat, idx++);
                    // #sijapp cond.end#
                    // #sijapp cond.if target is "SIEMENS2" | target is "MIDP2" #
                    saveOptionBoolean(Options.OPTION_CLASSIC_CHAT, chrgChat, idx++);
                    // #sijapp cond.end#
                    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" #
                    saveOptionBoolean(Options.OPTION_SWAP_SEND_AND_BACK, chrgChat, idx++);
                    // #sijapp cond.end#
                    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
                    saveOptionBoolean(Options.OPTION_TF_FLAGS, chrgChat, idx++);
                    // #sijapp cond.end#
                    saveOptionBoolean(Options.OPTION_UNTITLED_INPUT, chrgChat, idx++);
                    saveOptionString(Options.OPTION_MAX_HISTORY_SIZE); //quantity of messages in a chat
                    
                    idx = 0;
                    saveOptionBoolean(Options.OPTION_CP1251_HACK, chrgMessages, idx++);
                    // #sijapp cond.if protocols_ICQ is "true" #
                    saveOptionBoolean(Options.OPTION_DETECT_ENCODING, chrgMessages, idx++);
                    saveOptionBoolean(Options.OPTION_DELIVERY_NOTIFICATION, chrgMessages, idx++);
                    // #sijapp cond.end#
                    
                    idx = 0;
                    // #sijapp cond.if modules_XSTATUSES is "true" #
                    saveOptionBoolean(Options.OPTION_REPLACE_STATUS_ICON, chrgIcons, idx++);
                    // #sijapp cond.end#
                                        
                    idx = 0;
                    // #sijapp cond.if protocols_ICQ is "true" #
                    // #sijapp cond.if modules_XSTATUSES is "true" #
                    saveOptionBoolean(Options.OPTION_AUTO_XTRAZ, chrgStatus, idx++);
                    // #sijapp cond.end #
                    // #sijapp cond.end #
                    // #sijapp cond.if protocols_JABBER is "true" #                           //global status
                    // #sijapp cond.if modules_XSTATUSES is "true" #                          //global status
                    saveOptionBoolean(Options.OPTION_TITLE_IN_CONFERENCE, chrgStatus, idx++); //global status
                    // #sijapp cond.end #                                                     //global status
                    // #sijapp cond.end #                                                     //global status
                    
                    saveOptionSelector(Options.OPTION_FONT_SCHEME);
                    GraphicsEx.setFontScheme(Options.getInt(Options.OPTION_FONT_SCHEME));

//                    saveOptionInt(Options.OPTION_MAX_MSG_COUNT, 100, 20, 500);
                    saveOptionSelector(Options.OPTION_MSGSEND_MODE);
                    saveOptionSelector(Options.OPTION_INPUT_MODE);
                    saveOptionSelector(Options.OPTION_KEYBOARD);
                    
                    final String sysLang = ResourceBundle.getCurrUiLanguage();
                    final String newLang = Options.getString(Options.OPTION_UI_LANGUAGE);
                    if (!sysLang.equals(newLang)) {
                        Options.setBoolean(Options.OPTIONS_LANG_CHANGED, true);
                    }
                    ContactList.getInstance().update();
                    saveOptionString(Options.OPTION_REPEATER_KEY);  //key repeat
                    saveOptionString(Options.OPTION_REPEATER_TIME); //key repeat
                    if (jimm.Jimm.isPhone(jimm.Jimm.PHONE_SE) || jimm.Jimm.isPhone(jimm.Jimm.PHONE_SAMSUNG)) { //message icon
                    saveOptionString(Options.OPTION_CAPTION_ICON);                                             //message icon
                    }                                                                                          //message icon
                    break;
                    
                case OPTIONS_SIGNALING:
                    saveOptionSelector(Options.OPTION_VIBRATOR);
                    saveOptionString(Options.OPTION_VIBRATOR_TIME); //vibra time
                    // #sijapp cond.if modules_SOUND is "true" #
                    saveOptionSelector(Options.OPTION_ONLINE_NOTIF_MODE);
                    saveOptionGauge(Options.OPTION_ONLINE_NOTIF_VOL);
                    saveOptionSelector(Options.OPTION_OFFLINE_NOTIF_MODE); //offline sound
                    saveOptionGauge(Options.OPTION_OFFLINE_NOTIF_VOL);     //offline sound
                    saveOptionSelector(Options.OPTION_MESS_NOTIF_MODE);
                    saveOptionGauge(Options.OPTION_MESS_NOTIF_VOL);
                    saveOptionSelector(Options.OPTION_OTHER_NOTIF_MODE); //other sound
                    saveOptionGauge(Options.OPTION_OTHER_NOTIF_VOL);     //other sound
                    saveOptionSelector(Options.OPTION_TYPING_MODES);     //typing
                    saveOptionGauge(Options.OPTION_TYPING_VOL);          //typing
                    // #sijapp cond.end#
                    saveOptionSelector(Options.OPTION_TYPING_MODE);      //typing

                    saveOptionSelector(Options.OPTION_POPUP_WIN2);
                    idx = 0;
                    // #sijapp cond.if modules_PRESENCE is "true" #                         //presence
                    saveOptionBoolean(Options.OPTION_NOTICE_PRESENCE, chsNotifyAct, idx++); //presence
                    // #sijapp cond.end#                                                    //presence
                    saveOptionBoolean(Options.OPTION_EYE_NOTIF, chsNotifyAct, idx++); //eye sound
                    saveOptionBoolean(Options.OPTION_ALARM, chsNotifyAct, idx++);
                    saveOptionBoolean(Options.OPTION_POPUP_OVER_SYSTEM, chsNotifyAct, idx++);
                    saveOptionBoolean(Options.OPTION_TICKER_SYSTEM, chsNotifyAct, idx++); //ticker
                    // #sijapp cond.if target="MIDP2"#
                    saveOptionBoolean(Options.OPTION_BRING_UP, chsNotifyAct, idx++);
                    // #sijapp cond.end#
                    // #sijapp cond.if (target is "SIEMENS2") & (modules_SOUND is "true") #
                    saveOptionBoolean(Options.OPTION_VOLUME_BUGFIX, chsNotifyAct, idx++);
                    // #sijapp cond.end#
                    saveOptionBoolean(Options.OPTION_CUSTOM_GC, chsNotifyAct, idx++);
                    break;
                    
                    // #sijapp cond.if modules_ANTISPAM is "true"#
                case OPTIONS_ANTISPAM:
                    saveOptionString(Options.OPTION_ANTISPAM_MSG);
                    saveOptionString(Options.OPTION_ANTISPAM_ANSWER);
                    saveOptionString(Options.OPTION_ANTISPAM_HELLO);
                    saveOptionString(Options.OPTION_ANTISPAM_KEYWORDS);
                    saveOptionBoolean(Options.OPTION_ANTISPAM_ENABLE,  chrgAntispam, 0);
                    saveOptionBoolean(Options.OPTION_ANTISPAM_OFFLINE, chrgAntispam, 1);
                    break;
                    // #sijapp cond.end#
                    
                    // #sijapp cond.if modules_LIGHT is "true"#
                case OPTIONS_LIGHT:
                    saveLightValue(Options.OPTION_LIGHT_NONE, -1, 101, 0);
                    saveLightValue(Options.OPTION_LIGHT_KEY_PRESS, -1, 101, 100);
                    saveLightValue(Options.OPTION_LIGHT_MESSAGE, -1, 101, 100);
                    saveLightValue(Options.OPTION_LIGHT_ONLINE, -1, 101, 101);
                    saveLightValue(Options.OPTION_LIGHT_SYSTEM, 1, 100, 100);
                    saveLightValue(Options.OPTION_LIGHT_TICK, 1, 999, 15);
                    saveOptionBoolean(Options.OPTION_LIGHT, Options.OPTION_LIGHT, 0);
                    CustomLight.switchOn(Options.getBoolean(Options.OPTION_LIGHT));
                    break;
                    // #sijapp cond.end#
                    
                    // #sijapp cond.if modules_ABSENCE is "true" #
                case OPTIONS_ABSENCE:
                    saveOptionBoolean(Options.OPTION_AA_BLOCK, Options.OPTION_AA_BLOCK, 0);
                    Options.setInt(Options.OPTION_AA_TIME, form.getSelectorValue(Options.OPTION_AA_TIME) * 5);
                    saveOptionBoolean(Options.OPTION_AA_LOCK, Options.OPTION_AA_LOCK, 0); //autoblock
                    break;
                    // #sijapp cond.end#
                    
                    // #sijapp cond.if modules_TRAFFIC is "true"#
                case OPTIONS_TRAFFIC:
                    saveOptionDecimal(Options.OPTION_COST_OF_1M);
                    saveOptionDecimal(Options.OPTION_COST_PER_DAY);
                    Options.setInt(Options.OPTION_COST_PACKET_LENGTH,
                            Util.strToIntDef(form.getTextFieldValue(Options.OPTION_COST_PACKET_LENGTH), 0) * 1024);
                    saveOptionString(Options.OPTION_CURRENCY);
                    break;
                    // #sijapp cond.end#
                    
                case OPTIONS_TIMEZONE: {
                    /* Set up time zone*/
                    int timeZone = form.getSelectorValue(Options.OPTIONS_GMT_OFFSET) - 12;
                    Options.setInt(Options.OPTIONS_GMT_OFFSET, timeZone);
                    
                    /* Translate selected time to GMT */
                    int[] currDateTime = Util.createDate(Util.createCurrentDate(false));
                    int hour = currDateTime[Util.TIME_HOUR];
                    int selHour = form.getSelectorValue(Options.OPTIONS_LOCAL_OFFSET) - timeZone;
                    selHour = selHour - 12;
                    
                    /* Calculate diff. between selected GMT time and phone time */
                    int localOffset = (selHour + 12 + 24) % 24 - 12;
                    Options.setInt(Options.OPTIONS_LOCAL_OFFSET, localOffset);
                    break;
                }
                
            }
            
            /* Save options */
            Options.safeSave();
            back();
        }
        
    }
    
    private final void setCurrentProtocol() {
        Options.setVisibleAcounts();
    }
    public void showAccountEditor(Protocol p) {
        showAccountEditor(Options.getAccountIndex(p.getProfile()));
    }
    private void showAccountEditor(int accNum) {
        currentOptionsForm = OPTIONS_ACCOUNT;
        editAccountNum = accNum;
        Profile account = Options.getAccount(editAccountNum);
        form.clearForm();
        
        // #sijapp cond.if modules_MULTI is "true"#
        if (1 < Profile.protocolTypes.length) {
            int protocolIndex = 0;
            for (int i = 0; i < Profile.protocolTypes.length; ++i) {
                if (account.protocolType == Profile.protocolTypes[i]) {
                    protocolIndex = i;
                    break;
                }
            }
            form.addSelector(protocolTypeField, "protocol", Profile.protocolNames, protocolIndex);
        }
        // #sijapp cond.end #
        // #sijapp cond.if protocols_ICQ is "true" #
        //     #sijapp cond.if protocols_MRIM is "true" #
        //       #sijapp cond.if protocols_JABBER is "true" #
        form.addLatinTextField(uinField, "uin/e-mail/jid/login", account.userId, 64, TextField.ANY);
        //       #sijapp cond.else #
        form.addLatinTextField(uinField, "uin/e-mail", account.userId, 64, TextField.ANY);
        //       #sijapp cond.end #
        //     #sijapp cond.elseif protocols_JABBER is "true" #
        form.addLatinTextField(uinField, "uin/jid", account.userId, 64, TextField.ANY);
        //     #sijapp cond.else #
        form.addTextField(uinField, "uin", account.userId, 10, TextField.ANY);
        //     #sijapp cond.end #
        // #sijapp cond.elseif protocols_MRIM is "true" #
        form.addTextField(uinField, "e-mail", account.userId, 64, TextField.EMAILADDR);
        // #sijapp cond.elseif protocols_JABBER is "true" #
        form.addTextField(uinField, "jid", account.userId, 64, TextField.ANY);
        // #sijapp cond.else #
        // undefined protocol
        form.addLatinTextField(uinField, "UserID", account.userId, 64, TextField.ANY);
        // #sijapp cond.end #
        form.addTextField(passField, "password", account.password, 40, TextField.PASSWORD);
        form.addTextField(nickField, "nick", account.nick, 20, TextField.ANY);
        // #sijapp cond.if modules_MULTI is "true"#
        form.setItemStateListener(this);
        // #sijapp cond.end#
        show();
        // #sijapp cond.if modules_MULTI is "true"#
        form.setTextFieldLabel(uinField, Profile.protocolIds[form.getSelectorValue(protocolTypeField)]);
        // #sijapp cond.end#
    }

    public void itemStateChanged(Item item) {
        int itemId = form.getItemId(item);
        if (protocolTypeField == itemId) {
            form.setTextFieldLabel(uinField, Profile.protocolIds[form.getSelectorValue(protocolTypeField)]);
        }
    }
    public boolean setCurrentAccount(int accNum) {
        if (Options.getAccountCount() <= accNum) {
            return false;
        }
        if (accNum != Options.getCurrentAccount()) {
            Options.setCurrentAccount(accNum);
            Options.safeSave();
            setCurrentProtocol();
            updateAccountList();
        }
        return true;
    }
    private void selectAccount(int cmd) {
        int num = accountList.getCurrTextIndex();
        switch (cmd) {
            case MENU_ACCOUNT_BACK:
                accountList.back();
                break;
            case MENU_ACCOUNT_NEW:
                num = Options.getMaxAccountCount();
                break;
                // #sijapp cond.if protocols_JABBER is "true" #
            case MENU_ACCOUNT_CREATE:
                accountList.restore();
                new protocol.jabber.JabberRegistration(this).show();
                break;
                // #sijapp cond.end #
        }
        // #sijapp cond.if modules_MULTI is "true" #
        Profile account = Options.getAccount(num);
        Protocol p = ContactList.getInstance().getProtocol(account);
        if ((null != p) && p.isConnected()) {
            return;
        }
        // #sijapp cond.end #
                
        switch (cmd) {
            case MENU_ACCOUNT_DELETE:
                Options.delAccount(num);
                // #sijapp cond.if modules_MULTI is "true" #
                Options.setVisibleAcounts();
                // #sijapp cond.end #
                Options.safeSave();
                updateAccountList();
                accountList.restore();
                break;
                
                // #sijapp cond.if modules_MULTI is "true" #
            case MENU_ACCOUNT_SET_ACTIVE:
                if (num < Options.getAccountCount()) {
                    account.isActive = !account.isActive;
                    Options.saveAccount(account);
                    setCurrentProtocol();
                    updateAccountList();
                    accountList.restore();
                    break;
                }
                accountList.restore();
                showAccountEditor(num);
                break;

                // #sijapp cond.end #
            case MENU_ACCOUNT_SET_CURRENT:
                if (setCurrentAccount(num)) {
                    accountList.restore();
                    break;
                }
                // break absent. It isn't bug!
                // create account if not exist
            case MENU_ACCOUNT_NEW:
            case MENU_ACCOUNT_EDIT:
                accountList.restore();
                showAccountEditor(num);
                break;
        }
    }
    public void select(Select select, MenuModel model, int cmd) {
        if (accountMenu == model) {
            selectAccount(cmd);
            return;
        }
        if (hotkey == model) {
            currentHotkey = cmd;
            initHotkeyActionMenuUI();
            new Select(hotkeyAction).show();
            return;
        }
        if (hotkeyAction == model) {
            final int hotkeyAction = cmd;
            Options.setInt(currentHotkey, hotkeyAction);
            Options.safeSave();
            initHotkeyMenuUI();
            select.back();
            return;
        }
        
        if (colorScheme == model) {
            final int currentColorScheme = cmd;
            Scheme.setColorScheme(currentColorScheme);
            Options.safeSave();
            select.back();
            colorScheme = null;
            return;
        }
        
        // Delete all items
        form.clearForm();
        // Add elements, depending on selected option menu item
        currentOptionsForm = cmd;
        switch (currentOptionsForm) {
            case OPTIONS_ACCOUNT:
                updateAccountList();
                accountList.show();
                return;
                
            case OPTIONS_NETWORK:
                // #sijapp cond.if protocols_ICQ is "true" #
                // Initialize elements (network section)
                loadOptionString(Options.OPTION_SRV_HOST, "server_host", 512, TextField.ANY);
                loadOptionString(Options.OPTION_SRV_PORT, "server_port", 5, TextField.NUMERIC);
                
                // #sijapp cond.if modules_PROXY is "true"#
                createSelector("conn_type", "socket"+"|"+"proxy", Options.OPTION_CONN_TYPE);
                // #sijapp cond.else#
                //createSelector("conn_type", "socket", Options.OPTION_CONN_TYPE);
                // #sijapp cond.end#
                // #sijapp cond.end#
                
                form.addChoiceGroup(keepConnAliveChoiceGroup, "keep_conn_alive", Choice.MULTIPLE);
                setChecked(keepConnAliveChoiceGroup, "yes", Options.OPTION_KEEP_CONN_ALIVE);

                // #sijapp cond.if protocols_JABBER is "true" #                                      //applications priority
                loadOptionString(Options.OPTION_APL_PRIORITY, "apl_priority", 3, TextField.NUMERIC); //applications priority
                // #sijapp cond.end#                                                                 //applications priority
                
                loadOptionString(Options.OPTION_CONN_ALIVE_INVTERV, "timeout_interv", 3, TextField.NUMERIC);
                
                form.addChoiceGroup(connPropChoiceGroup, "conn_prop", Choice.MULTIPLE);
                setChecked(connPropChoiceGroup, "reconnect", Options.OPTION_RECONNECT);
                // #sijapp cond.if protocols_ICQ is "true" #
                setChecked(connPropChoiceGroup, "md5_login", Options.OPTION_MD5_LOGIN);
                // #sijapp cond.if target isnot "MOTOROLA"#
                setChecked(connPropChoiceGroup, "shadow_con", Options.OPTION_SHADOW_CON);
                // #sijapp cond.end#
                // #sijapp cond.end#
                
                form.addChoiceGroup(autoConnectChoiceGroup, "auto_connect", Choice.MULTIPLE);
                setChecked(autoConnectChoiceGroup, "yes", Options.OPTION_AUTO_CONNECT);
                
                loadOptionInt(Options.OPTION_RECONNECT_NUMBER, "reconnect_number", 2);
                break;
                
                // #sijapp cond.if protocols_ICQ is "true" #
                // #sijapp cond.if modules_PROXY is "true"#
            case OPTIONS_PROXY:
                createSelector("proxy_type", "proxy_socks4"+"|"+"proxy_socks5"+"|"+"proxy_guess", Options.OPTION_PRX_TYPE);
                
                loadOptionString(Options.OPTION_PRX_SERV, "proxy_server_host", 32, TextField.ANY);
                loadOptionString(Options.OPTION_PRX_PORT, "proxy_server_port", 5, TextField.NUMERIC);
                loadOptionString(Options.OPTION_PRX_NAME, "proxy_server_login", 32, TextField.ANY);
                loadOptionString(Options.OPTION_PRX_PASS, "proxy_server_pass", 32, TextField.PASSWORD);
                loadOptionString(Options.OPTION_AUTORETRY_COUNT, "auto_retry_count", 5, TextField.NUMERIC);
                break;
                // #sijapp cond.end#
                // #sijapp cond.end#
                
            case OPTIONS_INTERFACE:
                // Initialize elements (interface section)
                if (ResourceBundle.langAvailable.length > 1) {
                    int choiceType = Choice.EXCLUSIVE;
                    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
                    choiceType = Choice.POPUP;
                    // #sijapp cond.end#
                    form.addChoiceGroup(Options.OPTION_UI_LANGUAGE, "language", choiceType);
                    for (int j = 0; j < ResourceBundle.langAvailable.length; ++j) {
                        boolean selected = ResourceBundle.langAvailable[j].equals(Options.getString(Options.OPTION_UI_LANGUAGE));
                        form.addChoiceItem(Options.OPTION_UI_LANGUAGE, ResourceBundle.langAvailableName[j], selected);
                    }
                }
                
                form.addChoiceGroup(choiceInterfaceMisc, "misc", Choice.MULTIPLE);
                setChecked(choiceInterfaceMisc, "display_date", Options.OPTION_DISPLAY_DATE);
                setChecked(choiceInterfaceMisc, "swap_soft_key", Options.OPTION_SWAP_SOFT_KEY);
                setChecked(choiceInterfaceMisc, "show_softbar",  Options.OPTION_SHOW_SOFTBAR); //add
                // #sijapp cond.if modules_UPDATES is "true" # //add updates modules
                setChecked(choiceInterfaceMisc, "check_updates", Options.OPTION_CHECK_UPDATES);
                // #sijapp cond.end# //add updates modules
                
                createSelector("sort_by",
                        "sort_by_status" + "|" + "sort_by_online" + "|" + "sort_by_name",
                        Options.OPTION_CL_SORT_BY);
                
                form.addChoiceGroup(choiceContactList, "contact_list", Choice.MULTIPLE);
                setChecked(choiceContactList, "show_user_groups", Options.OPTION_USER_GROUPS);
                setChecked(choiceContactList, "hide_offline", Options.OPTION_CL_HIDE_OFFLINE);
                // #sijapp cond.if protocols_JABBER is "true" #                                        //hide conferences & transport
                setChecked(choiceContactList, "hide_offline_all", Options.OPTION_CL_HIDE_OFFLINE_ALL); //hide conferences & transport
                // #sijapp cond.end#                                                                   //hide conferences & transport
                setChecked(choiceContactList, "save_temp_contacts",   Options.OPTION_SAVE_TEMP_CONTACT);
                setChecked(choiceContactList, "contacts_with_msg_at_top", Options.OPTION_SORT_UP_WITH_MSG);
                setChecked(choiceContactList, "contacts_with_chat_at_top", Options.OPTION_SORT_UP_WITH_CHAT); //contacts to a chat up
                setChecked(choiceContactList, "show_status_line", Options.OPTION_SHOW_STATUS_LINE);
                
                
                form.addChoiceGroup(chrgChat, "chat", Choice.MULTIPLE);
                // #sijapp cond.if modules_SMILES is "true"#                        //add
                setChecked(chrgChat, "use_smiles",      Options.OPTION_USE_SMILES); //add
                // #sijapp cond.end#                                                //add
                // #sijapp cond.if modules_HISTORY is "true"#
                setChecked(chrgChat, "use_history",     Options.OPTION_HISTORY);
                setChecked(chrgChat, "show_prev_mess",  Options.OPTION_SHOW_LAST_MESS);
                // #sijapp cond.end#
                // #sijapp cond.if target is "SIEMENS2" | target is "MIDP2" #
                setChecked(chrgChat, "cl_chat",         Options.OPTION_CLASSIC_CHAT);
                // #sijapp cond.end#
                // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" #
                setChecked(chrgChat, "swap_send_and_back", Options.OPTION_SWAP_SEND_AND_BACK);
                // #sijapp cond.end#
                // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
                setChecked(chrgChat, "auto_case",       Options.OPTION_TF_FLAGS);
                // #sijapp cond.end#
                setChecked(chrgChat, "untitled_input",  Options.OPTION_UNTITLED_INPUT);
                loadOptionString(Options.OPTION_MAX_HISTORY_SIZE, "max_history", 4, TextField.NUMERIC); //quantity of messages in a chat
                
                // #sijapp cond.if modules_XSTATUSES is "true" #
                form.addChoiceGroup(chrgIcons, "icons", Choice.MULTIPLE);
                // #sijapp cond.end#
                // #sijapp cond.if modules_XSTATUSES is "true" #
                setChecked(chrgIcons, "replace_status_icon", Options.OPTION_REPLACE_STATUS_ICON);
                // #sijapp cond.end#

                form.addChoiceGroup(chrgStatus, "status", Choice.MULTIPLE);
                // #sijapp cond.if protocols_ICQ is "true" #
                // #sijapp cond.if modules_XSTATUSES is "true" #
                setChecked(chrgStatus, "auto_xtraz",  Options.OPTION_AUTO_XTRAZ);
                // #sijapp cond.end #
                // #sijapp cond.end #
                // #sijapp cond.if protocols_JABBER is "true" #                                     //global status
                // #sijapp cond.if modules_XSTATUSES is "true" #                                    //global status
                setChecked(chrgStatus, "title_in_conference",  Options.OPTION_TITLE_IN_CONFERENCE); //global status
                // #sijapp cond.end #                                                               //global status
                // #sijapp cond.end #                                                               //global status
                
                form.addChoiceGroup(chrgMessages, "messages", Choice.MULTIPLE);
                setChecked(chrgMessages, "cp1251",          Options.OPTION_CP1251_HACK);
                // #sijapp cond.if protocols_ICQ is "true" #
                setChecked(chrgMessages, "detect_encoding", Options.OPTION_DETECT_ENCODING);
                setChecked(chrgMessages, "delivery_notification", Options.OPTION_DELIVERY_NOTIFICATION);
                // #sijapp cond.end #
                
                createSelector(
                        "fonts",
                        "fonts_smallest" + "|" + "fonts_small" + "|" + "fonts_normal" + "|" + "fonts_large" + "|" + "fonts_largest",
                        Options.OPTION_FONT_SCHEME);
                createSelector(
                        "message_send_mode",
                        "msm_normal" + "|" + "detransliterate" + "|" + "transliterate" + "|" + "manually",
                        Options.OPTION_MSGSEND_MODE);
                createSelector(
                        "input_mode",
                        "default" + "|" + "latin" + "|" + "cyrillic",
                        Options.OPTION_INPUT_MODE);
                
                form.addChoiceGroup(choiceRepeater, "repeater", Choice.MULTIPLE);                      //key repeat
                loadOptionString(Options.OPTION_REPEATER_KEY, "repeater_key", 3, TextField.NUMERIC);   //key repeat
                loadOptionString(Options.OPTION_REPEATER_TIME, "repeater_time", 3, TextField.NUMERIC); //key repeat
//                loadOptionInt(Options.OPTION_MAX_MSG_COUNT, "max_message_count", 3);
                createSelector(
                        "keyboard_type",
                        "default" + "|" + "QWERTY" + "|" + "no",
                        Options.OPTION_KEYBOARD);
                if (jimm.Jimm.isPhone(jimm.Jimm.PHONE_SE) || jimm.Jimm.isPhone(jimm.Jimm.PHONE_SAMSUNG)) { //message icon
                loadOptionString(Options.OPTION_CAPTION_ICON, "caption_icon", 3, TextField.NUMERIC);       //message icon
                }                                                                                          //message icon
                break;
                
            case OPTIONS_SCHEME:
                showThemeSelector();
                return;
                
            case OPTIONS_HOTKEYS:
                initHotkeyMenuUI();
                new Select(hotkey).show();
                return;
                
                /* Initialize elements (Signaling section) */
            case OPTIONS_SIGNALING:
                
                /* Vibrator notification controls */
                
                createSelector(
                        "vibration",
                        "no" + "|" + "yes" + "|" + "when_locked",
                        Options.OPTION_VIBRATOR);
                loadOptionString(Options.OPTION_VIBRATOR_TIME, "vibration_time", 4, TextField.NUMERIC); //vibra time
                
                /* Message notification controls */
                // #sijapp cond.if modules_SOUND is "true" #
                createNotifyControls(Options.OPTION_MESS_NOTIF_MODE,
                        Options.OPTION_MESS_NOTIF_VOL,
                        Notify.NOTIFY_MESSAGE, "message_notification", false);

                createNotifyControls(Options.OPTION_OTHER_NOTIF_MODE,      //other sound
                        Options.OPTION_OTHER_NOTIF_VOL,                    //other sound
                        Notify.NOTIFY_OTHER, "other_notification", false); //other sound
                
                // #sijapp cond.end#
                
                /* Online notification controls */
                // #sijapp cond.if modules_SOUND is "true" #
                createNotifyControls(Options.OPTION_ONLINE_NOTIF_MODE,
                        Options.OPTION_ONLINE_NOTIF_VOL,
                        Notify.NOTIFY_ONLINE, "onl_notification", false);

                createNotifyControls(Options.OPTION_OFFLINE_NOTIF_MODE,    //offline sound
                        Options.OPTION_OFFLINE_NOTIF_VOL,                  //offline sound
                        Notify.NOTIFY_OFFLINE, "off_notification", false); //offline sound
                // #sijapp cond.end#
                
                /* Typing notification controls */
                // #sijapp cond.if target isnot "DEFAULT" #
                // #sijapp cond.if modules_SOUND is "true" #
                createNotifyControls(Options.OPTION_TYPING_MODES,     //typing
                        Options.OPTION_TYPING_VOL,                    //typing
                        Notify.NOTIFY_TYPING, "typing_notify", true); //typing
                // #sijapp cond.else#
                createNotifyControls(Options.OPTION_TYPING_MODES,
                        Options.OPTION_TYPING_VOL,
                        -1, "typing_notify", true);
                // #sijapp cond.end#                                  //typing
                // #sijapp cond.end #                                 //typing
                String list = "no" + "|" + "typing_incoming" +"|" + "typing_both";
                createSelector("typing_notify_mode", list, Options.OPTION_TYPING_MODE);


                
                /* Popup windows control */
                createSelector("popup_win",
                        "no" + "|" + "pw_forme" + "|" + "pw_all",
                        Options.OPTION_POPUP_WIN2);
                
                
                form.addChoiceGroup(chsNotifyAct, null, Choice.MULTIPLE);
                // #sijapp cond.if modules_PRESENCE is "true" #                              //presence
                setChecked(chsNotifyAct, "notice_presence", Options.OPTION_NOTICE_PRESENCE); //presence
                // #sijapp cond.end#                                                         //presence
                setChecked(chsNotifyAct, "eye_notif", Options.OPTION_EYE_NOTIF); //eye sound
                setChecked(chsNotifyAct, "alarm", Options.OPTION_ALARM);
                setChecked(chsNotifyAct, "popup_win_over_system", Options.OPTION_POPUP_OVER_SYSTEM);
                setChecked(chsNotifyAct, "ticker_system", Options.OPTION_TICKER_SYSTEM); //ticker
                // #sijapp cond.if target="MIDP2"#
                /* Midlet auto bring up controls on MIDP2 */
                setChecked(chsNotifyAct, "bring_up", Options.OPTION_BRING_UP);
                // #sijapp cond.end#
                
                // #sijapp cond.if (target is "SIEMENS2") & (modules_SOUND is "true") #
                /* Sound volume bugfix controls on SIEMENS2 */
                setChecked(chsNotifyAct, "volume_bugfix", Options.OPTION_VOLUME_BUGFIX);
                // #sijapp cond.end #
                
                setChecked(chsNotifyAct, "show_memory_alert", Options.OPTION_CUSTOM_GC);
                break;

                // #sijapp cond.if modules_PASSWORD is "true" # //password
            case OPTIONS_PASSWORD:                              //password
                PasswordNew.getInstance().show();               //password
                return;                                         //password
                // #sijapp cond.end#                            //password
                
                // #sijapp cond.if modules_ANTISPAM is "true"#
            case OPTIONS_ANTISPAM:
                form.addChoiceGroup(chrgAntispam, null, Choice.MULTIPLE);
                setChecked(chrgAntispam, "on", Options.OPTION_ANTISPAM_ENABLE);
                setChecked(chrgAntispam, "cut_offline", Options.OPTION_ANTISPAM_OFFLINE);
                loadOptionString(Options.OPTION_ANTISPAM_MSG, "antispam_msg", 256, TextField.ANY);
                loadOptionString(Options.OPTION_ANTISPAM_ANSWER, "antispam_answer", 256, TextField.ANY);
                loadOptionString(Options.OPTION_ANTISPAM_HELLO, "antispam_hello", 256, TextField.ANY);
                loadOptionString(Options.OPTION_ANTISPAM_KEYWORDS, "antispam_keywords", 512, TextField.ANY);
                break;
                // #sijapp cond.end#
                
                // #sijapp cond.if modules_LIGHT is "true"#
            case OPTIONS_LIGHT:
                form.addChoiceGroup(Options.OPTION_LIGHT, "light_control", Choice.MULTIPLE);
                setChecked(Options.OPTION_LIGHT, "on", Options.OPTION_LIGHT);
                
                loadLightValue("light_normal",    Options.OPTION_LIGHT_NONE);
                loadLightValue("light_key_press", Options.OPTION_LIGHT_KEY_PRESS);
                loadLightValue("light_message",   Options.OPTION_LIGHT_MESSAGE);
                loadLightValue("light_online",    Options.OPTION_LIGHT_ONLINE);
                loadLightValue("light_system",    Options.OPTION_LIGHT_SYSTEM);
                loadLightValue("light_normal_mode_timeout", Options.OPTION_LIGHT_TICK);
                break;
                // #sijapp cond.end#
                
                // #sijapp cond.if modules_ABSENCE is "true" #
            case OPTIONS_ABSENCE:
                form.addChoiceGroup(Options.OPTION_AA_BLOCK, null, Choice.MULTIPLE);
                setChecked(Options.OPTION_AA_BLOCK, "after_block", Options.OPTION_AA_BLOCK);
                form.addSelector(Options.OPTION_AA_TIME, "after_time", "off"+"|5 |10 |15 ", Options.getInt(Options.OPTION_AA_TIME) / 5);               
                form.addChoiceGroup(Options.OPTION_AA_LOCK, null, Choice.MULTIPLE);      //autoblock
                setChecked(Options.OPTION_AA_LOCK, "autoblock", Options.OPTION_AA_LOCK); //autoblock
                //form.addChoiceGroup(Options.OPTION_AUTOABSENCE, null, Choice.MULTIPLE);
                //setChecked(Options.OPTION_AUTOABSENCE, "autoanswer", Options.OPTION_AUTOABSENCE);
                //loadOptionString(Options.OPTION_AUTOANSWER, "answer", 256, TextField.ANY);
                break;
                // #sijapp cond.end#
                
                /* Initialize elements (cost section) */
                // #sijapp cond.if modules_TRAFFIC is "true"#
            case OPTIONS_TRAFFIC:
                loadOptionDecimal(Options.OPTION_COST_OF_1M, "cp1m");
                loadOptionDecimal(Options.OPTION_COST_PER_DAY, "cpd");
                form.addTextField(Options.OPTION_COST_PACKET_LENGTH,
                        "plength",
                        String.valueOf(Options.getInt(Options.OPTION_COST_PACKET_LENGTH) / 1024),
                        4, TextField.NUMERIC);
                loadOptionString(Options.OPTION_CURRENCY, "currency", 4, TextField.ANY);
                break;
                // #sijapp cond.end#
                
            case OPTIONS_TIMEZONE: {
                int choiceType = Choice.EXCLUSIVE;
                // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
                choiceType = Choice.POPUP;
                // #sijapp cond.end#
                
                form.addChoiceGroup(Options.OPTIONS_GMT_OFFSET, "time_zone", choiceType);
                int gmtOffset = Options.getInt(Options.OPTIONS_GMT_OFFSET);
                for (int i = -12; i <= 13; i++) {
                    form.addChoiceItem(Options.OPTIONS_GMT_OFFSET,
                            "GMT" + (i < 0 ? "" : "+") + i + ":00", gmtOffset == i);
                }
                
                int[] currDateTime = Util.createDate(Util.createCurrentDate(false));
                form.addChoiceGroup(Options.OPTIONS_LOCAL_OFFSET, "local_time", choiceType);
                int minutes = currDateTime[Util.TIME_MINUTE];
                int hour = currDateTime[Util.TIME_HOUR];
                int startHour = hour - Options.getInt(Options.OPTIONS_LOCAL_OFFSET)
                        - Options.getInt(Options.OPTIONS_GMT_OFFSET) - 12;
                for (int i = startHour; i < (startHour + 24); ++i) {
                    form.addChoiceItem(Options.OPTIONS_LOCAL_OFFSET,
                            ((i + 24) % 24) + (minutes < 10 ? ":0" : ":") + minutes,
                            hour == i);
                }
                
                break;
            }
        }
        form.endForm();
        /* Activate options form */
        show();
    }
    private void show() {
        form.show();
    }
    private void back() {
        form.back();
    }
}