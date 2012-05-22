/*
 * JabberForm.java
 *
 * Created on 12 Март 2009 г., 0:19
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.cl.ContactList;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.ui.FormEx;
import jimm.ui.PopupWindow;
import jimm.util.ResourceBundle;

/**
 *
 * @author Vladimir Krukov
 */
public class JabberForm implements CommandListener {
    private FormEx form;
    private JabberRegistration registration;
    private Jabber jabber;
    private String jid;
    private String id;
    private boolean waitingForm;
    public final static byte TYPE_REGISTER = 0;
    public final static byte TYPE_CAPTCHA = 1;
    public final static byte TYPE_NEW_ACCOUNT_DOMAIN = 2;
    public final static byte TYPE_NEW_ACCOUNT_CREATE = 3;
    private byte type = TYPE_CAPTCHA;

    
    private final static String S_EMAIL = "emai" + "l";
    private final static String S_USERNAME = "u" + "sername";
    private final static String S_PASSWORD = "p" + "assword";
    private final static String S_KEY = "k" + "e" + "y";
    /** Creates a new instance of JabberForm */
    public JabberForm(byte formType, Jabber protocol, String resourceJid, String title) {
        form = new FormEx(title, "ok", "back", this);
        jabber = protocol;
        jid = resourceJid;
        type = formType;
        id = newId();
        waitingForm = true;
    }
    public JabberForm(JabberRegistration reg) {
        form = new FormEx("registration", "ok", "cancel", this);
        byte formType = JabberForm.TYPE_NEW_ACCOUNT_DOMAIN;
        jabber = null;
        registration = reg;
        jid = "";
        type = formType;
        id = newId();
        waitingForm = true;
    }
    private String newId() {
        return "forms" + (System.currentTimeMillis() % 0xFFFF);
    }
    public String getJid() {
        return jid;
    }
    public boolean isWaiting() {
        return waitingForm;
    }
    public void show() {
        if (JabberForm.TYPE_NEW_ACCOUNT_DOMAIN == type) {
            form.addString(ResourceBundle.getString("new jabber-account"));
            form.addTextField(0, "domain", "jabga.ru", 50, TextField.ANY);
        } else {
            form.addString(ResourceBundle.getString("wait"));
        }
        form.endForm();
        form.show();
    }

    void back() {
        form.back();
    }
    public String getId() {
        return id;
    }

    private String getCaptchaXml() {
        return "<iq type='set' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(id)
                + "'><captcha xmlns='urn:xmpp:captcha'>"
                + getXmlForm()
                + "</captcha></iq>";
    }
    private String getRegisterXml() {
        return "<iq type='set' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(id)
                + "'><query xmlns='jabber:iq:register'>"
                + getXmlForm()
                + "</query></iq>";
    }

    private void doAction() {
        switch (type) {
            case TYPE_REGISTER:
                jabber.getConnection().register2(getRegisterXml(), getJid());
                break;
            case TYPE_CAPTCHA:
                jabber.getConnection().requestRawXml(getCaptchaXml());
                form.back();
                break;
            case TYPE_NEW_ACCOUNT_DOMAIN:
                String domain = form.getTextFieldValue(0);
                if (!StringConvertor.isEmpty(domain)) {
                    form.clearForm();
                    form.addString(ResourceBundle.getString("wait"));
                    jid = domain;
                    registration.getForm(jid);
                    type = TYPE_NEW_ACCOUNT_CREATE;
                }
                break;
            case TYPE_NEW_ACCOUNT_CREATE:
                registration.setUsername(getField(S_USERNAME));
                registration.setPassword(getField(S_PASSWORD));
                registration.register(getRegisterXml());
                form.back();
                break;
        }
    }
    public void commandAction(Command command, Displayable displayable) {
        if (form.saveCommand == command) {
            if ((0 < fields.size()) || (TYPE_NEW_ACCOUNT_DOMAIN == type)) {
                doAction();
            }
        }
        if (form.backCommand == command) {
            switch (type) {
            case TYPE_NEW_ACCOUNT_DOMAIN:
            case TYPE_NEW_ACCOUNT_CREATE:
                registration.cancel();
                break;
            }
            form.back();
        }
    }
    private void clearForm() {
        form.clearForm();
    }
    private void addInfo(String title, String instructions) {
        form.addString(title, instructions);
    }
    
