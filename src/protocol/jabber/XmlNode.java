/*
 * XmlNode.java
 *
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import jimm.JimmException;
import jimm.comm.StringConvertor;
import jimm.comm.Util;

/**
 * Very light-weight xml parser
 *
 * @author Matej Usaj
 * @author Vladimir Kryukov
 */
public final class XmlNode {
    public String name;
    public String value;
    private Hashtable attribs = new Hashtable();
    private Vector children = new Vector();
    
    private XmlNode() {}
    private XmlNode(String name) {
        this.name = name;
    }
    
    public XmlNode childAt(int index) {
        if (children.size() <= index) {
            return null;
        }
        return (XmlNode)children.elementAt(index);
    }
    public int childrenCount() {
        return children.size();
    }
    public String getAttribute(String key) {
        return (String)attribs.get(key);
    }
    public String getXmlns() {
        return getAttribute("xmlns");
    }
    public final static String S_ID = "i" + "d";
    public String getId() {
        return getAttribute(S_ID);
    }

    
    public static XmlNode parse(JabberXml jabberConnection) throws JimmException {
        char ch = jabberConnection.readChar();
        if ('<' != ch) {
            return null;
        }
        XmlNode xml = new XmlNode();
        boolean parsed = xml.parseNode(jabberConnection, removeXmlHeader(jabberConnection));
        return parsed ? xml : null;
    }
    
    private void setName(String tagName) {
        if (-1 == tagName.indexOf(':') || -1 != tagName.indexOf("stream:")) {
            name = tagName;
            return;
        }
        name = tagName.substring(tagName.indexOf(':') + 1);
    }

    private int getMaxDataSize(String name) {
        if (S_BINVAL.equals(name)) {
            if (jimm.Jimm.hasMemory(MAX_BIN_VALUE_SIZE * 2 * 2)) {
                return MAX_BIN_VALUE_SIZE * 2;
            }
            if (jimm.Jimm.hasMemory(MAX_BIN_VALUE_SIZE * 2)) {
                return MAX_BIN_VALUE_SIZE;
            }
        }
        return MAX_VALUE_SIZE;
    }
    private String readCdata(JabberXml jabberConnection) throws JimmException {
        StringBuffer out = new StringBuffer();
        char ch = jabberConnection.readChar();
        int maxSize = getMaxDataSize(name);
        int size = 0;
        for (int state = 0; state < 3;) {
            ch = jabberConnection.readChar();
            if (size == maxSize) {
                out.append(ch);
                size++;
            }
            if (']' == ch) {
                state = Math.min(state + 1, 2);
            } else if ((2 == state) && ('>' == ch)) {
                state++;
            } else {
                state = 0;
            }
            
        }
        out.delete(0, 7);
        out.delete(Math.max(0, out.length() - 3), out.length());
        return out.toString();
    }
    
    private void readEscapedChar(StringBuffer out, JabberXml jabberConnection) throws JimmException {
        StringBuffer buffer = new StringBuffer(6);
        int limit = 6;
        char ch = jabberConnection.readChar();
        while (';' != ch) {
            if (0 < limit) {
                buffer.append((char)ch);
                limit--;
            }
            ch = jabberConnection.readChar();
        }
        if (0 == buffer.length()) {
            out.append('&');
            return;
        }
        String code = buffer.toString();
        if ("quot".equals(code)) {
            out.append('\"');
        } else if ("gt".equals(code)) {
            out.append('>');
        } else if ("lt".equals(code)) {
            out.append('<');
        } else if ("apos".equals(code)) {
            out.append('\'');
        } else if ("amp".equals(code)) {
            out.append('&');
        } else if ('#' == buffer.charAt(0)) {
            try {
                buffer.deleteCharAt(0);
                int radix = 10;
                if ('x' == buffer.charAt(0)) {
                    buffer.deleteCharAt(0);
                    radix = 16;
                }
                out.append((char)Integer.parseInt(buffer.toString(), radix));
            } catch (Exception e) {
                out.append('?');
            }
        } else {
            out.append('&');
            out.append(code);
            out.append(';');
        }
    }
    private String readString(JabberXml jabberConnection, char endCh, int limit) throws JimmException {
        char ch = jabberConnection.readChar();
        if (endCh == ch) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        while (endCh != ch) {
            if (sb.length() < limit) {
                if ('\t' == ch) {
                    sb.append("  ");
                } else if ('&' != ch) {
                    sb.append(ch);
                } else {
                    readEscapedChar(sb, jabberConnection);
                }
            }
            ch = jabberConnection.readChar();
        }
        return sb.toString();
    }

