/*
 * ConferenceParticipants.java
 *
 * Created on 12 Апрель 2009 г., 22:20
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import DrawControls.*;
import DrawControls.icons.Icon;
import java.util.Vector;
import javax.microedition.lcdui.Font;
import jimm.JimmUI;
import jimm.cl.ContactList;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import jimm.util.ResourceBundle;
import jimm.comm.*;
import protocol.Contact;
import protocol.MessageEditor;

/**
 *
 * @author Vladimir Krukov
 */
public class ConferenceParticipants extends VirtualFlatList implements SelectListener {
    private JabberServiceContact conference;
    private Vector contacts = new Vector();
    
    /** Creates a new instance of ConferenceParticipants */
    private static final int COMMAND_REPLY = 0;
    private static final int COMMAND_PRIVATE = 1;
    private static final int COMMAND_INFO = 2;
    private static final int COMMAND_COPY = 3;
    private static final int COMMAND_KICK = 4;
    private static final int COMMAND_BAN = 5;
    private static final int COMMAND_DEVOICE = 6;
    private static final int COMMAND_VOICE = 7;
    private static final int COMMAND_USER_MENU = 8;
    private static final int COMMAND_MEMBER = 9; //commands
    private static final int COMMAND_MODER = 10; //commands
    private static final int COMMAND_ADMIN = 11; //commands
    private static final int COMMAND_NONE = 12;  //commands
    
    private int myRole;
    public ConferenceParticipants(JabberServiceContact conf) {
        super(conf.getName());
        conference = conf;
        myRole = getPriority(conference.getMyName());
        update();
    }
    
    protected final int getSize() {
        return contacts.size();
    }

    private final String getCurrentContact() {
        int contactIndex = getCurrItem();
        if ((contactIndex < 0) || (getSize() <= contactIndex)) {
            return null;
        }
        Object o = contacts.elementAt(contactIndex);
        if (o instanceof JabberContact.SubContact) {
            JabberContact.SubContact c = (JabberContact.SubContact)o;
            return c.resource;
        }
        return null;
    }

    protected void itemSelected() {
        if (conference.canWrite()) {
            getMenu().exec(null, COMMAND_REPLY);
        }
    }
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if (JimmUI.isHotKey(keyCode, actionCode)) {
            String nick = getCurrentContact();
            if (null != nick) {
                String jid = JabberXml.realJidToJimmJid(conference.getUin() + '/' + nick);
                Contact c = conference.getProtocol().createTempContact(jid);
                if (JimmUI.execHotKey(c, keyCode, actionCode)) {
                    return;
                }
            }
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }
    
    
    protected final MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        String nick = getCurrentContact();
        if (null == nick) {
            menu.addItem("back", MenuModel.BACK_COMMAND_CODE);
            menu.setActionListener(this);
            return menu;
        }

        int defaultCode = -1;
        if (conference.canWrite()) {
            menu.addItem("reply", COMMAND_REPLY);
            defaultCode = COMMAND_REPLY;
        }
        menu.addItem("private_chat", COMMAND_PRIVATE);
        menu.addItem("info", COMMAND_INFO);
        menu.addItem("user_menu", COMMAND_USER_MENU);
        menu.addItem("copy_text", COMMAND_COPY);
                
        if (JabberServiceContact.ROLE_MODERATOR == myRole) {
            int role = getPriority(nick);
            if (JabberServiceContact.ROLE_VISITOR != role) {  //commands
                    menu.addItem("admin", COMMAND_ADMIN);     //commands
                    menu.addItem("moder", COMMAND_MODER);     //commands
                    menu.addItem("member", COMMAND_MEMBER);   //commands
                    menu.addItem("none", COMMAND_NONE);       //commands
                    menu.addItem("devoice", COMMAND_DEVOICE); //commands
                    } else {                                  //commands
                    menu.addItem("voice", COMMAND_VOICE);     //commands
                    }                                         //commands
                    menu.addItem("kick", COMMAND_KICK);       //commands
                    menu.addItem("ban", COMMAND_BAN);         //commands
            }                                                 //commands

