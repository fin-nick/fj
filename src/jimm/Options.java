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
*******************************************************************************
File: src/jimm/Options.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis, Igor Palkin,
           Vladimir Kryukov
******************************************************************************/




package jimm;

import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.chat.message.Message;
import jimm.forms.*;
import jimm.io.Storage;
import protocol.Profile;
import protocol.icq.*;
import jimm.modules.*;
import protocol.mrim.*;
import jimm.ui.*;
import jimm.util.*;

import java.io.*;
import java.util.*;

import javax.microedition.lcdui.*;

import DrawControls.VirtualList;
import protocol.Status;


/**
 * Current record store format:
 *
 * Record #1: VERSION               (UTF8)
 * Record #2: OPTION KEY            (BYTE)
 *            OPTION VALUE          (Type depends on key)
 *            OPTION KEY            (BYTE)
 *            OPTION VALUE          (Type depends on key)
 *            OPTION KEY            (BYTE)
 *            OPTION VALUE          (Type depends on key)
 *            ...
 *
 * Option key            Option value
 *   0 -  63 (00XXXXXX)  UTF8
 *  64 - 127 (01XXXXXX)  INTEGER
 * 128 - 191 (10XXXXXX)  BOOLEAN
 * 192 - 224 (110XXXXX)  LONG
 * 225 - 255 (111XXXXX)  SHORT, BYTE-ARRAY (scrambled String)
 */
public class Options {
    
    /* Option keys */
//    static final int OPTION_NICK1                      =  21;   /* String */
//    static final int OPTION_UIN1                       =   0;   /* String */
//    static final int OPTION_PASSWORD1                  = 228;   /* String  */
//    static final int OPTION_NICK2                      =  22;   /* String */
//    static final int OPTION_UIN2                       =  14;   /* String  */
//    static final int OPTION_PASSWORD2                  = 229;   /* String  */
//    static final int OPTION_NICK3                      =  23;   /* String */
//    static final int OPTION_UIN3                       =  15;   /* String  */
//    static final int OPTION_PASSWORD3                  = 230;   /* String  */
    static final int OPTIONS_CURR_ACCOUNT              =  86;   /* int     */
    
    public static final int OPTION_SRV_HOST            =   1;   /* String  */
    public static final int OPTION_SRV_PORT            =   2;   /* String  */
    public static final int OPTION_KEEP_CONN_ALIVE     = 128;   /* boolean */
    public static final int OPTION_CONN_ALIVE_INVTERV  =  13;   /* String  */
//    public static final int OPTION_ASYNC               = 166;   /* boolean */
    public static final int OPTION_CONN_TYPE           =  83;   /* int     */
    public static final int OPTION_AUTO_CONNECT        = 138;   /* boolean */
    // #sijapp cond.if target isnot  "MOTOROLA"#
    public static final int OPTION_SHADOW_CON          = 139;   /* boolean */
    // #sijapp cond.end#
    // #sijapp cond.if modules_UPDATES is "true" # //add updates modules
    public static final int OPTION_UPDATE_CHECK_TIME  =  64;   /* int     */
    public static final int OPTION_LAST_VERSION       =  27;   /* String  */    
    public static final int OPTION_CHECK_UPDATES      = 174;   /* boolean */
    // #sijapp cond.end# //add updates modules
    
    public static final int OPTION_AA_BLOCK           = 175;   /* boolean */
    public static final int OPTION_AA_TIME            = 106;   /* int     */
    public static final int OPTION_AA_LOCK            = 146;   /* boolean */    //autoblock
    //public static final int OPTION_AUTOANSWER         =  28;   /* String  */

