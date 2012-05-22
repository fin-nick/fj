/*
 * SomeStatusForm.java
 *
 * Created on 22 Январь 2010 г., 12:40
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.forms;

import jimm.Jimm;
import jimm.cl.ContactList;
import jimm.ui.menu.*;
import protocol.Protocol;
import protocol.StatusInfo;

/**
 *
 * @author Vladimir Krukov
 */
public class SomeStatusForm implements SelectListener {
    
    private MenuModel menu = new MenuModel();
    private Object prevDisplay;
    protected Protocol protocol;
    protected int statusIndex;
    public SomeStatusForm(Protocol protocol) {
        this.protocol = protocol;
    }

    // You can overload it
    protected void addStatuses(MenuModel menu) {
        addStatuses(menu, protocol.getStatusList());
    }
    // You can overload it
    protected void statusSelected(int statusIndex) {
        setStatus(statusIndex, null);
        back();
    }

    private void prepare() {
        menu.clean();
        addStatuses(menu);
        menu.setActionListener(this);
    }
    protected final void setStatus(int statusIndex, String str) {
        protocol.setOnlineStatus(statusIndex, str);
    }
    private final void addStatuses(MenuModel menu, byte[] statuses) {
        StatusInfo info = protocol.getStatusInfo();
        for (int i = 0; i < statuses.length; ++i) {
            menu.addItem(info.getName(statuses[i]), info.getIcon(statuses[i]), statuses[i]);
        }
        menu.setDefaultItemCode(protocol.getProfile().statusIndex);
    }

    public final void select(Select select, MenuModel model, int statusIndex) {
        this.statusIndex = statusIndex;
        statusSelected(statusIndex);
    }
    public final void show() {
        prepare();
        prevDisplay = Jimm.getCurrentDisplay();
        new Select(menu).show();
    }
    public final void back() {
        ContactList.updateMainMenu();
        Jimm.setDisplay(prevDisplay);
    }
}