        menu.addItem("back", MenuModel.BACK_COMMAND_CODE);
        menu.setActionListener(this);
        return menu;
    }

    public void clear() {
        contacts.removeAllElements();
        
    }
    private void update() {
        super.lock();
        int currentIndex = getCurrItem();
        clear();
        addLayerToListOfSubcontacts("moderators", JabberServiceContact.ROLE_MODERATOR);     //it is changed
        addLayerToListOfSubcontacts("participants", JabberServiceContact.ROLE_PARTICIPANT); //it is changed
        addLayerToListOfSubcontacts("visitors", JabberServiceContact.ROLE_VISITOR);         //it is changed
        setCurrentItem(currentIndex);
        super.unlock();
    }
    
    private final int getPriority(String nick) {
        JabberContact.SubContact c = getContact(nick);
        return (null == c) ? JabberServiceContact.ROLE_VISITOR : c.priority;
    }
    
    private final JabberContact.SubContact getContact(String nick) {
        if (StringConvertor.isEmpty(nick)) {
            return null;
        }
        Vector subcontacts = conference.subcontacts;
        for (int i = 0; i < subcontacts.size(); ++i) {
            JabberContact.SubContact contact = (JabberContact.SubContact)subcontacts.elementAt(i);
            if (nick.equals(contact.resource)) {
                return contact;
            }
        }
        return null;
    }
    
    private void addLayerToListOfSubcontacts(String layer, byte priority) {
        boolean hasLayer = false;
        Vector subcontacts = conference.subcontacts;
        for (int i = 0; i < subcontacts.size(); ++i) {
            JabberContact.SubContact contact = (JabberContact.SubContact)subcontacts.elementAt(i);
            hasLayer = (contact.priority == priority);
            if (hasLayer) {
                break;
            }
        }
        if (!hasLayer) {
            return;
        }

        contacts.addElement(layer);
        
        final int maxLength = 40;
        for (int i = 0; i < subcontacts.size(); ++i) {
            JabberContact.SubContact contact = (JabberContact.SubContact)subcontacts.elementAt(i);
            if (contact.priority == priority) {
                contacts.addElement(contact);
            }
        }
    }
    protected int getItemHeight(int itemIndex) {
        Object o = contacts.elementAt(itemIndex);
        if (o instanceof JabberContact.SubContact) {
            JabberContact.SubContact c = (JabberContact.SubContact)o;
            int height = getDefaultFont().getHeight() + 1;
            // #sijapp cond.if modules_CLIENTS is "true" #
            if (null != c.client.getIcon()) {
                height = Math.max(height, c.client.getIcon().getHeight());
            }
            // #sijapp cond.end #
            Icon icon = conference.getProtocol().getStatusInfo().getIcon(c.status);
            if (null != icon) {
                height = Math.max(height, icon.getHeight());
            }
            height = Math.max(height, CanvasEx.minItemHeight);
            return height;
        }
        return getFontSet()[Font.STYLE_BOLD].getHeight() + 1;
    }

    private final Icon[] leftIcons = new Icon[1];
    private final Icon[] rightIcons = new Icon[1];
    protected void drawItemData(GraphicsEx g, int index, int x1, int y1, int x2, int y2) {
        g.setThemeColor(THEME_TEXT);
        Object o = contacts.elementAt(index);
        if (o instanceof JabberContact.SubContact) {
            JabberContact.SubContact c = (JabberContact.SubContact)o;
            g.setFont(getDefaultFont());
            leftIcons[0] = conference.getProtocol().getStatusInfo().getIcon(c.status);
            // #sijapp cond.if modules_CLIENTS is "true" #
            rightIcons[0] = c.client.getIcon();
            // #sijapp cond.end #
            g.drawString(leftIcons, c.resource, rightIcons,
                    x1, y1, x2 - x1, y2 - y1);
            return;
        }
        String header = (String)o;
        g.setFont(getFontSet()[Font.STYLE_BOLD]);
        g.drawString(header, x1, y1, x2 - x1, y2 - y1);
    }

    private void showContactMenu(String nick) {
        String jid = JabberXml.realJidToJimmJid(conference.getUin() + '/' + nick);
        JabberServiceContact c = (JabberServiceContact)conference.getProtocol().createTempContact(jid);
        MenuModel m = c.getContextMenu();
        if (null != m) {
            new Select(m).show();
        }
    }
    public void select(Select select, MenuModel model, int cmd) {
        if (MenuModel.BACK_COMMAND_CODE == cmd) {
            back();
            return;
        }
        String nick = getCurrentContact();
        if (null == nick) {
            return;
        }
        switch (cmd) {
            case COMMAND_COPY:
                JimmUI.setClipBoardText(getCaption(), nick);
                restore();
                break;

            case COMMAND_USER_MENU:
                showContactMenu(nick);
                break;
                
            case COMMAND_REPLY:
                MessageEditor editor = ContactList.getInstance().getMessageEditor();
                if (editor.isActive(conference)) {
                    InputTextBox box = editor.getTextBox();
                    String text = box.getRawString();
                    if (!StringConvertor.isEmpty(text)) {
                        String space = box.getSpace();
                        if (text.endsWith(space)) {
                        } else if (1 == space.length()) {
                            text += space;
                        } else {
                            text += text.endsWith(" ") ? " " : space;
                        }
                        if (text.endsWith("," + space)) {
                            text += nick + "," + space;
                        } else {
                            text += nick + space;
                        }
                        box.setString(text);
                        box.show();
                        return;
                    }
                }
                conference.getChat().writeMessageTo(nick);
                break;
                
            case COMMAND_PRIVATE:
                conference.resourceSelected(nick);
                break;
                
            case COMMAND_INFO:
                String jid = conference.getRealJid(nick);
                if (null == jid) {
                    jid = JabberXml.realJidToJimmJid(conference.getUin() + '/' + nick);
                }
                Contact c = conference.getProtocol().createTempContact(jid);
                c.showUserInfo();
                break;
                
            case COMMAND_KICK:
                conference.setMucRole(nick, "n" + "o" + "ne");
                update();
                break;
                
            case COMMAND_BAN:
                conference.setMucAffiliation(nick, "o" + "utcast");
                update();
                break;
                
            case COMMAND_DEVOICE:
                conference.setMucRole(nick, "v" + "isitor");
                update();
                break;
                
            case COMMAND_VOICE:
                conference.setMucRole(nick, "partic" + "ipant");
                update();
                break;

            case COMMAND_MEMBER:                                      //commands
                conference.setMucAffiliation(nick, "m" + "ember");    //commands
                update();                                             //commands
		break;                                                //commands

            case COMMAND_MODER:                                       //commands
                conference.setMucRole(nick, "m" + "oderator");        //commands
                update();                                             //commands
		break;                                                //commands

            case COMMAND_ADMIN:                                       //commands
                conference.setMucAffiliation(nick, "a" + "dmin");     //commands
                update();                                             //commands
		break;                                                //commands

            case COMMAND_NONE:                                        //commands
                conference.setMucAffiliation(nick, "n" + "o" + "ne"); //commands
                update();                                             //commands
		break;                                                //commands

        }
    }
}
// #sijapp cond.end #
