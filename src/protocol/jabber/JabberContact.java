/*
 * JabberContact.java
 *
 * Created on 13 Июль 2008 г., 10:11
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import DrawControls.icons.*;
import java.io.*;
import java.util.Vector;
import jimm.*;
import jimm.chat.ChatTextList;
import jimm.chat.message.PlainMessage;
import jimm.search.*;
import jimm.comm.*;
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.util.ResourceBundle;
import protocol.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class JabberContact extends Contact {
    /** Creates a new instance of JabberContact */
    public JabberContact(Jabber jabber, String jid, String name) {
        protocol = jabber;
        this.uin = jid;
        this.setName((null == name) ? jid : name);
        setOfflineStatus();
    }
    
    protected String currentResource;
    // #sijapp cond.if modules_CLIENTS is "true" #
    protected JabberClient client = JabberClient.noneClient;
    // #sijapp cond.end #

    public boolean isMeVisible() {
        return true;
    }

    public void showUserInfo() {
        UserInfo data = null;
        if (getProtocol().isConnected()) {
            data = ((Jabber)getProtocol()).getConnection().getUserInfo(this);
            data.setProfileView(new TextListEx(getName()));
            data.setProfileViewToWait();

        } else {
            data = new UserInfo(getProtocol(), getUin());
            data.uin = getUin();
            data.nick = getName();
            data.setProfileView(new TextListEx(getName()));
            data.updateProfileView();
        }
        data.getProfileView().show();
    }

    public void showStatus() {
        try {
            if (isOnline() && isSingleUserContact()) {
                String jid = getUin();
                if (!(this instanceof JabberServiceContact)) {
                    jid += '/' + getCurrentSubContact().resource;
                }
                ((Jabber)getProtocol()).getConnection().requestClientVersion(jid);
            }
        } catch (Exception e) {
        }


        String statusMessage = status.getText();
        // #sijapp cond.if modules_XSTATUSES is "true" #
        String xstatusMessage = "";
        if (JabberXStatus.noneXStatus != xstatus) {
            xstatusMessage = xstatus.getText();
            String s = StringConvertor.notNull(statusMessage);
            if (!StringConvertor.isEmpty(xstatusMessage)
                    && s.startsWith(xstatusMessage)) {
                xstatusMessage = statusMessage;
                statusMessage = null;
            }
        }
        // #sijapp cond.end #

        statusView.init(this);
        // #sijapp cond.if modules_CLIENTS is "true" #
        if (isSingleUserContact()) {
            statusView.addClient(client.getIcon(), client.getName());
        }
        // #sijapp cond.end #
        statusView.addStatus(getProtocol(), status);
        statusView.addStatusText(statusMessage);
        
        // #sijapp cond.if modules_XSTATUSES is "true" #
        if (JabberXStatus.noneXStatus != xstatus) {
    	    statusView.addStatus(xstatus.getIcon(), xstatus.getName());
    	    statusView.addStatusText(xstatusMessage);
        }
        // #sijapp cond.end #
        statusView.showIt();
    }
    public String getStatusMessage() {
        String message = status.getText();
        if (!StringConvertor.isEmpty(message)) {
            return message;
        }
        // #sijapp cond.if modules_XSTATUSES is "true" #
        if (JabberXStatus.noneXStatus != xstatus) {
            message = xstatus.getText();
            if (!StringConvertor.isEmpty(message)) {
                return message;
            }
            message = xstatus.getName();
            if (!StringConvertor.isEmpty(message)) {
                return message;
            }
        }
        // #sijapp cond.end #
        return getProtocol().getStatusInfo().getName(status);
    }

    public boolean isConference() {
        return false;
    }

    public String getDefaultGroupName() {
        return ResourceBundle.getString(JabberGroup.GENERAL_GROUP);
    }
    
    /////////////////////////////////////////////////////////////////////////
    public void getLeftIcons(Icon[] leftIcons) {
        leftIcons[0] = getProtocol().getStatusInfo().getIcon(status);
        // #sijapp cond.if modules_XSTATUSES is "true" #
        Icon x = xstatus.getIcon();
        if (!Options.getBoolean(Options.OPTION_REPLACE_STATUS_ICON) || (null == x)) {
            leftIcons[1] = x;
        } else {
            leftIcons[0] = x;
        }
        // #sijapp cond.end #
        Icon messageIcon = getMessageIcon();
        if (null != messageIcon) {
            leftIcons[0] = messageIcon;
        }
        leftIcons[2] = isAuth() ? null : authIcon.iconAt(0);
    }
    public void getRightIcons(Icon[] rightIcons) {
        // #sijapp cond.if modules_CLIENTS is "true" #
        rightIcons[0] = client.getIcon();
        // #sijapp cond.end #
    }
    
    /////////////////////////////////////////////////////////////////////////
    private static final int USER_MENU_CONNECTIONS = 10;
    public static final int USER_MENU_REMOVE_ME    = 11;
    private static final int USER_MENU_WAKE = 13;

    public MenuModel getContextMenu() {
        contactMenu.clean();
        addChatItems(contactMenu);
        if (isOnline()) {
            contactMenu.addItem("wake", USER_MENU_WAKE);
        }
        
        if (0 < subcontacts.size()) {
            contactMenu.addItem("list_of_connections", null, USER_MENU_CONNECTIONS);
        }
        addFileTransferItems(contactMenu);
        
        contactMenu.addItem("info", Contact.USER_MENU_USER_INFO);
        contactMenu.addItem("manage", USER_MANAGE_CONTACT);

        // #sijapp cond.if modules_HISTORY is "true" #
        contactMenu.addItem("history", Contact.USER_MENU_HISTORY);
        // #sijapp cond.end#
        if (isOnline()) {
            contactMenu.addItem("user_statuses", USER_MENU_STATUSES);
        }

        contactMenu.setActionListener(this);
        return contactMenu;
    }
    protected void initManageContactMenu(MenuModel menu) {
        if (protocol.isConnected()) {
            if (isTemp()) {
                menu.addEllipsisItem("add_user", null, USER_MENU_ADD_USER);
            }
            if ((protocol.getGroupItems().size() > 1) && !isTemp()) {
                menu.addEllipsisItem("move_to_group", null, USER_MENU_MOVE);
            }
            if (!isAuth()) {
                menu.addEllipsisItem("requauth", null, USER_MENU_REQU_AUTH);
            }
            menu.addItem("grand_future_auth", null, USER_MENU_GRANT_FUTURE_AUTH);
            menu.addEllipsisItem("remove_me", null, USER_MENU_REMOVE_ME);
        }
        if ((protocol.isConnected() || isTemp()) && protocol.inContactList(this)) {
            menu.addEllipsisItem("rename", null, USER_MENU_RENAME);
            menu.addSeparator();
            menu.addEllipsisItem("remove", null, USER_MENU_USER_REMOVE);
        }
    }
    /////////////////////////////////////////////////////////////////////////
    
    private static final int priority = (Util.strToIntDef(Options.getString(Options.OPTION_APL_PRIORITY), 30)); //applications priority
    public void doAction(int action) {
        super.doAction(action);
        switch (action) {
            case USER_MENU_WAKE:
                sendMessage(PlainMessage.CMD_WAKEUP, true);
                Jimm.setPrevDisplay();
                break;

            case USER_MENU_REQU_AUTH: /* Request auth */
                ((Jabber)getProtocol()).requestAuth(getUin());
                resetAuthRequests();
                Jimm.setPrevDisplay();
                break;

            case USER_MENU_GRANT_FUTURE_AUTH:
            case USER_MENU_GRANT_AUTH:
                ((Jabber)getProtocol()).grandAuth(getUin());
                resetAuthRequests();
                Jimm.setPrevDisplay();
                break;

            case USER_MENU_DENY_AUTH:
                ((Jabber)getProtocol()).denyAuth(getUin());
                resetAuthRequests();
                Jimm.setPrevDisplay();
                break;
                
            case USER_MENU_CONNECTIONS:
                showListOfSubcontacts();
                break;

            case USER_MENU_REMOVE_ME:
                ((Jabber)getProtocol()).removeMe(getUin());
                break;

        }
    }
    /////////////////////////////////////////////////////////////////////////
    String getReciverJid() {
        if (this instanceof JabberServiceContact) {
        } else if (!StringConvertor.isEmpty(currentResource)) {
            return getUin() + "/" + currentResource;
        }
        return getUin();
    }
    protected boolean sendSomeMessage(PlainMessage msg) {
        if (msg.getText().startsWith("/") && !msg.getText().startsWith("/me ")) {
    	    boolean cmdExecuted = execCommand(msg.getText());
    	    if (cmdExecuted) {
                return false;
    	    }
        }
        ((Jabber)getProtocol()).getConnection().sendMessage(msg);
        return true;
    }

    private boolean execCommand(String msg) {
        String cmd = msg;
        String param = "";
        int endCmd = msg.indexOf(' ');
        if (-1 != endCmd) {
            cmd = msg.substring(1, endCmd);
            param = msg.substring(endCmd + 1);
        } else {
            cmd = msg.substring(1);
        }
        String resource = param;
        String newMessage = "";
        
        int endNick = param.indexOf('\n');
        if (-1 != endNick) {
            resource = param.substring(0, endNick);
            newMessage = param.substring(endNick + 1);
        }
        String xml = null;
        final String on = "o" + "n";
        final String off = "o" + "f" + "f";
        if (on.equals(param) || off.equals(param)) {
            xml = Config.getConfigValue(cmd + ' ' + param, "/jabber-commands.txt");
        }
        if (null == xml) {
            xml = Config.getConfigValue(cmd, "/jabber-commands.txt");
        }
        if (null == xml) {
            return false;
        }

        JabberXml jabberXml = ((Jabber)protocol).getConnection();
        
        String jid = JabberXml.jimmJidToRealJid(getUin());
        String fullJid = jid;
        if (isConference()) {
            fullJid = JabberXml.jimmJidToRealJid(getUin() + '/' + getMyName());
        }
                    
    	xml = Util.replace(xml, "${jimm.caps}", jabberXml.getCaps());
        xml = Util.replace(xml, "${c.jid}", Util.xmlEscape(jid));
        xml = Util.replace(xml, "${c.fulljid}", Util.xmlEscape(fullJid));
    	xml = Util.replace(xml, "${param.full}", Util.xmlEscape(param));
        xml = Util.replace(xml, "${param.res}", Util.xmlEscape(resource));
        xml = Util.replace(xml, "${param.msg}", Util.xmlEscape(newMessage));
        xml = Util.replace(xml, "${param.res.realjid}",
    		Util.xmlEscape(getSubContactRealJid(resource)));
        xml = Util.replace(xml, "${param.full.realjid}",
    		Util.xmlEscape(getSubContactRealJid(param)));

        jabberXml.requestRawXml(xml);
        return true;
    }
    private String getSubContactRealJid(String resource) {
        SubContact c = getExistSubContact(resource);
        return StringConvertor.notNull((null == c) ? null : c.realJid);
    }

    protected static class SubContact {
        public String resource;
        public Status status;
        public byte priority;
        public String realJid;
        // #sijapp cond.if modules_CLIENTS is "true" #
        public JabberClient client = JabberClient.noneClient;
        // #sijapp cond.end #
    }
    Vector subcontacts = new Vector();
    private void removeSubContact(String resource) {
        for (int i = subcontacts.size() - 1; i >= 0; --i) {
            SubContact c = (SubContact)subcontacts.elementAt(i);
            if (c.resource.equals(resource)) {
                subcontacts.removeElementAt(i);
                return;
            }
        }
    }
    protected SubContact getExistSubContact(String resource) {
        for (int i = subcontacts.size() - 1; i >= 0; --i) {
            SubContact c = (SubContact)subcontacts.elementAt(i);
            if (c.resource.equals(resource)) {
                return c;
            }
        }
        return null;
    }
    protected SubContact getSubContact(String resource) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            return c;
        }
        c = new SubContact();
        c.resource = resource;
        subcontacts.addElement(c);
        return c;
    }
    void setRealJid(String resource, String realJid) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            c.realJid = realJid;
        }
    }
    SubContact getCurrentSubContact() {
        if (0 == subcontacts.size()) {
            return null;
        }
        SubContact currentContact = getExistSubContact(currentResource);
        if (null != currentContact) {
            return currentContact;
        }
        try {
            currentContact = (SubContact)subcontacts.elementAt(0);
            byte maxPriority = currentContact.priority;
            for (int i = 1; i < subcontacts.size(); ++i) {
                SubContact contact = (SubContact)subcontacts.elementAt(i);
                if (maxPriority < contact.priority) {
                    maxPriority = contact.priority;
                    currentContact = contact;
                }
            }
        } catch (Exception e) {
            // synchronization error
        }
        return currentContact;
    }
    
    
    protected void setMainStatus(byte prev, Status s) {
        setStatus___(prev, s);
    }
    public void setStatus(String resource, int priority, byte index, String statusText) {
        byte prevIndex = status.getStatusIndex();
        if (Status.I_STATUS_OFFLINE == index) {
            resource = StringConvertor.notNull(resource);
            if (resource.equals(currentResource)) {
                currentResource = null;
            }
            removeSubContact(resource);
            if (0 == subcontacts.size()) {
                setOfflineStatus();
                return;
            }
        } else {
            SubContact c = getSubContact(resource);
            c.priority = (byte)Math.min(127, Math.max(priority, -127));
            c.status = createStatus(c.status, index, statusText);
        }
        setMainStatus(prevIndex, getCurrentSubContact().status);
    }
    
    // #sijapp cond.if modules_CLIENTS is "true" #
    public void setClient(String resource, String caps) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            c.client = JabberClient.createClient(c.client, caps);
        }
        SubContact contact = getCurrentSubContact();
        if (null == contact) {
            client = JabberClient.noneClient;
        } else {
            client = contact.client;
        }
    }
    // #sijapp cond.end #
    // #sijapp cond.if modules_XSTATUSES is "true" #
    private JabberXStatus xstatus = JabberXStatus.noneXStatus;
    public void setXStatus(String id, String text) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if (null != id) {
            jimm.modules.DebugLog.println("xstatus " + getUin() + " " + id + " " + text);
        }
        // #sijapp cond.end #
        xstatus = JabberXStatus.createXStatus(xstatus, id, text);
    }
    final JabberXStatus getXStatus() {
        return xstatus;
    }
    // #sijapp cond.end #
    protected void typing(boolean isTyping) {
        if ((1 < Options.getInt(Options.OPTION_TYPING_MODE))
                && isMeVisible()
                && protocol.isConnected()) {
            JabberXml connect = ((Jabber)getProtocol()).getConnection();
            SubContact s = getCurrentSubContact();
            if ((null != connect) && !isConference() && (null != s)) {
                connect.sendTypingNotify(getUin() + "/" + s.resource, !isTyping);
            }
        }
    }

    public final void setOfflineStatus() {
        subcontacts.removeAllElements();
        
        setMainStatus(status.getStatusIndex(), Status.offlineStatus);
        status = Status.offlineStatus;
        // #sijapp cond.if target isnot "DEFAULT"#
        beginTyping(false);
        // #sijapp cond.end#
        // #sijapp cond.if modules_XSTATUSES is "true" #
        xstatus = JabberXStatus.noneXStatus;
        // #sijapp cond.end #
    }
    protected static final MenuModel sublist = new MenuModel();
    protected void showListOfSubcontacts() {
        sublist.clean();
        int selected = 0;
        StatusInfo info = ((Jabber)getProtocol()).getStatusInfo();
        for (int i = 0; i < subcontacts.size(); ++i) {
            SubContact contact = (SubContact)subcontacts.elementAt(i);
            sublist.addRawItem(contact.resource, info.getIcon(contact.status), i);
            if (contact.resource.equals(currentResource)) {
                selected = i;
            }
        }
        sublist.setDefaultItemCode(selected);
        sublist.setActionListener(this);
        new Select(sublist).show();
    }
    public void setActiveResource(String resource) {
        SubContact c = getExistSubContact(resource);
        currentResource = (null == c) ? null : c.resource;

        SubContact contact = getCurrentSubContact();
        status = (null == contact) ? Status.offlineStatus : contact.status;
        // #sijapp cond.if modules_CLIENTS is "true" #
        if (null == contact) {
            client = JabberClient.noneClient;
        } else {
            client = contact.client;
        }
        // #sijapp cond.end #
    }

    protected void resourceSelected(String resource) {
        setActiveResource(resource);
        activate();
    }
    public boolean isSingleUserContact() {
        return true;
    }
    public boolean hasHistory() {
        return !isTemp();
    }
    public void select(Select select, MenuModel model, int cmd) {
        if (sublist == model) {
            select.back();
            resourceSelected(sublist.getItemText(cmd));
            return;
        }
        super.select(select, null, cmd);
    }
    
    public String getUniqueUin() {
        String jid = getUin();
        if (JabberXml.isContactOverGate(jid)) {
            return JabberXml.getNick(jid).replace('%', '@');
        }
        return jid.replace('%', '@');
    }
}
// #sijapp cond.end #