/*
 * StatusMessage.java
 *
 * Created on 28 Январь 2010 г., 20:37
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_ICQ is "true" #
package jimm.chat.message;

import jimm.comm.Util;
import protocol.Contact;
import protocol.Protocol;

/**
 *
 * @author Vladimir Krukov
 */
public class StatusMessage extends PlainMessage {
    // Static variables for message type;
    public static final int MESSAGE_TYPE_AUTO     = 0x0000;
    public static final int MESSAGE_TYPE_NORM     = 0x0001;
    public static final int MESSAGE_TYPE_EXTENDED = 0x001a;
    public static final int MESSAGE_TYPE_AWAY     = 0x03e8;
    public static final int MESSAGE_TYPE_OCC      = 0x03e9;
    public static final int MESSAGE_TYPE_NA       = 0x03ea;
    public static final int MESSAGE_TYPE_DND      = 0x03eb;
    public static final int MESSAGE_TYPE_FFC      = 0x03ec;

//    public static final int MESSAGE_TYPE_UNKNOWN  = 0x0000; // Unknown message, only used internally by this plugin
    public static final int MESSAGE_TYPE_PLAIN    = 0x0001; // Plain text (simple) message
//    public static final int MESSAGE_TYPE_CHAT     = 0x0002; // Chat request message
    public static final int MESSAGE_TYPE_FILEREQ  = 0x0003; // File request / file ok message
    public static final int MESSAGE_TYPE_URL      = 0x0004; // URL message (0xFE formatted)
//    public static final int MESSAGE_TYPE_AUTHREQ  = 0x0006; // Authorization request message (0xFE formatted)
//    public static final int MESSAGE_TYPE_AUTHDENY = 0x0007; // Authorization denied message (0xFE formatted)
//    public static final int MESSAGE_TYPE_AUTHOK   = 0x0008; // Authorization given message (empty)
//    public static final int MESSAGE_TYPE_SERVER   = 0x0009; // Message from OSCAR server (0xFE formatted)
    public static final int MESSAGE_TYPE_ADDED    = 0x000C; // "You-were-added" message (0xFE formatted)
//    public static final int MESSAGE_TYPE_WWP      = 0x000D; // Web pager message (0xFE formatted)
//    public static final int MESSAGE_TYPE_EEXPRESS = 0x000E; // Email express message (0xFE formatted)
//    public static final int MESSAGE_TYPE_CONTACTS = 0x0013; // Contact list message
    public static final int MESSAGE_TYPE_PLUGIN   = 0x001A; // Plugin message described by text string
//    public static final int MESSAGE_TYPE_AWAY     = 0x03E8; // Auto away message
//    public static final int MESSAGE_TYPE_OCC      = 0x03E9; // Auto occupied message
//    public static final int MESSAGE_TYPE_NA       = 0x03EA; // Auto not available message
//    public static final int MESSAGE_TYPE_DND      = 0x03EB; // Auto do not disturb message
//    public static final int MESSAGE_TYPE_FFC      = 0x03EC; // Auto free for chat message

    // Message type
    private int messageType;
    
	// Constructs an outgoing message
	public StatusMessage(Protocol protocol, Contact rcvr, int messageType) {
		super(protocol, rcvr, Util.createCurrentDate(false), "");
        this.messageType = messageType;
	}
    public int getMessageType() {
        return messageType;
    }
}
// #sijapp cond.end #