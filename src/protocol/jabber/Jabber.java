/*
 * Jabber.java
 *
 * Created on 12 Июль 2008 г., 19:05
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import DrawControls.icons.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Vector;
import jimm.*;
import jimm.comm.*;
import jimm.search.*;
import jimm.util.ResourceBundle;
import protocol.*;

/**
 *
 * @author Vladimir Krukov
 */
public class Jabber extends Protocol {
    private JabberXml connection;
    private Vector rejoinList = new Vector();
    private static final Hashtable statusIcons = new Hashtable ();
    private ImageList createStatusIcons(byte type) {
        String file = "jabber";
        // #sijapp cond.if modules_MULTI is "true" #
        switch (type) {
            case Profile.PROTOCOL_GTALK:    file = "gtalk"; break;
            case Profile.PROTOCOL_FACEBOOK: file = "facebook"; break;
            case Profile.PROTOCOL_LJ:       file = "livejournal"; break;
            case Profile.PROTOCOL_YANDEX:   file = "ya"; break;
            case Profile.PROTOCOL_VK:       file = "vk"; break;
            case Profile.PROTOCOL_QIP:      file = "qip"; break;
            case Profile.PROTOCOL_OVI:      file = "ovi"; break;
        }
        // #sijapp cond.end #
        ImageList icons = (ImageList)statusIcons.get(file);
        if (null != icons) {
            return icons;
        }
        // #sijapp cond.if modules_MULTI is "true" #
        if (null != file) {
            icons = ImageList.createImageList("/" + file + "-status.png");
            if (0 < icons.size()) {
                statusIcons.put(file, icons);
                return icons;
            }
        }
        // #sijapp cond.end #
        icons = ImageList.createImageList("/jabber-status.png");
        statusIcons.put(file, icons);
        return icons;
    }
    
    public void addRejoin(String jid) {
        if (!rejoinList.contains(jid)) {
            rejoinList.addElement(jid);
        }
    }
    public void removeRejoin(String jid) {
        rejoinList.removeElement(jid);
    }
    public void rejoin() {
        for (int i = 0; i < rejoinList.size(); ++i) {
            String jid = (String)rejoinList.elementAt(i);
            JabberServiceContact conf = (JabberServiceContact)getItemByUIN(jid);
            if ((null != conf) && !conf.isOnline()) {
                conf.join();
            }
        }
    }
    public boolean isReconnect() {
        return 0 < rejoinList.size();
    }
    
    JabberXml getConnection() {
        return connection;
    }
    /** Creates a new instance of Jabber */
    public Jabber(byte type) {
        ImageList icons = createStatusIcons(type);
        int[] statusWidth = new int[]{5, 1, 2, 0, 3, 4};
        int[] statusIconIndex = new int[]{1, 0, 3, 4, 6, 5};
        if (icons.size() < 7) {
            statusIconIndex[4] = 5;
        }
        String[] names = new String[]{"status_offline", "status_online", "status_away", "status_chat", "status_na", "status_dnd"};
        info = new StatusInfo(icons, statusWidth, statusIconIndex, names);
    }
    public boolean isEmpty() {
        return super.isEmpty() || (getUserId().indexOf('@') <= 0);
    }

    public boolean isConnected() {
        return (null != connection) && connection.isConnected();
    }

    protected void startConnection() {
        connection = new JabberXml(this);
        connection.start();
    }

    public boolean hasS2S() {
        // #sijapp cond.if modules_MULTI is "true" #
        switch (getProfile().protocolType) {
            case Profile.PROTOCOL_FACEBOOK:
            case Profile.PROTOCOL_VK:
                return false;
        }
        // #sijapp cond.end #
        return true;
    }
    public boolean hasVCardEditor() {
        // #sijapp cond.if modules_MULTI is "true" #
        switch (getProfile().protocolType) {
            case Profile.PROTOCOL_FACEBOOK:
            case Profile.PROTOCOL_YANDEX:
            case Profile.PROTOCOL_VK:
                return false;
        }
        // #sijapp cond.end #
        return true;
    }
    protected void userCloseConnection() {
        rejoinList.removeAllElements();
    }

    protected void closeConnection() {
        JabberXml c = connection;
        connection = null;
        if (null != c) {
            c.disconnect();
        }
    }

