/*
 * JabberServiceContact.java
 *
 * Created on 4 Январь 2009 г., 19:54
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import java.util.Vector;
import DrawControls.icons.*;
import javax.microedition.lcdui.*;
import jimm.JimmUI;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.chat.message.*;
import jimm.search.*;
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.chat.*;
import jimm.util.ResourceBundle;
import protocol.*;
import jimm.modules.*; //add for a eye sound
import jimm.*;         //add for a applications priority

/**
 *
 * @author Vladimir Kryukov
 */
public class JabberServiceContact extends JabberContact implements CommandListener {
    private static final int GATE_CONNECT = 0;
    private static final int GATE_DISCONNECT = 1;
    private static final int GATE_REGISTER = 3;
    private static final int GATE_UNREGISTER = 4;
    private static final int GATE_ADD = 5;
    public static final int CONFERENCE_CONNECT = 6;
    public static final int CONFERENCE_DISCONNECT = 7;
    public static final int CONFERENCE_OPTIONS = 8;
    private static final int CONFERENCE_ADD = 9;
    
    private boolean isPrivate;
    private boolean isConference;
    private boolean isGate;
    
    private boolean autojoin;
    private String password;
    private String myNick;

    private String baseMyNick;

    public void setAutoJoin(boolean auto) {
        autojoin = auto;
    }
    public boolean isAutoJoin() {
        return autojoin;
    }
    
    public void setMucRole(String nick, String role) {
        ((Jabber)getProtocol()).getConnection().setMucRole(getUin(), nick, role);
    }
    public void setMucAffiliation(String nick, String affiliation) {
        SubContact c = getExistSubContact(nick);
        if ((null == c) || (null == c.realJid)) {
            return;
        }
        ((Jabber)getProtocol()).getConnection().setMucAffiliation(getUin(),
                c.realJid, affiliation);
    }
    
    public void setPassword(String pass) {
        password = pass;
    }
    public String getPassword() {
        return password;
    }
    
    /** Creates a new instance of JabberContact */
    public JabberServiceContact(Jabber jabber, String jid, String name) {
        super(jabber, jid, name);
        isGate = JabberXml.isGate(getUin());

        if (isGate) {
            isPrivate = false;
            return;
        }

        isPrivate = (-1 != jid.indexOf('/'));
        if (isPrivate) {
            String resource = JabberXml.getResource(jid, "");
            setName(resource + "@" + JabberXml.getNick(jid));
            setPrivateContactStatus();
            return;
        }

        isConference = JabberXml.isConference(jid);
        if (isConference) {
            setMyName(getDefaultName());
            if (jid.equals(name)) {
                setName(JabberXml.getNick(jid));
            }
        }
    }
    
    /////////////////////////////////////////////////////////////////////////
    // #sijapp cond.if modules_XSTATUSES is "true" #
    public void setXStatus(String id, String text) {
    }
    // #sijapp cond.end #
    
    public void getRightIcons(Icon[] icons) {
        if (!isConference) {
            super.getRightIcons(icons);
        }
    }
    /////////////////////////////////////////////////////////////////////////
    protected void typing(boolean isTyping) {
    }
    
    private final String getDefaultName() {
        String nick = getProtocol().getProfile().nick;
        if (StringConvertor.isEmpty(nick)) {
            return JabberXml.getNick(protocol.getUserId());
        }
        return nick;
    }
    public final void setMyName(String nick) {
        myNick = nick;
        if (StringConvertor.isEmpty(myNick)) {
            myNick = getDefaultName();
        }
        if (!isOnline()) {
            baseMyNick = myNick;
        }
    }
    public String getMyName() {
        return isConference ? myNick : super.getMyName();
    }
    
    public boolean isConference() {
        return isConference;
    }

    public boolean isGate() { //hide conferences & transport
        return isGate;        //hide conferences & transport
    }                         //hide conferences & transport

