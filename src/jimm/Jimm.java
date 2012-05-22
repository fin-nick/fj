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
 File: src/jimm/Jimm.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Vladimir Kryukov
 *******************************************************************************/


package jimm;


import javax.microedition.io.ConnectionNotFoundException;
import jimm.chat.*;
import jimm.cl.*;
import jimm.comm.*;
import jimm.history.*;
import jimm.modules.*;
import jimm.modules.traffic.*;
import jimm.search.Search;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.timers.*;
import jimm.util.ResourceBundle;

import java.util.Timer;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import protocol.Protocol;
// #sijapp cond.if modules_PASSWORD is "true" # //password
import jimm.modules.*;                          //password
// #sijapp cond.end#                            //password


public class Jimm extends MIDlet implements Runnable {
    public static final String VERSION = "###VERSION###";
    public static String lastVersion;
    public static String lastDate;

    // Application main object
    private static Jimm jimm = null;
    public static Jimm getJimm() {
        return jimm;
    }

    // Display object
    private Display display = null;


    /****************************************************************************/

    public static final byte PHONE_SE             = 0;
    public static final byte PHONE_SE_SYMBIAN     = 1;
    public static final byte PHONE_NOKIA          = 2;
    public static final byte PHONE_NOKIA_S40      = 3;
    public static final byte PHONE_NOKIA_S60      = 4;
    public static final byte PHONE_NOKIA_N80      = 5;
    public static final byte PHONE_INTENT_JTE     = 6;
    public static final byte PHONE_JBED           = 7;
    public static final byte PHONE_SIEMENS_SGOLG2 = 8;
    public static final byte PHONE_SAMSUNG        = 9;
    public static final byte PHONE_ANDROID        = 10;
    
    private static final String getPhone() {
        final String platform = getSystemProperty("microedition.platform", null);
        // #sijapp cond.if target is "MIDP2" #
        if (null == platform) {
            try {
                Class.forName("com.nokia.mid.ui.DeviceControl");
                return "Nokia";
            } catch (Exception e) {
            }
        }
        // #sijapp cond.elseif target is "MOTOROLA" #
        if (null == platform) {
            return "Motorola";
        }
        // #sijapp cond.end #
        return platform;
    }
    
    public static final String microeditionPlatform = getPhone();
    public static final String microeditionProfiles = getSystemProperty("microedition.profiles", null);
    // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" #
    public static final byte generalPhoneType = getGeneralPhone();

    private static byte getGeneralPhone() {
        String device = getPhone();
        if (null == device) {
            return -1;
        }
        device = device.toLowerCase();
        // #sijapp cond.if target is "SIEMENS2"#
        if (!((device.indexOf("65") != -1)
                || (device.indexOf("66") != -1)
                || (device.indexOf("70") != -1)
                || (device.indexOf("72") != -1)
                || (device.indexOf("75") != -1 && device.indexOf("s") < 0))) {
            return PHONE_SIEMENS_SGOLG2;
        }
        // #sijapp cond.end#
        // #sijapp cond.if target is "MIDP2" #
        // #sijapp cond.if modules_ANDROID is "true" #
        if (-1 != device.indexOf("microemulator-android")) {
            return PHONE_ANDROID;
        }
        // #sijapp cond.end#
        if (device.indexOf("ericsson") != -1) {
            if ((-1 != getSystemProperty("com.sonyericsson.java.platform", "")
                        .toLowerCase().indexOf("sjp"))) {
                return PHONE_SE_SYMBIAN;
            }
            return PHONE_SE;
        }
        if (-1 != device.indexOf("platform=s60")) {
            return PHONE_NOKIA_S60;
        }
        if (device.indexOf("nokia") != -1) {
            if (device.indexOf("nokian80") != -1) {
                return PHONE_NOKIA_N80;
            }
            String dir = getSystemProperty("fileconn.dir.private", "");
            // s40 (6233) does not have this property
            if (-1 != dir.indexOf("/private/")) {
                // it is s60 v3 fp1
                return PHONE_NOKIA_S60;
            }
            // Nokia s40 has only one dot into version
            int firstDotIndex = device.indexOf('.');
            if (-1 != firstDotIndex) {
                if (-1 != device.indexOf('.', firstDotIndex + 1)) {
                    return PHONE_NOKIA_S60;
                }
            }
            return PHONE_NOKIA_S40;
        }
        if (device.indexOf("samsung") != -1) {
            return PHONE_SAMSUNG;
        }
        if (device.indexOf("jbed") != -1) {
            return PHONE_JBED;
        }
        if (device.indexOf("intent") != -1) {
            return PHONE_INTENT_JTE;
        }
        // #sijapp cond.end #
        return -1;
    }
    public static boolean isPhone(final byte phone) {
        // #sijapp cond.if target is "MIDP2" #
        if (PHONE_NOKIA == phone) {
            return (PHONE_NOKIA_S40 == generalPhoneType)
                    || (PHONE_NOKIA_S60 == generalPhoneType)
                    || (PHONE_NOKIA_N80 == generalPhoneType);
        }
        if (PHONE_SE == phone) {
            return (PHONE_SE_SYMBIAN == generalPhoneType)
                    || (PHONE_SE == generalPhoneType);
        }
        // #sijapp cond.end #
        return phone == generalPhoneType;
    }
    // #sijapp cond.end #
    public static boolean hasMemory(int requared) {
        // #sijapp cond.if target is "MIDP2" #
        if (isPhone(PHONE_SE)) {
            return true;
        }
        if (isPhone(PHONE_NOKIA_S60)) {
            return true;
        }
        if (isPhone(PHONE_JBED)) {
            return true;
        }
        if (isPhone(PHONE_INTENT_JTE)) {
            return true;
        }
        // #sijapp cond.if modules_ANDROID is "true" #
        if (isPhone(PHONE_ANDROID)) {
            return true;
        }
        // #sijapp cond.end #
        // #sijapp cond.end #
        System.gc();
        long free = Runtime.getRuntime().freeMemory();
        return (requared < free);
    }