    private int getNextGroupId() {
        while (true) {
            int id = Util.nextRandInt() % 0x1000;
            for (int i = groups.size() - 1; i >= 0; --i) {
                Group group = (Group)groups.elementAt(i);
                if (group.getId() == id) {
                    id = -1;
                    break;
                }
            }
            if (0 <= id) {
                return id;
            }
        }
    }
    public Group createGroup(String name) {
        Group group = new JabberGroup(this, name, getNextGroupId());
        int mode = Group.MODE_FULL_ACCESS;
        if (JabberGroup.CONFERENCE_GROUP.equals(name)) {
            mode &= ~Group.MODE_EDITABLE;
            mode |= Group.MODE_TOP;
        } else if (JabberGroup.GATE_GROUP.equals(name)) {
            mode &= ~Group.MODE_EDITABLE;
            mode |= Group.MODE_BOTTOM;
        }
        group.setMode(mode);
        return group;
    }
    /**
     * Create or get group.
     *
     * WARNING! This method adds new group to list of group.
     */
    public Group getOrCreateGroup(String groupName) {
        if (StringConvertor.isEmpty(groupName)) {
            return null;
        }
        Group group = getGroup(groupName);
        if (null == group) {
            group = createGroup(groupName);
            addGroup(group);
        }
        return group;
    }

    public Contact createContact(String uin, String name) {
        name = (null == name) ? uin : name;
        uin = JabberXml.realJidToJimmJid(uin);
        
        boolean isGate = (-1 == uin.indexOf('@'));
        boolean isConference = JabberXml.isConference(uin);
        if (isGate || isConference) {
            JabberServiceContact c = new JabberServiceContact(this, uin, name);
            if (c.isConference() && isConnected()) {
                c.setGroup(getOrCreateGroup(c.getDefaultGroupName()));
            }
            return c;
        }
        
        return new JabberContact(this, uin, name);
    }
    
    protected void s_searchUsers(Search cont) {
        // FIXME
        UserInfo info = new UserInfo(this);
        info.uin = cont.getSearchParam(Search.UIN);
        if (null != info.uin) {
            cont.addResult(info);
        }
        cont.finished();
    }
    public final static int PRIORITY = (Util.strToIntDef(Options.getString(Options.OPTION_APL_PRIORITY), 30)); //applications priority
    protected void s_updateOnlineStatus() {
        // FIXME
        Status s = status;
        connection.setStatus(s.getStatusIndex(), "", PRIORITY);
    }

    protected void s_addedContact(Contact contact) {
        connection.updateContact((JabberContact)contact);
    }

    protected void s_addGroup(Group group) {
    }

    protected void s_removeGroup(Group group) {
    }

    protected void s_removedContact(Contact contact) {
        boolean unregister = JabberXml.isGate(getUserId()) 
                && !JabberXml.getDomain(getUserId()).equals(contact.getUin());
        if (unregister) {
           getConnection().unregister(contact.getUin());
        }
        connection.removeContact(contact.getUin());
        if (unregister) {
           getConnection().removeGateContacts(contact.getUin());
        }
    }

    protected void s_renameGroup(Group group, String name) {
        group.setName(name);
        connection.updateContacts(contacts);
    }

    protected void s_moveContact(Contact contact, Group to) {
        contact.setGroup(to);
        connection.updateContact((JabberContact)contact);
    }

    protected void s_renameContact(Contact contact, String name) {
        contact.setName(name);
        connection.updateContact((JabberContact)contact);
    }
    public void grandAuth(String uin) {
        connection.sendSubscribed(uin);
    }
    public void denyAuth(String uin) {
        connection.sendUnsubscribed(uin);
    }
    public void autoDenyAuth(String uin) {
        denyAuth(uin);
    }
    public void requestAuth(String uin) {
        connection.requestSubscribe(uin);
    }
    
