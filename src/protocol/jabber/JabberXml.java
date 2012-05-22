/*
 * JabberXml.java
 *
 * Created on 12 Июль 2008 г., 19:51
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import jimm.chat.message.*;
import jimm.comm.MD5;
import jimm.search.*;
import jimm.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.ui.Progress;
import jimm.util.ResourceBundle;
import protocol.*;
import javax.microedition.lcdui.*;

// #sijapp cond.if modules_ZLIB is "true" #
import com.jcraft.jzlib.*;
// #sijapp cond.end #
/**
 *
 * @author Vladimir Krukov
 */
public class JabberXml extends JabberConnectionBase {

    private String fullJid_;
    private final String domain_;
    private String resource;
    private final boolean xep0048 = false;

    private static final String[] statusCodes = {
            "u" + "navailable",
            "o" + "nline",
            "a" + "way",
            "c" + "h" + "a" + "t",
            "x" + "a",
            "d" + "nd"};
    public byte nativeStatus2StatusIndex(String rawStatus) {
        for (byte i = 0; i < statusCodes.length; ++i) {
            if (statusCodes[i].equals(rawStatus)) {
                return i;
            }
        }
        return Status.I_STATUS_ONLINE;
    }
    public String getNativeStatus(byte statusIndex) {
        return (Status.I_STATUS_ONLINE == statusIndex) ? "" : statusCodes[statusIndex];
    }

    public static String realJidToJimmJid(String realJid) {
        if (isIrcConference(realJid)) {
            int index = realJid.indexOf('!');
            if (-1 != index) {
                return realJid.substring(index + 1)
                + '/' + realJid.substring(0, index);
            }
            index = realJid.indexOf('%');
            if ((-1 != index) && (-1 != realJid.indexOf('/', realJid.indexOf('@')))) {
                return realJid.substring(index + 1);
            }
        }
        String resource = getResource(realJid, null);
        String jid = getShortJid(realJid);
        return (null == resource) ? jid : (jid + '/' + resource);
    }

    public static String jimmJidToRealJid(String jimmJid) {
        if (isIrcConference(jimmJid) && (-1 != jimmJid.indexOf('/', jimmJid.indexOf('@')))) {
            String bareJid = getShortJid(jimmJid);
            if (-1 != bareJid.indexOf('%')) {
                bareJid = bareJid.substring(bareJid.indexOf('%') + 1);
            }
            return getResource(jimmJid, "") + '!' + bareJid;
        }
        return jimmJid;
    }

    public static String getDomain(String jid) {
        jid = getShortJid(jid);
        return jid.substring(jid.indexOf('@') + 1);
    }
    public static String getResource(String fullJid, String defResource) {
        int resourceStart = fullJid.indexOf('/') + 1;
        if (0 < resourceStart) {
            return fullJid.substring(resourceStart);
        }
        return defResource;
    }
    private static boolean isConferenceDomain(String jid, int start) {
        return jid.startsWith("conference.", start)
                || jid.startsWith("conf.", start)
                || jid.startsWith("muc.", start)
                || jid.startsWith("irc.", start);
    }
    public static boolean isConference(String jid) {
        int index = jid.indexOf('@');
        if (-1 < index) {
            if (isConferenceDomain(jid, index + 1)) {
                return true;
            }
            int index1 = jid.lastIndexOf('%', index);
            if (-1 < index1) {
                return isConferenceDomain(jid, index1 + 1);
            }
        }
        return false;
    }

    public static boolean isGate(String jid) {
        return (-1 == jid.indexOf('@')) && (0 < jid.length());
    }
    private static final String[] icqTransports = {"icq.", "picq.", "pyicq."};
    private boolean isPyIcqGate(String jid) {
        if (!isGate(jid)) {
            return false;
        }
        for (int i = 0; i < icqTransports.length; ++i) {
            if (jid.startsWith(icqTransports[i])) {
                return true;
            }
        }
        return false;
    }
    private boolean isMrim(String jid) {
        return (-1 != jid.indexOf("@mrim."));
    }
    public static boolean isIrcConference(String jid) {
        return (-1 != jid.indexOf("@irc."));
    }
    private static final String[] transports = {"@mrim.", "@icq.", "@picq.", "@pyicq.", "@sicq.", "@j2j."};
    public static boolean isContactOverGate(String jid) {
        for (int i = 0; i < transports.length; ++i) {
            if (-1 != jid.indexOf(transports[i])) {
                return true;
            }
        }
        return false;
    }

    public static String getShortJid(String fullJid) {
        int resourceStart = fullJid.indexOf('/');
        if (-1 != resourceStart) {
            return StringConvertor.toLowerCase(fullJid.substring(0, resourceStart));
        }
        return StringConvertor.toLowerCase(fullJid);
    }
    public static String getNick(String jid) {
        return jid.substring(0, jid.indexOf('@'));
    }

    static final boolean isTrue(String val) {
        return S_TRUE.equals(val) || "1".equals(val);
    }

    public String getCaps() {
        return "<c xmlns='http://jabber.org/protocol/caps'"
                + " node='http://stranger.kiev.ua/caps' ver='"
                + Util.xmlEscape(verHash)
                + "' hash='md5'/>";
    }

    public JabberXml(Jabber jabber) {
        super(jabber);
        resource = jabber.getResource();
        fullJid_ = jabber.getUserId() + '/' + resource;
        domain_ = getDomain(fullJid_);
    }
    private void setProgress(int percent) {
        getJabber().setConnectingProgress(percent);
    }

    // #sijapp cond.if modules_ZLIB is "true" #
    public void setStreamCompression() throws JimmException {
        setProgress(20);
        socket.activateStreamCompression();
        write(getOpenStreamXml(domain_));

        // #sijapp cond.if modules_DEBUGLOG is "true" #
        jimm.modules.DebugLog.println("zlib turn on");
        // #sijapp cond.end #
        readXmlNode(true); // "stream:stream"
        parseAuth(readXmlNode(true));
    }
    // #sijapp cond.end #

    public Jabber getJabber() {
        return (Jabber)protocol;
    }

    char readChar() throws JimmException {
        try {
            byte bt = socket.readByte();
            if (0 <= bt) {
                return (char)bt;
            }
            if ((bt & 0xE0) == 0xC0) {
                byte bt2 = socket.readByte();
                return (char)(((bt & 0x3F) << 6) | (bt2 & 0x3F));

            } else if ((bt & 0xF0) == 0xE0) {
                byte bt2 = socket.readByte();
                byte bt3 = socket.readByte();
                return (char)(((bt & 0x1F) << 12) | ((bt2 & 0x3F) << 6) | (bt3 & 0x3F));

            } else {
                int seqLen = 0;
                if ((bt & 0xF8) == 0xF0) seqLen = 3;
                else if ((bt & 0xFC) == 0xF8) seqLen = 4;
                else if ((bt & 0xFE) == 0xFC) seqLen = 5;
                for (; 0 < seqLen; --seqLen) {
                    bt = socket.readByte();
                }
                return '?';
            }
        } catch (Exception e) {
            throw new JimmException(120, 0);
        }
    }

    private boolean hasInPackets() throws JimmException {
        return socket.isConnected() && (0 < available());
    }