    public static String getAppProperty(String key, String defval) {
        String res = null;
        try {
            res = jimm.getAppProperty(key);
        } catch (Exception e) {
        }
        return StringConvertor.isEmpty(res) ? defval : res;
    }
    public static boolean isSetAppProperty(String key) {
        String res = getAppProperty(key, "");
        return "yes".equals(res) || "true".equals(res);
    }
    private static String getSystemProperty(String key, String defval) {
        String res = null;
        try {
            res = System.getProperty(key);
        } catch (Exception e) {
        }
        return StringConvertor.isEmpty(res) ? defval : res;
    }

    // #sijapp cond.if target is "MIDP2"#
    public static boolean isS60v5() {
        String platform = StringConvertor.notNull(Jimm.microeditionPlatform);
        return -1 != platform.indexOf("sw_platform_version=5.0");
    }
    // #sijapp cond.end#
    // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA" #
    public static void platformRequestUrl(String url) throws ConnectionNotFoundException {
        // #sijapp cond.if protocols_JABBER is "true" #
        if (-1 == url.indexOf(':')) {
            url = "xmpp:" + url;
        }
        if (url.startsWith("xmpp:")) {
            Search search = new Search(ContactList.getInstance().getCurrentProtocol());
            search.setSearchParam(Search.UIN, Util.getUrlWithoutProtocol(url));
            search.show(Search.TYPE_NOFORM);
            return;
        }
        // #sijapp cond.end #
        if (url.equals("jimm:update")) {
            StringBuffer url_ = new StringBuffer();
            url_.append("http://jabga.ru/fin_jabber.jar");
            url_.append(ResourceBundle.getCurrUiLanguage());
            url_.append("&protocols=###PROTOCOLS###&cdata=");
            url_.append(Config.loadResource("build.dat"));
            url = url_.toString();
        }
        Jimm.getJimm().platformRequest(url.trim());
    }
    public static void platformRequestAndExit(String url) {
        try {
            platformRequestUrl(url);
            Jimm.getJimm().quit();
        } catch (Exception e) {
            /* Do nothing */
        }
    }
    // #sijapp cond.end #
    