    public boolean isVisibleInContactList() {                                      //hide conferences & transport
        if (!Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE_ALL)) return true;  //hide conferences & transport
        return isConference() || isGate() ? super.isVisibleInContactList() : true; //hide conferences & transport
    }                                                                              //hide conferences & transport
    
    void nickChainged(String oldNick, String newNick) {
        if (isConference) {
            if (myNick.equals(oldNick)) {
                setMyName(newNick);
                baseMyNick = newNick;
            }
            String jid = JabberXml.realJidToJimmJid(getUin() + "/" + oldNick);
            JabberServiceContact c = (JabberServiceContact)protocol.getItemByUIN(jid);
            if (null != c) {
                c.nickChainged(oldNick, newNick);
            }
            
        } else if (isPrivate) {
            uin = JabberXml.getShortJid(uin) + "/" + newNick;
            setName(newNick + "@" + JabberXml.getNick(getUin()));
            setOfflineStatus();
        }
    }
    void nickOnline(String nick) {
        if (hasChat()) {
            getChat().setWritable(canWrite());
        }
        StatusInfo info = ((Jabber)getProtocol()).getStatusInfo();
        SubContact sc = getExistSubContact(nick);
        if (isCurrent() && (null != sc)) {
            showTopLine(nick + ": " + info.getName(sc.status));

        // #sijapp cond.if modules_PRESENCE is "true" #              //presence
        if (Options.getBoolean(Options.OPTION_NOTICE_PRESENCE)) {    //presence
            if (!getMyName().equals(nick)) {                         //presence
            String text = null;                                      //presence
            text = ("/me " + nick + ": " + info.getName(sc.status)); //presence
            getProtocol().addMessage(new SystemNotice(getProtocol(), //presence
            SystemNotice.SYS_NOTICE_PRESENCE, getUin(), text));      //presence
            }                                                        //presence
        }                                                            //presence
        // #sijapp cond.end#                                         //presence
        }
        if (myNick.equals(nick)) {
            Status s = getStatus();
            status = createStatus(s, Status.I_STATUS_ONLINE, s.getText());
            ((Jabber)getProtocol()).addRejoin(getUin());
        }
    }
    void nickError(String nick, int code, String reasone) {
        if (409 == code) {
            if (Status.I_STATUS_ONLINE != status.getStatusIndex()) {
                setOfflineStatus();
            }
            if (!StringConvertor.isEmpty(reasone)) {
                getProtocol().addMessage(new SystemNotice(getProtocol(),
                        SystemNotice.SYS_NOTICE_ERROR, getUin(), reasone));
            }
            if (baseMyNick != myNick) {
                myNick = baseMyNick;
            }
        }
    }
    void nickOffline(String nick, int code, String reasone) {
        if (getMyName().equals(nick)) {
            if (isOnline()) {
                ((Jabber)getProtocol()).removeRejoin(getUin());
            }
            String text = null;
            if (301 == code) {
                text = "I was baned";
            } else if (307 == code) {
                text = "I was kicked";
            }
            if (null != text) {
                if (!StringConvertor.isEmpty(reasone)) {
                    text += " (" + reasone + ")";
                }
                text += '.';
                getProtocol().addMessage(new SystemNotice(getProtocol(),
                        SystemNotice.SYS_NOTICE_ERROR, getUin(), text));
            }
            for (int i = 0; i < subcontacts.size(); ++i) {
                ((SubContact)subcontacts.elementAt(i)).status = Status.offlineStatus;
            }
            String startUin = getUin() + '/';
            Vector contactList = getProtocol().getContactItems();
            for (int i = contactList.size() - 1; 0 <= i; --i) {
                Contact c = (Contact)contactList.elementAt(i);
                if (c.getUin().startsWith(startUin)) {
                    c.setOfflineStatus();
                }
            }
            setOfflineStatus();

        // #sijapp cond.if modules_MAGIC_EYE is "true" #
        } else {
            if (301 == code) {
                jimm.modules.MagicEye.addAction(protocol, getUin(), nick + " was baned", reasone);
                // #sijapp cond.if modules_SOUND is "true" #     //eye sound
                Notify.playSoundNotification(Notify.NOTIFY_EYE); //eye sound
                // #sijapp cond.end #                            //eye sound
            } else if (307 == code) {
                jimm.modules.MagicEye.addAction(protocol, getUin(), nick + " was kicked", reasone);
                // #sijapp cond.if modules_SOUND is "true" #     //eye sound
                Notify.playSoundNotification(Notify.NOTIFY_EYE); //eye sound
                // #sijapp cond.end #                            //eye sound
            }
        // #sijapp cond.end #
        }
        if (hasChat()) {
            getChat().setWritable(canWrite());
        }
        if (isCurrent()) {
            StatusInfo info = ((Jabber)getProtocol()).getStatusInfo();
            showTopLine(nick + ": " + info.getName(Status.I_STATUS_OFFLINE));

        // #sijapp cond.if modules_PRESENCE is "true" #                            //presence
        if (Options.getBoolean(Options.OPTION_NOTICE_PRESENCE)) {                  //presence
            if (!getMyName().equals(nick)) {                                       //presence
            String text = null;                                                    //presence
            text = ("/me " + nick + ": " + info.getName(Status.I_STATUS_OFFLINE)); //presence
            getProtocol().addMessage(new SystemNotice(getProtocol(),               //presence
            SystemNotice.SYS_NOTICE_PRESENCE, getUin(), text));                    //presence
            }                                                                      //presence
        }                                                                          //presence
        // #sijapp cond.end#                                                       //presence
        }
    }
    
    // #sijapp cond.if modules_HISTORY is "true" #
    public void showHistory() {
    }
    // #sijapp cond.end#
    public void showUserInfo() {
        if (isConference) {
            if (isOnline() || !protocol.isConnected()) {
                doAction(USER_MENU_USERS_LIST);
            } else {
                ServiceDiscovery sd = ((Jabber)protocol).getServiceDiscovery();
                sd.setServer(getUin());
                sd.showIt();
            }

        } else {
            if (isPrivate) {
                String confJid = JabberXml.getShortJid(getUin());
                String nick = JabberXml.getNick(getUin());
                JabberServiceContact conf = (JabberServiceContact)getProtocol().getItemByUIN(confJid);
                String realJid = (null == conf) ? null : conf.getRealJid(nick);
                if (null != realJid) {
                    Contact c = getProtocol().createTempContact(realJid);
                    c.showUserInfo();
                    return;
                }
            }
            super.showUserInfo();
        }
    }
    String getRealJid(String nick) {
        SubContact sc = getExistSubContact(nick);
        return (null == sc) ? null : sc.realJid;
    }
    
    public final String getDefaultGroupName() {
        if (isConference) {
            return ResourceBundle.getString(JabberGroup.CONFERENCE_GROUP);
        }
        if (isGate) {
            return ResourceBundle.getString(JabberGroup.GATE_GROUP);
        }
        return null;
    }
    public void setSubject(String subject) {
        if (isConference && isOnline()) {
            Status s = getStatus();
            status = createStatus(s, Status.I_STATUS_ONLINE, subject);
        }
    }
    
    /** Creates a new instance of JabberServiceContact */
    public MenuModel getContextMenu() {
        if (!protocol.isConnected()) {
            return null;
        }
        contactMenu.clean();
        if (isGate) {
            if (isOnline()) {
                contactMenu.addItem("disconnect", null, GATE_DISCONNECT);
                contactMenu.setDefaultItemCode(GATE_DISCONNECT);
            } else {
                contactMenu.addItem("connect", null, GATE_CONNECT);
                contactMenu.addItem("register", null, GATE_REGISTER);
                contactMenu.addItem("unregister", null, GATE_UNREGISTER);
                contactMenu.setDefaultItemCode(GATE_CONNECT);
            }
        }
        if (isConference) {
            if (isOnline()) {
                contactMenu.addItem("disconnect", null, CONFERENCE_DISCONNECT);
                contactMenu.addItem("list_of_users", null, USER_MENU_USERS_LIST);
                contactMenu.setDefaultItemCode(CONFERENCE_DISCONNECT);
            } else {
                contactMenu.addItem("connect", null, CONFERENCE_CONNECT);
                contactMenu.setDefaultItemCode(CONFERENCE_CONNECT);
            }
            contactMenu.addItem("options", null, CONFERENCE_OPTIONS);
        }
        if ((isOnline() && isConference && canWrite()) || isPrivate) {
            addChatItems(contactMenu);
            addFileTransferItems(contactMenu);
        }
        if (isPrivate || isGate) {
            contactMenu.addItem("info", Contact.USER_MENU_USER_INFO);
        }
        if (isPrivate) {
            initManageContactMenu(contactMenu);
        } else {
            contactMenu.addItem("manage", USER_MANAGE_CONTACT);
        }
        if (isOnline() && !isGate) {
            contactMenu.addItem("user_statuses", USER_MENU_STATUSES);
        }
        contactMenu.setActionListener(this);
        return contactMenu;
    }
    protected void initManageContactMenu(MenuModel menu) {
        if (protocol.isConnected()) {
            if (isConference && isTemp()) {
                menu.addItem("add_user", null, CONFERENCE_ADD);
            }
            if (isGate) {
                if ((1 < protocol.getGroupItems().size()) && !isTemp()) {
                    menu.addEllipsisItem("move_to_group", null, USER_MENU_MOVE);
                }
                if (!isAuth()) {
                    menu.addEllipsisItem("requauth", null, USER_MENU_REQU_AUTH);
                }
                menu.addItem("add_user", null, GATE_ADD);
                menu.addEllipsisItem("remove_me", null, USER_MENU_REMOVE_ME);
            }
        }
        if (protocol.inContactList(this)) {
            if (!isPrivate) {
                menu.addEllipsisItem("rename", null, USER_MENU_RENAME);
                menu.addSeparator();
            }
            menu.addEllipsisItem("remove", null, USER_MENU_USER_REMOVE);
        }
    }
    protected void setMainStatus(byte prev, Status s) {
        if (isPrivate) {
            status = s;
        } else if (isGate) {
            super.setMainStatus(prev, s);
        }
    }
    
    public String getNick(String resource) {
        SubContact c = getExistSubContact(resource);
        return (null == c) ? resource : c.resource;
    }
    
    protected void resourceSelected(String resource) {
        String jid = JabberXml.realJidToJimmJid(getUin() + "/" + resource);
        JabberServiceContact c = (JabberServiceContact)protocol.getItemByUIN(jid);
        if (null == c) {
            c = (JabberServiceContact)protocol.createTempContact(jid);
            protocol.addTempContact(c);
        }
        c.activate();
    }

    boolean canWrite() {
        if (isOnline()) {
            if (isConference) {
                SubContact sc = getExistSubContact(getMyName());
                return (null != sc) && (ROLE_VISITOR != sc.priority);
            }
            return true;
        }
        return !isPrivate;
    }
    public void activate() {
        if (isOnline() || isPrivate || hasChat()) {
            super.activate();
            
        } else if (isConference && protocol.isConnected()) {
            doAction(CONFERENCE_CONNECT);
        }
    }
    
    public boolean isSingleUserContact() {
        return isPrivate || isGate;
    }
    public boolean hasHistory() {
        return false;
    }
    
    public static final byte ROLE_MODERATOR = 2;
    public static final byte ROLE_PARTICIPANT = 1;
    public static final byte ROLE_VISITOR = 0;
    
    private static final int priority = (Util.strToIntDef(Options.getString(Options.OPTION_APL_PRIORITY), 30)); //applications priority
    public void doAction(int action) {
        super.doAction(action);
        Jabber jabber = (Jabber)getProtocol();
        
        switch (action) {
            case GATE_CONNECT:
                jabber.getConnection().presence(this, getUin(), priority, null);
                getProtocol().getContactList().activate();
                break;
                
            case GATE_DISCONNECT:
                jabber.getConnection().presence(this, getUin(), JabberXml.PRESENCE_UNAVAILABLE, null);
                getProtocol().getContactList().activate();
                break;
                
            case GATE_REGISTER:
                jabber.getConnection().register(getUin());
                break;
                
            case GATE_UNREGISTER:
                jabber.getConnection().unregister(getUin());
                jabber.getConnection().removeGateContacts(getUin());
                getProtocol().getContactList().activate();
                break;

            case GATE_ADD:
                Search s = new Search(getProtocol());
                s.setJabberGate(getUin());
                s.show(Search.TYPE_LITE);
                break;
                
            case USER_MENU_USERS_LIST:
                new ConferenceParticipants(this).show();
                break;
                
            case CONFERENCE_CONNECT:
                join();
                createChat().activate();
                break;
                
            case CONFERENCE_OPTIONS:
                showOptionsForm();
                break;
                
            case CONFERENCE_DISCONNECT:
                jabber.getConnection().presence(this, getUin() + "/" + getMyName(), JabberXml.PRESENCE_UNAVAILABLE, null);
                nickOffline(getMyName(), 0, null);
                removePrivateContacts();
                getProtocol().getContactList().activate();
                break;
                
            case CONFERENCE_ADD:
                protocol.addContact(this);
                getProtocol().getContactList().activate();
                break;
        }
    }
    private void showOptionsForm() {
        enterData = new FormEx("conference", "ok", "cancel", this);
        enterData.addTextField(NICK, "nick", myNick, 32, TextField.ANY);
        enterData.addTextField(PASSWORD, "password", password, 32, TextField.ANY);
        if (!isTemp()) {
            enterData.addCheckBox(AUTOJOIN, "autojoin", isAutoJoin());
        }
        enterData.endForm();
        enterData.show();
        if (!JabberXml.isIrcConference(getUin())) {
            Jabber jabber = (Jabber)getProtocol();
            jabber.getConnection().requestConferenceInfo(getUin());
        }
    }
    public void dismiss() {
        if (isOnline() && isConference) {
            Jabber jabber = (Jabber)getProtocol();
            jabber.getConnection().presence(this, getUin(),
                    JabberXml.PRESENCE_UNAVAILABLE, null);
        }
        super.dismiss();
    }
    
    void setConferenceInfo(String description) {
        if (null != enterData) {
            enterData.addString("description", description);
        }
    }
    
    private static FormEx enterData = null;
    private static final int NICK = 0;
    private static final int PASSWORD = 1;
    private static final int AUTOJOIN = 2;
    private static final int OLD_GATE = 3;
    private static final int NEW_GATE = 4;
    private static final int ACC_ID = 5;
    private static final int ACC_PASSWD = 6;
    
    public void join() {
        Jabber jabber = (Jabber)getProtocol();
        String jid = getUin();
        String myNick = this.myNick;
        setStatus(myNick, 0, Status.I_STATUS_ONLINE, "");

        Status s = getStatus();
        status = createStatus(s, Status.I_STATUS_AWAY, s.getText());

        JabberXml connection = jabber.getConnection();
        connection.presence(this, jid + "/" + myNick, priority, password);
        if (JabberXml.isIrcConference(jid) && !StringConvertor.isEmpty(password)) {
            String nickserv = jid.substring(jid.indexOf('%') + 1) + "/NickServ";
            connection.sendMessage(nickserv, "/quote NickServ IDENTIFY " + password);
            connection.sendMessage(nickserv, "IDENTIFY " + password);
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (enterData.backCommand == c) {
            enterData.back();
            enterData = null;
            
        } else if (enterData.saveCommand == c) {
            if (isConference) {
                String oldNick = myNick;
                setMyName(enterData.getTextFieldValue(NICK));
                setAutoJoin(!isTemp() && enterData.getChoiceItemValue(AUTOJOIN, 0));
                setPassword(enterData.getTextFieldValue(PASSWORD));
                
                boolean needUpdate = !isTemp();
                if (needUpdate) {
                    ((Jabber)getProtocol()).getConnection().saveConferences();
                }
                if (isOnline() && !oldNick.equals(myNick)) {
                    join();
                }
            }
            getProtocol().getContactList().activate();
            enterData = null;
        }
    }
    private final void removePrivateContacts() {
        Vector contacts = protocol.getContactItems();
        String conferenceJid = getUin() + '/';
        for (int i = contacts.size() - 1; 0 <= i; --i) {
            JabberContact c = (JabberContact)contacts.elementAt(i);
            if (c.getUin().startsWith(conferenceJid) && 0 == c.getUnreadMessageCount()) {
                protocol.removeContact(c);
            }
        }
    }
    public final void setPrivateContactStatus() {
        if (!isPrivate) {
            return;
        }
        String jid = getUin();
        String nick = JabberXml.getResource(jid, "");
        JabberServiceContact conf = (JabberServiceContact)protocol.getItemByUIN(JabberXml.getShortJid(jid));
        SubContact sc = (null == conf) ? null : conf.getExistSubContact(nick);
        if (null == sc) {
            setOfflineStatus();
            // #sijapp cond.if modules_CLIENTS is "true" #
            client = JabberClient.noneClient;
            // #sijapp cond.end #

        } else {
            if (subcontacts.isEmpty()) {
                subcontacts.addElement(sc);
            }
            subcontacts.setElementAt(sc, 0);
            status = sc.status;
            // #sijapp cond.if modules_CLIENTS is "true" #
            client = sc.client;
            // #sijapp cond.end #
        }
    }
}
// #sijapp cond.end #