    private boolean isXData;
    private Vector fields = new Vector();
    private Vector types = new Vector();
    private Vector values = new Vector();
    private void addField(String name, String type, String label, String value) {
        int num = fields.size();
        name = StringConvertor.notNull(name);
        type = StringConvertor.notNull(type);
        value = StringConvertor.notNull(value);
        fields.addElement(name);
        types.addElement(type);
        values.addElement(value);
        
        if (S_HIDDEN.equals(type)) {
            
        } else if (S_FIXED.equals(type)) {
            form.addString(value);
            
        } else if (S_TEXT_SINGLE.equals(type)) {
            form.addTextField(num, label, value, 64, TextField.ANY);

        } else if (S_TEXT_MULTI.equals(type)) {
            int size = Math.max(512, value.length());
            form.addTextField(num, label, value, size, TextField.ANY);
            
        } else if (S_TEXT_PRIVATE.equals(type)) {
            form.addTextField(num, label, value, 64, TextField.PASSWORD);
            
        } else if (S_BOOLEAN.equals(type)) {
            form.addCheckBox(num, label, JabberXml.isTrue(value));

        } else if ("".equals(type)) {
    	    form.addTextField(num, label, value, 64, TextField.ANY);
        }
    }
    public void showCaptcha(XmlNode baseNode) {
        final String S_CAPTCHA = "c" + "aptcha";
        XmlNode captcha = baseNode.getFirstNodeRecursive(S_CAPTCHA);
        id = baseNode.getAttribute("i" + "d");
        loadFromXml(captcha, baseNode);
        form.show();
    }
    private String getField(String name) {
        for (int i = 0; i < fields.size(); ++i) {
            String field = (String)fields.elementAt(i);
            if (name.equals(field)) {
                return form.getTextFieldValue(i);
            }
        }
        return null;
    }
    private void addField(XmlNode field, String type) {
        final String S_VALUE = "va" + "lue";
        final String S_OPTION = "o" + "ption";
        final String S_LABEL = "la" + "bel";
        String name = field.getAttribute("var");
        String label = field.getAttribute(S_LABEL);
        String value = field.getFirstNodeValue(S_VALUE);
        if (S_LIST_SINGLE.equals(type)) {
            int selectedIndex = 0;
            int totalCount = 0;
            StringBuffer items = new StringBuffer();
            StringBuffer labels = new StringBuffer();
            field.removeNode(S_VALUE);
            for (int i = 0;i < field.childrenCount(); ++i) {
                XmlNode opt = field.childAt(i);
                if (S_OPTION.equals(opt.name)) {
                    String curValue = opt.getFirstNodeValue(S_VALUE);
                    labels.append('|').append(opt.getAttribute(S_LABEL));
                    items.append('|').append(curValue);
                    if (value.equals(curValue)) {
                        selectedIndex = totalCount;
                    }
                    totalCount++;
                }
            }
            items.deleteCharAt(0);
            labels.deleteCharAt(0);

            int num = fields.size();
            fields.addElement(name);
            types.addElement(type);
            values.addElement(items.toString());
            form.addSelector(num, label, labels.toString(), selectedIndex);

        } else {
            addField(name, type, label, value);
        }
    }
    public void loadFromXml(XmlNode xml, XmlNode baseXml) {
        waitingForm = false;
        id = newId();
        clearForm();
        XmlNode xmlForm = xml.getFirstNodeRecursive("x");
        isXData = (null != xmlForm);

        if (isXData) {
            addInfo(xmlForm.getFirstNodeValueRecursive("ti" + "tle"),
                    xmlForm.getFirstNodeValueRecursive("instruct" + "ions"));
            for (int i = 0; i < xmlForm.childrenCount(); ++i) {
                XmlNode item = xmlForm.childAt(i);
                if (item.is("fie" + "ld")) {
                    String type = item.getAttribute("ty" + "pe");
                    // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA" #
                    if ((null == type) || "".equals(type)) {
                        String bs64img = baseXml.getFirstNodeValueRecursive("d" + "ata");
                        if (null != bs64img) {
                            byte[] imgBytes = Util.base64decode(bs64img);
                            bs64img = null;
                            form.addImage(Image.createImage(imgBytes, 0, imgBytes.length));
                        }
                    }
                    // #sijapp cond.end #
                    addField(item, type);
                }
            }
            form.endForm();
            return;
        }
        addInfo(xml.getFirstNodeValue("ti" + "tle"),
                xml.getFirstNodeValue("instruct" + "ions"));
        
        for (int i = 0; i < xml.childrenCount(); ++i) {
            XmlNode item = xml.childAt(i);
            if (item.is(S_EMAIL)) {
                addField(S_EMAIL, S_TEXT_SINGLE, "e-mail", "");
                
            } else if (item.is(S_USERNAME)) {
                addField(S_USERNAME, S_TEXT_SINGLE, "nick", "");
                
            } else if (item.is(S_PASSWORD)) {
                addField(S_PASSWORD, S_TEXT_PRIVATE, "password", "");
                
            } else if (item.is(S_KEY)) {
                addField(S_KEY, S_HIDDEN, "", "");
            }
        }
        form.endForm();
    }
    private static final String S_TEXT_SINGLE = "text-single";
    private static final String S_LIST_SINGLE = "list-single";
    private static final String S_TEXT_PRIVATE = "text-private";
    private static final String S_HIDDEN = "hid" + "den";
    private static final String S_BOOLEAN = "bo" + "olean";
    private static final String S_FIXED = "f" + "ixed";
    private static final String S_TEXT_MULTI = "text-multi";