    private void write(String xml) throws JimmException {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.systemPrintln("[OUT]:\n" + xml);
        // #sijapp cond.end #
        write(StringConvertor.stringToByteArrayUtf8(xml));
    }
    protected void writePacket(String packet) throws JimmException {
        write(packet);
    }
    private XmlNode readXmlNode(boolean notEmpty) throws JimmException {
        while (hasInPackets() || notEmpty) {
            XmlNode x = XmlNode.parse(this);
            if (null != x) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.systemPrintln("[IN]:\n" + x.toString());
                // #sijapp cond.end #
                return x;
            }
        }
        return null;
    }

    byte[] pingPacket = new byte[]{' '};
    protected void sendPingPacket() throws JimmException {
        write(pingPacket);
    }

    // -----------------------------------------------------------------------
    private void sendRequest(String request) throws JimmException {
        write(request);
    }
    // -----------------------------------------------------------------------

    private boolean isGTalk_ = false;
    private boolean authorized_ = false;
    private boolean rosterLoaded = false;

    private void setAuthStatus(boolean authorized) throws JimmException {
        if (!authorized_) {
            authorized_ = authorized;
            if (!authorized) {
                getJabber().setPassword(null);
                throw new JimmException(111, 0);
            }
        }
    }
    JabberXml() {
        super(null);
        domain_ = "";
    }
    XmlNode newAccountConnect(String domain, String server) throws JimmException {
        domain = Util.xmlEscape(domain);
        connectTo(server);
        write(getOpenStreamXml(domain));
        readXmlNode(true); // "stream:stream"
        XmlNode features = readXmlNode(true); // "stream:features"
        if (!features.contains("regis" + "ter")) {
            return null;
        }
        write("<iq type='get' to='" + domain
                + "' id='1'><query xmlns='jabber:iq:register'/></iq>");
        return readXmlNode(true);
    }
    XmlNode newAccountRegister(String xml) throws JimmException {
        write(xml);
        XmlNode x = readXmlNode(true);
        socket.close();
        return x;
    }

    protected void connect() throws JimmException {
        setProgress(0);

        initFeatures();

        connectTo(getJabber().getSocketUrl());

        write(getOpenStreamXml(domain_));
        setProgress(10);
        /* Authenticate with the server */
        readXmlNode(true); // "stream:stream"
        //parseAuth(hasInPackets() ? readXmlNode(true) : null);
        parseAuth(readXmlNode(true));
        while (!authorized_) {
            loginParse(readXmlNode(true));
        }
        setProgress(50);
        write(GET_ROSTER_XML);
        // #sijapp cond.if modules_UPDATES is "true" # //add updates modules
        // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
        jimm.ui.timers.GetVersion.checkUpdates();
        // #sijapp cond.end# //add updates modules
        // #sijapp cond.end#
    }
    protected void processPacket() throws JimmException {
        XmlNode x = null;
        try {
            x = readXmlNode(false);
            if (null == x) {
                return;
            }
            parse(x);
            x = null;

        } catch (JimmException e) {
            throw e;

        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("Jabber parse", e);
            if (null != x) {
                DebugLog.println("xml: " + x.toString());
            }
            // #sijapp cond.end #
        }
    }
    // -----------------------------------------------------------------------

    /**
     * Parse inbound xml for authentication
     *
     * @param x Received xml
     * @param protocol {@link Jabber} instance
     */
    private void parseAuth(XmlNode x) throws JimmException {
        if ((null == x) || !x.is("stream:features")) {
            nonSaslLogin();
        } else {
            loginParse(x);
        }
    }
    private void nonSaslLogin() throws JimmException {
        String user = JabberXml.getNick(protocol.getUserId());
        sendRequest(
                "<iq type='set' to='" + domain_ + "' id='login'>"
                + "<query xmlns='jabber:iq:auth'>"
                + "<username>" + Util.xmlEscape(user) + "</username>"
                + "<password>" + Util.xmlEscape(protocol.getPassword()) + "</password>"
                + "<resource>"+ Util.xmlEscape(resource) + "</resource>"
                + "</query>"
                + "</iq>");
        XmlNode answer = readXmlNode(true);
        setAuthStatus(S_RESULT.equals(answer.getAttribute(S_TYPE)));
    }

    private void loginParse(XmlNode x) throws JimmException {
        if (x.is("stream:features")) {
            parseStreamFeatures(x);
            return;

        // #sijapp cond.if modules_ZLIB is "true" #
        } else if (x.is("compressed")) {
            setStreamCompression();
            return;
        // #sijapp cond.end #

            /* Reply to DIGEST-MD5 challenges */
        } else if (x.is("challenge")) {
            parseChallenge(x);
            return;

        } else if (x.is("failure")) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.systemPrintln("[INFO-JABBER] Failed");
            // #sijapp cond.end #
            setAuthStatus(false);
            return;

        } else if (x.is("success")) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.systemPrintln("[INFO-JABBER] Auth success");
            DebugLog.systemPrintln("auth " + authorized_);
            // #sijapp cond.end #
            sendRequest(getOpenStreamXml(domain_));
            return;

        } else if (x.is("iq")) {
            XmlNode iqQuery = x.childAt(0);
            String id = x.getId();
            if ("sess".equals(id)) {
                setAuthStatus(true);
                return;
            }
            if (null == iqQuery) {
                return;
            }
            String queryName = iqQuery.name;

            // non sasl login
            if (IQ_TYPE_ERROR == getIqType(x)) {
                if ("jabber:iq:auth".equals(iqQuery.getXmlns())) {
                    setAuthStatus(false);
                }
            }

            if ("bind".equals(queryName)) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.systemPrintln("[INFO-JABBER] Send open session request");
                // #sijapp cond.end #
                fullJid_ = iqQuery.getFirstNodeValue(XmlNode.S_JID);
                sendRequest("<iq type='set' id='sess'>"
                        + "<session xmlns='urn:ietf:params:xml:ns:xmpp-session'/>"
                        + "</iq>");
                return;

            }
        }
        parse(x);
    }

    /**
     * Parse inbound xml and execute apropriate action
     *
     * @param x Received xml
     * @param protocol {@link Jabber} instance
     */
    private void parse(XmlNode x) throws JimmException {
        if (x.is("iq")) {
            parseIq(x);

        } else if (x.is("presence")) {
            parsePresence(x);

        } else if (x.is("m" + "essage")) {
            parseMessage(x);

        } else if (x.is("stream:error")) {
            setAuthStatus(false);
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            XmlNode err = (null == x.childAt(0)) ? x : x.childAt(0);
            DebugLog.systemPrintln("[INFO-JABBER] Stream error!: " + err.name + "," + err.value);
            // #sijapp cond.end #
        }
    }

    private String generateId(String key) {
        return key + (System.currentTimeMillis() % 0xFFFF);
    }
    private String generateId() {
        return "jimm" + (System.currentTimeMillis() % 0xFFFF);
    }

    private boolean isNoAutorized(String subscription) {
        return S_NONE.equals(subscription) || S_FROM.equals(subscription);
    }
    private final byte IQ_TYPE_RESULT = 0;
    private final byte IQ_TYPE_GET = 1;
    private final byte IQ_TYPE_SET = 2;
    private final byte IQ_TYPE_ERROR = 3;
    private byte getIqType(XmlNode iq) {
        String iqType = iq.getAttribute(S_TYPE);
        if (S_RESULT.equals(iqType)) {
            return IQ_TYPE_RESULT;
        }
        if (S_GET.equals(iqType)) {
            return IQ_TYPE_GET;
        }
        if (S_SET.equals(iqType)) {
            return IQ_TYPE_SET;
        }
        return IQ_TYPE_ERROR;
    }
    private void parseIqError(XmlNode iqNode, String from) throws JimmException {
        XmlNode errorNode = iqNode.getFirstNode(S_ERROR);
        iqNode.removeNode(S_ERROR);

        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if (null == errorNode) {
            DebugLog.println("Error without description is stupid");
        } else {
            DebugLog.systemPrintln(
                    "[INFO-JABBER] <IQ> error received: " +
                    "Code=" + errorNode.getAttribute(S_CODE) + " " +
                    "Value=" + getError(errorNode));
        }
        // #sijapp cond.end #

        XmlNode query = iqNode.childAt(0);
        if (null == query) {
            // some bad happend

        } else if (S_VCARD.equals(query.name)) {
            loadVCard(null, from);

        } else if (S_QUERY.equals(query.name)) {
            String xmlns = query.getXmlns();
            if ("jabber:iq:register".equals(xmlns) && (null != jabberForm)) {
                jabberForm.error(getError(errorNode));

            } else if ("jabber:iq:roster".equals(query.name)) {
                //FIXME: stop loading if roster service was down.
            }
        }
    }

    private void sendIqError(String query, String xmlns, String from, String id) {
        putPacketIntoQueue("<iq type='error' to='"
                + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "'>"
                + "<" + query + " xmlns='" + Util.xmlEscape(xmlns) + "'/>"
                + "<error type='cancel'>"
                + "<feature-not-implemented xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>"
                + "</error>"
                + "</iq>");
    }
    private boolean isMy(String from) {
        if (StringConvertor.isEmpty(from)) return true;
        if (getJabber().getUserId().equals(JabberXml.getShortJid(from))) return true;
        return false;
    }
    private ServiceDiscovery serviceDiscovery = null;

    private void processEmptyId(String id, byte iqType, String from) {
        if (IQ_TYPE_RESULT != iqType) {
            return;
        }
        if (id.startsWith(S_VCARD)) {
            loadVCard(null, from);
        }
        if ((null != jabberForm) && jabberForm.getId().equals(id)) {
            jabberForm.success();
            jabberForm = null;
        }
    }
    /**
     * Parse the <<lit>iq</lit>> node and launch apropriate action
     *
     * @param iq {@link XmlNode} to parse
     */
    private void parseIq(XmlNode iq) throws JimmException {
        String from = iq.getAttribute(S_FROM);
        byte iqType = getIqType(iq);
        String id = iq.getId();
        if (StringConvertor.isEmpty(id)) {
            id = generateId();
        }
        // #sijapp cond.if modules_FILES is "true"#
        if (null != ibb) {
            boolean processed = processIbb(iq, iqType, id);
            if (processed) {
                return;
            }
        }
        // #sijapp cond.end#
        if (IQ_TYPE_ERROR == iqType) {
            parseIqError(iq, from);
            return;
        }

        XmlNode iqQuery = iq.childAt(0);
        if (null == iqQuery) {
            processEmptyId(id, iqType, from);
            return;
        }
        String queryName = iqQuery.name;
        Jabber jabber = getJabber();

        if (S_QUERY.equals(queryName)) {
            String xmlns = iqQuery.getXmlns();
            if ("jabber:iq:roster".equals(xmlns)) {
                if (!isMy(from)) {
                    return;
                }
                if ((IQ_TYPE_RESULT == iqType) && !rosterLoaded) {
                    rosterLoaded = true;
                    TemporaryRoster roster = new TemporaryRoster(jabber);
                    jabber.setContactListStub();

                    while (0 < iqQuery.childrenCount()) {
                        XmlNode itemNode = (XmlNode)iqQuery.popChildNode();
                        String jid = itemNode.getAttribute(XmlNode.S_JID);
                        Contact contact = roster.makeContact(jid);
                        contact.setName(itemNode.getAttribute(XmlNode.S_NAME));

                        String groupName = itemNode.getFirstNodeValue(S_GROUP);
                        if (StringConvertor.isEmpty(groupName) || isConference(jid)) {
                            groupName = contact.getDefaultGroupName();
                        }
                        contact.setGroup(roster.getOrCreateGroup(groupName));

                        String subscription = itemNode.getAttribute("subsc" + "ription");
                        contact.setBooleanValue(Contact.CONTACT_NO_AUTH, isNoAutorized(subscription));
                        roster.addContact(contact);
                    }
                    if (!isConnected()) {
                        return;
                    }
                    jabber.setContactList(roster.getGroups(), roster.mergeContacts());
                    Contact selfContact = jabber.getItemByUIN(jabber.getUserId());
                    if (null != selfContact) {
                        selfContact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
                        jabber.ui_updateContact(selfContact);
                    }

                    jabber.s_updateOnlineStatus();
                    // #sijapp cond.if modules_XSTATUSES is "true" #
                    JabberXStatus xstatus = jabber.getXStatus();
                    String xcode = xstatus.getCode();
                    if ((null != xcode) && !xcode.startsWith(JabberXStatus.XSTATUS_START)) {
                        setXStatus(xstatus);
                    }
                    // #sijapp cond.end #
                    getBookmarks();
                    getJabber().safeSave();
                    setProgress(100);

                } else if (IQ_TYPE_SET == iqType) {
                    while (0 < iqQuery.childrenCount()) {
                        XmlNode itemNode = (XmlNode)iqQuery.popChildNode();

                        String subscription = itemNode.getAttribute("subsc" + "ription");
                        String jid = itemNode.getAttribute(XmlNode.S_JID);

                        if (isConference(jid)) {

                        } else if ((S_REMOVE).equals(subscription)) {
                            jabber.removeLocalContact(jabber.getItemByUIN(jid));

                        } else {
                            String name = itemNode.getAttribute(XmlNode.S_NAME);
                            Contact contact = jabber.createTempContact(jid);
                            String group = itemNode.getFirstNodeValue(S_GROUP);
                            if (StringConvertor.isEmpty(group) || isConference(contact.getUin())) {
                                group = contact.getDefaultGroupName();
                            }
                            contact.setName(name);
                            contact.setGroup(jabber.getOrCreateGroup(group));
                            contact.setTempFlag(false);
                            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, isNoAutorized(subscription));
                            jabber.addLocalContact(contact);
                        }
                    }
                    Contact selfContact = jabber.getItemByUIN(jabber.getUserId());
                    if (null != selfContact) {
                        selfContact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
                        jabber.ui_updateContact(selfContact);
                    }
                }
                return;

            } else if ("http://jabber.org/protocol/disco#info".equals(xmlns)) {
                if (IQ_TYPE_GET == iqType) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("<iq type='result' to='")
                    .append(Util.xmlEscape(from))
                    .append("' id='").append(Util.xmlEscape(id)).append("'>");
                    sb.append("<query xmlns='http://jabber.org/protocol/disco#info'>");
                    sb.append(featureList);
                    sb.append("</query></iq>");
                    write(sb.toString());
                    return;
                }
                if (IQ_TYPE_RESULT != iqType) {
                    return;
                }
                String name = iqQuery.getFirstNodeAttribute("identity", XmlNode.S_NAME);
                ((JabberServiceContact)jabber.createTempContact(from)).setConferenceInfo(name);
                return;

            } else if ("http://jabber.org/protocol/disco#items".equals(xmlns)) {
                if (IQ_TYPE_GET == iqType) {
                    sendIqError(S_QUERY, xmlns, from, id);
                    return;
                }
                ServiceDiscovery disco = serviceDiscovery;
                if (null == disco) {
                    return;
                }
                serviceDiscovery = null;
                disco.setTotalCount(iqQuery.childrenCount());
                while (0 < iqQuery.childrenCount()) {
                    XmlNode item = iqQuery.popChildNode();
                    String name = item.getAttribute(XmlNode.S_NAME);
                    String jid = item.getAttribute(XmlNode.S_JID);
                    disco.addItem(name, jid);
                }
                disco.update();
                return;

            } else if ("jabber:iq:register".equals(xmlns)) {
                if ((null != jabberForm) && jabberForm.getId().equals(id)) {
                    if (jabberForm.isWaiting()) {
                        jabberForm.loadFromXml(iqQuery, iqQuery);

                    } else {
                        processEmptyId(id, iqType, from);
                    }
                }
                return;

            } else if ("jabber:iq:private".equals(xmlns)) {
                if (!isMy(from)) {
                    return;
                }
                XmlNode storage = iqQuery.getFirstNode("sto" + "rage", "storage:bookmarks");
                if (null != storage) {
                    loadBookmarks(storage);
                }
                return;

            } else if ("jabber:iq:version".equals(xmlns)) {
                // #sijapp cond.if modules_CLIENTS is "true" #
                if (IQ_TYPE_RESULT == iqType) {
                    String name = iqQuery.getFirstNodeValue(XmlNode.S_NAME);
                    String ver = iqQuery.getFirstNodeValue("v" + "ersion");
                    String os = iqQuery.getFirstNodeValue("o" + "s");
                    name = Util.notUrls(name);
                    ver = Util.notUrls(ver);
                    os = Util.notUrls(os);
                    // #sijapp cond.if modules_DEBUGLOG is "true" #
                    DebugLog.println("ver " + from + " " + name + " " + ver + " in " + os);
                    // #sijapp cond.end #
                    // #sijapp cond.if modules_MAGIC_EYE is "true" #                      //version in eye
                    String jid = isConference(from) ? from : getShortJid(from);           //version in eye
                    MagicEye.addAction(getJabber(), jid, name + " " + ver + " in " + os); //version in eye
                    // #sijapp cond.end #                                                 //version in eye
                    return;
                }
                // #sijapp cond.end #
                if (IQ_TYPE_GET == iqType) {
                    putPacketIntoQueue("<iq type='result' to='"
                            + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "'>"
                            + "<query xmlns='jabber:iq:version'><name>Jimm</name><version>"

                            // #sijapp cond.if modules_ANISMILES isnot "true" #               //answer version
                            // #sijapp cond.if modules_MULTI isnot "true" #                   //answer version
                            + Util.xmlEscape(jimm.Jimm.VERSION + " [###DATE###]")             //answer version
                            // #sijapp cond.end #                                             //answer version
                            // #sijapp cond.end #                                             //answer version

                            // #sijapp cond.if modules_ANISMILES isnot "true" #               //answer version
                            // #sijapp cond.if modules_MULTI is "true" #                      //answer version
                            + Util.xmlEscape(jimm.Jimm.VERSION + " M" + " [###DATE###]")      //answer version
                            // #sijapp cond.end #                                             //answer version
                            // #sijapp cond.end #                                             //answer version

                            // #sijapp cond.if modules_ANISMILES is "true" #                  //answer version
                            // #sijapp cond.if modules_MULTI isnot "true" #                   //answer version
                            + Util.xmlEscape(jimm.Jimm.VERSION + " A" + " [###DATE###]")      //answer version
                            // #sijapp cond.end #                                             //answer version
                            // #sijapp cond.end #                                             //answer version

                            // #sijapp cond.if modules_ANISMILES is "true" #                  //answer version
                            // #sijapp cond.if modules_MULTI is "true" #                      //answer version
                            + Util.xmlEscape(jimm.Jimm.VERSION + " MA" + " [###DATE###]")     //answer version
                            // #sijapp cond.end #                                             //answer version
                            // #sijapp cond.end #                                             //answer version

                            + "</version><os>"
                            + Util.xmlEscape(jimm.Jimm.microeditionPlatform)
                            + "</os></query></iq>");
                    // #sijapp cond.if modules_MAGIC_EYE is "true" #
                    String jid = isConference(from) ? from : getShortJid(from);
                    MagicEye.addAction(jabber, jid, "get_version");
                    // #sijapp cond.end #
                }
                return;

            } else if ("jabber:iq:last".equals(xmlns)) {
                if (IQ_TYPE_GET == iqType) {
                    // #sijapp cond.if modules_MAGIC_EYE is "true" #
                    String jid = isConference(from) ? from : getShortJid(from);
                    MagicEye.addAction(jabber, jid, "last_activity_request");
                    // #sijapp cond.end #
                    long time = System.currentTimeMillis() / 1000 - jabber.getLastStatusChangeTime();
                    putPacketIntoQueue("<iq type='result' to='" + Util.xmlEscape(from)
                            + "' id='" + Util.xmlEscape(id) + "'>"
                            + "<query xmlns='jabber:iq:last' seconds='"
                            + time
                            + "'/></iq>");
                }
                return;
            }

        } else if (S_TIME.equals(queryName)) {
            if (IQ_TYPE_GET != iqType) {
                return;
            }
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            String jid = isConference(from) ? from : getShortJid(from);
            MagicEye.addAction(jabber, jid, "get_time");
            // #sijapp cond.end #
            int gmtOffset = Options.getInt(Options.OPTIONS_GMT_OFFSET);
            putPacketIntoQueue("<iq type='result' to='" + Util.xmlEscape(from)
                    + "' id='" + Util.xmlEscape(id) + "'>"
                    + "<time xmlns='urn:xmpp:time'><tzo>"
                    + (0 <= gmtOffset ? "+" : "-") + Util.makeTwo(Math.abs(gmtOffset)) + ":00"
                    + "</tzo><utc>"
                    + Util.getUtcDateString(Util.createCurrentDate(true))
                    + "</utc></time></iq>");
            return;

        } else if (("p" + "ing").equals(queryName)) {
            writePacket("<iq to='" + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "' type='result'/>");
            return;

        } else if (("pu" + "bsub").equals(queryName)) {
            if (!isMy(from)) {
                return;
            }
            loadBookmarks(iqQuery.getFirstNodeRecursive("sto" + "rage"));
            return;

        } else if (S_VCARD.equals(queryName)) {
            if (IQ_TYPE_RESULT == iqType) {
                loadVCard(iqQuery, from);
            }
            return;

        } else if ("x".equals(queryName)) {
            String xmlns = iqQuery.getXmlns();
            if ("http://jabber.org/protocol/rosterx".equals(xmlns)) {
                if ((null != autoSubscribeDomain) && from.equals(autoSubscribeDomain)) {
                    putPacketIntoQueue("<iq type='result' to='"
                            + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "' />");
                    parseRosterExchange(iqQuery, '@' + autoSubscribeDomain);
                    return;
                }
            }
        }

        if (IQ_TYPE_GET == iqType) {
            sendIqError(iqQuery.name, iqQuery.getXmlns(), from, id);
        }
    }


    public void saveVCard(UserInfo userInfo) {
        if (null == userInfo.vCard) {
            userInfo.vCard = XmlNode.getEmptyVCard();
        }
        userInfo.vCard.setValue("NICKNAME", userInfo.nick);
        userInfo.vCard.setValue("BDAY", userInfo.birthDay);
        userInfo.vCard.setValue("URL", userInfo.homePage);
        userInfo.vCard.setValue("FN", userInfo.getName());
        userInfo.vCard.setValue("N", null, "GIVEN", userInfo.firstName);
        userInfo.vCard.setValue("N", null, "FAMILY", userInfo.lastName);
        userInfo.vCard.setValue("EMAIL", new String[]{"INTERNET"}, "USERID", userInfo.email);
        userInfo.vCard.setValue("TEL", new String[]{"HOME", "VOICE"}, "NUMBER", userInfo.cellPhone);

        userInfo.vCard.setValue("ADR", new String[]{"HOME"}, "STREET", userInfo.homeAddress);
        userInfo.vCard.setValue("ADR", new String[]{"HOME"}, "LOCALITY", userInfo.homeCity);
        userInfo.vCard.setValue("ADR", new String[]{"HOME"}, "REGION", userInfo.homeState);

        userInfo.vCard.setValue("TEL", new String[]{"WORK", "VOICE"}, "NUMBER", userInfo.workPhone);
        userInfo.vCard.setValue("ORG", null, "ORGNAME", userInfo.workCompany);
        userInfo.vCard.setValue("ORG", null, "ORGUNIT", userInfo.workDepartment);
        userInfo.vCard.setValue("TITLE", userInfo.workPosition);
        userInfo.vCard.cleanXmlTree();

        StringBuffer packet = new StringBuffer();
        packet.append("<iq type='set' id='").append(generateId()).append("'>");
        userInfo.vCard.toString(packet);
        packet.append("</iq>");
        putPacketIntoQueue(packet.toString());
    }

    private void loadVCard(XmlNode vCard, String from) {
        UserInfo userInfo = singleUserInfo;
        if ((null == userInfo) || !from.equals(userInfo.realUin)) {
            return;
        }
        userInfo.auth = false;
        userInfo.uin = from;
        if (isConference(from)) {
            Contact c = getJabber().getItemByUIN(getShortJid(from));
            if (c instanceof JabberServiceContact) {
                JabberContact.SubContact sc = ((JabberServiceContact)c).getExistSubContact(getResource(from, null));
                if ((null != sc) && (null != sc.realJid)) {
                    userInfo.uin = sc.realJid;
                }
            }
        }

        if (null == vCard) {
            userInfo.updateProfileView();
            singleUserInfo = null;
            return;
        }

        String name[] = new String[3];
        name[0] = vCard.getFirstNodeValue("N", "GIVEN");
        name[1] = vCard.getFirstNodeValue("N", "MIDDLE");
        name[2] = vCard.getFirstNodeValue("N", "FAMILY");
        if (StringConvertor.isEmpty(Util.implode(name, ""))) {
            userInfo.firstName = vCard.getFirstNodeValue("FN");
            userInfo.lastName = null;
        } else {
            userInfo.lastName = name[2];
            name[2] = null;
            userInfo.firstName = Util.implode(name, " ");
        }
        userInfo.nick = vCard.getFirstNodeValue("NICKNAME");
        userInfo.birthDay = vCard.getFirstNodeValue("BDAY");
        userInfo.email = vCard.getFirstNodeValue("EMAIL", new String[]{"INTERNET"}, "USERID", true);
        userInfo.about = vCard.getFirstNodeValue("DESC");
        userInfo.homePage = vCard.getFirstNodeValue("URL");

        userInfo.homeAddress = vCard.getFirstNodeValue("ADR", new String[]{"HOME"}, "STREET", true);
        userInfo.homeCity = vCard.getFirstNodeValue("ADR", new String[]{"HOME"}, "LOCALITY", true);
        userInfo.homeState = vCard.getFirstNodeValue("ADR", new String[]{"HOME"}, "REGION", true);
        userInfo.cellPhone = vCard.getFirstNodeValue("TEL", new String[]{"HOME", "VOICE"}, "NUMBER", true);

        userInfo.workCompany = vCard.getFirstNodeValue("ORG", null, "ORGNAME");
        userInfo.workDepartment = vCard.getFirstNodeValue("ORG", null, "ORGUNIT");
        userInfo.workPosition = vCard.getFirstNodeValue("TITLE");
        userInfo.workPhone = vCard.getFirstNodeValue("TEL", new String[]{"WORK", "VOICE"}, "NUMBER");

        if (!isGate(from)) {
            userInfo.setOptimalName();
        }
        if (userInfo.isEditable()) {
            userInfo.vCard = vCard;
        }
        userInfo.updateProfileView();

        try {
            XmlNode bs64photo = vCard.getFirstNode("PHOTO");
            bs64photo = (null == bs64photo) ? null : bs64photo.getFirstNode("BINVAL");
            byte[] avatarBytes = null;
            if (null != bs64photo) {
                avatarBytes = userInfo.isEditable()
                        ? bs64photo.getBinValue()
                        : bs64photo.popBinValue();
            }

            if ((null != avatarBytes) && Jimm.hasMemory(avatarBytes.length * 2)) {
                Image avatar = Image.createImage(avatarBytes, 0, avatarBytes.length);
                avatarBytes = null;
                userInfo.setAvatar(avatar);
                userInfo.updateProfileView();
            }
        } catch (OutOfMemoryError er) {
        } catch (Exception e) {
        }

        singleUserInfo = null;
    }

    private void loadBookmarks(XmlNode storage) {
        if (null == storage) {
            return;
        }
        if (!"storage:bookmarks".equals(storage.getXmlns())) {
            return;
        }
        Vector jids = new Vector();
		Vector names = new Vector();
		Vector nicks = new Vector();
		Vector autojoins = new Vector();
        if (0 == storage.childrenCount()) {
            Config conf = new Config().loadLocale("/jabber-services.txt");
            for (int i = 0; i < conf.getKeys().length; ++i) {
				String jid = conf.getKeys()[i];
                if (!JabberXml.isConference(jid)) {
                    break;
                }
				String name = conf.getValues()[i];
				String nick = getJabber().getProfile().nick;
				if (StringConvertor.isEmpty(nick)) {
					nick = getNick(getJabber().getUserId());
				}
				boolean autojoin = true;

                jids.addElement(jid);
				names.addElement(name);
				nicks.addElement(nick);
				autojoins.addElement(new Boolean(autojoin));
            }
        } else {
			while (0 < storage.childrenCount()) {
				XmlNode item = storage.popChildNode();

				String jid = item.getAttribute(XmlNode.S_JID);
				if ((null == jid) || !isConference(jid)) {
					continue;
				}
				String name = item.getAttribute(XmlNode.S_NAME);
				String nick = item.getFirstNodeValue(S_NICK);
				boolean autojoin = isTrue(item.getAttribute("au" + "tojoin"));

                jids.addElement(jid);
				names.addElement(name);
				nicks.addElement(nick);
				autojoins.addElement(new Boolean(autojoin));
			}
		}
        Group group = getJabber().getOrCreateGroup(ResourceBundle.getString(JabberGroup.CONFERENCE_GROUP));
        int autoJoinCount = getJabber().isReconnect() ? 0 : 7;
        Vector groups = getJabber().getGroupItems();
        Vector contacts = getJabber().getContactItems();
        for (int i = 0; i < jids.size(); ++i) {
			String jid = (String)jids.elementAt(i);
			String name = (String)names.elementAt(i);
			String nick = (String)nicks.elementAt(i);
			boolean autojoin = ((Boolean)autojoins.elementAt(i)).booleanValue();


            JabberServiceContact conference = (JabberServiceContact)getJabber().createTempContact(jid, name);
            conference.setMyName(nick);
            conference.setTempFlag(false);
            conference.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
            conference.setAutoJoin(autojoin);
            conference.setGroup(group);
            if (-1 == Util.getIndex(contacts, conference)) {
                contacts.addElement(conference);
            }
            if (conference.isAutoJoin() && (0 < autoJoinCount)) {
                conference.join();
                autoJoinCount--;
            }
        }
        getJabber().setContactListAddition(group);
        getJabber().safeSave();
        getJabber().rejoin();
    }

    private static final String S_TEXT = "te" + "xt";
    private static final String S_FROM = "fr" + "om";
    private static final String S_TYPE = "ty" + "pe";
    private static final String S_ERROR = "e" + "rror";
    private static final String S_NONE = "n" + "o" + "ne";
    private static final String S_NODE = "n" + "o" + "de";
    private static final String S_NICK = "ni" + "ck";
    private static final String S_SET = "s" + "e" + "t";
    private static final String S_REMOVE = "r" + "emove";
    private static final String S_RESULT = "r" + "esult";
    private static final String S_GROUP = "g" + "roup";
    private static final String S_ITEM = "i" + "tem";
    private static final String S_ITEMS = "i" + "tems";
    private static final String S_TRUE = "t" + "rue";
    private static final String S_FALSE = "fa" + "lse";
    private static final String S_GET = "g" + "e" + "t";
    private static final String S_TIME = "t" + "ime";
    private static final String S_TITLE = "t" + "itle";
    private static final String S_CODE = "c" + "ode";
    private static final String S_QUERY = "que" + "ry";
    private static final String S_STATUS = "st" + "atus";
    private static final String S_VCARD = "vCard";
    private static final String S_SUBJECT = "subje" + "ct";
    private static final String S_BODY = "b" + "ody";
    private static final String S_URL = "u" + "r" + "l";
    private static final String S_DESC = "d" + "es"+ "c";
    private static final String S_COMPOSING = "c" + "omposing";
    private static final String S_ACTIVE = "ac" + "tive";
    private static final String S_PAUSED = "p" + "aused";

    /**
     * Parse the <<lit>presence</lit>> node and launch apropriate action
     *
     * @param x {@link XmlNode} to parse
     * @param p {@link Jabber} instance
     */
    private void parsePresence(XmlNode x) {
        final String fromFull = x.getAttribute(S_FROM);
        final String from = getShortJid(fromFull);
        final String resource = getResource(fromFull, "");
        String type = x.getAttribute(S_TYPE);

        if (S_ERROR.equals(type)) {
            XmlNode errorNode = x.getFirstNode(S_ERROR);
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.systemPrintln(
                    "[INFO-JABBER] <IQ> error received: " +
                    "Code=" + errorNode.getAttribute(S_CODE) + " " +
                    "Value=" + errorNode.getFirstNodeValue(S_TEXT));
            // #sijapp cond.end #

            boolean showError = isGate(from);
            if (isConference(from)) {
                JabberServiceContact conf = (JabberServiceContact)getJabber().getItemByUIN(from);
                if (null != conf) {
                    int code = Util.strToIntDef(errorNode.getAttribute(S_CODE), -1);
                    if (409 == code) {
                        conf.nickError(resource, code, getError(errorNode));
                        return;
                    } else {
                        conf.doAction(JabberServiceContact.CONFERENCE_DISCONNECT);
                        showError = true;
                    }
                }
            }
            if (showError) {
                getJabber().addMessage(new SystemNotice(getJabber(),
                        SystemNotice.SYS_NOTICE_ERROR, from, getError(errorNode)));
            }

            Contact c = getJabber().getItemByUIN(from);
            if (null == c) {
                return;
            }
            c.setOfflineStatus();
            return;
        }

        if (("subscr" + "ibe").equals(type)) {
            if (isAutoGateContact(from)) {
                sendSubscribed(from);
                requestSubscribe(from);
            } else {
                getJabber().addMessage(new SystemNotice(getJabber(), SystemNotice.SYS_NOTICE_AUTHREQ, from, null));
            }
            autoRenameContact(from, x);
            return;
        }
        if (("subscr" + "ibed").equals(type)) {
            if (!isAutoGateContact(from)) {
                getJabber().setAuthResult(from, true);
            }
            autoRenameContact(from, x);
            return;
        }
        if (("unsubscr" + "ibed").equals(type)) {
            getJabber().setAuthResult(from, false);
            return;
        }
        if (null == type) {
            type = x.getFirstNodeValue("sh" + "ow");
        }
        if (null == type) {
            type = "";
        }

        JabberContact contact = (JabberContact)getJabber().getItemByUIN(from);
        if (null == contact) {
            String fullJid = realJidToJimmJid(fromFull);
            contact = (JabberContact)getJabber().getItemByUIN(fullJid);
            if (null == contact) {
                return;
            }
        }

        int priority = Util.strToIntDef(x.getFirstNodeValue("prior" + "ity"), 0);
        String statusString = x.getFirstNodeValue(S_STATUS);

        if (isConference(from)) {
            XmlNode xMuc = x.getXNode("http://jabber.org/protocol/muc#user");
            XmlNode item = (null == xMuc) ? null : xMuc.getFirstNode(S_ITEM);
            JabberServiceContact conf = (JabberServiceContact)contact;
            String reasone = null;
            if (null != item) {
                String role = item.getAttribute(XmlNode.S_ROLE);
                if (("m" + "oderator").equals(role)) {
                    priority = JabberServiceContact.ROLE_MODERATOR;

                } else if (("p" + "articipant").equals(role)) {
                    priority = JabberServiceContact.ROLE_PARTICIPANT;

                } else if (S_NONE.equals(role)) {
                    reasone = item.getFirstNodeValue("r" + "eason");
                    item = null;

                } else {// "visitor"
                    priority = JabberServiceContact.ROLE_VISITOR;
                }
            }
            updateConfContact(conf, resource, type, statusString, priority);
            if (null != item) {
                String newNick = item.getAttribute(XmlNode.S_NICK);
                if (null != newNick) {
                    updateConfContact(conf, newNick, "", "", priority);
                    conf.nickChainged(resource, newNick);
                } else {
                    conf.nickOnline(resource);
                }

                String realJid = item.getAttribute(XmlNode.S_JID);
                if (null != realJid) {
                    conf.setRealJid(resource, getShortJid(realJid));
                }
                // #sijapp cond.if modules_CLIENTS is "true" #
                contact.setClient(resource, x.getFirstNodeAttribute("c", S_NODE));
                // #sijapp cond.end #

            } else {
                int code = 0;
                if (null != xMuc) {
                    code = Util.strToIntDef(xMuc.getFirstNodeAttribute(S_STATUS, S_CODE), 0);
                }
                conf.nickOffline(resource, code, reasone);
            }

            if (conf.getMyName().equals(resource)) {
                getJabber().ui_changeContactStatus(conf);
            }
            updateConfPrivate(from, resource);

        } else {
            // #sijapp cond.if modules_XSTATUSES is "true" #
            if (!("u" + "navailable").equals(type)) {
                if ((JabberXStatus.noneXStatus == contact.getXStatus())
                        || !contact.getXStatus().isPep()) {
                    XmlNode xNode = x.getXNode(S_FEATURE_XSTATUS);
                    String id = getXStatus(xNode);

                    String xtext = null;
                    if (null != id) {
                        xtext = xNode.getFirstNodeValue(S_TITLE);
                        String s = StringConvertor.notNull(statusString);
                        if (StringConvertor.isEmpty(xtext)) {
                            xtext = null;

                        } else if (s.startsWith(xtext)) {
                            xtext = statusString;
                            statusString = null;
                        }
                    }
                    contact.setXStatus(id, xtext);
                }
                if (isPyIcqGate(from)) {
                    setXStatusToIcqTransport((JabberServiceContact)contact);
                }
            }
            // #sijapp cond.end #
            contact.setStatus(resource, priority, nativeStatus2StatusIndex(type), statusString);
            // #sijapp cond.if modules_CLIENTS is "true" #
            if (contact.isOnline()) {
                contact.setClient(resource, x.getFirstNodeAttribute("c", S_NODE));
            }
            // #sijapp cond.end #

            if (contact.getUin().equals(contact.getName())) {
                getJabber().renameContact(contact, getNickFromNode(x));
            }
            getJabber().ui_changeContactStatus(contact);
        }
    }
    private String getNickFromNode(XmlNode x) {
        String name = x.getFirstNodeValueRecursive("n" + "ickname");
        return (null == name) ? x.getFirstNodeValue(S_NICK) : name;
    }
    private void autoRenameContact(String jid, XmlNode x) {
        String name = getNickFromNode(x);
        if (null == name) {
            return;
        }
        Contact contact = getJabber().getItemByUIN(jid);
        if (null == contact) {
            return;
        }
        if (contact.getUin().equals(contact.getName())) {
            getJabber().renameContact(contact, name);
        }
    }

    // #sijapp cond.if modules_XSTATUSES is "true" #
    private void parseEvent(XmlNode eventNode, String fullJid) {
        if (null == eventNode) {
            return;
        }
        JabberContact contact = (JabberContact)getJabber().getItemByUIN(getShortJid(fullJid));
        if (null == contact) {
            return;
        }

        XmlNode statusNode = eventNode.getFirstNode(S_ITEMS);
        String eventType = "";
        if (null != statusNode) {
            eventType = statusNode.getAttribute(S_NODE);
            int start = eventType.lastIndexOf('/');
            if (-1 != start) {
                eventType = eventType.substring(start + 1);
            }
            statusNode = statusNode.getFirstNode(S_ITEM);
        }
        if (null != statusNode) {
            statusNode = statusNode.childAt(0);
        }
        if (-1 == "|mood|activity|tune".indexOf(eventType)) {
            return;
        }

        if ((null == statusNode) || (0 == statusNode.childrenCount())) {
            JabberXStatus x = contact.getXStatus();
            if ((JabberXStatus.noneXStatus != x) && x.isType(eventType)) {
                contact.setXStatus("", "");
            }
            return;
        }
        String text = statusNode.getFirstNodeValue(S_TEXT);
        statusNode.removeNode(S_TEXT);
        StringBuffer status = new StringBuffer();
        while (null != statusNode) {
            status.append(':').append(statusNode.name);
            statusNode = statusNode.childAt(0);
        }
        status.deleteCharAt(0);
        if ((JabberXStatus.noneXStatus == contact.getXStatus())
                || contact.getXStatus().isPep()) {
            contact.setXStatus(status.toString(), text);
        }
    }

    private String getXStatus(XmlNode x) {
        return (null == x) ? null : (JabberXStatus.XSTATUS_START + x.getId());
    }
    // #sijapp cond.end #

    private void parseMessageEvent(XmlNode messageEvent, String from) {
        if (null == messageEvent) {
            return;

        }
        if (messageEvent.contains("offl" + "ine")) {
            // <x><offline/><id/></x>
            setMessageSended(messageEvent.getFirstNodeValue(XmlNode.S_ID),
                    PlainMessage.NOTIFY_FROM_SERVER);
            return;
        }
        // #sijapp cond.if modules_SOUND is "true" #
        if (0 < Options.getInt(Options.OPTION_TYPING_MODE)) {
            getJabber().beginTyping(from, messageEvent.contains(S_COMPOSING));
        }
        // #sijapp cond.end #
    }
    // #sijapp cond.if modules_SOUND is "true" #
    private void parseChatState(XmlNode message, String from) {
        if (0 < Options.getInt(Options.OPTION_TYPING_MODE)) {
            if (message.contains(S_ACTIVE)
                    || message.contains("gon" + "e")
                    || message.contains(S_PAUSED)
                    || message.contains("inactiv" + "e")) {
                getJabber().beginTyping(from, false);

            } else if (message.contains(S_COMPOSING)) {
                getJabber().beginTyping(from, true);
            }
        }
    }
    // #sijapp cond.end #
    private String getDate(XmlNode message) {
        XmlNode offline = message.getXNode("jabber:x:delay");
        if (null == offline) {
            offline = message.getFirstNode("d" + "elay");
        }
        return (null == offline) ? null : offline.getAttribute("stamp");
    }

    private void prepareFirstPrivateMessage(String jid) {
        final JabberServiceContact conf =
                (JabberServiceContact)getJabber().getItemByUIN(getShortJid(jid));
        if (null == conf) { // don't have conference
            return;
        }

        JabberContact.SubContact sub = conf.getExistSubContact(JabberXml.getResource(jid, ""));
        if (null == sub) { // don't have contact
            return;
        }

        if (JabberServiceContact.ROLE_MODERATOR == sub.priority) {
            // moderators without antispam
            getJabber().addTempContact(getJabber().createTempContact(jid));
        }
    }
    /**
     * Parse the <<lit>message</lit>> node and launch apropriate action
     *
     * @param msg {@link XmlNode} to parse
     * @param p {@link Jabber} instance
     * @param j {@link ProtocolInteraction} instance
     */
    private void parseMessage(XmlNode msg) {
        msg.removeNode("h" + "tml");

        String type = msg.getAttribute(S_TYPE);
        boolean isGroupchat = ("groupc" + "hat").equals(type);
        boolean isError = S_ERROR.equals(type);

        String fullJid = msg.getAttribute(S_FROM);
        boolean isConference = isConference(fullJid);
        if (!isGroupchat) {
            fullJid = realJidToJimmJid(fullJid);
        }
        String from = getShortJid(fullJid);
        if (isConference && !isGroupchat) {
            from = fullJid;
        }
        String resource = getResource(fullJid, null);

        String subject = msg.getFirstNodeValue(S_SUBJECT);
        String text = msg.getFirstNodeValue(S_BODY);
        if ((null != subject) && (null == text)) {
            text = "";
        }
        if ("jubo@nologin.ru".equals(from) && msg.contains("juick")) {
            parseJuickMessage(msg, text, "JuBo");
            return;
        }
        if ("juick@juick.com".equals(from)) {
            parseJuickMessage(msg, text, null);
            return;
        }

        if (!isConference) {
            if (msg.contains("atte" + "ntion")) {
                text = PlainMessage.CMD_WAKEUP;
                subject = null;
            }
        }
        // #sijapp cond.if modules_SOUND is "true" #
        if (isConference ? !isGroupchat : true) {
            parseChatState(msg, from);
        }
        // #sijapp cond.end #

        if (null == text) {
            if (msg.contains("recei" + "ved")) {
                setMessageSended(msg.getId(), PlainMessage.NOTIFY_FROM_CLIENT);
                return;
            }
            if (!isConference(from) && !isError) {
                parseMessageEvent(msg.getXNode("jabber:x:event"), from);
                // #sijapp cond.if modules_XSTATUSES is "true" #
                parseEvent(msg.getFirstNode("ev" + "ent"), fullJid);
                // #sijapp cond.end #
            }
            return;
        }
        // #sijapp cond.if modules_DEBUGLOG isnot "true" #
        msg.removeNode(S_SUBJECT);
        msg.removeNode(S_BODY);
        // #sijapp cond.end #
        if ((null != subject) && (-1 == text.indexOf(subject))) {
            text = subject + "\n\n" + text;
        }
        text = StringConvertor.trim(text);

        final JabberContact c = (JabberContact)getJabber().getItemByUIN(from);

        if (msg.contains(S_ERROR)) {
            final String errorText = getError(msg.getFirstNode(S_ERROR));
            if (null != errorText) {
                text = errorText + "\n-------\n" + text;
            }

        } else {
            if ((null != c) && msg.contains("c" + "aptcha")) {
                final JabberForm form = new JabberForm(JabberForm.TYPE_CAPTCHA,
                        getJabber(), from, "captcha");
                form.showCaptcha(msg);
                return;
            }

            final XmlNode oobNode = msg.getXNode("jabber:x:oob");
            if (null != oobNode) {
                String url = oobNode.getFirstNodeValue(S_URL);
                if (null != url) {
                    text += "\n\n" + url;
                    String desc = oobNode.getFirstNodeValue(S_DESC);
                    if (null != desc) {
                        text += "\n" + desc;
                    }
                    msg.removeNode(S_URL);
                    msg.removeNode(S_DESC);
                }
            }

            if (!isGroupchat && msg.contains("reques" + "t") && (null != msg.getId())) {
                putPacketIntoQueue("<message to='" + Util.xmlEscape(fullJid)
                    + "' id='" + Util.xmlEscape(msg.getId())
                    + "'><received xmlns='urn:xmpp:receipts'/></message>");
            }

            if (c instanceof JabberServiceContact) {
                isConference = ((JabberServiceContact)c).isConference();
                if ((null != subject) && isConference && isGroupchat) {
                    ((JabberServiceContact)c).setSubject(subject);
                    subject = null;
                    resource = null;
                }
            }
        }
        if (StringConvertor.isEmpty(text)) {
            return;
        }

        text = StringConvertor.convert(isMrim(from)
                ? StringConvertor.MRIM2JIMM : StringConvertor.JABBER2JIMM,
                text);

        final String date = getDate(msg);
        final boolean isOnlineMessage = (null == date);
        long time = isOnlineMessage ? Util.createCurrentDate(false) : Util.createDate(date, false);
        final PlainMessage message = new PlainMessage(from, getJabber(), time, text, !isOnlineMessage);

        if (null == c) {
            if (isConference && !isGroupchat) {
                prepareFirstPrivateMessage(from);
            }

        } else {
            if (isConference) {
                final JabberServiceContact conf = (JabberServiceContact)c;

                if (isGroupchat && (null != resource)) {
                    if (isOnlineMessage && resource.equals(conf.getMyName())) {
                        setMessageSended(msg.getId(), PlainMessage.NOTIFY_FROM_CLIENT);
                        return;
                    }
                    message.setName(conf.getNick(resource));
                }

            } else {
                c.setActiveResource(resource);
            }
        }

        getJabber().addMessage(message);
    }
    private void parseJuickMessage(XmlNode msg, String text, String botNick) {
        text = StringConvertor.notNull(text);
        String userNick = getNickFromNode(msg);
        if (null == userNick) {
            userNick = msg.getFirstNodeAttribute("juick", "uname");
        }
        if (StringConvertor.isEmpty(userNick)) {
            userNick = null;
        }
        String nick = userNick;
        if (null != botNick) {
            nick = StringConvertor.notNull(userNick) + "@" + botNick;
            //if (null != nick) {
            //    int index = text.indexOf(userNick);
            //    text = text.substring(index + userNick.length() + 2);
            //}
        }
        if ((null != userNick) && text.startsWith(userNick)) {
            text = text.substring(userNick.length() + 2);
        }

        int lastEnter = text.lastIndexOf('\n');
        if (-1 != lastEnter) {
            int http = text.indexOf("http", lastEnter);
            if (-1 != http) {
                text = text.substring(0, http - 1);
            }
        }
        text = StringConvertor.convert(StringConvertor.JABBER2JIMM, text);
        if (StringConvertor.isEmpty(text)) {
            return;
        }


        String date = getDate(msg);
        long time = (null == date) ? Util.createCurrentDate(false) : Util.createDate(date, false);
        PlainMessage message = new PlainMessage("juick@juick.com", getJabber(), time, text, false);
        if (null != nick) {
            message.setName(('@' == nick.charAt(0)) ? nick.substring(1) : nick);
        }
        getJabber().addMessage(message);
    }

    private String getError(XmlNode errorNode) {
        if (null == errorNode) {
            return S_ERROR;
        }
        String errorText = errorNode.getFirstNodeValue(S_TEXT);
        if (null == errorText) {
            errorText = errorNode.value;
        }
        if (null == errorText) {
            errorText = "error " + errorNode.getAttribute(S_CODE);
            if (null != errorNode.childAt(0)) {
                errorText += ": " + errorNode.childAt(0).name;
            }
        }
        return errorText;
    }

    private boolean isMechanism(XmlNode list, String myMechanism) {
        for (int i = 0; i < list.childrenCount(); ++i) {
            XmlNode mechanism = list.childAt(i);
            if (mechanism.is("mechanism") && myMechanism.equals(mechanism.value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse the <<lit>stream:features</lit>> node and launch apropriate action
     *
     * @param x {@link XmlNode} to parse
     * @param p {@link Jabber} instance
     * @param j {@link ProtocolInteraction} instance
     */
    private void parseStreamFeatures(XmlNode x) throws JimmException {
        XmlNode x2 = null;
        if (0 == x.childrenCount()) {
            nonSaslLogin();
            return;
        }
        // #sijapp cond.if modules_ZLIB is "true" #
        /* Check for stream compression method */
        x2 = x.getFirstNode("compression");
        if ((null != x2) && "zlib".equals(x2.getFirstNodeValue("method"))) {
            sendRequest("<compress xmlns='http://jabber.org/protocol/compress'><method>zlib</method></compress>");
            return;
        }
        // #sijapp cond.end #

        /* Check for authentication mechanisms */
        x2 = x.getFirstNode("mechanisms");
        if ((null != x2) && x2.contains("mechanism")) {

            String auth = "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' ";


            String googleToken = null;
            /* X-GOOGLE-TOKEN authentication */
            if (isMechanism(x2, "X-GOOGLE-TOKEN")) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.systemPrintln("[INFO-JABBER] Using X-GOOGLE-TOKEN");
                // #sijapp cond.end #
                isGTalk_ = true;
                googleToken = getGoogleToken(getJabber().getUserId(), getJabber().getPassword());
                if (null == googleToken) {
                    throw new JimmException(111, 1);
                }
            }

            /* DIGEST-MD5 authentication */
            if (isMechanism(x2, "DIGEST-MD5")) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.systemPrintln("[INFO-JABBER] Using DIGEST-MD5");
                // #sijapp cond.end #
                auth += "mechanism='DIGEST-MD5'/>";

            } else if (null != googleToken) {
                auth += "mechanism='X-GOOGLE-TOKEN'>" + googleToken + "</auth>";

                /* PLAIN authentication */
            } else if (isMechanism(x2, "PLAIN")) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.systemPrintln("[INFO-JABBER] Using PLAIN");
                // #sijapp cond.end #
                auth += "mechanism='PLAIN'>";
                Util data = new Util();
                data.writeString(getJabber().getUserId(), true);
                data.writeByte(0);
                data.writeString(getNick(getJabber().getUserId()), true);
                data.writeByte(0);
                data.writeString(getJabber().getPassword(), true);
                auth += MD5.toBase64(data.toByteArray());
                auth += "</auth>";

            } else if (isGTalk_) {
                nonSaslLogin();
                return;

            } else {
                /* Unknown authentication method */
                setAuthStatus(false);
                return;
            }

            sendRequest(auth);
            return;
        }
        /* Check for resource bind */
        if (x.contains("bind")) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.systemPrintln("[INFO-JABBER] Send bind request");
            // #sijapp cond.end #
            sendRequest("<iq type='set' id='bind'>"
                    + "<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>"
                    + "<resource>" + Util.xmlEscape(resource) + "</resource>"
                    + "</bind>"
                    + "</iq>");
            return;
        }
        x2 = x.getFirstNode("a"+"uth", "http://jabber.org/features/iq-auth");
        if (null != x2) {
            nonSaslLogin();
            return;
        }

    }

    /**
     * Parse the <<lit>challenge</lit>> node and launch apropriate action
     *
     * @param x {@link XmlNode} to parse
     * @param p {@link Jabber} instance
     * @param j {@link ProtocolInteraction} instance
     */
    private void parseChallenge(XmlNode x) throws JimmException {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.systemPrintln("[INFO-JABBER] Received challenge");
        // #sijapp cond.end #
        String challenge = MD5.decodeBase64(x.value);
        String resp = "<response xmlns='urn:ietf:params:xml:ns:xmpp-sasl'";

        int nonceIndex = challenge.indexOf("nonce=");
        if (nonceIndex >= 0) {
            nonceIndex += 7;
            String nonce = challenge.substring(nonceIndex, challenge.indexOf('\"', nonceIndex));
            String cnonce = "123456789abcd";

            resp += ">";
            resp += responseMd5Digest(
                    JabberXml.getNick(getJabber().getUserId()),
                    getJabber().getPassword(),
                    domain_,// TODO check it
                    "xmpp/" + domain_,
                    nonce,
                    cnonce);
            resp += "</response>";
        } else {
            resp += "/>";
        }


        sendRequest(resp);
    }

    /**
     * This routine generates MD5-DIGEST response via SASL specification
     * (From BOMBUS project)
     *
     * @param user
     * @param pass
     * @param realm
     * @param digest_uri
     * @param nonce
     * @param cnonce
     * @return
     */
    private static String responseMd5Digest(String user, String pass,
            String realm, String digestUri, String nonce, String cnonce) {
        MD5 hUserRealmPass = new MD5();
        hUserRealmPass.init();
        hUserRealmPass.updateASCII(user);
        hUserRealmPass.update((byte) ':');
        hUserRealmPass.updateASCII(realm);
        hUserRealmPass.update((byte) ':');
        hUserRealmPass.updateASCII(pass);
        hUserRealmPass.finish();

        MD5 hA1 = new MD5();
        hA1.init();
        hA1.update(hUserRealmPass.getDigestBits());
        hA1.update((byte) ':');
        hA1.updateASCII(nonce);
        hA1.update((byte) ':');
        hA1.updateASCII(cnonce);
        hA1.finish();

        MD5 hA2 = new MD5();
        hA2.init();
        hA2.updateASCII("AUTHENTICATE:");
        hA2.updateASCII(digestUri);
        hA2.finish();

        MD5 hResp = new MD5();
        hResp.init();
        hResp.updateASCII(hA1.getDigestHex());
        hResp.update((byte) ':');
        hResp.updateASCII(nonce);
        hResp.updateASCII(":00000001:");
        hResp.updateASCII(cnonce);
        hResp.updateASCII(":auth:");
        hResp.updateASCII(hA2.getDigestHex());
        hResp.finish();

        return MD5.toBase64(StringConvertor.stringToByteArrayUtf8(
                new StringBuffer()
                .append("username=\"").append(user)
                .append("\",realm=\"").append(realm)
                .append("\",nonce=\"").append(nonce)
                .append("\",nc=00000001,cnonce=\"").append(cnonce)
                .append("\",qop=auth,digest-uri=\"").append(digestUri)
                .append("\",response=\"").append(hResp.getDigestHex())
                .append("\",charset=utf-8").toString()));
    }


    /**
     * Generates X-GOOGLE-TOKEN response by communication with
     * http://www.google.com
     * (From mGTalk project)
     *
     * @param userName
     * @param passwd
     * @return
     */
    private String getGoogleToken(String jid, String passwd) {
        try {
            String escapedJid = Util.urlEscape(jid);
            String first = "Email=" + escapedJid
                    + "&Passwd=" + Util.urlEscape(passwd)
                    + "&PersistentCookie=false&source=googletalk";

            HttpsConnection c = (HttpsConnection) Connector
                    .open("https://www.google.com:443/accounts/ClientAuth?" + first);

            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.systemPrintln("[INFO-JABBER] Connecting to www.google.com");
            // #sijapp cond.end #
            DataInputStream dis = c.openDataInputStream();
            String str = readLine(dis);
            if (str.startsWith("SID=")) {
                String SID = str.substring(4, str.length());
                str = readLine(dis);
                String LSID = str.substring(5, str.length());
                first = "SID=" + SID + "&LSID=" + LSID + "&service=mail&Session=true";
                dis.close();
                c.close();
                c = (HttpsConnection) Connector
                        .open("https://www.google.com:443/accounts/IssueAuthToken?" + first);

                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.systemPrintln("[INFO-JABBER] Next www.google.com connection");
                // #sijapp cond.end #
                dis = c.openDataInputStream();
                str = readLine(dis);

                Util data = new Util();
                data.writeByte(0);
                data.writeString(JabberXml.getNick(jid), true);
                data.writeByte(0);
                data.writeString(str, true);
                String token = MD5.toBase64(data.toByteArray());
                dis.close();
                c.close();
                return token;
            }

        } catch (Exception ex) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            ex.printStackTrace();
            DebugLog.systemPrintln("EX: " + ex.toString());
            // #sijapp cond.end #
        }
        return null;
    }

    /**
     * Service routine for google token
     * (From mGTalk project)
     *
     * @param dis
     * @return
     */
    private static String readLine(DataInputStream dis) {
        StringBuffer s = new StringBuffer();
        try {
            for (byte ch = dis.readByte(); ch != -1; ch = dis.readByte()) {
                if (ch == '\n') {
                    return s.toString();
                }
                s.append((char)ch);
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            e.printStackTrace();
            // #sijapp cond.end#
        }
        return s.toString();
    }

    private void updateConfContact(JabberServiceContact conf, String resource,
                String status, String statusText, int role) {
        conf.setStatus(resource, role, nativeStatus2StatusIndex(status), statusText);
    }
    private void updateConfPrivate(String jid, String resource) {
        String privateJid = realJidToJimmJid(jid + '/' + resource);
        Contact privateContact = getJabber().getItemByUIN(privateJid);
        if (null != privateContact) {
            ((JabberServiceContact)privateContact).setPrivateContactStatus();
            getJabber().ui_changeContactStatus(privateContact);
        }
    }

    public void updateContacts(Vector contacts) {
        StringBuffer xml = new StringBuffer();

        int itemCount = 0;
        xml.append("<iq type='set' id='").append(generateId())
                .append("'><query xmlns='jabber:iq:roster'>");
        for (int i = 0; i < contacts.size(); i++) {
            JabberContact contact = (JabberContact)contacts.elementAt(i);
            if (isConference(contact.getUin())) {
                continue;
            }
            itemCount++;
            xml.append("<item name='");
            xml.append(Util.xmlEscape(contact.getName()));
            xml.append("' jid='");
            xml.append(Util.xmlEscape(contact.getUin()));
            Group group = contact.getGroup();
            if (null != group) {
                xml.append("'><group>");
                xml.append(Util.xmlEscape(group.getName()));
                xml.append("</group></item>");
            } else {
                xml.append("'/>");
            }
        }
        xml.append("</query></iq>");
        if (0 < itemCount) {
            putPacketIntoQueue(xml.toString());
        }
    }
    private void parseRosterExchange(XmlNode x, String domain) {
        StringBuffer xml = new StringBuffer();
        Jabber j = (Jabber)protocol;
        Vector subscribes = new Vector();
        for (int i = 0; i < x.childrenCount(); ++i) {
            XmlNode item = x.childAt(i);
            String jid = item.getAttribute(XmlNode.S_JID);
            if ((null != domain) && !jid.endsWith(domain)) {
                continue;
            }
            boolean isDelete = item.getAttribute("a" + "ction").equals("d" + "elete");
            boolean isModify = item.getAttribute("a" + "ction").equals("m" + "odify");

            JabberContact contact = (JabberContact)j.getItemByUIN(jid);
            if (null == contact) {
                if (isModify || isDelete) {
                    continue;
                }
                contact = (JabberContact)j.createTempContact(jid);
                contact.setBooleanValue(Contact.CONTACT_NO_AUTH, true);
            }
            String group = item.getFirstNodeValue(S_GROUP);
            if (!isDelete) {
                contact.setName(item.getAttribute(XmlNode.S_NAME));
                if (StringConvertor.isEmpty(group)) {
                    group = contact.getDefaultGroupName();
                }
                contact.setGroup(j.getOrCreateGroup(group));
                if ((null != group) && group.equals(contact.getDefaultGroupName())) {
                    group = null;
                }
                contact.setTempFlag(false);
                if (!contact.isAuth()) {
                    subscribes.addElement(contact);
                }
            }

            xml.append("<item jid='").append(Util.xmlEscape(jid));
            if (isDelete) {
                xml.append("' subscription='remove'/>");
                continue;
            } else if (!isModify) {
                xml.append("' ask='subscribe");
            }
            xml.append("' name='");
            xml.append(Util.xmlEscape(contact.getName()));
            if (null != group) {
                xml.append("'><group>")
                        .append(Util.xmlEscape(group))
                        .append("</group></item>");
            } else {
                xml.append("'/>");
            }
        }
        if (0 < xml.length()) {
            putPacketIntoQueue("<iq type='set' id='" + generateId()
                    + "'><query xmlns='jabber:iq:roster'>"
                    + xml.toString() + "</query></iq>");
            xml = new StringBuffer();
            for (int i = 0; i < subscribes.size(); ++i) {
                xml.append("<presence type='subscribe' to='")
                        .append(Util.xmlEscape(((Contact)subscribes.elementAt(i)).getUin()))
                        .append("'/>");
            }
            if (0 < xml.length()) {
                putPacketIntoQueue(xml.toString());
            }
        }
    }


    public String getConferenceStorage() {
        StringBuffer xml = new StringBuffer();
        Vector contacts = getJabber().getContactItems();
        xml.append("<storage xmlns='storage:bookmarks'>");
        for (int i = 0; i < contacts.size(); i++) {
            JabberContact contact = (JabberContact)contacts.elementAt(i);
            if (!contact.isConference() || contact.isTemp()) {
                continue;
            }
            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);

            JabberServiceContact conf = (JabberServiceContact)contact;
            xml.append("<conference autojoin='");
            xml.append(conf.isAutoJoin() ? S_TRUE : S_FALSE);
            xml.append("' name='");
            xml.append(Util.xmlEscape(contact.getName()));
            xml.append("' jid='");
            xml.append(Util.xmlEscape(contact.getUin()));
            if (!StringConvertor.isEmpty(conf.getPassword())) {
                xml.append("' password='");
                xml.append(Util.xmlEscape(conf.getPassword()));
            }
            xml.append("'><nick>");
            xml.append(Util.xmlEscape(conf.getMyName()));
            xml.append("</nick></conference>");
        }
        xml.append("</storage>");
        return xml.toString();
    }
    public void saveConferences() {
        StringBuffer xml = new StringBuffer();

        String storage = getConferenceStorage();
        xml.append("<iq type='set'><query xmlns='jabber:iq:private'>");
        xml.append(storage);
        xml.append("</query></iq>");

        // XEP-0048
        if (xep0048) {
            xml.append("<iq type='set'>");
            xml.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
            xml.append("<publish node='storage:bookmarks'><item id='current'>");
            xml.append(storage);
            xml.append("</item></publish></pubsub></iq>");
        }

        putPacketIntoQueue(xml.toString());
    }
    public void removeGateContacts(String gate) {
        if (StringConvertor.isEmpty(gate)) {
            return;
        }
        gate = "@" + gate;
        Vector contacts = getJabber().getContactItems();
        StringBuffer xml = new StringBuffer();

        xml.append("<iq type='set' id='").append(generateId())
            .append("'><query xmlns='jabber:iq:roster'>");
        for (int i = 0; i < contacts.size(); i++) {
            JabberContact contact = (JabberContact)contacts.elementAt(i);
            if (!contact.getUin().endsWith(gate)) {
                continue;
            }

            xml.append("<item subscription='remove' jid='");
            xml.append(Util.xmlEscape(contact.getUin()));
            xml.append("'/>");
        }
        xml.append("</query></iq>");

        putPacketIntoQueue(xml.toString());
    }

    public void updateContact(JabberContact contact) {
        if (contact.isConference() && isConference(contact.getUin()) && !isGTalk_) {
            contact.setTempFlag(false);
            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
            String groupName = contact.getDefaultGroupName();
            Group group = getJabber().getOrCreateGroup(groupName);
            contact.setGroup(group);
            saveConferences();
            return;
        }

        Group g = contact.getGroup();
        if (contact.isConference()) {
            g = getJabber().getOrCreateGroup(contact.getDefaultGroupName());

        } else if (g.getName().equals(contact.getDefaultGroupName())) {
            g = null;
        }

        putPacketIntoQueue("<iq type='set' id='" + generateId()
                + "'><query xmlns='jabber:iq:roster'>"
                + "<item name='" + Util.xmlEscape(contact.getName())
                + "' jid='" + Util.xmlEscape(contact.getUin()) + "'>"
                + (null == g ? "" : "<group>" + Util.xmlEscape(g.getName()) + "</group>")
                + "</item>"
                + "</query></iq>");
    }
    public void removeContact(String jid) {
        if (isConference(jid) && !isGTalk_) {
            saveConferences();
        }
        putPacketIntoQueue("<iq type='set' id='" + generateId()
                + "'><query xmlns='jabber:iq:roster'>"
                + "<item subscription='remove' jid='" + Util.xmlEscape(jid) + "'/>"
                + "</query></iq>");
    }

    public void getBookmarks() {
        putPacketIntoQueue("<iq type='get' id='0'><query xmlns='jabber:iq:private'><storage xmlns='storage:bookmarks'/></query></iq>");
        // XEP-0048
        if (xep0048) {
            putPacketIntoQueue("<iq type='get' id='1'><pubsub xmlns='http://jabber.org/protocol/pubsub'><items node='storage:bookmarks'/></pubsub></iq>");
        }
    }


    /**
     * Get roster request
     */
    private static final String GET_ROSTER_XML = "<iq type='get' id='roster'>"
            + "<query xmlns='jabber:iq:roster'/>"
            + "</iq>";

    /**
     * Get open stream request
     */
    private final String getOpenStreamXml(String server) {
        return "<?xml version='1.0'?>"
                + "<stream:stream xmlns='jabber:client' "
                + "xmlns:stream='http:/" + "/etherx.jabber.org/streams' "
                + "version='1.0' "
                + "to='" + server + "'"
                + " xml:lang='" + jimm.util.ResourceBundle.getLanguageCode()+ "'>";
    }

    private void getVCard(String jid) {
        putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(generateId(S_VCARD)) + "'>"
                + "<vCard xmlns='vcard-temp' version='2.0' prodid='-/"
                + "/HandGen/" + "/NONSGML vGen v1.0/" + "/EN'/>"
                + "</iq>");
    }

    private void sendMessage(String to, String msg, String type, boolean notify, String id) {
        to = jimmJidToRealJid(to);
        boolean buzz = msg.startsWith(PlainMessage.CMD_WAKEUP) && S_CHAT.equals(type);
        if (buzz) {
            type = S_HEADLINE;
            notify = false;
            if (!isContactOverGate(to)) {
                msg = msg.substring(PlainMessage.CMD_WAKEUP.length()).trim();
                if (StringConvertor.isEmpty(msg)) {
                    msg = "/me " + ResourceBundle.getString("wake_you_up");
                }
            }
        }
        if (isMrim(to)) {
            msg = StringConvertor.convert(StringConvertor.JIMM2MRIM, msg);
        }
        String chatState = "";
        if ((1 < Options.getInt(Options.OPTION_TYPING_MODE)) && S_CHAT.equals(type)) {
            chatState = getChatStateTag(S_ACTIVE);
        }
        putPacketIntoQueue("<message to='" + Util.xmlEscape(to) + "'"
                + " type='" + type + "' id='" + Util.xmlEscape(id) + "'>"
                + (isGTalk_ ? "<nos:x value='disabled' xmlns:nos='google:nosave'/>" : "")
                + (buzz ? "<attention xmlns='urn:xmpp:attention:0'/>" : "")
                + "<body>" + Util.xmlEscape(msg) + "</body>"
                + (notify ? "<request xmlns='urn:xmpp:receipts'/><x xmlns='jabber:x:event'><offline/></x>" : "")
                + chatState
                + "</message>");
    }
    private static final String S_CHAT = "c" + "hat";
    private static final String S_GROUPCHAT = "groupc" + "hat";
    private static final String S_HEADLINE = "h" + "eadline";

    void sendMessage(String to, String msg) {
        String type = S_CHAT;
        if (isConference(to) && (-1 == to.indexOf('/'))) {
            type = S_GROUPCHAT;
        }
        sendMessage(to, msg, type, false, generateId());
    }

    /**
     * Sends a message to a user
     *
     * @param msg Message to send
     * @param to Receivers jid
     */
    void sendMessage(PlainMessage message) {
        JabberContact toContact = (JabberContact)message.getRcvr();
        String to = (null == toContact) ? message.getRcvrUin() : toContact.getReciverJid();
        String type = S_CHAT;
        if (isConference(to) && (-1 == to.indexOf('/'))) {
            type = S_GROUPCHAT;
        }
        message.setMessageId((int)(System.currentTimeMillis() % 0xFFFF));
        boolean notify = true;

        sendMessage(to, message.getText(), type, S_CHAT.equals(type),
                String.valueOf(message.getMessageId()));

        if (notify) {
            messages.addElement(message);
            // remove old messages
            setMessageSended(null, PlainMessage.NOTIFY_FROM_CLIENT);
        }
    }
    private String getChatStateTag(String state) {
        return "<" + state + " xmlns='http://jabber.org/protocol/chatstates'/>";
    }
    void sendTypingNotify(String to, boolean cancel) {
        String tag = getChatStateTag(cancel ? S_PAUSED : S_COMPOSING);
        putPacketIntoQueue("<message to='" + Util.xmlEscape(to)
                + "' id='0'>" + tag + "</message>");
    }


    public static final int PRESENCE_UNAVAILABLE = -1;
    void presence(JabberServiceContact conf, String to, int priority, String password) {
        String xml;
        if (0 <= priority) {
            xml = "<presence to='"+ Util.xmlEscape(to) + "'>";

            if (conf.isConference()) {
                String xNode = "";
                if (!StringConvertor.isEmpty(password)) {
                    xNode += "<password>" + Util.xmlEscape(password) + "</password>";
                }
                long time = conf.hasChat() ? conf.getChat().getLastMessageTime() : 0;
                time = (0 == time) ? 24*60*60 : (Util.createCurrentDate(false) - time);
                xNode += "<history maxstanzas='20' seconds='" + time + "'/>";
                if (!StringConvertor.isEmpty(xNode)) {
                    xml += "<x xmlns='http://jabber.org/protocol/muc'>" + xNode + "</x>";
                }

               } else {
                   xml += "<priority>" + priority + "</priority>";
               }

                Status status = getJabber().getStatus(); //global status
                xml += ("<show>" + getNativeStatus(status.getStatusIndex()) + "</show>"); //global status
                if (Options.getBoolean(Options.OPTION_TITLE_IN_CONFERENCE)) { //global status
                String xstatusTitle = getJabber().getProfile().xstatusTitle;  //global status
                xml += (StringConvertor.isEmpty(xstatusTitle) ? "" : "<status>" + Util.xmlEscape(xstatusTitle) + "</status>"); //global status
                }

            xml += (0 < priority ? "<priority>" + priority + "</priority>" : ""); // add
            xml += getCaps() + "</presence>";
            putPacketIntoQueue(xml);

        } else {
            putPacketIntoQueue("<presence type='unavailable' to='" + Util.xmlEscape(to)
                + "'><status>actum est, ilicet</status></presence>"); //text quit
        }
    }

    void setStatus(byte statusIndex, String msg, int priority) {
        setStatus(getNativeStatus(statusIndex), msg, priority);
    }
    void setStatus(String status, String msg, int priority) {
        // #sijapp cond.if modules_XSTATUSES is "true" #
        // FIXME
        JabberXStatus xstatus = getJabber().getXStatus();
        String xXml = getQipXStatus(xstatus);
        if (0 != xXml.length()) {
            msg = xstatus.getText();
        }
        // #sijapp cond.end #
        String xml = "<presence>"
                + (StringConvertor.isEmpty(status) ? "" : "<show>" + status + "</show>")
                + (StringConvertor.isEmpty(msg) ? "" : "<status>" + Util.xmlEscape(msg) + "</status>")
                + (0 < priority ? "<priority>" + priority + "</priority>" : "")
                + getCaps()
                // #sijapp cond.if modules_XSTATUSES is "true" #
                + xXml
                // #sijapp cond.end #
                + "</presence>";
        putPacketIntoQueue(xml);
	setConferencesStatus(status, msg, priority); //global status
        }

    void setConferencesStatus(String status, String msg, int priority) {                                 //global status
        String xml;                                                                                      //global status
        Vector contacts = getJabber().getContactItems();                                                 //global status
        for (int i = 0; i < contacts.size(); ++i) {                                                      //global status
        Contact contact = (Contact)contacts.elementAt(i);                                                //global status
        if (contact instanceof JabberContact) {                                                          //global status
        if (((JabberContact)contact).isConference() && contact.isOnline()) {           	                 //global status
        if (0 <= priority) {                                                                             //global status
            xml = "<presence to='" + contact.getUin() + "'>";                                            //global status
            xml += (StringConvertor.isEmpty(status) ? "" : "<show>" + status + "</show>");               //global status
            if (Options.getBoolean(Options.OPTION_TITLE_IN_CONFERENCE)) {                                //global status
            xml += (StringConvertor.isEmpty(msg) ? "" : "<status>" + Util.xmlEscape(msg) + "</status>"); //global status
            }                                                                                            //global status
            xml += (0 < priority ? "<priority>" + priority + "</priority>" : "");                        //global status
            xml += getCaps() + "</presence>";                                                            //global status
            putPacketIntoQueue(xml);                                                                     //global status
                }	                                                                                 //global status
            }	                                                                                         //global status
        }	                                                                                         //global status
    }                                                                                                    //global status
}                                                                                                        //global status

    public void sendSubscribed(String jid) {
        requestPresence(jid, "s" + "ubscribed");
    }
    public void sendUnsubscribed(String jid) {
        requestPresence(jid, "u" + "nsubscribed");
    }
    public void requestSubscribe(String jid) {
        requestPresence(jid, "s" + "ubscribe");
    }


    private void requestPresence(String jid, String type) {
        putPacketIntoQueue("<presence type='" + Util.xmlEscape(type) + "' to='" + Util.xmlEscape(jid) + "'/>");
    }
    private void requestIq(String jid, String xmlns, String id) {
        putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(jid)
                + "' id='" + Util.xmlEscape(id) + "'><query xmlns='" + xmlns + "'/></iq>");
    }
    private void requestIq(String jid, String xmlns) {
        requestIq(jid, xmlns, generateId());
    }

    public void requestClientVersion(String jid) {
        requestIq(jid, "jabber:iq:version");
    }
    public void requestConferenceInfo(String jid) {
        requestIq(jid, "http://jabber.org/protocol/disco#info");
    }
    public void requestConferenceUsers(String jid) {
        requestIq(jid, "http://jabber.org/protocol/disco#items");
        serviceDiscovery = getJabber().getServiceDiscovery();
    }

    public void requestDiscoItems(String server) {
        requestIq(server, "http://jabber.org/protocol/disco#items");
        serviceDiscovery = getJabber().getServiceDiscovery();
    }

    void requestRawXml(String xml) {
        putPacketIntoQueue(xml);
    }
    protected void sendClosePacket() {
        try {
            write("<presence type='unavailable'><status>Logged out</status></presence>");
        } catch (Exception e) {
        }
    }
    public void setMucRole(String jid, String nick, String role) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "'><query xmlns='http://jabber.org/protocol/muc#admin'><item nick='"
                + Util.xmlEscape(nick)
                + "' role='" + Util.xmlEscape(role)
                + "'/></query></iq>");
    }
    public void setMucAffiliation(String jid, String userJid, String affiliation) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "'><query xmlns='http://jabber.org/protocol/muc#admin'><item affiliation='"
                + Util.xmlEscape(affiliation)
                + "' jid='" + Util.xmlEscape(userJid)
                + "'/></query></iq>");
    }

    private UserInfo singleUserInfo;
    UserInfo getUserInfo(JabberContact contact) {
        singleUserInfo = new UserInfo(getJabber(), contact.getUin());
        getVCard(contact.getUin());
        return singleUserInfo;
    }
    private String autoSubscribeDomain;
    private JabberForm jabberForm;
    void register2(String rawXml, String jid) {
        autoSubscribeDomain = jid;
        requestRawXml(rawXml);
    }
    private boolean isAutoGateContact(String jid) {
        return !StringConvertor.isEmpty(autoSubscribeDomain)
        && (jid.equals(autoSubscribeDomain) || jid.endsWith('@' + autoSubscribeDomain));
    }
    void register(String jid) {
        jabberForm = new JabberForm(JabberForm.TYPE_REGISTER, getJabber(), jid, "registration");
        requestIq(jid, "jabber:iq:register", jabberForm.getId());
        jabberForm.show();
    }
    void unregister(String jid) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
        + "' id='unreg1'><query xmlns='jabber:iq:register'><remove/></query></iq>");
    }


    // #sijapp cond.if modules_XSTATUSES is "true" #
    private void sendXStatus(String xstatus, String text) {
        String[] path = Util.explode(Util.xmlEscape(xstatus), ':');
        StringBuffer sb = new StringBuffer();
        String typeUrl = "http://jabber.org/protocol/" + path[0];

        sb.append("<iq type='set' id='").append(generateId());
        sb.append("'><pubsub xmlns='http://jabber.org/protocol/pubsub'>");
        sb.append("<publish node='").append(typeUrl).append("'><item>");
        sb.append("<").append(path[0]).append(" xmlns='").append(Util.xmlEscape(typeUrl));
        if (1 == path.length) {
            sb.append("'/>");
        } else {
            sb.append("'><").append(path[1]);
            if (2 == path.length) {
                sb.append("/>");
            } else {
                sb.append("><").append(path[2]).append("/></").append(path[1]).append(">");
            }
            if (!StringConvertor.isEmpty(text)) {
                sb.append("<text>").append(Util.xmlEscape(text)).append("</text>");
            }
            sb.append("</").append(path[0]).append(">");
        }
        sb.append("</item></publish></pubsub></iq>");
        putPacketIntoQueue(sb.toString());
    }
    private String getQipXStatus(JabberXStatus xstatus) {
        if (JabberXStatus.noneXStatus == xstatus) {
            return "";
        }
        String code = xstatus.getCode();
        if ((null == code) || !code.startsWith(JabberXStatus.XSTATUS_START)) {
            return "";
        }
        if (code.equals(JabberXStatus.XSTATUS_TEXT_NONE)) {
            return "";
        }
        String id = code.substring(JabberXStatus.XSTATUS_START.length());
        return "<x xmlns='" + S_FEATURE_XSTATUS + "' id='"
                + Util.xmlEscape(id) + "'><title>"
                + Util.xmlEscape(xstatus.getText())
                + "</title></x>";
    }

    private static final String S_FEATURE_XSTATUS = "http://qip.ru/x-status";
    void setXStatus(JabberXStatus xstatus) {
        String xstatusCode = xstatus.getCode();
        if (null == xstatusCode) {
            return;
        }

        setXStatusToIcqTransports();

        if (xstatusCode.startsWith(JabberXStatus.XSTATUS_START)) {
            Status status = getJabber().getStatus();
            setStatus(getNativeStatus(status.getStatusIndex()), status.getText(), Jabber.PRIORITY);
            return;
        }
        final String mood = "mo"+"od";
        final String activity = "acti" + "vity";
        if (!xstatusCode.startsWith(mood)) {
            sendXStatus(mood, null);
        }
        if (!xstatusCode.startsWith(activity)) {
            sendXStatus(activity, null);
        }
        if (xstatusCode.startsWith(mood) || xstatusCode.startsWith(activity)) {
            sendXStatus(xstatusCode, xstatus.getText());
        }
    }

    private void setXStatusToIcqTransport(JabberServiceContact gate) {
        JabberXStatus x = getJabber().getXStatus();
        String xstatus = x.getIcqXStatus();
        if (null == xstatus) {
            return;
        }
        String desc = "None".equals(xstatus) ? null : x.getText();
        desc = StringConvertor.notNull(desc);
        if (gate.isOnline() && isPyIcqGate(gate.getUin())) {
            String out = "<iq type='set' id='" + generateId() + "' to='"
                + Util.xmlEscape(gate.getUin())
                + "'><command xmlns='http://jabber.org/protocol/commands' node='setxstatus' action='complete'><x xmlns='jabber:x:data' type='submit'><field var='xstatus_desc'><value>"
                + Util.xmlEscape(desc)
                + "</value></field><field var='xstatus_name'><value>"
                + Util.xmlEscape(xstatus)
                + "</value></field></x></command></iq>";
            putPacketIntoQueue(out);
        }
    }
    private void setXStatusToIcqTransports() {
        if (null == getJabber().getXStatus().getIcqXStatus()) {
            return;
        }
        Vector contacts = getJabber().getContactItems();
        for (int i = contacts.size() - 1; i >= 0; --i) {
            JabberContact c = (JabberContact)contacts.elementAt(i);
            if (c.isOnline() && isPyIcqGate(c.getUin())) {
                setXStatusToIcqTransport((JabberServiceContact)c);
            }
        }
    }
    // #sijapp cond.end #

    private String getVerHash(Vector features) {
        StringBuffer sb = new StringBuffer();
        sb.append("client/phone/" + "/Jimm<");
        for (int i = 0; i < features.size(); ++i) {
            sb.append(features.elementAt(i)).append('<');
        }
        return MD5.toBase64(new MD5().calculate(StringConvertor.stringToByteArrayUtf8(sb.toString())));
    }
    private String getFeatures(Vector features) {
        StringBuffer sb = new StringBuffer();
        sb.append("<identity category='client' type='phone' name='Jimm'/>");
        for (int i = 0; i < features.size(); ++i) {
            sb.append("<feature var='").append(features.elementAt(i)).append("'/>");
        }
        return sb.toString();
    }
    private void initFeatures() {
        Vector features = new Vector();
        features.addElement("bugs");
        // #sijapp cond.if modules_XSTATUSES is "true" #
        features.addElement("http://jabber.org/protocol/activity");
        features.addElement("http://jabber.org/protocol/activity+notify");
        // #sijapp cond.end #
        // #sijapp cond.if modules_SOUND is "true" #
        if (0 < Options.getInt(Options.OPTION_TYPING_MODE)) {
            features.addElement("http://jabber.org/protocol/chatstates");
        }
        // #sijapp cond.end #
        features.addElement("http://jabber.org/protocol/disco#info");
        // #sijapp cond.if modules_XSTATUSES is "true" #
        features.addElement("http://jabber.org/protocol/mood");
        features.addElement("http://jabber.org/protocol/mood+notify");
        // #sijapp cond.end #
        features.addElement("http://jabber.org/protocol/rosterx");
        // #sijapp cond.if modules_XSTATUSES is "true" #
        features.addElement(S_FEATURE_XSTATUS);//"http://qip.ru/x-status");
        // #sijapp cond.end #
        features.addElement("jabber:iq:last");
        features.addElement("jabber:iq:version");
        features.addElement("urn:xmpp:attention:0");
        features.addElement("urn:xmpp:time");

        verHash = getVerHash(features);
        featureList = getFeatures(features);
    }
    private String verHash = "";
    private String featureList = "";

    private Vector messages = new Vector();
    private void setMessageSended(String id, int state) {
        long msgId = Util.strToIntDef(id, -1);
        if (-1 < msgId) {
            PlainMessage msg = null;
            for (int i = 0; i < messages.size(); ++i) {
                PlainMessage m = (PlainMessage)messages.elementAt(i);
                if (m.getMessageId() == msgId) {
                    msg = m;
                    break;
                }
            }
            if (null != msg) {
                msg.setSendingState(state);
                messages.removeElement(msg);
            }
        }
        long date = Util.createCurrentDate(false) - 5 * 60;
        for (int i = messages.size() - 1; i >= 0; --i) {
            PlainMessage m = (PlainMessage)messages.elementAt(i);
            if (date > m.getNewDate()) {
                messages.removeElement(m);
            }
        }
    }

    // #sijapp cond.if modules_FILES is "true"#
    private IBBFileTransfer ibb;
    void setIBB(IBBFileTransfer transfer) {
        SplashCanvas.setMessage(ResourceBundle.getString("ft_transfer"));
        Progress.getProgress().setProgress(0);
        ibb = transfer;
        putPacketIntoQueue(ibb.getRequest());
    }
    private boolean processIbb(XmlNode iq, byte type, String id) {
        id = StringConvertor.notNull(id);
        if (!id.startsWith("jimmibb_")) {
            return false;
        }
        if (IQ_TYPE_RESULT != type) {
            // something bad happend
            ibb.destroy();
            ibb = null;
            jimm.cl.ContactList.activate();
            return true;
        }
        if ("jimmibb_si".equals(id)) {
            Progress.getProgress().setProgress(10);
            putPacketIntoQueue(ibb.initTransfer());
            return true;
        }

        if ("jimmibb_close".equals(id)) {
            return true;
        }
        if (Progress.getProgress().isCanceled()) {
            putPacketIntoQueue(ibb.close());
            ibb.destroy();
            ibb = null;
            return true;
        }

        Progress.getProgress().setProgress(ibb.getPercent());
        String stanza = ibb.nextBlock();
        if (null == stanza) {
            stanza = ibb.close();
            ibb.destroy();
            ibb = null;
            Progress.getProgress().setProgress(100);
        }
        putPacketIntoQueue(stanza);
        return true;
    }
    // #sijapp cond.end#
}
// #sijapp cond.end #