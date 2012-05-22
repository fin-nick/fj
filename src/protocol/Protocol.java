/*
 * Protocol.java
 *
 * Created on 13 Май 2008 г., 12:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import DrawControls.icons.*;
import java.io.*;
import java.util.Vector;
import javax.microedition.lcdui.TextBox;
import javax.microedition.rms.*;
import jimm.*;
import jimm.chat.ChatHistory;
import jimm.chat.message.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.modules.*;
import jimm.search.*;
import jimm.ui.*;
import jimm.util.ResourceBundle;
// #sijapp cond.if modules_TRAFFIC is "true" #
import jimm.modules.traffic.Traffic;
// #sijapp cond.end#
/**
 *
 * @author vladimir
 */
abstract public class Protocol {
    protected Vector contacts = new Vector();
    protected Vector groups = new Vector();
    private Profile account;
    private String password;
    private String uin = "";
    protected StatusInfo info;
    
    private final String getContactListRS() {
        String rms = "cl-" + getUserId();
        return (32 < rms.length()) ? rms.substring(0, 32) : rms;
    }
    
    public final String getUserId() {
        return uin;
    }
    public boolean isEmpty() {
        return StringConvertor.isEmpty(uin);
    }
    
    public final String getNick() {
        String nick = account.nick;
        return (nick.length() == 0) ? ResourceBundle.getString("me") : nick;
    }

    public final Profile getProfile() {
        return account;
    }
    
    public final String getPassword() {
        return (null == password) ? account.password : password;
    }
    public final void setPassword(String pass) {
        password = pass;
    }
    
    public final void setAccount(Profile account) {
        this.account = account;
        String rawUin = account.userId;
        // #sijapp cond.if modules_MULTI is "true" #
        String domain = null;
        switch (account.protocolType) {
            case Profile.PROTOCOL_GTALK:    domain = "@gmail.com"; break;
            case Profile.PROTOCOL_FACEBOOK: domain = "@chat.facebook.com"; break;
            case Profile.PROTOCOL_LJ:       domain = "@livejournal.com"; break;
            case Profile.PROTOCOL_YANDEX:   domain = "@ya.ru"; break;
            case Profile.PROTOCOL_VK:       domain = "@vk.com"; break;
            case Profile.PROTOCOL_QIP:      domain = "@qip.ru"; break;
            case Profile.PROTOCOL_OVI:      domain = "@ovi.com"; break;
        }
        if ((Profile.PROTOCOL_VK == account.protocolType) && (1 < rawUin.length())) {
            if (Character.isDigit(rawUin.charAt(0))) {
                rawUin = "id" + rawUin;
                account.userId = rawUin;
            }
        }
        if ((null != domain) && !StringConvertor.isEmpty(rawUin)) {
            if (-1 == rawUin.indexOf('@')) {
                rawUin += domain;
            }
        }
        // #sijapp cond.end #
        uin = StringConvertor.isEmpty(rawUin) ? "" : processUin(rawUin);
        if (!StringConvertor.isEmpty(account.password)) {
            setPassword(null);
        }
        
    }
    protected String processUin(String uin) {
        return uin;
    }
    /** Creates a new instance of Protocol */
    public Protocol() {
    }
    public final void ui_changeContactStatus(Contact contact) {
        getContactList().getManager().updateContactStatus(contact);
    }
    public final void ui_updateContact(Contact contact) {
        getContactList().getManager().updateContact(contact);
    }
    public final void ui_setActiveContact(Contact contact) {
        getContactList().getManager().setActiveContact(contact);
    }
    
    public Icon getCurrentStatusIcon() {
        if (isConnected() && !isConnecting()) {
            return getStatusInfo().getIcon(status);
        }
        return getStatusInfo().getIcon(Status.offlineStatus);
    }
    
    public void setContactListStub() {
        contacts = new Vector();
    }
    public void setContactList(Vector groups, Vector contacts) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if ((contacts.size() > 0) && !(contacts.elementAt(0) instanceof Contact)) {
            DebugLog.panic("contacts is not list of Contact");
            contacts = new Vector();
        }
        if ((groups.size() > 0) && !(groups.elementAt(0) instanceof Group)) {
            DebugLog.panic("groups is not list of Group");
            groups = new Vector();
        }
        // #sijapp cond.end #
        this.contacts = contacts;
        this.groups = groups;
        ChatHistory.instance.restoreContactsWithChat(this);