    protected String getXmlForm() {
        for (int i = 0; i < fields.size(); ++i) {
            String type = (String)types.elementAt(i);
            if (S_LIST_SINGLE.equals(type)) {
                String[] list = Util.explode((String)values.elementAt(i), '|');
                values.setElementAt(list[form.getSelectorValue(i)], i);
            } else if (type.startsWith("text-")) {
                values.setElementAt(form.getTextFieldValue(i), i);
            } else if (S_BOOLEAN.equals(type)) {
                values.setElementAt(form.getCheckBoxValue(i) ? "1" : "0", i);
            } else if ("".equals(type)) {
                values.setElementAt(form.getTextFieldValue(i), i);
            }
        }
        StringBuffer sb = new StringBuffer();
        if (!isXData) {
            for (int i = 0; i < fields.size(); ++i) {
                sb.append("<").append((String)fields.elementAt(i)).append(">");
                sb.append(Util.xmlEscape((String)values.elementAt(i)));
                sb.append("</").append((String)fields.elementAt(i)).append(">");
            }
            return sb.toString();
        }
        sb.append("<x xmlns='jabber:x:data' type='submit'>");
        for (int i = 0; i < fields.size(); i++) {
            sb.append("<field type='");
            sb.append(Util.xmlEscape((String)types.elementAt(i)));
            sb.append("' var='");
            sb.append(Util.xmlEscape((String)fields.elementAt(i)));
            sb.append("'><value>");
            sb.append(Util.xmlEscape((String)values.elementAt(i)));
            sb.append("</value></field>");
        }
        sb.append("</x>");
        
        return sb.toString();
    }

    void error(String description) {
        PopupWindow.showShadowPopup("error", description);
    }

    void success() {
        ContactList.activate();
    }
}
// #sijapp cond.end #