    private boolean parseNode(JabberXml jabberConnection, char ch0) throws JimmException {
        // tag name
        char ch = ch0;
        if ('!' == ch) {
            readCdata(jabberConnection);
            return false;
        }
        if ('/' == ch) {
            ch = jabberConnection.readChar();
            while ('>' != ch) {
                ch = jabberConnection.readChar();
            }
            return false;
        }
        StringBuffer tagName = new StringBuffer();
        while (' ' != ch && '>' != ch) {
            tagName.append((char)ch);
            ch = jabberConnection.readChar();
            if ('/' == ch) {
                setName(tagName.toString());
                ch = jabberConnection.readChar(); // '>'
                return true;
            }
        }
        setName(tagName.toString());
        tagName = null;
        
        // tag attributes
        while ('>' != ch) {
            while (' ' == ch) {
                ch = jabberConnection.readChar();
            }
            if ('/' == ch) {
                ch = jabberConnection.readChar(); // '>'
                return true;
            }
            if ('>' == ch) {
                break;
            }
            StringBuffer attrName = new StringBuffer();
            while ('=' != ch) {
                attrName.append((char)ch);
                ch = jabberConnection.readChar();
            }
            
            char startValueCh = jabberConnection.readChar(); // '"' or '\''
            String attribValue = readString(jabberConnection, startValueCh, 2*1024);
            if (0 < attrName.length()) {
                if (null == attribValue) {
                    attribValue = "";
                }
                attribs.put(getAttrName(attrName), attribValue);
            }
            ch = jabberConnection.readChar();
        }
        if ("stream:stream".equals(name)) {
            return true;
        }
        // tag body
        value = readString(jabberConnection, '<', getMaxDataSize(name));
        
        // sub tags
        while (true) {
            ch = jabberConnection.readChar();
            if ('!' == ch) {
                value = readCdata(jabberConnection);
                
            } else {
                XmlNode xml = new XmlNode();
                if (!xml.parseNode(jabberConnection, ch)) {
                    break;
                }
                children.addElement(xml);
            }
            
            ch = jabberConnection.readChar();
            while ('<' != ch) {
                ch = jabberConnection.readChar();
            }
        }
        if (StringConvertor.isEmpty(value)) {
            value = null;
        }
        return true;
    }
    private static final int MAX_BIN_VALUE_SIZE = 80 * 1024; //it is changed
    private static final int MAX_VALUE_SIZE = 10 * 1024;
    public static final String S_JID = "j" + "i" + "d";
    public static final String S_NICK = "n" + "ick";
    public static final String S_NAME = "n" + "ame";
    public static final String S_ROLE = "ro" + "le";
    private static final String S_BINVAL = "BINVAL";
    public static final String S_XMLNS = "x" + "mlns";
            
    private String getAttrName(StringBuffer buffer) {
        String result = buffer.toString();
        if (S_JID.equals(result)) {
            return S_JID;
        } else if (S_NAME.equals(result)) {
            return S_NAME;
        }
        return result;
    }
    
    private static char removeXmlHeader(JabberXml jabberConnection) throws JimmException {
        char ch = jabberConnection.readChar();
        if ('?' != ch) {
            return ch;
        }
        while ('?' != ch) {
            ch = jabberConnection.readChar();
        }
        
        ch = jabberConnection.readChar(); // '>'
        
        ch = jabberConnection.readChar();
        while ('<' != ch) {
            ch = jabberConnection.readChar();
        }
        
        return jabberConnection.readChar();
    }
    