        for (int i = 0; i < groups.size(); ++i) {
            ((Group)groups.elementAt(i)).updateContacts();
        }
        
        getContactList().update(this);
    }
    // #sijapp cond.if protocols_JABBER is "true" #
    public void setContactListAddition(Group group) {
        group.updateContacts();
        group.updateGroupData();
        getContactList().update(this);
    }
    // #sijapp cond.end#
    
    /* ********************************************************************* */
    // #sijapp cond.if modules_MULTI is "true"#
    // #sijapp cond.end#
    private byte progress = 100;
    public final void setConnectingProgress(int percent) {
        if (percent < 0) {
            percent = 100;
        }
        this.progress = (byte)percent;
        // #sijapp cond.if modules_MULTI is "true"#
        getContactList().getManager().invalidate();
        // #sijapp cond.else#
        Progress progress = Progress.getProgress();
        if (0 == percent) {
            progress.init(this);
            SplashCanvas.setStatusToDraw(getStatusInfo().getIcon(status));
        }
        progress.setProgress(percent);
        // #sijapp cond.end#
    }
    public final boolean isConnecting() {
        return 100 != progress;
    }
    public final byte getConnectingProgress() {
        return progress;
    }
    /* ********************************************************************* */
    // #sijapp cond.if modules_FILES is "true"#  
    public void sendFile(FileTransfer transfer, String filename, String description) {
    }
    // #sijapp cond.end#
    public void getAvatar(UserInfo userInfo) {
    }
    /* ********************************************************************* */
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    private byte privateStatus = 0;
    protected abstract void s_setPrivateStatus();
    public void setPrivateStatus(byte status) {
        privateStatus = status;
        if (isConnected()) {
            s_setPrivateStatus();
        }
    }
    public byte getPrivateStatus() {
        return privateStatus;
    }
    // #sijapp cond.end #
    /* ********************************************************************* */
    public final void safeLoad() {
        if ("".equals(getUserId())) {
            return;
        }
        if (isConnected()) {
            return;
        }
        try {
            // Check whether record store exists
            String[] recordStores = RecordStore.listRecordStores();
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            if (null == recordStores) {
                recordStores = new String[0];
            }
            // #sijapp cond.end #
            String rmsName = getContactListRS();
            boolean exist = false;
            for (int i = 0; i < recordStores.length; ++i) {
                if (rmsName.equals(recordStores[i])) {
                    exist = true;
                    break;
                }
            }
            if (exist) {
                load();
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("roster load", e);
            // #sijapp cond.end #
            setContactList(new Vector(), new Vector());
        }
    }
    
    public final void safeSave() {
        // Try to delete the record store
        if ("".equals(getUserId())) {
            return;
        }
        synchronized (this) {
            try {
                RecordStore.deleteRecordStore(getContactListRS());
            } catch (Exception e) {
                // Do nothing
            }
            
            RecordStore cl = null;
            try {
                // Create new record store
                cl = RecordStore.openRecordStore(getContactListRS(), true);
                save(cl);
            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.panic("roster save", e);
                // #sijapp cond.end #
            }
            try {
                // Close record store
                cl.closeRecordStore();
            } catch (Exception e) {
                // Do nothing
            }
        }
    }
    // Tries to load contact list from record store
    private void load() throws Exception {
        // Initialize vectors
        Vector cItems = new Vector();
        Vector gItems = new Vector();
        
        // Open record store
        RecordStore cl = RecordStore.openRecordStore(getContactListRS(), false);
        try {
            // Temporary variables
            byte[] buf;
            ByteArrayInputStream bais;
            DataInputStream dis;
            
            // Get version info from record store
            buf = cl.getRecord(1);
            bais = new ByteArrayInputStream(buf);
            dis = new DataInputStream(bais);
            if (!dis.readUTF().equals(Jimm.VERSION)) {
                throw new Exception();
            }
            
            // Get version ids from the record store
            loadProtocolData(cl.getRecord(2));
            
            // Read all remaining items from the record store
            for (int marker = 3; marker <= cl.getNumRecords(); ++marker) {
                try {
                    buf = cl.getRecord(marker);
                    if ((null == buf) || (0 == buf.length)) {
                        continue;
                    }
                    
                    bais = new ByteArrayInputStream(buf);
                    dis = new DataInputStream(bais);
                    // Loop until no more items are available
                    while (0 < dis.available()) {
                        // Get type of the next item
                        byte type = dis.readByte();
                        switch (type) {
                            case 0:
                                cItems.addElement(loadContact(dis));
                                break;
                            case 1:
                                gItems.addElement(loadGroup(dis));
                                break;
                        }
                    }
                } catch (EOFException e) {
                }
            }
            // #sijapp cond.if modules_DEBUGLOG is "true"#
            DebugLog.memoryUsage("clload");
            // #sijapp cond.end#
        } finally {
            // Close record store
            cl.closeRecordStore();
        }
        setContactList(gItems, cItems);
    }
    protected Contact loadContact(DataInputStream dis) throws Exception {
        String uin = dis.readUTF();
        String name = dis.readUTF();
        int groupId = dis.readInt();
        byte booleanValues = dis.readByte();
        Contact contact = createContact(uin, name);
        contact.setGroupId(groupId);
        contact.setBooleanValues(booleanValues);
        return contact;
    }
    protected Group loadGroup(DataInputStream dis) throws Exception {
        int groupId = dis.readInt();
        String name = dis.readUTF();
        Group group = createGroup(name);
        group.setGroupId(groupId);
        group.setExpandFlag(dis.readBoolean());
        return group;
    }
    protected void loadProtocolData(byte[] data) throws Exception {
    }
    protected byte[] saveProtocolData() throws Exception {
        return new byte[0];
    }
    
    // Save contact list to record store
    private void save(RecordStore cl) throws Exception {
        // Temporary variables
        ByteArrayOutputStream baos;
        DataOutputStream dos;
        byte[] buf;
        
        // Add version info to record store
        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);
        dos.writeUTF(Jimm.VERSION);
        buf = baos.toByteArray();
        cl.addRecord(buf, 0, buf.length);
        
        // Add version ids to the record store
        baos.reset();
        buf = saveProtocolData();
        cl.addRecord(buf, 0, buf.length);
        
        // Initialize buffer
        baos.reset();
        
        // Iterate through all contact items
        int cItemsCount = contacts.size();
        int totalCount  = cItemsCount + groups.size();
        for (int i = 0; i < totalCount; ++i) {
            if (i < cItemsCount) {
                dos.writeByte(0);
                saveContact(dos, (Contact)contacts.elementAt(i));
            } else {
                dos.writeByte(1);
                saveGroup(dos, (Group)groups.elementAt(i - cItemsCount));
            }
            
            // Start new record if it exceeds 4000 bytes
            if ((baos.size() >= 4000) || (i == totalCount - 1)) {
                // Save record
                buf = baos.toByteArray();
                cl.addRecord(buf, 0, buf.length);
                
                // Initialize buffer
                baos.reset();
            }
        }
    }
    protected void saveContact(DataOutputStream out, Contact contact) throws Exception {
        out.writeUTF(contact.getUin());
        out.writeUTF(contact.getName());
        out.writeInt(contact.getGroupId());
        out.writeByte(contact.getBooleanValues());
    }
    protected void saveGroup(DataOutputStream out, Group group) throws Exception {
        out.writeInt(group.getId());
        out.writeUTF(group.getName());
        out.writeBoolean(group.isExpanded());
    }
    
    /* ********************************************************************* */
    
    protected void s_removeContact(Contact contact) {};
    protected void s_removedContact(Contact contact) {};
    public void removeContact(Contact contact) {
        // Check whether contact item is temporary
        boolean exec = false;
        if (contact.isTemp()) {
        } else if (isConnected()) {
            // Request contact item removal
            exec = true;
            s_removeContact(contact);
        } else {
            return;
        }
        cl_removeContact(contact);
        if (exec) {
            s_removedContact(contact);
        }
    }
    
    abstract protected void s_renameContact(Contact contact, String name);
    public void renameContact(Contact contact, String name) {
        if (StringConvertor.isEmpty(name)) {
            return;
        }
        if (!inContactList(contact)) {
            contact.setName(name);
            return;
        }
        if (contact.isTemp()) {
        } else if (isConnected()) {
            s_renameContact(contact, name);
        } else {
            return;
        }
        contact.setName(name);
        cl_renameContact(contact);
    }
    abstract protected void s_removeGroup(Group group);
    public void removeGroup(Group group) {
        s_removeGroup(group);
        cl_removeGroup(group);
    }
    abstract protected void s_renameGroup(Group group, String name);
    public void renameGroup(Group group, String name) {
        s_renameGroup(group, name);
        group.setName(name);
        cl_renameGroup(group);
    }
    
    abstract protected void s_moveContact(Contact contact, Group to);
    public void moveContactTo(Contact contact, Group to) {
        s_moveContact(contact, to);
        cl_moveContact(contact, to);
    }
    protected void s_addContact(Contact contact) {};
    protected void s_addedContact(Contact contact) {}
    public void addContact(Contact contact) {
        s_addContact(contact);
        contact.setTempFlag(false);
        cl_addContact(contact);
        safeSave();
        s_addedContact(contact);
    }
    
    private void removeFromAnyGroup(Contact c) {
        Vector groups = getGroupItems();
        for (int i = 0; i < groups.size(); ++i) {
            if (((Group)groups.elementAt(i)).removeContact(c)) {
                return;
            }
        }
    }
    public void addTempContact(Contact contact) {
        cl_addContact(contact);
    }
    public void addLocalContact(Contact contact) {
        if (null == contact) {
            return;
        }
        if (!inContactList(contact)) {
            contacts.addElement(contact);
        }
        Group g = contact.getGroup();
        
        if (null != g) {
            if (-1 == Util.getIndex(g.getContacts(), contact)) {
                removeFromAnyGroup(contact);
                g.addContact(contact);
            }
        } else {
            removeFromAnyGroup(contact);
        }
        getContactList().getManager().addLocalContact(contact);
    }
    
    abstract protected void s_addGroup(Group group);
    public void addGroup(Group group) {
        s_addGroup(group);
        cl_addGroup(group);
    }
    
    abstract public boolean isConnected();
    abstract protected void startConnection();
    abstract protected void closeConnection();
    protected void userCloseConnection() {
    }
    private void disconnect(boolean user) {
        setConnectingProgress(-1);
        closeConnection();
        if (user) {
            userCloseConnection();
        }
        /* Reset all contacts oconnectionffine */ 
        setStatusesOffline();
        /* Disconnect */

        // #sijapp cond.if modules_TRAFFIC is "true" #
        Traffic.getInstance().safeSave();
        // #sijapp cond.end#
        getContactList().update(this);
    }
    public final void disconnect() {
        disconnect(true);
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.println("disconnect");
        // #sijapp cond.end #
    }
    
    abstract public Group createGroup(String name);
    abstract protected Contact createContact(String uin, String name);
    public Contact createTempContact(String uin, String name) {
        Contact contact = getItemByUIN(uin);
        if (null != contact) {
            return contact;
        }
        contact = createContact(uin, name);
        if (null != contact) {
            contact.setTempFlag(true);
            if (null == contact.getStatus()) {
                contact.setOfflineStatus();
            }
        }
        return contact;
    }
    public Contact createTempContact(String uin) {
        return createTempContact(uin, uin);
    }
    
    abstract protected void s_searchUsers(Search cont);
    public void searchUsers(Search cont) {
        s_searchUsers(cont);
    }
    
    public Vector getContactItems() {
        return contacts;
    }
    public Vector getGroupItems() {
        return groups;
    }
    // #sijapp cond.if modules_SOUND is "true" #
    public final void beginTyping(String uin, boolean type) {
        Contact item = getItemByUIN(uin);
        if ((null == item) && type) {
            // #sijapp cond.if modules_ANTISPAM is "true" #
            if (Options.getBoolean(Options.OPTION_ANTISPAM_ENABLE)) {
                return;
            }
            // #sijapp cond.end #
            item = createTempContact(uin);
            addTempContact(item);
        }
        beginTyping(item, type);
    }
    private final void beginTyping(Contact item, boolean type) {
        if (null == item) {
            return;
        }
        if (item.isTyping() != type) {
            item.beginTyping(type);
            item.setStatusImage();
            if (item.hasChat()) {
                item.getChat().beginTyping(type);
            }
            getContactList().beginTyping(item, type);
        }
    }
    // #sijapp cond.end #
    
    protected void setStatusesOffline() {
        for (int i = contacts.size() - 1; i >= 0; --i) {
            Contact c = (Contact)contacts.elementAt(i);
            c.setOfflineStatus();
            c.status = Status.offlineStatus;
        }
        if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
            for (int i = groups.size() - 1; i >= 0; --i) {
                ((Group)groups.elementAt(i)).updateGroupData();
            }
        }
    }
    
    // Returns number of unread messages
    public int getUnreadMessCount() {
        int result = 0;
        for (int i = contacts.size() - 1; i >= 0; --i) {
            Contact contact = (Contact)contacts.elementAt(i);
            result += contact.getUnreadMessageCount();
        }
        return result;
    }
    
    // Returns number of unread messages
    public int getOnlineCount() {
        int result = 0;
        for (int i = contacts.size() - 1; i >= 0; --i) {
            Contact contact = (Contact)contacts.elementAt(i);
            if (contact.isOnline()) {
                result++;
            }
        }
        return result;
    }
    
    public Contact getItemByUIN(String uin) {
        for (int i = contacts.size() - 1; i >= 0; --i) {
            Contact contact = (Contact)contacts.elementAt(i);
            if (contact.getUin().equals(uin)) {
                return contact;
            }
        }
        return null;
    }
    public Group getGroupById(int id) {
        for (int i = groups.size() - 1; 0 <= i; --i) {
            Group group = (Group)groups.elementAt(i);
            if (group.getId() == id) {
                return group;
            }
        }
        return null;
    }
    
    public Group getGroup(String name) {
        for (int i = groups.size() - 1; 0 <= i; --i) {
            Group group = (Group)groups.elementAt(i);
            if (group.getName().equals(name)) {
                return group;
            }
        }
        return null;
    }
    
    public final ContactList getContactList() {
        return ContactList.getInstance();
    }
    
    public boolean inContactList(Contact contact) {
        return -1 != Util.getIndex(contacts, contact);
    }
    protected Status status = new Status();
    private long lastStatusChangeTime;
    
    public final Status getStatus() {
        return status;
    }
    public final StatusInfo getStatusInfo() {
        return info;
    }
    
    protected abstract void s_updateOnlineStatus();
    public final void setOnlineStatus(int statusIndex, String msg) {
        account.statusIndex = (byte)statusIndex;
        account.statusMessage = msg;
        Options.saveAccount(account);
        
        setLastStatusChangeTime();
        status.setStatusIndex((byte)statusIndex);
        if (isConnected()) {
            s_updateOnlineStatus();
        }
    }
    public final void init() {
        setLastStatusChangeTime();
        status.setStatusIndex(account.statusIndex);
    }
    
    private void cl_addContact(Contact contact) {
        if (null == contact) {
            return;
        }
        removeFromAnyGroup(contact);
        Group g = contact.getGroup();
        if (null != g) {
            g.addContact(contact);
        }
        if (!inContactList(contact)) {
            contacts.addElement(contact);
        }
        getContactList().getManager().addContact(contact);
    }
    private void cl_renameContact(Contact contact) {
        safeSave();
        ui_setActiveContact(contact);
    }
    private void cl_moveContact(Contact contact, Group to) {
        removeFromAnyGroup(contact);
        contact.setGroup(to);
        if (null != to) {
            to.addContact(contact);
        }
        safeSave();
        getContactList().getManager().moveContact(contact);
    }
    private void cl_removeContact(Contact contact) {
        if (!inContactList(contact)) {
            return;
        }
        contacts.removeElement(contact);
        removeFromAnyGroup(contact);
        getContactList().getManager().removeContact(contact);
        contact.dismiss();
        safeSave();
    }
    private void cl_addGroup(Group group) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if (groups.contains(group)) {
            DebugLog.panic("Group '" + group.getName() + "' already added");
        }
        // #sijapp cond.end #
        groups.addElement(group);
        getContactList().getManager().addGroup(group);
        safeSave();
    }
    private void cl_renameGroup(Group group) {
        safeSave();
    }
    private void cl_removeGroup(Group group) {
        groups.removeElement(group);
        getContactList().getManager().removeGroup(group);
        safeSave();
    }
    
    public void removeLocalContact(Contact contact) {
        cl_removeContact(contact);
    }
    
    public long getLastStatusChangeTime() {
        return lastStatusChangeTime;
    }
    private void setLastStatusChangeTime() {
        lastStatusChangeTime = System.currentTimeMillis() / 1000;
    }
    
    private boolean isEmptyMessage(String text) {
        for (int i = 0; i < text.length(); ++i) {
            if (' ' < text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public void addMessage(Message message) {
        Contact cItem = (Contact)getItemByUIN(message.getSndrUin());
        // #sijapp cond.if modules_ANTISPAM is "true" #
        if ((null == cItem) && AntiSpam.isSpam(this, message)) {
            return;
        }
        // #sijapp cond.end #
        
        // Add message to contact
        if (null == cItem) {
            // Create a temporary contact entry if no contact entry could be found
            // do we have a new temp contact
            cItem = createTempContact(message.getSndrUin());
            addTempContact(cItem);
        }
        if (null == cItem) {
            return;
        }
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        if (cItem.inIgnoreList()) {
            return;
        }
        // #sijapp cond.end #
        // #sijapp cond.if modules_SOUND is "true" #
        beginTyping(cItem, false);
        // #sijapp cond.end #
        boolean isPlain = (message instanceof PlainMessage);
        if (isPlain && isEmptyMessage(message.getText())) {
            return;
        }
        addMessageToChat(cItem, message);
    }
    private void addMessageToChat(Contact contact, Message message) {
        // #sijapp cond.if protocols_MRIM is "true" #
        if (contact instanceof protocol.mrim.MrimPhoneContact) {
            // #sijapp cond.if target is "MIDP2" #
            Jimm.setMinimized(false);
            // #sijapp cond.end #
            contact.showPopupWindow(contact.getName(), message.getText());
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            MagicEye.addAction(contact.getProtocol(), contact.getUin(),
                    "SMS", message.getText());
            // #sijapp cond.end #
            return;
        }
        // #sijapp cond.end #
        // Adds a message to the message display
        contact.getChat().addMessage(message);
        ChatHistory.instance.registerChat(contact);
        ChatHistory.instance.updateChatList();
        getContactList().addMessage(contact, message);
    }
    
    public final void setAuthResult(String uin, boolean auth) {
        Contact c = getItemByUIN(uin);
        if (null == c) {
            return;
        }
        if (auth == c.isAuth()) {
            return;
        }
        c.setBooleanValue(Contact.CONTACT_NO_AUTH, !auth);
        if (!auth) {
            c.setOfflineStatus();
        }
        ui_changeContactStatus(c);
    }
    
    
    private int reconnect_attempts;
    public final void connect() {
        reconnect_attempts = Options.getInt(Options.OPTION_RECONNECT_NUMBER);
        //setStatusesOffline();
        disconnect(false);
        startConnection();
        setLastStatusChangeTime();
    }
    // Puts the comm. subsystem into STATE_CONNECTED
    public final void setConnected() {
        reconnect_attempts = Options.getInt(Options.OPTION_RECONNECT_NUMBER);
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_CONNECT);
        // #sijapp cond.end #
    }
    
    public final void processException(JimmException e) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.println("process exception: " + e.getMessage());
        // #sijapp cond.end #
        if (e.isReconnectable()) {
            reconnect_attempts--;
            if (0 < reconnect_attempts) {
                int err = e.getErrCode();
                if (121 ==  err || 118 == err || 100 == err) {
                    try {
                        int iter = Options.getInt(Options.OPTION_RECONNECT_NUMBER) - reconnect_attempts;
                        int sleep = Math.min(iter * 10, 2 * 60);
                        Thread.sleep(sleep * 1000);
                    } catch (Exception ex) {
                    }
                }
                disconnect(false);
                Options.nextServer();
                startConnection();
                return;
            }
        }
        // Critical exception
        if (e.isCritical()) {
            // Reset comm. subsystem
            // #sijapp cond.if modules_FILES is "true"#
            if (!e.isPeer()) {
                disconnect(false);
            }
            // #sijapp cond.else#
            disconnect(false);
            // #sijapp cond.end#
        }
        JimmException.handleException(e);
    }
    
    /**
     *  Release all resources used by the protocol.
     */
    public void dismiss() {
        disconnect();
        for (int i = contacts.size() - 1; i >= 0; --i) {
            ((Contact)contacts.elementAt(i)).dismiss();
        }
        Vector gItems = getGroupItems();
        for (int i = groups.size() - 1; i >= 0; --i) {
            ((Group)groups.elementAt(i)).dismiss();
        }
        account = null;
        contacts = null;
        groups = null;
    }
    
    public String getUinName() {
        return "UIN";
    }
    
    public void autoDenyAuth(String uin) {
    }
    
    public abstract void saveUserInfo(UserInfo info);

    public abstract byte[] getStatusList();
}
