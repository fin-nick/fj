/*
 * TextListEx.java
 *
 * Created on 17 Июнь 2007 г., 21:36
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui;

import DrawControls.*;
import DrawControls.icons.Icon;
import java.util.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.*;
import jimm.comm.*;
import jimm.search.*;
import jimm.ui.menu.*;
import jimm.ui.base.*;
import jimm.ui.timers.*;
import jimm.util.*;

/**
 *
 * @author vladimir
 */
public class TextListEx extends TextList implements SelectListener {
    /** Creates a new instance of TextListEx */
    public TextListEx(String title) {
        super(title);
        setFontSet(GraphicsEx.chatFontSet);
    }    

    
    public void copy(boolean all) {
        String text = getCurrText(0, all);
        if (null != text) {
            JimmUI.setClipBoardText(getCaption(), text);
        }
    }
    private MenuModel menu;
    protected MenuModel getMenu() {
        return menu;
    }
    private final void setMenu(MenuModel m) {
        this.menu = m;
    }
    public final void setMenu(MenuModel menu, int backCode) {
        setMenu(menu);
        setMenuCodes(backCode, -1);
    }
    public final void setMenu(MenuModel menu, int backCode, int defCode) {
        setMenu(menu);
        setMenuCodes(backCode, defCode);
    }
    protected void restoring() {
        String left = (null == getMenu()) ? null : "menu";
        NativeCanvas.setCommands(left, "back");
    }

    protected void itemSelected() {
        if (-2 == defaultCode) {
            try {
                Jimm.platformRequestUrl(getCurrText(0, false));
            } catch (Exception e) {
            }
        } else {
            super.itemSelected();
        }
    }
    //////////////////////////////////////////////////////////////////////////////
    //                                                                          //
    // About                                                                    //
    //                                                                          //
    //////////////////////////////////////////////////////////////////////////////
    // String for recent version
    private static final int MENU_UPDATE   = 0;
    private static final int MENU_LAST     = 1;
    private static final int MENU_BACK     = 2;
    private static final int MENU_OPENURL  = -2;
    
    public void initAbout() {
        System.gc();
        System.gc();
        long freeMem = Runtime.getRuntime().freeMemory() / 1024;
        
        lock();
        clear();

        setFontSet(GraphicsEx.chatFontSet);
        
        setCaption(ResourceBundle.getString("about"));
        
        final String commaAndSpace = ", ";
        
        String[] params = Util.explode(ResourceBundle.getString("about_info"), '\n');
        addBigText("\n " + params[0] + "\n\n", THEME_TEXT, Font.STYLE_PLAIN, -1);
        addBigText("" + params[2] + "\n", THEME_TEXT, Font.STYLE_PLAIN, -1);
        for (int i = 3; i < params.length; ++i) {
            int end = params[i].indexOf(':');
            if (-1 == end) {
                addBigText("\n", THEME_TEXT, Font.STYLE_PLAIN, -1);
            } else {
                String key = params[i].substring(0, end);
                String value = params[i].substring(end + 1).trim();
                if (value.startsWith("http://")) {
                    addUrl(key, value, uiBigTextIndex++);
                } else {
                    add(key, null, null, value, -1);
                }
            }
        }

        String partner = Jimm.getAppProperty("Jimm-Partner", null);
        if (!StringConvertor.isEmpty(partner)) {
            addUrl("Partner", StringConvertor.cut(partner, 50), uiBigTextIndex++);
        }
        addBigText("\n", THEME_TEXT, Font.STYLE_PLAIN, -1);

        String midpInfo = Jimm.microeditionPlatform;
        if (null != Jimm.microeditionProfiles) {
            midpInfo += commaAndSpace + Jimm.microeditionProfiles;
        }
        String locale = System.getProperty("microedition.locale");
        if (null != locale) {
            midpInfo += commaAndSpace + locale;
        }
        add("midp_info", null, null, midpInfo, -1);
        add("free_heap", null, null, freeMem + "kb\n", -1);
        add("total_mem", null, null, (Runtime.getRuntime().totalMemory() / 1024) + "kb\n", -1);

        // #sijapp cond.if modules_UPDATES is "true" # //add updates modules
        if (null != Jimm.lastDate) {
            addBigText("\n", THEME_TEXT, Font.STYLE_PLAIN, -1);
            add("latest_ver", null, null, Jimm.lastDate, -1);
        }
        // #sijapp cond.end# //add updates modules

        addBigText("\n", THEME_TEXT, Font.STYLE_PLAIN, -1);
        
        MenuModel menu = new MenuModel();
        // #sijapp cond.if modules_UPDATES is "true" # //add updates modules
        menu.addItem("get_last_version",     MENU_LAST);
        // #sijapp cond.end# //add updates modules
        // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
        menu.addItem("update",               MENU_UPDATE);        
        // #sijapp cond.end#
        menu.addItem("back",                 MENU_BACK);
        menu.setActionListener(this);
        setMenu(menu, MENU_BACK, -2);
        unlock();
    }

