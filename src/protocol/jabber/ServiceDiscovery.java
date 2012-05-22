/*
 * ServiceDiscovery.java
 *
 * Created on 9 Февраль 2009 г., 15:58
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import DrawControls.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.comm.StringConvertor;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import jimm.util.ResourceBundle;
import jimm.comm.*;
import protocol.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class ServiceDiscovery extends TextList implements SelectListener, CommandListener {
    
    private boolean isConferenceList = false;
    private int totalCount = 0;
    
    //private TextListEx list;
    private final Jabber jabber;
    private String serverJid;
    private InputTextBox serverBox;
    private InputTextBox searchBox;
    private boolean shortView;
    private Vector jids = new Vector();

    private static final int COMMAND_ADD = 0;
    private static final int COMMAND_SET = 1;
    private static final int COMMAND_REGISTER = 2;
    private static final int COMMAND_SEARCH = 3;
    private static final int COMMAND_SET_SERVER = 4;
    private static final int COMMAND_HOME = 5;
    private static final int COMMAND_BACK = 6;

    public ServiceDiscovery(Jabber protocol) {
        super(ResourceBundle.getString("service_discovery"));
        jabber = protocol;
        
        serverBox = new InputTextBox("service_discovery_server", 64);
        serverBox.setCommandListener(this);
        
        searchBox = new InputTextBox("service_discovery_search", 64);
        searchBox.setCommandListener(this);
    }
    
    protected MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        String jid = getCurrentJid();
        if (JabberXml.isConference(jid)) {
            menu.addItem("service_discovery_add", COMMAND_ADD);
            setMenuCodes(COMMAND_BACK, COMMAND_ADD);

        } else if (JabberXml.isGate(jid) && JabberXml.isContactOverGate("@" + jid)) {
            menu.addItem("register", COMMAND_REGISTER);
            setMenuCodes(COMMAND_BACK, COMMAND_REGISTER);

        } else {
            menu.addItem("select", COMMAND_SET);
            if (JabberXml.isGate(jid)) {
                menu.addItem("register", COMMAND_REGISTER);
            }
            setMenuCodes(COMMAND_BACK, COMMAND_SET);
        }
        menu.addItem("service_discovery_search", COMMAND_SEARCH);
        menu.addItem("service_discovery_server", COMMAND_SET_SERVER);
        menu.addItem("service_discovery_home", COMMAND_HOME);
        menu.addItem("back", COMMAND_BACK);
        menu.setActionListener(this);
        return menu;
    }
    private String getCurrentJid() {
        int currentIndex = getCurrTextIndex();
        return (-1 == currentIndex) ? "" : getJid(currentIndex);
    }
    
    private void addServer(boolean active) {
        if (0 < serverJid.length()) {
            final int serverIndex = active ? jids.size() : -1;
            addBigText(serverJid, TextList.THEME_TEXT, Font.STYLE_BOLD, serverIndex);
            addBigText("\n", TextList.THEME_TEXT, Font.STYLE_PLAIN, serverIndex);
            if (-1 != serverIndex) {
                jids.addElement(serverJid);
            }
        }
    }
    public void clear() {
        super.clear();
        jids.removeAllElements();
        addServer(false);
    }
    public void setTotalCount(int count) {
        super.clear();
        jids.removeAllElements();
        addServer(true);
        totalCount = count;
        shortView |= (totalCount > 400);
        lock();
    }

    private String getJid(int num) {
        if (num < jids.size()) {
            String rawJid = (String)jids.elementAt(num);
            if (rawJid.endsWith("@")) {
                return rawJid + serverJid;
            }
            return rawJid;
        }
        return "";
    }
    private String makeShortJid(String jid) {
        if (isConferenceList) {
            return jid.substring(0, jid.indexOf('@') + 1);
        }
        return jid;
    }
    private String makeReadableJid(String jid) {
        if (isConferenceList) {
            return jid;
        }
        if (JabberXml.isConference(serverJid)) {
            return JabberXml.getResource(jid, jid);
        }
        jid = Util.replace(jid, "@conference.jabber.ru", "@c.j.ru");
        return Util.replace(jid, "@conference.", "@c.");
    }
    
    public void addItem(String name, String jid) {
        if (StringConvertor.isEmpty(jid)) {
            return;
        }
        int index = jids.size();
        String shortJid = makeShortJid(jid);
        String visibleJid = makeReadableJid(shortJid);
        if (shortView) {
            addBigText(visibleJid, TextList.THEME_TEXT, Font.STYLE_PLAIN, index);
            
        } else {
            addBigText(visibleJid, TextList.THEME_TEXT, Font.STYLE_BOLD, index);
            if (StringConvertor.isEmpty(name)) {
                name = shortJid;
            }
            addBigText("\n", TextList.THEME_TEXT, Font.STYLE_PLAIN, index);
            addBigText(name, TextList.THEME_TEXT, Font.STYLE_PLAIN, index);
        }
        
        doCRLF(index);
        jids.addElement(shortJid);
        if (0 == (jids.size() % 50)) {
            unlock();
            lock();
        }
    }

    public void showIt() {
        if (StringConvertor.isEmpty(serverJid)) {
            setServer("");
        }
        show();
    }
    public void update() {
        unlock();
        invalidate();
    }

    private void addUnique(String text, String jid) {
        if (-1 == jids.indexOf(jid)) {
            addItem(text, jid);
        }
    }
    
    private void addBuildInList() {
        addUnique("Jimm aspro", "jimm-aspro@conference.jabber.ru");
        addBigText("\n", TextList.THEME_TEXT, Font.STYLE_PLAIN, -1);
        String domain = JabberXml.getDomain(jabber.getUserId());
        
        addUnique("My server", domain);
        addUnique("Conferences on " + domain, "conference." + domain);
    }

    public void setServer(String jid) {
        totalCount = 0;
        shortView = false;
        serverJid = jid;
        isConferenceList = (-1 == jid.indexOf('@')) && JabberXml.isConference('@' + jid);
        clear();
        if (0 == jid.length()) {
            lock();
            Config conf = new Config().loadLocale("/jabber-services.txt");
            boolean conferences = true;
            for (int i = 0; i < conf.getKeys().length; ++i) {
                if (conferences && !JabberXml.isConference(conf.getKeys()[i])) {
                    conferences = false;
                    addBuildInList();
                }
                addUnique(conf.getValues()[i], conf.getKeys()[i]);
            }
            if (conferences) {
                addBuildInList();
            }
            unlock();
            return;
        }
        if (JabberXml.isConference(serverJid)) {
            shortView = true;
        }
        addBigText(ResourceBundle.getString("wait"), CanvasEx.THEME_TEXT, Font.STYLE_PLAIN, -1);
        jabber.getConnection().requestDiscoItems(serverJid);
    }
    
    public void select(Select select, MenuModel model, int cmd) {
        String jid = getCurrentJid();
        if (!StringConvertor.isEmpty(jid)) {
            switch (cmd) {
                case COMMAND_ADD:
                    Contact c = jabber.createTempContact(jid);
                    jabber.addTempContact(c);
                    jabber.ui_setActiveContact(c);
                    jabber.getContactList().activate();
                    break;

                case COMMAND_SET:
                    setServer(jid);
                    restore();
                    break;

                case COMMAND_REGISTER:
                    jabber.getConnection().register(jid);
                    break;
            }
        }
        switch (cmd) {
            case COMMAND_SEARCH:
                searchBox.show();
                break;
                
            case COMMAND_SET_SERVER:
                serverBox.setString(serverJid);
                serverBox.show();
                break;
                
            case COMMAND_HOME:
                setServer("");
                restore();
                break;
                
            case COMMAND_BACK:
                back();
                break;
        }
    }
    
    public void commandAction(Command command, Displayable displayable) {
        if (serverBox.isOkCommand(command)) {
            setServer(serverBox.getString());
            restore();
            
        } else if (searchBox.isOkCommand(command)) {
            String text = searchBox.getString();
            if (isConferenceList) {
                text = StringConvertor.toLowerCase(text);
            }
            int currentIndex = getCurrTextIndex() + 1;
            for (int i = currentIndex; i < jids.size(); ++i) {
                String jid = (String)jids.elementAt(i);
                if (-1 != jid.indexOf(text)) {
                    setCurrTextIndex(i);
                    break;
                }
            }
            restore();
        }
    }
}
// #sijapp cond.end #
