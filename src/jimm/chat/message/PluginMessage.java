/*
 * PlaginMessage.java
 *
 * Created on 2 ���� 2007 �., 15:45
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_ICQ is "true" #
package jimm.chat.message;

import jimm.*;
import jimm.comm.*;
import protocol.icq.plugin.*;
import protocol.Protocol;

/**
 *
 * @author vladimir
 */
public class PluginMessage extends PlainMessage {
    
    private Plugin plugin;

    /** Creates a new instance of PlaginMessage */
    public PluginMessage(Protocol protocol, Plugin plugin) {
        super(protocol, plugin.getRcvr(), Util.createCurrentDate(false), "");
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
// #sijapp cond.end #