    private String getDefaultServer(String domain) {
        // #sijapp cond.if modules_MULTI is "true" #
        switch (getProfile().protocolType) {
            case Profile.PROTOCOL_GTALK:    return "talk.google.com";
            case Profile.PROTOCOL_FACEBOOK: return "chat.facebook.com";
            case Profile.PROTOCOL_LJ:       return "xmpp.services.livejournal.com";
            case Profile.PROTOCOL_YANDEX:   return "xmpp.yandex.ru";
            case Profile.PROTOCOL_VK:       return "vkmessenger.com";
            case Profile.PROTOCOL_QIP:      return "webim.qip.ru";
            case Profile.PROTOCOL_OVI:      return "ssl:chat.ovi.com:5223";
        }
        // #sijapp cond.end #
        if ("jabber.ru".equals(domain)) return domain;
        if ("ya.ru".equals(domain)) return "xmpp.yandex.ru";
        if ("rambler.ru".equals(domain)) return "jc.rambler.ru";
        if ("gmail.com".equals(domain)) return "talk.google.com";
        if ("qip.ru".equals(domain)) return "webim.qip.ru";
        if ("livejournal.com".equals(domain)) return "xmpp.services.livejournal.com";
        if ("vk.com".equals(domain)) return "vkmessenger.com";
        if ("vkontakte.ru".equals(domain)) return "vkmessenger.com";
        if ("chat.facebook.com".equals(domain)) return domain;
        return null;
    }
    
    String getSocketUrl() {
        String domain = JabberXml.getDomain(getUserId());
        String server = Config.getConfigValue(domain, "/jabber-servers.txt");
        String defaultServer = getDefaultServer(domain);
        if (StringConvertor.isEmpty(server) && (null == defaultServer)) {
            server = new protocol.net.SrvResolver("8.8.8.8")
                    .get(Util.explode("_xmpp-client._tcp." + domain, '.'));
        }
        if (StringConvertor.isEmpty(server)) {
            server = domain;
        }
        
        String[] url = Util.explode(server, ':');
        String[] socketUrl = new String[3];
        final String S_SOCKET = "s"+"ocket";
        final String S_SSL = "ss"+"l";
        final String S_5222 = "5222";
        if (3 == url.length) {
            socketUrl[0] = url[0];
            socketUrl[1] = url[1];
            socketUrl[2] = url[2];
            
        } else if (2 == url.length) {
            socketUrl[0] = url[1].equals(S_5222) ? S_SOCKET : S_SSL;
            socketUrl[1] = url[0];
            socketUrl[2] = url[1];
            
        } else if (1 == url.length) {
            socketUrl[0] = S_SOCKET;
            socketUrl[1] = url[0];
            socketUrl[2] = S_5222;
        }
        if (null != defaultServer) {
            socketUrl[1] = defaultServer;
            url = Util.explode(defaultServer, ':');
            if (3 == url.length) {
                socketUrl = url;
            }
        }
        return socketUrl[0] + "://" + socketUrl[1] + ":" + socketUrl[2];
    }

    private String resource;
    protected String processUin(String uin) {
        resource = JabberXml.getResource(uin,
                jimm.Jimm.getAppProperty("Jimm-Jabber-Resource", "Jimm"));
        return JabberXml.getShortJid(uin);
    }
    public String getResource() {
        return resource;
    }

    protected void s_setPrivateStatus() {
    }

    void removeMe(String uin) {
        connection.sendUnsubscribed(uin);
    }

    private ServiceDiscovery disco = new ServiceDiscovery(this);
    public ServiceDiscovery getServiceDiscovery() {
        return disco;
    }
    public String getUinName() {
        return "JID";
    }

    // #sijapp cond.if modules_FILES is "true"#  
    public void sendFile(FileTransfer transfer, String filename, String description) {
        getConnection().setIBB(new IBBFileTransfer(filename, description, transfer));
    }
    // #sijapp cond.end#
    // #sijapp cond.if modules_XSTATUSES is "true" #
    private JabberXStatus xStatus = new JabberXStatus();
    public JabberXStatus getXStatus() {
        xStatus.setStatusIndex(getProfile().xstatusIndex);
        xStatus.setText(getProfile().xstatusTitle);
        return xStatus;
    }
    public void setXStatus(int statusIndex, String text) {
        getProfile().xstatusIndex = (byte)statusIndex;
        getProfile().xstatusTitle = text;
        getProfile().xstatusDescription = "";
        Options.saveAccount(getProfile());
        if (null != connection) {
            connection.setXStatus(getXStatus());
        }
    }
    // #sijapp cond.end #
    public void saveUserInfo(UserInfo userInfo) {
        if (isConnected()) {
            getConnection().saveVCard(userInfo);
        }
    }

    private static final byte[] statuses = {
        Status.I_STATUS_CHAT,
        Status.I_STATUS_ONLINE,
        Status.I_STATUS_AWAY,
        Status.I_STATUS_XA,
        Status.I_STATUS_DND};
    public byte[] getStatusList() {
        return statuses;
    }
}
// #sijapp cond.end #
