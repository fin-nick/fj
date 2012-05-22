/*
 * DisplayableEx.java
 *
 * Created on 23 Октябрь 2008 г., 23:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.base;

import javax.microedition.lcdui.Displayable;
import jimm.Jimm;

/**
 *
 * @author Vladimir Krukov
 */
public abstract class DisplayableEx {
    
    protected Object prevDisplay;

    public abstract void restore();
    public abstract void setDisplayableToDisplay();
    
    public final void show(Object prev) {
        showing();
        if (prev != this) {
            prevDisplay = prev;
        }
        restore();
    }
    public final void show() {
        show((Object)Jimm.getCurrentDisplay());
    }
    
    public final void back() {
        if (prevDisplay instanceof DisplayableEx) {
            ((DisplayableEx)prevDisplay).restore();

        } else if (prevDisplay instanceof Displayable) {
            Jimm.setDisplay(prevDisplay);
        }
        prevDisplay = null;
    }
    
    protected void showing() {
    }
    public void closed() {
    }
}
