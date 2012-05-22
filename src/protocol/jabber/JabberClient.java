/*
 * JabberClient.java
 *
 * Created on 26 Апрель 2009 г., 19:50
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
// #sijapp cond.if modules_CLIENTS is "true" #
package protocol.jabber;

import DrawControls.icons.*;
import jimm.comm.Config;
import jimm.comm.StringConvertor;

/**
 *
 * @author Vladimir Krukov
 */
public class JabberClient {
    private static final ImageList clientIcons = ImageList.createImageList("/jabber-clients.png");
    private static final String[] clientCaps;
    private static final String[] clientNames;
    static {
        Config cfg = new Config().load("/jabber-clients.txt");
        clientCaps = cfg.getKeys();
        clientNames = cfg.getValues();
    }
    public static final JabberClient noneClient = new JabberClient();
    private static final byte CLI_NONE = -1;

    private byte clientIndex = CLI_NONE;
    /** Creates a new instance of JabberClient */
    private JabberClient() {
    }

    public static JabberClient createClient(JabberClient prev, String caps) {
        if (null == prev) {
            prev = noneClient;
        }
        if (StringConvertor.isEmpty(caps)) {
            return prev;
        }
        caps = caps.toLowerCase();
        byte clientIndex = CLI_NONE;
        for (byte capsIndex = 0; capsIndex < clientCaps.length; capsIndex++) {
            if (-1 != caps.indexOf(clientCaps[capsIndex])) {
                clientIndex = capsIndex;
                break;
            }
        }
        if (CLI_NONE == clientIndex) {
            return noneClient;
        }
        JabberClient result = (noneClient == prev) ? new JabberClient() : prev;
        result.clientIndex = clientIndex;
        return result;
    }
    
    public Icon getIcon() {
        return clientIcons.iconAt(clientIndex);
    }
    public String getName() {
        if (CLI_NONE == clientIndex) {
            return null;
        }
        return clientNames[clientIndex];
    }
}
// #sijapp cond.end #
// #sijapp cond.end #