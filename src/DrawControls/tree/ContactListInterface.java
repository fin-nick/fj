/*
 * ContactListInterface.java
 *
 * Created on 23 Март 2010 г., 21:36
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package DrawControls.tree;

import java.util.Vector;
import protocol.*;

/**
 *
 * @author Vladimir Krukov
 */
public interface ContactListInterface {
    Vector rebuildFlatItems(Vector drawItems);
    void _updateContact(Contact contact);
    void _updateContactFully(Contact contact);
    void groupChanged_(Group group, boolean addGroup);
    void buildTree_(Protocol protocol);
    
    Protocol getProtocol(int accountIndex);
    int getProtocolCount();
    void clear();
    
    void addProtocol(Protocol prot);
    void removeAllProtocols();
}