    public void run() {
        try {
            // #sijapp cond.if modules_TRAFFIC is "true" #
            // Create traffic Object (and update progress indicator)
            Traffic.getInstance().load();
            // #sijapp cond.end#
            // #sijapp cond.if modules_SOUND is "true"#
            // #sijapp cond.if target is "SIEMENS1" | target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
            Notify.getSound().initSounds();
            // #sijapp cond.end#
            // #sijapp cond.end#
            // #sijapp cond.if modules_LIGHT is "true" #
            CustomLight.switchOn(Options.getBoolean(Options.OPTION_LIGHT));
            // #sijapp cond.end#

            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // #sijapp cond.if modules_DEBUGLOG is "true"#
        DebugLog.startTests();
        // #sijapp cond.end#
    }
    private void initialize() {
        ResourceBundle.loadLanguageList();

        Options.loadOptions();
        ResourceBundle.setCurrUiLanguage(Options.getString(Options.OPTION_UI_LANGUAGE));
        Options.resetLangDependedOpts();

        NativeCanvas.setFullScreen(true);
        Scheme.load();
        Scheme.setColorScheme(Options.getInt(Options.OPTION_COLOR_SCHEME));
        GraphicsEx.setFontScheme(Options.getInt(Options.OPTION_FONT_SCHEME));
        UIUpdater.startUIUpdater();

        SplashCanvas.setMessage(ResourceBundle.getString("loading"));
        SplashCanvas.showSplash();
        SplashCanvas.setProgress(5);
        SplashCanvas.setMessage(ResourceBundle.getString("l_05")); //add

        // back loading (traffic, sounds and light)
        new Thread(this).start();
        // Get display object (and update progress indicator)

        // init message editor
        SplashCanvas.setProgress(10);
        SplashCanvas.setMessage(ResourceBundle.getString("l_10")); //add
        // #sijapp cond.if modules_SMILES is "true" #
        Emotions.instance.load();
        // #sijapp cond.end#
        SplashCanvas.setProgress(25);
        SplashCanvas.setMessage(ResourceBundle.getString("l_25")); //add
        StringConvertor.load();
        SplashCanvas.setProgress(35);
        SplashCanvas.setMessage(ResourceBundle.getString("l_35")); //add
        Templates.getInstance().load();
        ContactList.getInstance().initMessageEditor();
        
        // init contact list
        SplashCanvas.setProgress(45);
        SplashCanvas.setMessage(ResourceBundle.getString("l_45")); //add
        Options.loadAccounts();
        SplashCanvas.setProgress(70);
        SplashCanvas.setMessage(ResourceBundle.getString("l_70")); //add
        ContactList.getInstance();
        Options.setVisibleAcounts();
    }

    private boolean paused = true;
    private void restore() {
//        UIUpdater.startUIUpdater();
//        // #sijapp cond.if modules_LIGHT is "true" #
//        CustomLight.switchOn(false);
//        CustomLight.switchOn(Options.getBoolean(Options.OPTION_LIGHT));
//        CustomLight.setLightMode(CustomLight.ACTION_SYSTEM_OFF);
//        // #sijapp cond.end#
        // #sijapp cond.if modules_ABSENCE is "true" #
        AutoAbsence.instance.online();
        // #sijapp cond.end #
        paused = false;
        if (null == currentScreen) {
            //setDisplayable(null);
            //ContactList.activate();
            return;
        }
        Displayable d = jimm.display.getCurrent();
        if (null == d) {
            if (currentScreen instanceof CanvasEx) {
                d = NativeCanvas.getInstance();
            }
        }
        if (null != d) {
            // #sijapp cond.if target is "MIDP2"#
            if (isS60v5()) {
                setDisplayable(null);
            }
            // #sijapp cond.end#
            setDisplayable(d);
        }
    }
    
    public Display getDisplay() {
        return display;
    }

    // Start Jimm
    public void startApp() throws MIDletStateChangeException {
        if (!paused && (null != Jimm.jimm)) {
            return;
        }
        paused = false;
        if (null == display) {
            display = Display.getDisplay(this);
        }
        // Return if MIDlet has already been initialized
        if (null != Jimm.jimm) {
            restore();
            return;
        }
        locked = false;
        // Save MIDlet reference
        Jimm.jimm = this;

        initialize();

        // #sijapp cond.if modules_UPDATES is "true" # //add updates modules
        // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
        if (GetVersion.showUpdates()) {
            ContactList.activate();
            PopupWindow.createNewVersion().show();
            return;
        }
        // #sijapp cond.end# //add updates modules
        // #sijapp cond.end#

        // #sijapp cond.if modules_PASSWORD is "true" # //password
        if (PasswordEnter.init()) {                     //password
            PasswordEnter.show(0);                      //password
        } else {                                        //password
            continueStartApp();                         //password
           }                                            //password
        }                                               //password
        public static void continueStartApp() {         //password
        // #sijapp cond.end#                            //password
        
        ContactList cl = ContactList.getInstance();
        // Activate contact list
        if (cl.getCurrentProtocol().isEmpty()) {
            ContactList.activate();
            cl.activateMainMenu();
            new OptionsForm().showAccountEditor(cl.getCurrentProtocol());

        } else if (Options.getBoolean(Options.OPTION_AUTO_CONNECT)) {
            // #sijapp cond.if modules_MULTI is "true"#
            ContactList.activate();
            // #sijapp cond.end#
            cl.getCurrentProtocol().connect();

        } else {
            ContactList.activate();
        }
    }

    // Pause
    public void pauseApp() {
        hideApp();
    }
    public void hideApp() {
        paused = true;
        if (currentScreen instanceof FormEx) {
            return;
        }
        if (currentScreen instanceof InputTextBox) {
            return;
        }
        //currentScreen = null;
        locked = false;
        // #sijapp cond.if modules_ABSENCE is "true" #
        AutoAbsence.instance.away();
        // #sijapp cond.end #
    }

    public void quit() {
        ContactList cl = ContactList.getInstance();
        boolean wait = false;
        try {
        if (cl.isConnected()) {
            cl.disconnect();
            wait = true;
        }
        } catch (Exception e) {
            return;
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            /* Do nothing */
        }
        cl.safeSave();
        if (wait) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
                /* Do nothing */
            }
        }
        try {
            Jimm.getJimm().destroyApp(true);
        } catch (Exception e) {
            /* Do nothing */
        }
    }
    /**
     * Destroy Jimm
     */
    public void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        // #sijapp cond.if modules_TRAFFIC is "true" #
        // Save traffic
        Traffic.getInstance().safeSave();
        // #sijapp cond.end#
        jimm.display.setCurrent(null);
        notifyDestroyed();
    }

    public static void setDisplayable(Displayable d) {
        jimm.display.setCurrent(d);
    }
    private Object currentScreen = null;
    public static void setDisplay(Object d) {
        if (jimm.paused) {
            return;
        }
        NativeCanvas.stopKeyRepeating();

        if (jimm.currentScreen instanceof DisplayableEx) {
            ((DisplayableEx)jimm.currentScreen).closed();

        // #sijapp cond.if modules_LIGHT is "true" #
        } else if ((jimm.currentScreen instanceof Displayable)) {
            CustomLight.setLightMode(CustomLight.ACTION_SYSTEM_OFF);
        // #sijapp cond.end#
        }

        synchronized (jimm) {
            if (d instanceof DisplayableEx) {                
                ((DisplayableEx)d).setDisplayableToDisplay();
                jimm.currentScreen = d;

            } else if (d instanceof Displayable) {
                // #sijapp cond.if modules_LIGHT is "true" #
                CustomLight.setLightMode(CustomLight.ACTION_SYSTEM);
                // #sijapp cond.end#
                setDisplayable((Displayable)d);
                jimm.currentScreen = d;

            // #sijapp cond.if modules_DEBUGLOG is "true" #
            // #sijapp cond.if target is "MIDP2"#
            } else if ((null == d) && Jimm.isPhone(Jimm.PHONE_SE)) {
                DebugLog.panic("displayable object is null");
            // #sijapp cond.end#

            } else if (null != d) {
                DebugLog.panic("not displayable object is " + d.getClass().getName());
            // #sijapp cond.end#
            }
        }
    }
    public static Object getCurrentDisplay() {
        return jimm.currentScreen;
    }

    public static void setPrevDisplay() {
        Object screen = jimm.currentScreen;
        if (screen instanceof DisplayableEx) {
            ((DisplayableEx)screen).back();
        }
    }
    public static boolean isPaused() {
        if (jimm.paused) {
            return true;
        }
        Displayable d = jimm.display.getCurrent();
        return (null == d) || !d.isShown();
    }

    private boolean locked = false;
    public static boolean isLocked() {
        return jimm.locked;
    }
    
    public static void lockJimm() {
        jimm.locked = true;
        SplashCanvas.instance.lockJimm();
        // #sijapp cond.if modules_ABSENCE is "true" #
        AutoAbsence.instance.away();
        // #sijapp cond.end #
    }
    public static void unlockJimm() {
        jimm.locked = false;
        ContactList.activate();
        NativeCanvas.getInstance().refreshClock();
        // #sijapp cond.if modules_ABSENCE is "true" #
        AutoAbsence.instance.online();
        // #sijapp cond.end #
    }
    
    // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" #
    // Set the minimize state of midlet
    public static void setMinimized(boolean mini) {
        // #sijapp cond.if target is "MIDP2" #
        if (mini) {
            jimm.hideApp();
            setDisplayable(null);

        } else {
            try{
                jimm.paused = isPaused();
                jimm.startApp();
            } catch(Exception exc1) {
            }
            jimm.paused = false;
        }
        // #sijapp cond.end #
        // #sijapp cond.if target is "SIEMENS2"#
        try{
            platformRequestUrl(isPhone(PHONE_SIEMENS_SGOLG2)
                    ? "native://NAT_MAIN_MENU" : "native://ELSE_STR_MYMENU");
        } catch(Exception exc1) {
        }
        // #sijapp cond.end#
    }
    // #sijapp cond.end #
}