    public final XmlNode popChildNode() {
        XmlNode node = childAt(0);
        children.removeElementAt(0);
        return node;
    }
    public final void removeNode(String name) {
        for (int i = 0; i < children.size(); ++i) {
            if (childAt(i).is(name)) {
                children.removeElementAt(i);
                return;
            }
        }
    }

    public final boolean is(String name) {
        return this.name.equals(name);
    }

    public XmlNode getFirstNodeRecursive(String name) {
        for (int i = 0; i < children.size(); ++i) {
            XmlNode node = childAt(i);
            if (node.is(name)) {
                return node;
            }
            XmlNode result = node.getFirstNodeRecursive(name);
            if (null != result) {
                return result;
            }
        }
        return null;
    }
    public String getFirstNodeValueRecursive(String name) {
        XmlNode node = getFirstNodeRecursive(name);
        return (null == node) ? null : node.value;
    }
    
    /**
     * Get first occurance of a node with a specified name.<br>
     * This method goes in-depth first, not level-by-level
     *
     * @param name Name of the requested node
     * @return {@link XmlNode} node or null if the node was not found.
     */
    public XmlNode getFirstNode(String name) {
        for (int i = 0; i < children.size(); ++i) {
            XmlNode node = childAt(i);
            if (node.is(name)) {
                return node;
            }
        }
        return null;
    }
    

    public XmlNode getFirstNode(String name, String xmlns) {
        for (int i = 0; i < children.size(); ++i) {
            XmlNode node = childAt(i);
            if (node.is(name) && xmlns.equals(node.getXmlns())) {
                return node;
            }
        }
        return null;
    }
    
    public XmlNode getXNode(String xmlns) {
        return getFirstNode("x", xmlns);
    }

    public String getFirstNodeValue(String name) {
        XmlNode node = getFirstNode(name);
        return (null == node) ? null : node.value;
    }
    public String getFirstNodeValue(String parentNodeName, String nodeName) {
        XmlNode parentNode = getFirstNode(parentNodeName);
        return (null == parentNode) ? null : parentNode.getFirstNodeValue(nodeName);
    }
    public String getFirstNodeValue(String tag, String[] cond, String subtag) {
        for (int i = 0; i < childrenCount(); ++i) {
            XmlNode node = childAt(i);
            if (node.is(tag) && node.isContains(cond)) {
                return node.getFirstNodeValue(subtag);
            }
        }
        return null;
    }
    public String getFirstNodeValue(String tag, String[] subtags, String subtag, boolean isDefault) {
        String result = getFirstNodeValue(tag, subtags, subtag);
        if (null != result) {
            return result;
        }
        for (int i = 0; i < childrenCount(); ++i) {
            XmlNode node = childAt(i);
            if (node.is(tag) && (0 < node.childrenCount())) {
                XmlNode firstNode = node.childAt(0);
                if (null != firstNode.value) {
                    return node.getFirstNodeValue(subtag);
                }
            }
        }
        return null;
    }
    
    public String getFirstNodeAttribute(String name, String key) {
        XmlNode node = getFirstNode(name);
        return (null == node) ? null : node.getAttribute(key);
    }
    
    /**
     * Check if the xml contains a node with a specified name
     *
     * @param name Name of the requested node
     * @return true if the node was found, false otherwise.
     */
    public boolean contains(String name) {
        return null != getFirstNode(name);
    }
    
