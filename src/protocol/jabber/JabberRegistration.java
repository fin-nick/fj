package protocol.jabber;
// #s ijapp cond.if modules_REG is "true" #

import javax.microedition.lcdui.*;
import jimm.JimmException;
import jimm.Options;
import jimm.OptionsForm;
import jimm.cl.ContactList;
import jimm.comm.StringConvertor;
import jimm.ui.FormEx;
import jimm.ui.PopupWindow;
import protocol.Profile;

/**
 *
 * @author Vladimir Kryukov
 */
public class JabberRegistration implements Runnable {
    
    private JabberForm form;
    private JabberXml connection;
    private OptionsForm opts;
    public JabberRegistration(OptionsForm of) {
        form = new JabberForm(this);
        opts = of;
    }
    public void show() {
        form.show();
    }

    private String domain = "";
    private String xml = null;
    private String username;
    private String password;
    public void run() {
        String error = null;
        try {
            connection = new JabberXml();
            XmlNode xform = connection.newAccountConnect(domain, "socket://" + domain + ":5222");
            form.loadFromXml(xform.childAt(0), xform);
            while (null == xml) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
            if (0 == xml.length()) {
                throw new JimmException(0, 0);
            }
            XmlNode n = connection.newAccountRegister(xml);
            if (("r" + "esult").equals(n.getAttribute("t" + "ype"))) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                jimm.modules.DebugLog.println("xml " + n.toString());
                // #sijapp cond.end#
                Profile account = new Profile();
                account.protocolType = Profile.PROTOCOL_JABBER;
                account.userId = username + "@" + domain;
                account.password = password;
                account.nick = "";
                account.isActive = true;
                opts.addAccount(Options.getMaxAccountCount(), account);
            } else {
                error = "can not create account";
            }
            
        } catch (JimmException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            connection.disconnect();
        } catch (Exception ex) {
        }
        form.back();
        opts = null;
        if (null != error) {
            new PopupWindow("registration", error).show();
        }
    }
    
    void setUsername(String username) {
        this.username = username;
    }
    void setPassword(String pass) {
        this.password = StringConvertor.notNull(pass);
    }
    void register(String form) {
        xml = form;
    }

    void getForm(String domain) {
        this.domain = domain;
        new Thread(this).start();
    }

    void cancel() {
        xml = "";
    }
}
// #s ijapp cond.end#