    public static final int OPTION_RECONNECT           = 149;   /* boolean */
    public static final int OPTION_RECONNECT_NUMBER    =  91;   /* int */
    //public static final int OPTION_HTTP_USER_AGENT     =  17;   /* String  */
    //public static final int OPTION_HTTP_WAP_PROFILE    =  18;   /* String  */
    public static final int OPTION_UI_LANGUAGE         =   3;   /* String  */
    public static final int OPTION_DISPLAY_DATE        = 129;   /* boolean */
    public static final int OPTION_CL_SORT_BY          =  65;   /* int     */
    public static final int OPTION_CL_HIDE_OFFLINE     = 130;   /* boolean */
    // #sijapp cond.if protocols_JABBER is "true" #                             //hide conferences & transport
    public static final int OPTION_CL_HIDE_OFFLINE_ALL = 132;   /* boolean */   //hide conferences & transport
    // #sijapp cond.end#                                                        //hide conferences & transport
    public static final int OPTION_MESS_NOTIF_MODE     =  66;   /* int     */
    public static final int OPTION_MESS_NOTIF_VOL      =  67;   /* int     */
    public static final int OPTION_ONLINE_NOTIF_MODE   =  68;   /* int     */
    public static final int OPTION_ONLINE_NOTIF_VOL    =  69;   /* int     */
    public static final int OPTION_OFFLINE_NOTIF_MODE  = 110;   /* int     */   //offline sound
    public static final int OPTION_OFFLINE_NOTIF_VOL   = 111;   /* int     */   //offline sound
    public static final int OPTION_VIBRATOR            =  75;   /* int     */
    public static final int OPTION_VIBRATOR_TIME       =  30;   /* String  */   //vibra time
    public static final int OPTION_REPEATER_KEY        =  31;   /* String  */   //key repeat
    public static final int OPTION_REPEATER_TIME       =  32;   /* String  */   //key repeat
    public static final int OPTION_CAPTION_ICON        =  34;   /* String  */   //message icon
    public static final int OPTION_MAX_HISTORY_SIZE    =  33;   /* String  */   //quantity of messages in a chat
    public static final int OPTION_APL_PRIORITY        =  36;   /* String  */   //applications priority
    public static final int OPTION_TYPING_MODES        =  94;   /* int     */   //typing
    public static final int OPTION_TYPING_MODE         =  88;   /* int     */   //typing
    public static final int OPTION_TYPING_VOL          =  89;   /* int     */   //typing
    public static final int OPTION_OTHER_NOTIF_MODE    = 108;   /* int     */   //other sound
    public static final int OPTION_OTHER_NOTIF_VOL     = 102;   /* int     */   //other sound
    public static final int OPTION_EYE_NOTIF           = 131;   /* boolean */   //eye sound
    // #sijapp cond.if modules_PRESENCE is "true" #                             //presence
    public static final int OPTION_NOTICE_PRESENCE     = 134;   /* boolean */   //presence
    // #sijapp cond.end#                                                        //presence
    // #sijapp cond.if (target is "SIEMENS2") & (modules_SOUND is "true") #    
    public static final int OPTION_VOLUME_BUGFIX       = 155; /* boolean */
    // #sijapp cond.end #    
    public static final int OPTION_CP1251_HACK         = 133;   /* boolean */
    // #sijapp cond.if modules_TRAFFIC is "true" #
    public static final int OPTION_COST_OF_1M     =  70;   /* int     */
    public static final int OPTION_COST_PER_DAY        =  71;   /* int     */
    public static final int OPTION_COST_PACKET_LENGTH  =  72;   /* int     */
    public static final int OPTION_CURRENCY            =   6;   /* String  */
    // #sijapp cond.end #
    public static final int OPTION_ONLINE_STATUS       = 192;   /* long    */
    public static final int OPTION_DETECT_ENCODING     = 153;   /* boolean */
    public static final int OPTION_DELIVERY_NOTIFICATION      = 173;   /* boolean */
    public static final int OPTION_REPLACE_STATUS_ICON = 152;   /* boolean */
    //public static final int OPTION_SHOW_LISTS_ICON     = 154;   /* boolean */
    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
    public static final int OPTION_TF_FLAGS            = 169;   /* boolean */
    // #sijapp cond.end#
    public static final int OPTION_MSGSEND_MODE        =  95;   /* int     */
    public static final int OPTIONS_CLIENT             =  96;   /* int     */
    
    public static final int OPTION_PRIVATE_STATUS      =  93;   /* int     */
    //public static final int OPTION_CHAT_SMALL_FONT     = 135;   /* boolean */
    //public static final int OPTION_SMALL_FONT          = 157;   /* boolean */
    public static final int OPTION_USER_GROUPS         = 136;   /* boolean */
    public static final int OPTION_HISTORY             = 137;   /* boolean */
    public static final int OPTION_SHOW_LAST_MESS      = 142;   /* boolean */
    public static final int OPTION_CLASSIC_CHAT        = 143;   /* boolean */
    public static final int OPTION_COLOR_SCHEME        =  73;   /* int     */
    public static final int OPTION_FONT_SCHEME         = 107;   /* int     */
    public static final int OPTION_STATUS_MESSAGE      =   7;   /* String  */
    public static final int OPTION_KEYBOARD            = 109;   /* int     */
    
    public static final int OPTION_XSTATUS             =  92;   /* int     */
//    public static final int OPTION_XTRAZ_ENABLE        = 156;   /* boolean */
    public static final int OPTION_XTRAZ_TITLE         =  19;   /* String  */
    public static final int OPTION_XTRAZ_DESC          =  20;   /* String  */
    //public static final int OPTION_AUTO_STATUS         = 161;   /* boolean */
    public static final int OPTION_AUTO_XTRAZ          = 162;   /* boolean */
    // #sijapp cond.if protocols_JABBER is "true" #                             //global status
    public static final int OPTION_TITLE_IN_CONFERENCE = 140;   /* boolean */   //global status
    // #sijapp cond.end#                                                        //global status
    
    public static final int OPTION_ANTISPAM_MSG        =  24;   /* String  */
    public static final int OPTION_ANTISPAM_HELLO      =  25;   /* String  */
    public static final int OPTION_ANTISPAM_ANSWER     =  26;   /* String  */
    public static final int OPTION_ANTISPAM_ENABLE     = 158;   /* boolean */
    public static final int OPTION_ANTISPAM_OFFLINE    = 159;   /* boolean */
    public static final int OPTION_ANTISPAM_KEYWORDS   =  35;   /* String  */   //it is changed

