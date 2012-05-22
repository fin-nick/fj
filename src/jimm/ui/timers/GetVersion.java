/*
 * GetVersion.java
 *
 * Created on 20 Июнь 2007 г., 23:31
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.timers;

import DrawControls.icons.*;
import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.modules.*;
import jimm.modules.traffic.*;
import jimm.ui.*;
import jimm.util.*;

/**
 * Try to get current Jimm version from Jimm server
 *
 * @author vladimir
 */
public class GetVersion implements Runnable {
    
    public static final int TYPE_DATE    = 0;
    public static final int TYPE_SHADOW  = 1;
    public static final int TYPE_AVATAR = 2;
    public static final int TYPE_URL = 3;
    
    private static final int CHECK_UPDATES_INTERVAL = 6 /* days */;
    private static final int SHOW_NEW_VERSION_INTERVAL = 3 /* days */;
    private static final String versionUrl = "http://version.jimm.net.ru/###PROTOCOLS###?###DATE###";
    private int type;
    private String url;
    // #sijapp cond.if (protocols_MRIM is "true") or (protocols_VK is "true") or (protocols_ICQ is "true") #
    private jimm.search.UserInfo userInfo;
    // #sijapp cond.end#
    
    
    private String getContent(String url) {
        HttpConnection httemp = null;
        InputStream istemp = null;
        String content = "";
        
        try {
            httemp = (HttpConnection) Connector.open(url);
            httemp.setRequestProperty("Connection", "cl" + "ose");
            if (HttpConnection.HTTP_OK != httemp.getResponseCode()) {
                throw new IOException();
            }
            
            istemp = httemp.openInputStream();
            int length = (int) httemp.getLength();
            if (-1 != length) {
                byte[] bytes = new byte[length];
                istemp.read(bytes);
                content = new String(bytes);
                
            } else {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                while (true) {
                    int ch = istemp.read();
                    if (-1 == ch) break;
                    bytes.write(ch);
                }
                content = new String(bytes.toByteArray());
                bytes.close();
            }
            
        } catch (Exception e) {
            content = "Error: " + e.getMessage();
        }
        try {
            httemp.close();
            istemp.close();
        } catch (Exception e) {
        }
        return StringConvertor.removeCr(content);
    }
    
    private void getVersion() {
        String content = getContent(versionUrl);
        String version = content;
        if (-1 != content.indexOf('\n')) {
            version = content.substring(0, content.indexOf('\n'));
        }
        if (TYPE_DATE == type) {
            Jimm.lastDate = version;
            // #sijapp cond.if modules_UPDATES is "true" # //add updates modules
            // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
            setLastVersion(version);
            // #sijapp cond.end# //add updates modules
            // #sijapp cond.end#
        }
        ContactList.getInstance().updateAbout();
    }
    // #sijapp cond.if modules_UPDATES is "true" # //add updates modules
    // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
    public static void updateProgram() {
        Jimm.platformRequestAndExit("jimm:update");
    }
    
    private static int[] getVersionDate(String str) {
        String[] svers = Util.explode(str, '.');
        int[] ivers = new int[3];
        for (int num = 0; num < ivers.length; num++) {
            ivers[num] = Util.strToIntDef(num < svers.length ? svers[num] : "", 0);
        }
        return ivers;
    }
    private static void setLastVersion(String version) {
        Options.setString(Options.OPTION_LAST_VERSION, version);
        if (hasNewVersion()) {
            Options.setInt(Options.OPTION_UPDATE_CHECK_TIME, 0);
        }
        Options.safeSave();
    }
    
    public static void checkUpdates() {
        if (Options.getBoolean(Options.OPTION_CHECK_UPDATES) && !hasNewVersion()) {
            final int today = (int)(System.currentTimeMillis() / (24L * 60 * 60 * 1000));
            final int nextCheck = Options.getInt(Options.OPTION_UPDATE_CHECK_TIME);
            if (nextCheck <= today) {
                new GetVersion(TYPE_DATE).get();
                final int nextDay = today + CHECK_UPDATES_INTERVAL;
                Options.setInt(Options.OPTION_UPDATE_CHECK_TIME, nextDay);
                Options.safeSave();
            }
        }
    }
    public static boolean showUpdates() {
        if (Options.getBoolean(Options.OPTION_CHECK_UPDATES) && hasNewVersion()) {
            final int today = (int)(System.currentTimeMillis() / (24L * 60 * 60 * 1000));
            final int nextCheck = Options.getInt(Options.OPTION_UPDATE_CHECK_TIME);
            if (nextCheck <= today) {
                final int nextDay = today + SHOW_NEW_VERSION_INTERVAL;
                Options.setInt(Options.OPTION_UPDATE_CHECK_TIME, nextDay);
                Options.safeSave();
                return true;
            }
        }
        return false;
    }
    private static boolean hasNewVersion() {
        final String lastSVersion = Options.getString(Options.OPTION_LAST_VERSION);
        if (0 == lastSVersion.length()) {
            return false;
        }
        final int[] curVersion = getVersionDate("###DATE###");
        final int[] lastVersion = getVersionDate(lastSVersion);
        if (curVersion[2] < lastVersion[2]) return true;
        if (curVersion[2] > lastVersion[2]) return false;
        if (curVersion[1] < lastVersion[1]) return true;
        if (curVersion[1] > lastVersion[1]) return false;
        if (curVersion[0] < lastVersion[0]) return true;
        return false;
    }
    // #sijapp cond.end# //add updates modules
    // #sijapp cond.end#
    