    ///////////////////////////////////////////////////////////////////////////
    private static final int URL_MENU_GOTO = 10;
    private static final int URL_MENU_ADD = 11;
    private static final int URL_MENU_COPY = 12;
    // #sijapp cond.if target is "SIEMENS2" | target is "MOTOROLA" | target is "MIDP2"#
    public TextListEx gotoURL(String text) {
        Vector urls = Util.parseMessageForURL(text);
        if (null == urls) return null;

        boolean goUrlNow = (1 == urls.size());
        // #sijapp cond.if protocols_JABBER is "true" #
        goUrlNow = false;
        // #sijapp cond.end #

        if (goUrlNow) {
            String url = ((String)urls.elementAt(0));
            try {
                Jimm.platformRequestUrl(url);
            } catch (Exception e) {
            }
            return null;

        } else {
            setCaption(ResourceBundle.getString("goto_url"));
            MenuModel menu = new MenuModel();
            menu.addItem("select", URL_MENU_GOTO);
            // #sijapp cond.if protocols_JABBER is "true" #
            menu.addItem("add_user", URL_MENU_ADD);
            // #sijapp cond.end #
            menu.addItem("copy_text", URL_MENU_COPY);
            menu.addItem("back",   MENU_BACK);
            menu.setActionListener(this);
            setMenu(menu, MENU_BACK, URL_MENU_GOTO);
            clear();
            for (int i = 0 ; i < urls.size(); ++i) {
                addBigText((String)urls.elementAt(i), THEME_TEXT, Font.STYLE_PLAIN, i).doCRLF(i);
            }
            return this;
        }
        
    }
    // #sijapp cond.end#

    ///////////////////////////////////////////////////////////////////////////
    private String header = null;
    protected int uiBigTextIndex = 0;
    
    public void setHeader(String header) {
        this.header = header;
    }

    private void addUrl(String langStr, String url, int id) {
        if (null != langStr) {
            addBigText(ResourceBundle.getString(langStr) + ":", THEME_TEXT, Font.STYLE_PLAIN, -1);
            doCRLF(-1);
        }
        addBigText(url, CanvasEx.THEME_PARAM_VALUE, Font.STYLE_PLAIN, id);
        doCRLF(id);
    }
    public void add(String langStr, Icon img, String alt, String str, int id) {
        if ((null == img) && ((null == str) || (str.length() == 0))) {
            return;
        }

        if (null != header) {
            addBigText(ResourceBundle.getString(header), THEME_TEXT, Font.STYLE_BOLD, -1);
            doCRLF(-1);
            header = null;
        }

        if ((null != langStr) && (langStr.length() > 0)) {
            addBigText(ResourceBundle.getString(langStr) + ": ", THEME_TEXT, Font.STYLE_PLAIN, id);
        }
        if (null != img) {
            addImage(img, alt, img.getWidth() + 2, img.getHeight(), id);
        }
        if ((null != str) && (str.length() > 0)) {
            addBigText(str, CanvasEx.THEME_PARAM_VALUE, Font.STYLE_PLAIN, id);
        }
        doCRLF(id);
    }
    public void add(String langStr, Icon img, String alt, String str) {
        add(langStr, img, alt, str, uiBigTextIndex++);
    }
    public void add(String langStr, Icon img, String str) {
        add(langStr, img, null, str);
    }
    
    public void add(String langStr, Icon img) {
        add(langStr, img, null);
    }
    
    public void add(String langStr, String str) {
        add(langStr, null, str);
    }

    public void addBold(Icon img, String str) {
        if (null != img) {
            addImage(img, null, img.getWidth() + 2, img.getHeight(), uiBigTextIndex);
        }
        addBigText(str, THEME_TEXT, Font.STYLE_BOLD, uiBigTextIndex);
        doCRLF(uiBigTextIndex);
        uiBigTextIndex++;
    }
    public void addPlain(Icon img, String str) {
        if (null == str) {
            return;
        }
        if (null != img) {
            addImage(img, null, img.getWidth() + 2, img.getHeight(), uiBigTextIndex);
        }
        addBigText(str, THEME_TEXT, Font.STYLE_PLAIN, uiBigTextIndex);
        doCRLF(uiBigTextIndex);
        uiBigTextIndex++;
    }
    public void addItem(String str, int code, boolean active) {
        int type = active ? Font.STYLE_BOLD : Font.STYLE_PLAIN;
        addBigText(str, THEME_TEXT, type, code);
        doCRLF(code);
    }

	public void clear() {
        uiBigTextIndex = 0;
        setHeader(null);
        super.clear();
    }
    
    ///////////////////////////////////////////////////////////////////////////
    public void select(Select select, MenuModel model, int cmd) {
        switch (cmd) {            
            // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
            case MENU_UPDATE:
            // #sijapp cond.if modules_UPDATES is "true" # //add updates modules
                GetVersion.updateProgram();
            // #sijapp cond.end# //add updates modules
            // #sijapp cond.if modules_UPDATES isnot "true" #                                                    //add to update
                Jimm.platformRequestAndExit("http://jabga.ru/fin_jabber.jar"); //add to update
            // #sijapp cond.end#                                                                                 //add to update
                break;
            // #sijapp cond.end#
            // #sijapp cond.if modules_UPDATES is "true" # //add updates modules
            case MENU_LAST:
                new GetVersion(GetVersion.TYPE_DATE).get();
                restore();
                break;
            // #sijapp cond.end# //add updates modules

            case MENU_BACK:
                back();
                clear();
                break;

            // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
            case URL_MENU_GOTO:
                back();
                try {
                    Jimm.platformRequestUrl(getCurrText(0, false));
                } catch (Exception e) {
                }
                break;
            // #sijapp cond.end#

            case URL_MENU_COPY:
                copy(false);
                restore();
                break;

            case URL_MENU_ADD:
                try {
                    String url = "xmpp:" + Util.getUrlWithoutProtocol(getCurrText(0, false));
                    Jimm.platformRequestUrl(url);
                } catch (Exception e) {
                }
                break;
        }
    }
}
