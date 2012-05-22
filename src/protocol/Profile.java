/*
 * Profile.java
 *
 * Created on 23 Январь 2010 г., 15:51
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

/**
 *
 * @author Vladimir Krukov
 */
public final class Profile {
    
    public static final int PROTOCOL_ICQ = 0;
    public static final int PROTOCOL_MRIM = 1;
    public static final int PROTOCOL_JABBER = 2;
    public static final int PROTOCOL_VKWEB = 3;
    public static final int PROTOCOL_MSN = 4;
    public static final int PROTOCOL_FACEBOOK = 10;
    public static final int PROTOCOL_LJ = 11;
    public static final int PROTOCOL_YANDEX = 12;
    public static final int PROTOCOL_VK = 13;
    public static final int PROTOCOL_GTALK = 14;
    public static final int PROTOCOL_QIP = 15;
    public static final int PROTOCOL_OVI = 16;
    public static final String protocolNames = (""
            // #sijapp cond.if protocols_ICQ is "true"#
            + "|ICQ"
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MRIM is "true" #
            + "|MRIM"
            // #sijapp cond.end #
            // #sijapp cond.if protocols_JABBER is "true" #
            + "|Jabber"
            // #sijapp cond.if modules_MULTI is "true" #
            + "|Facebook"
            + "|VKontakte"
            + "|LiveJournal"
            + "|GTalk"
            + "|Ya.Online"
            + "|Nokia Ovi"
            + "|QIP"
            // #sijapp cond.end #
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MSN is "true" #
            + "|MSN"
            // #sijapp cond.end #
            ).substring(1);
    public static final byte[] protocolTypes = new byte[] {
            // #sijapp cond.if protocols_ICQ is "true"#
            PROTOCOL_ICQ,
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MRIM is "true" #
            PROTOCOL_MRIM,
            // #sijapp cond.end #
            // #sijapp cond.if protocols_JABBER is "true" #
            PROTOCOL_JABBER,
            // #sijapp cond.if modules_MULTI is "true" #
            PROTOCOL_FACEBOOK,
            PROTOCOL_VK,
            PROTOCOL_LJ,
            PROTOCOL_GTALK,
            PROTOCOL_YANDEX,
            PROTOCOL_OVI,
            PROTOCOL_QIP,
            // #sijapp cond.end #
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MSN is "true" #
            PROTOCOL_MSN,
            // #sijapp cond.end #
            };
    public static final String[] protocolIds = new String[] {
            // #sijapp cond.if protocols_ICQ is "true"#
            "UIN/E-mail",
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MRIM is "true" #
            "e-mail",
            // #sijapp cond.end #
            // #sijapp cond.if protocols_JABBER is "true" #
            "jid",
            // #sijapp cond.if modules_MULTI is "true" #
            "Login",
            "Login/ID",
            "Login",
            "Login",
            "Login",
            "Login",
            "Login",
            // #sijapp cond.end #
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MSN is "true" #
            "LiveID/E-mail",
            // #sijapp cond.end #
            };

    /** Creates a new instance of Profile */
    public Profile() {
        protocolType = Profile.protocolTypes[0];
    }
    public byte protocolType;
    public String userId = "";
    public String password = "";
    public String nick = "";
    
    public byte statusIndex = 1;
    public String statusMessage;

    public byte xstatusIndex = -1;
    public String xstatusTitle;
    public String xstatusDescription;
    public boolean isActive;
}