    // #sijapp cond.if target isnot "MOTOROLA"#
    private volatile boolean shadowConnectionActive = false;
    private volatile ContentConnection shadowConnection = null;
    private void shadowConnection() {
        // Make the shadow connection for Nokia 6230 or other devices
        // if needed
        if (shadowConnectionActive) {
            return;
        }
        ContentConnection ctemp = null;
        DataInputStream istemp = null;
        
        try {
            shadowConnectionActive = true;
            ctemp = (ContentConnection)Connector.open("http://http.proxy.icq.com/hello");
            istemp = ctemp.openDataInputStream();
        } catch (Exception e) {
        }
        try {
            if (null != shadowConnection) {
                shadowConnection.close();
            }
        } catch (Exception e) {
        }
        shadowConnection = ctemp;
        shadowConnectionActive = false;
    }
    // #sijapp cond.end#
    
    // #sijapp cond.if (protocols_MRIM is "true") or (protocols_VK is "true") or (protocols_ICQ is "true") #
    private byte[] read(InputStream in, int length) throws IOException {
        if (0 == length) {
            return null;
            
        }
        if (0 < length) {
            byte[] bytes = new byte[length];
            int readCount = 0;
            while (readCount < bytes.length) {
                int c = in.read(bytes, readCount, bytes.length - readCount);
                if (-1 == c) break;
                readCount += c;
            }
            return bytes;
        }
        
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < 100*1024; ++i) {
            int ch = in.read();
            if (-1 == ch) break;
            bytes.write(ch);
        }
        byte[] content = bytes.toByteArray();
        bytes.close();
        return content;
    }
    private Image getAvatar() {
        HttpConnection httemp = null;
        InputStream istemp = null;
        Image avatar = null;
        try {
            httemp = (HttpConnection) Connector.open(url);
            if (HttpConnection.HTTP_OK != httemp.getResponseCode()) {
                throw new IOException();
            }
            istemp = httemp.openInputStream();
            byte[] avatarBytes = read(istemp, (int)httemp.getLength());
            // #sijapp cond.if modules_TRAFFIC is "true" #
            Traffic.getInstance().addInTraffic(avatarBytes.length);
            // #sijapp cond.end#
            avatar = javax.microedition.lcdui.Image.createImage(avatarBytes, 0, avatarBytes.length);
            avatarBytes = null;
        } catch (Exception e) {
        }
        try {
            httemp.close();
            istemp.close();
        } catch (Exception e) {
        }
        return avatar;
    }
    public GetVersion(int type, jimm.search.UserInfo ui, String url) {
        userInfo = ui;
        this.type = type;
        this.url = url;
    }
    // #sijapp cond.end#
    
    // Timer routine
    public void run() {
        // #sijapp cond.if target isnot "MOTOROLA"#
        if (TYPE_SHADOW == type) {
            shadowConnection();
            return;
        }
        // #sijapp cond.end#
        // #sijapp cond.if (protocols_MRIM is "true") or (protocols_ICQ is "true") or (protocols_VK is "true") #
        if (TYPE_AVATAR == type) {
            userInfo.setAvatar(getAvatar());
            if (null != userInfo.avatar) {
                userInfo.updateProfileView();
            }
            return;
        }
        // #sijapp cond.end#
        if (TYPE_URL == type) {
            getContent(url);
            return;
        }
        getVersion();
    }
    // #sijapp cond.if protocols_VK is "true" #
    public final Icon getPhoto() {
        Image avatar = getAvatar();
        return new Icon(avatar, 0, 0, avatar.getWidth(), avatar.getHeight());
    }
    // #sijapp cond.end#
    
    public GetVersion(int type) {
        this.type = type;
    }
    public GetVersion(String url) {
        this.type = TYPE_URL;
        this.url = url;
    }
    public void get() {
        try {
            new Thread(this).start();
            Thread.sleep(10);
        } catch (Exception e) {
        }
    }
}