    public static final int OPTION_SAVE_TEMP_CONTACT   = 147;   /* boolean */
    
    public static final int OPTION_USE_SMILES          = 141;   /* boolean */   //add
    public static final int OPTION_MD5_LOGIN           = 144;   /* boolean */
    // #sijapp cond.if modules_PROXY is "true" #
    public static final int OPTION_PRX_TYPE            =  76;   /* int     */
    public static final int OPTION_PRX_SERV            =   8;   /* String  */
    public static final int OPTION_PRX_PORT            =   9;   /* String  */
    public static final int OPTION_AUTORETRY_COUNT     =  10;   /* String  */
    public static final int OPTION_PRX_NAME            =  11;   /* String  */
    public static final int OPTION_PRX_PASS            =  12;   /* String  */
    // #sijapp cond.end#
    
    public static final int OPTIONS_GMT_OFFSET         =  87;   /* int     */
    public static final int OPTIONS_LOCAL_OFFSET       =  90;   /* int     */
    
    //public static final int OPTION_FULL_SCREEN         = 145;   /* boolean */
    public static final int OPTION_SILENT_MODE         = 150;   /* boolean */
    public static final int OPTION_BRING_UP            = 151;   /* boolean */
    
    protected static final int OPTIONS_LANG_CHANGED    = 148;
    
    public static final int OPTION_POPUP_WIN2          =  84;   /* int     */
    public static final int OPTION_EXT_CLKEY0          =  77;   /* int     */
    public static final int OPTION_EXT_CLKEYSTAR       =  78;   /* int     */
    public static final int OPTION_EXT_CLKEY4          =  79;   /* int     */
    public static final int OPTION_EXT_CLKEY6          =  80;   /* int     */
    public static final int OPTION_EXT_CLKEYCALL       =  81;   /* int     */
    public static final int OPTION_EXT_CLKEYPOUND      =  82;   /* int     */
    public static final int OPTION_VISIBILITY_ID       =  85;   /* int     */
    
    public static final int OPTION_UNTITLED_INPUT      = 160;   /* boolean */
    
    public static final int OPTION_LIGHT               = 163;   /* boolean */
    public static final int OPTION_LIGHT_NONE          =  97;   /* int     */
    public static final int OPTION_LIGHT_ONLINE        =  98;   /* int     */
    public static final int OPTION_LIGHT_KEY_PRESS     =  99;   /* int     */
    //public static final int OPTION_LIGHT_CONNECT       = 100;   /* int     */
    public static final int OPTION_LIGHT_MESSAGE       = 101;   /* int     */
    public static final int OPTION_LIGHT_SYSTEM        = 103;   /* int     */
    public static final int OPTION_LIGHT_TICK          = 104;   /* int     */

    public static final int OPTION_INPUT_MODE          = 105;   /* int     */
    
    public static final int OPTION_SWAP_SOFT_KEY       = 164;   /* boolean */
    //public static final int OPTION_SHOW_AUTH_ICON      = 165;   /* boolean */
    public static final int OPTION_SHOW_SOFTBAR        = 167;   /* boolean */   //add
    public static final int OPTION_CUSTOM_GC           = 168;   /* boolean */
    public static final int OPTION_POPUP_OVER_SYSTEM   = 170;   /* boolean */
    public static final int OPTION_SORT_UP_WITH_MSG    = 171;   /* boolean */
    public static final int OPTION_SORT_UP_WITH_CHAT   = 179;   /* boolean */   //contacts to a chat up
    public static final int OPTION_SWAP_SEND_AND_BACK  = 172;   /* boolean */
    public static final int OPTION_TICKER_SYSTEM       = 178;   /* boolean */   //ticker
    public static final int OPTION_SHOW_STATUS_LINE    = 177;   /* boolean */

    public static final int OPTION_ALARM  = 176;   /* boolean */

    //Hotkey Actions
    public static final int HOTKEY_NONE      =  0;
    public static final int HOTKEY_INFO      =  2;
    //public static final int HOTKEY_NEWMSG    =  3;
    public static final int HOTKEY_ONOFF     =  4;
    //public static final int HOTKEY_OPTIONS   =  5;
    //public static final int HOTKEY_MENU      =  6;
    public static final int HOTKEY_LOCK      =  7;
    public static final int HOTKEY_HISTORY   =  8;
    public static final int HOTKEY_MINIMIZE  =  9;
    //public static final int HOTKEY_CLI_INFO  = 10;
    //public static final int HOTKEY_FULLSCR   = 11;
    public static final int HOTKEY_SOUNDOFF  = 12;
    public static final int HOTKEY_STATUSES  = 13;
    public static final int HOTKEY_MAGIC_EYE = 14;
    public static final int HOTKEY_LIGHT     = 15;
    public static final int HOTKEY_OPEN_CHATS = 16;
    public static final int HOTKEY_COLLAPSE_ALL = 17;
    
