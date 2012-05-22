/*
 * JabberGroup.java
 *
 * Created on 28 Март 2008 г., 22:31
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;
import protocol.Group;

/**
 *
 * @author vladimir
 */
public class JabberGroup extends Group {
    public JabberGroup(Jabber jabber, String name, int id) {
        super(jabber, name);
        setGroupId(id);
    }
    public static final String GENERAL_GROUP = "general";        //it is changed
    public static final String GATE_GROUP = "transports";        //it is changed
    public static final String CONFERENCE_GROUP = "conferences"; //it is changed
}
// #sijapp cond.end #