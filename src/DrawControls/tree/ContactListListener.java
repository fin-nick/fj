/*
 * ContactListListener.java
 *
 * Created on 21 Декабрь 2009 г., 19:00
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package DrawControls.tree;

import protocol.Contact;

/**
 *
 * @author Vladimir Krukov
 */
public interface ContactListListener {
    void setCurrentContact(Contact contact);
    void activateMainMenu();
}