    private static Vector listOfProfiles = new Vector();
    public static Profile getProfile(int num) {
        return (Profile)listOfProfiles.elementAt(num);
    }

    /**************************************************************************/
    public static void setCurrentAccount(int num) {
        num = Math.min(num, getAccountCount());
        Options.setInt(Options.OPTIONS_CURR_ACCOUNT, num);
    }
    public static int getCurrentAccount() {
        return Options.getInt(Options.OPTIONS_CURR_ACCOUNT);
    }
    public static void setVisibleAcounts() {
        ContactList cl = ContactList.getInstance();
        // #sijapp cond.if modules_MULTI is "true" #
        int count = cl.addProtocols(listOfProfiles);
        if (0 == count) {
            Profile p = Options.getAccount(0);
            p.isActive = true;
            cl.addProtocol(p);
        }
        // #sijapp cond.else #
        cl.addProtocol(Options.getAccount(Options.getCurrentAccount()));
        // #sijapp cond.end #
        cl.updateMainMenu();

    }
    public static void delAccount(int num) {
        int current = getCurrentAccount();
        listOfProfiles.removeElementAt(num);
        if (current == num) {
            current = 0;
        }
        if (num < current) {
            current--;
        }
        if (listOfProfiles.size() < current) {
            current = 0;
        }
        setCurrentAccount(current);
        Storage s = new Storage("j-accounts");
        try {
            s.open(false);
            for (; num < listOfProfiles.size(); ++num) {
                s.setRecord(num + 1, writeAccount(getProfile(num)));
            }
            for (; num < s.getNumRecords(); ++num) {
                s.setRecord(num + 1, new byte[0]);
            }
        } catch (Exception e) {
        }
        s.close();
    }
    public static void setAccount(int num, Profile account) {
        int maxSize = getMaxAccountCount();
        int size = getAccountCount();
        if (size <= num) {
            num = Math.min(size, maxSize - 1);
        }
        if (num < size) {
            listOfProfiles.setElementAt(account, num);
        } else {
            listOfProfiles.addElement(account);
        }
        saveAccount(account);
     }
    public static void saveAccount(Profile account) {
        int num = listOfProfiles.indexOf(account);
        if (num < 0) {
            return;
        }
        if (StringConvertor.isEmpty(account.userId)) {
            return;
        }
        Storage s = new Storage("j-accounts");
        try {
            s.open(true);
            byte[] hash = writeAccount(account);
            if (num < s.getNumRecords()) {
                s.setRecord(num + 1, hash);
            } else {
                s.addRecord(hash);
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("save account #" + num, e);
            // #sijapp cond.end#
        }
        s.close();
    }
    private static byte[] writeAccount(Profile account) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(account.protocolType);
            dos.writeUTF(StringConvertor.notNull(account.userId));
            dos.writeUTF(StringConvertor.notNull(account.password));
            dos.writeUTF(StringConvertor.notNull(account.nick));
            dos.writeByte(account.statusIndex);
            dos.writeUTF(StringConvertor.notNull(account.statusMessage));
            dos.writeByte(account.xstatusIndex);
            dos.writeUTF(StringConvertor.notNull(account.xstatusTitle));
            dos.writeUTF(StringConvertor.notNull(account.xstatusDescription));
            dos.writeBoolean(account.isActive);
            
            byte[] hash = Util.decipherPassword(baos.toByteArray());
            baos.close();
            return hash;
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("write account" + account.userId, e);
            // #sijapp cond.end#
            return new byte[0];
        }
    }
    private static Profile readProfile(byte[] data) {
        Profile p = new Profile();
        try {
            byte[] buf = Util.decipherPassword(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            DataInputStream dis = new DataInputStream(bais);
            
            p.protocolType = dis.readByte();
            p.userId = dis.readUTF();
            p.password = dis.readUTF();
            p.nick = dis.readUTF();
            p.statusIndex = dis.readByte();
            p.statusMessage = dis.readUTF();
            p.xstatusIndex = dis.readByte();
            p.xstatusTitle = dis.readUTF();
            p.xstatusDescription = dis.readUTF();
            p.isActive = true;
            if (0 < dis.available()) {
                p.isActive = dis.readBoolean();
            }
            bais.close();
        } catch (IOException ex) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("read account", ex);
            // #sijapp cond.end#
        }
        return p;
    }
    public static void loadAccounts() {
        listOfProfiles.removeAllElements();
        Storage s = new Storage("j-accounts");
        try {
            s.open(false);
            int accountCount = s.getNumRecords();
            for (int i = 0 ; i < accountCount; ++i) {
                byte[] data = s.getRecord(i + 1);
                if ((null == data) || (0 == data.length)) {
                    break;
                }
                Profile p = readProfile(data);
                if (!StringConvertor.isEmpty(p.userId)) {
                    listOfProfiles.addElement(p);
                }
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("load accounts", e);
            // #sijapp cond.end#
            // migrate
            final int OPTION_NICK1                      =  21;   /* String */
            final int OPTION_UIN1                       =   0;   /* String */
            final int OPTION_PASSWORD1                  = 228;   /* String  */
            final int OPTION_NICK2                      =  22;   /* String */
            final int OPTION_UIN2                       =  14;   /* String  */
            final int OPTION_PASSWORD2                  = 229;   /* String  */
            final int OPTION_NICK3                      =  23;   /* String */
            final int OPTION_UIN3                       =  15;   /* String  */
            final int OPTION_PASSWORD3                  = 230;   /* String  */
            addProfile(OPTION_UIN1, OPTION_PASSWORD1, OPTION_NICK1);
            addProfile(OPTION_UIN2, OPTION_PASSWORD2, OPTION_NICK2);
            addProfile(OPTION_UIN3, OPTION_PASSWORD3, OPTION_NICK3);
        }
        s.close();
    }
    private static void addProfile(int uinOpt, int passOpt, int nickOpt) {
        String uin = getString(uinOpt);
        if (!StringConvertor.isEmpty(uin)) {
            Profile p = new Profile();
            p.userId = uin;
            p.password = getString(passOpt);
            p.nick = getString(nickOpt);
            setAccount(getMaxAccountCount(), p);
            setString(uinOpt, "");
        }
    }
    public static int getMaxAccountCount() {
        return 10;
    }
    public static int getAccountCount() {
        return listOfProfiles.size();
    }
    public static final Profile getAccount(int num) {
        if (listOfProfiles.size() <= num) {
            return new Profile();
        }
        return (Profile)listOfProfiles.elementAt(num);
    }
    public static final int getAccountIndex(Profile profile) {
        return Math.max(0, listOfProfiles.indexOf(profile));
    }
    
    /**************************************************************************/
    
    // Hashtable containing all option key-value pairs
    //static private Hashtable options = new Hashtable(128);
    private static Object[] options = new Object[256];
    
    
    public static void loadOptions() {
        // Try to load option values from record store and construct options form
        try {
            setDefaults();
            initAccounts();
            load();
        // Use default values if loading option values from record store failed
        } catch (Exception e) {
            setDefaults();
            setBoolean(OPTIONS_LANG_CHANGED, true);
        }
    }
    private static void initAccounts() {
        setInt    (Options.OPTIONS_CURR_ACCOUNT,      0);
    }
    /* Set default values
       This is done before loading because older saves may not contain all new values */
    private static void setDefaults() {
        setString (Options.OPTION_SRV_HOST,           "login.icq.com");
        setString (Options.OPTION_SRV_PORT,           "5190");
        setBoolean(Options.OPTION_KEEP_CONN_ALIVE,    true);
        setBoolean(Options.OPTION_RECONNECT,          true);
        setInt    (Options.OPTION_RECONNECT_NUMBER,   10);
        setString (Options.OPTION_CONN_ALIVE_INVTERV, "120");
        //setInt    (Options.OPTION_CONN_PROP,          0);
//        setBoolean(Options.OPTION_ASYNC,              true); //it is changed
        setInt    (Options.OPTION_CONN_TYPE,          0);
        // #sijapp cond.if target is "MIDP2"#
        setBoolean(Options.OPTION_SHADOW_CON,         Jimm.isPhone(Jimm.PHONE_NOKIA));
        // #sijapp cond.elseif target isnot "MOTOROLA" #
        setBoolean(Options.OPTION_SHADOW_CON,         false);
        // #sijapp cond.end#
        setBoolean(Options.OPTION_MD5_LOGIN,          true);
        setBoolean(Options.OPTION_AUTO_CONNECT,       false);
        setString (Options.OPTION_UI_LANGUAGE,        ResourceBundle.getSystemLanguage());
        setBoolean(Options.OPTION_DISPLAY_DATE,       true);  //it is changed
        setBoolean(Options.OPTION_EYE_NOTIF,          true);  //eye sound
        // #sijapp cond.if modules_PRESENCE is "true" #       //presence
        setBoolean(Options.OPTION_NOTICE_PRESENCE,    false); //presence
        // #sijapp cond.end#                                  //presence
        setInt    (Options.OPTION_CL_SORT_BY,         1);     //it is changed
        setBoolean(Options.OPTION_CL_HIDE_OFFLINE,    false);
        // #sijapp cond.if protocols_JABBER is "true" #        //hide conferences & transport
        setBoolean(Options.OPTION_CL_HIDE_OFFLINE_ALL,  true); //hide conferences & transport
        // #sijapp cond.end#                                   //hide conferences & transport
        // #sijapp cond.if modules_UPDATES is "true" # //add updates modules
        setBoolean(Options.OPTION_CHECK_UPDATES,         false); //it is changed
        // #sijapp cond.end# //add updates modules
        setBoolean(Options.OPTION_TICKER_SYSTEM,         false); //add
        setBoolean(Options.OPTION_SORT_UP_WITH_MSG,      true);  //add
        setBoolean(Options.OPTION_SORT_UP_WITH_CHAT,     true);  //contacts to a chat up
        setBoolean(Options.OPTION_SAVE_TEMP_CONTACT,     true);  //add
        setBoolean(Options.OPTION_AUTO_XTRAZ,            true);  //add
        // #sijapp cond.if protocols_JABBER is "true" #          //global status
        setBoolean(Options.OPTION_TITLE_IN_CONFERENCE,   false); //global status
        // #sijapp cond.end#                                     //global status
        setBoolean(Options.OPTION_POPUP_OVER_SYSTEM,     true);  //add
        setBoolean(Options.OPTION_TICKER_SYSTEM,         false); //add
        setBoolean(Options.OPTION_DELIVERY_NOTIFICATION, true);  //add

        setInt    (Options.OPTION_MESS_NOTIF_MODE,    2); //it is changed
        setInt    (Options.OPTION_ONLINE_NOTIF_MODE,  2); //it is changed
        setInt    (Options.OPTION_OFFLINE_NOTIF_MODE, 2); //offline sound
        setInt    (Options.OPTION_TYPING_MODES,       2); //typing
        setInt    (Options.OPTION_TYPING_MODE,        2); //it is changed
        setInt    (Options.OPTION_OTHER_NOTIF_MODE,   2); //other sound 
        setInt    (Options.OPTION_MESS_NOTIF_VOL,     90); //it is changed
        setInt    (Options.OPTION_ONLINE_NOTIF_VOL,   40); //it is changed
        setInt    (Options.OPTION_OFFLINE_NOTIF_VOL,  40); //offline sound
        setInt    (Options.OPTION_TYPING_VOL,         40); //it is changed
        setInt    (Options.OPTION_OTHER_NOTIF_VOL,    50); //other sound
        setString (Options.OPTION_VIBRATOR_TIME,          "150"); //vibra time
        setString (Options.OPTION_REPEATER_KEY,           "150"); //key repeat
        setString (Options.OPTION_REPEATER_TIME,           "60"); //key repeat
        setString (Options.OPTION_CAPTION_ICON,             "0"); //message icon
        setString (Options.OPTION_MAX_HISTORY_SIZE,       "100"); //quantity of messages in a chat
        // #sijapp cond.if protocols_JABBER is "true" #           //applications priority
        setString (Options.OPTION_APL_PRIORITY,            "30"); //applications priority
        // #sijapp cond.end#                                      //applications priority

        setBoolean(Options.OPTION_DETECT_ENCODING,    true);
        setBoolean(Options.OPTION_TF_FLAGS,           false);
//        setInt    (Options.OPTION_MAX_MSG_COUNT,      100);

        setInt    (Options.OPTION_MSGSEND_MODE,       0);      
        setBoolean(Options.OPTION_CP1251_HACK, ResourceBundle.isCyrillic(getString(Options.OPTION_UI_LANGUAGE)));
        setInt    (Options.OPTION_VIBRATOR,           1); //it is changed
        // #sijapp cond.if (target is "SIEMENS2") & (modules_SOUND is "true") #    
        setBoolean(Options.OPTION_VOLUME_BUGFIX,      true);
        // #sijapp cond.end #
        // #sijapp cond.if target is "MIDP2"#
        if (Jimm.isS60v5()) {
            setBoolean(Options.OPTION_SWAP_SEND_AND_BACK, true);
        }
        // #sijapp cond.end #

        // #sijapp cond.if modules_ANTISPAM is "true" #
        setString (Options.OPTION_ANTISPAM_KEYWORDS,           "http sms www @conf");
        // #sijapp cond.end #
        // #sijapp cond.if modules_TRAFFIC is "true" #
        setInt    (Options.OPTION_COST_OF_1M,    0);
        setInt    (Options.OPTION_COST_PER_DAY,       0);
        setInt    (Options.OPTION_COST_PACKET_LENGTH, 1024);
        setString (Options.OPTION_CURRENCY,           "$");
        // #sijapp cond.end #
        setLong   (Options.OPTION_ONLINE_STATUS,      Status.I_STATUS_ONLINE);
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        setInt    (Options.OPTION_PRIVATE_STATUS,     PrivateStatusForm.PSTATUS_NOT_INVISIBLE);
        // #sijapp cond.end #
        // #sijapp cond.if protocols_ICQ is "true" #
        // #sijapp cond.if modules_XSTATUSES is "true" #
        setBoolean(Options.OPTION_REPLACE_STATUS_ICON,    false);
        setInt    (Options.OPTION_XSTATUS,            XStatus.XSTATUS_NONE);
        // #sijapp cond.end #
        // #sijapp cond.end #
        // #sijapp cond.if protocols_MRIM is "true" #
        // #sijapp cond.if modules_XSTATUSES is "true" #
        setBoolean(Options.OPTION_REPLACE_STATUS_ICON,    false);
        setInt    (Options.OPTION_XSTATUS,            MrimXStatus.XSTATUS_NONE);
        // #sijapp cond.end #
        // #sijapp cond.end #
        
        setBoolean(Options.OPTION_USER_GROUPS,        true);
        setBoolean(Options.OPTION_HISTORY,            true); //it is changed
        setInt    (Options.OPTION_COLOR_SCHEME,       1);    //it is changed
        setBoolean(Options.OPTION_USE_SMILES,         true); //add
        setInt    (Options.OPTION_FONT_SCHEME,        0);    //it is changed
        setBoolean(Options.OPTION_SHOW_LAST_MESS,     false);
        setBoolean(Options.OPTION_DELIVERY_NOTIFICATION, false);
        // #sijapp cond.if modules_STYLUS is "true"#
        setBoolean(Options.OPTION_SHOW_STATUS_LINE, false); //it is changed
        // #sijapp cond.else#
        setBoolean(Options.OPTION_SHOW_STATUS_LINE, false);
        // #sijapp cond.end#
        // #sijapp cond.if modules_PROXY is "true" #
        setInt    (Options.OPTION_PRX_TYPE,           0);
        setString (Options.OPTION_PRX_SERV,           "");
        setString (Options.OPTION_PRX_PORT,           "1080");
        setString (Options.OPTION_AUTORETRY_COUNT,    "1");
        setString (Options.OPTION_PRX_NAME,           "");
        setString (Options.OPTION_PRX_PASS,           "");
        // #sijapp cond.end #
        setInt    (Options.OPTION_VISIBILITY_ID,      0);
        // #sijapp cond.if modules_CLIENTS is "true" #
        // #sijapp cond.if protocols_ICQ is "true" #
        setInt    (Options.OPTIONS_CLIENT,            ClientDetector.instance.getDefaultClientForMask());
        // #sijapp cond.end #
        // #sijapp cond.end #
        
        setBoolean(Options.OPTION_SILENT_MODE,        false);

        setInt    (Options.OPTION_EXT_CLKEYSTAR,      HOTKEY_MAGIC_EYE); //it is changed
        setInt    (Options.OPTION_EXT_CLKEY0,         HOTKEY_INFO); //it is changed
        setInt    (Options.OPTION_EXT_CLKEY4,         HOTKEY_ONOFF); //it is changed
        setInt    (Options.OPTION_EXT_CLKEY6,         HOTKEY_STATUSES); //it is changed
        setInt    (Options.OPTION_EXT_CLKEYCALL,      HOTKEY_HISTORY); //it is changed
        setInt    (Options.OPTION_EXT_CLKEYPOUND,     HOTKEY_OPEN_CHATS); //it is changed
        
        setInt    (Options.OPTION_POPUP_WIN2,         1); //it is changed
        setBoolean(Options.OPTION_CLASSIC_CHAT,       false);
        
        // #sijapp cond.if target is "MOTOROLA"#
        setBoolean(Options.OPTION_CUSTOM_GC,          true);
        // #sijapp cond.else #
        setBoolean(Options.OPTION_CUSTOM_GC,          false);
        // #sijapp cond.end #
        
        // #sijapp cond.if target="MIDP2"#
        setBoolean(Options.OPTION_BRING_UP,           false); //it is changed
        // #sijapp cond.end#
        
        int time = TimeZone.getDefault().getRawOffset() / (1000 * 60 * 60);
        /* Offset (in hours) between GMT time and local zone time 
           GMT_time + GMT_offset = Local_time */
        setInt    (Options.OPTIONS_GMT_OFFSET,        time);
        
        /* Offset (in hours) between GMT time and phone clock 
           Phone_clock + Local_offset = GMT_time */
        setInt    (Options.OPTIONS_LOCAL_OFFSET,      0);
        
        setBoolean(OPTIONS_LANG_CHANGED,              false);
        setBoolean(OPTION_SHOW_SOFTBAR,               true); //add
        
        // #sijapp cond.if modules_LIGHT is "true" #
        setInt(Options.OPTION_LIGHT_NONE,      0);
        setInt(Options.OPTION_LIGHT_ONLINE,    101);
        setInt(Options.OPTION_LIGHT_KEY_PRESS, 100);
        setInt(Options.OPTION_LIGHT_MESSAGE,   100);
        setInt(Options.OPTION_LIGHT_SYSTEM,    100);
        setInt(Options.OPTION_LIGHT_TICK,      15);
        // #sijapp cond.if target="MOTOROLA"#
        Options.setBoolean(Options.OPTION_LIGHT, true);
        // #sijapp cond.else#
        Options.setBoolean(Options.OPTION_LIGHT, false);
        // #sijapp cond.end#
        // #sijapp cond.end #
        setBoolean(OPTION_ALARM, true);
            
    }
    
//    /* Experimental */
//    private void loadDefault() {
//        Config config = new Config().load("/config.txt");
//        String[] keys = config.getKeys();
//        String[] values = config.getValues();
//        for (int i = 0; i < keys.length; i++) {
//            int key = Util.strToIntDef(keys[i], -1);
//            if (key < 0) {
//            } else if (key < 64) {  /* 0-63 = String */
//                setString(key, values[i]);
//            } else if (key < 128) {  /* 64-127 = int */
//                setInt(key, Util.strToIntDef(values[i], 0));
//            } else if (key < 192) {  /* 128-191 = boolean */
//                setBoolean(key, 0 != Util.strToIntDef(values[i], 0));
//            } else if (key < 224) {  /* 192-223 = long */
//                setLong(key, Util.strToIntDef(values[i], 0));
//            }
//        }
//    }
    
    public static void resetLangDependedOpts() {
    }

    /* Load option values from record store */
    private static void load() throws IOException {
        /* Read all option key-value pairs */
        byte[] buf = Storage.loadSlot(Storage.SLOT_OPTIONS);
        if (buf == null) {
            return;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        DataInputStream dis = new DataInputStream(bais);
        while (dis.available() > 0) {
            int optionKey = dis.readUnsignedByte();
            if (optionKey < 64) {  /* 0-63 = String */
                setString(optionKey, dis.readUTF());
            } else if (optionKey < 128) {  /* 64-127 = int */
                setInt(optionKey, dis.readInt());
            } else if (optionKey < 192) {  /* 128-191 = boolean */
                setBoolean(optionKey, dis.readBoolean());
            } else if (optionKey < 224) {  /* 192-223 = long */
                setLong(optionKey, dis.readLong());
            } else {  /* 226-255 = Scrambled String */
                byte[] optionValue = new byte[dis.readUnsignedShort()];
                dis.readFully(optionValue);
                optionValue = Util.decipherPassword(optionValue);
                setString(optionKey, StringConvertor.utf8beByteArrayToString(optionValue, 0, optionValue.length));
            }
        }
    }


    /* Save option values to record store */
    private static void save() throws IOException {
        /* Temporary variables */
        
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.profilerStart();
        // #sijapp cond.end #

        /* Save all option key-value pairs */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (int key = 0; key < options.length; ++key) {
            if (null == options[key]) {
                continue;
            }
            dos.writeByte(key);
            if (key < 64) {  /* 0-63 = String */
                dos.writeUTF((String)options[key]);
            } else if (key < 128) {  /* 64-127 = int */
                dos.writeInt(((Integer)options[key]).intValue());
            } else if (key < 192) {  /* 128-191 = boolean */
                dos.writeBoolean(((Boolean)options[key]).booleanValue());
            } else if (key < 224) {  /* 192-223 = long */
                dos.writeLong(((Long)options[key]).longValue());
            } else if (key < 256) {  /* 226-255 = Scrambled String */
                String str = (String)options[key];
                byte[] optionValue = StringConvertor.stringToByteArrayUtf8(str);
                optionValue = Util.decipherPassword(optionValue);
                dos.writeShort(optionValue.length);
                dos.write(optionValue);
            }
        }
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.profilerStep("make options");
        // #sijapp cond.end #

        /* Close record store */
        Storage.saveSlot(Storage.SLOT_OPTIONS, baos.toByteArray());
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.profilerStep("safeSlot(OPTIONS)");
        // #sijapp cond.end #
    }

    public static synchronized void safeSave() {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        long profiler = DebugLog.profilerStart();
        // #sijapp cond.end #
        try {
            save();
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.println("options: " + e.toString());
            // #sijapp cond.end #
            JimmException.handleException(new JimmException(172, 0, true));
        }
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.profilerStep("safeSave", profiler);
        // #sijapp cond.end #
    }

    /* Option retrieval methods (no type checking!) */
    public static String getString(int key) {
        String value = (String)options[key];
        return (null == value) ? "" : value;
    }
    
    public static int getInt(int key) {
        Integer value = (Integer) options[key];
        return (null == value) ? 0 : value.intValue();
    }
    
    public static boolean getBoolean(int key) {
        Boolean value = (Boolean) options[key];
        return (null == value) ? false : value.booleanValue();
    }
    
    public static long getLong(int key) {
        Long value = (Long) options[key];
        return (null == value) ? 0 : value.longValue();
    }


    /* Option setting methods (no type checking!) */
    public static void setString(int key, String value) {
        options[key] = value;
    }
    public static void setInt(int key, int value) {
        options[key] = new Integer(value);
    }
    
    public static void setBoolean(int key, boolean value) {
        options[key] = new Boolean(value);
    }
    
    public static void setLong(int key, long value) {
        options[key] = new Long(value);
    }

    /**************************************************************************/
    
    
        
    public static void nextServer() {
        String servers = Options.getString(Options.OPTION_SRV_HOST);
        char delim = (servers.indexOf(' ') < 0) ? '\n' : ' ';
        servers = servers.replace('\n', ' ');
        String[] serverList = Util.explode(servers, ' ');
        if (serverList.length < 2) {
            return;
        }
        StringBuffer newSrvs = new StringBuffer();
        for (int i = 1; i < serverList.length; i++) {
            newSrvs.append(serverList[i]);
            newSrvs.append(delim);
        }
        newSrvs.append(serverList[0]);
        Options.setString(Options.OPTION_SRV_HOST, newSrvs.toString());
    }
}