    // #sijapp cond.if modules_DEBUGLOG is "true" #
    private String _toString(StringBuffer sb, String spaces) {
        sb.append(spaces).append("<").append(name);
        if (0 != attribs.size()) {
            Enumeration e = attribs.keys();
            while (e.hasMoreElements()) {
                Object k = e.nextElement();
                sb.append(" ").append(k).append("='").append(attribs.get(k)).append("'");
            }
        }
        if (0 != childrenCount()) {
            sb.append(">");
            sb.append("\n");
            for (int i = 0; i < childrenCount(); i++) {
                childAt(i)._toString(sb, spaces + " ");
                sb.append("\n");
            }
            sb.append(spaces).append("</").append(name).append(">");
        } else if (null != value) {
            sb.append(">");
            sb.append(value);
            sb.append("</").append(name).append(">");
        } else {
            sb.append("/>");
        }
        return sb.toString();
    }
    public String toString() {
        StringBuffer sb = new StringBuffer();
        _toString(sb, "");
        return sb.toString();
    }
    // #sijapp cond.end #
    
    public String popValue() {
        String result = value;
        value = null;
        return result;
    }
    
    public byte[] popBinValue() {
        if (null == value) {
            return null;
        }
        return Util.base64decode(popValue());
    }
    public byte[] getBinValue() {
        if (null == value) {
            return null;
        }
        return Util.base64decode(value);
    }
    
    
    private boolean isContains(String[] subtags) {
        if (null == subtags) {
            return true;
        }
        int subTagsCount = 0;
        for (int subtagIndex = 0; subtagIndex < subtags.length; ++subtagIndex) {
            for (int i = 0; i < childrenCount(); ++i) {
                if (childAt(i).is(subtags[subtagIndex])) {
                    subTagsCount++;
                    break;
                }
            }
        }
        return subTagsCount == subtags.length;
    }

    public void setValue(String subtag, String value) {
        XmlNode content = getFirstNode(subtag);
        if (null == content) {
            content = new XmlNode(subtag);
            children.addElement(content);
        }
        content.value = value;
    }

    public void setValue(String tag, String[] subtags, String subtag, String value) {
        for (int i = 0; i < childrenCount(); ++i) {
            XmlNode node = childAt(i);
            if (node.is(tag) && node.isContains(subtags)) {
                node.setValue(subtag, value);
                return;
            }
        }
        if (StringConvertor.isEmpty(value)) {
            return;
        }

        XmlNode node = new XmlNode(tag);
        children.addElement(node);
        if (null != subtags) {
            for (int i = 0; i < subtags.length; ++i) {
                node.children.addElement(new XmlNode(subtags[i]));
            }
        }
        node.setValue(subtag, value);
    }

    private boolean isEmptySubNodes() {
        if (null != value) return false;
        for (int i = childrenCount() - 1; i >= 0; --i) {
            if (null != childAt(i).value) {
                return false;
            }
        }
        return true;
    }
    public void cleanXmlTree() {
        for (int i = childrenCount() - 1; i >= 0; --i) {
            if (childAt(i).isEmptySubNodes()) {
                children.removeElementAt(i);
            }
        }
    }

    public void toString(StringBuffer sb) {
        sb.append('<').append(name);
        if (0 != attribs.size()) {
            Enumeration e = attribs.keys();
            while (e.hasMoreElements()) {
                String k = (String)e.nextElement();
                String v = (String)attribs.get(k);
                sb.append(' ').append(Util.xmlEscape(k)).append("='")
                        .append(Util.xmlEscape(v)).append("'");
            }
        }
        if ((0 == childrenCount()) && StringConvertor.isEmpty(value)) {
            sb.append("/>");
            return;
        }
        sb.append('>');

        if (0 != childrenCount()) {
            for (int i = 0; i < childrenCount(); ++i) {
                childAt(i).toString(sb);
            }

        } else if (null != value) {
            sb.append(Util.xmlEscape(value));
        }

        sb.append("</").append(name).append(">");
    }
    public static XmlNode getEmptyVCard() {
        XmlNode vCard = new XmlNode("vCard");
        vCard.attribs.put(S_XMLNS, "vcard-temp");
        vCard.attribs.put("v"+"ersion", "2.0");
        vCard.attribs.put("prodid", "-/"+"/HandGen/"+"/NONSGML vGen v1.0/"+"/EN");
        return vCard;
    }
}
// #sijapp cond.end #