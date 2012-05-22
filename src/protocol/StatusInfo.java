/*
 * StatusInfo.java
 *
 * Created on 27 Август 2010 г., 11:13
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import DrawControls.icons.*;
import jimm.util.ResourceBundle;

/**
 *
 * @author Vladimir Kryukov
 */
public class StatusInfo {
    public final ImageList statusIcons;
    public final int[] statusWidth;
    public final int[] statusIconIndex;
    public final String[] statusNames;
    
    /** Creates a new instance of StatusInfo */
    public StatusInfo(ImageList statuses, int[] width, int[] index, String[] names) {
        statusIcons = statuses;
        statusWidth = width;
        statusIconIndex = index;
        statusNames = names;
    }
    public String getName(byte statusIndex) {
        return ResourceBundle.getString(statusNames[statusIndex]);
    }
    public String getName(Status status) {
        return getName((null == status) ? Status.I_STATUS_OFFLINE : status.getStatusIndex());
    }

    public Icon getIcon(byte statusIndex) {
        return statusIcons.iconAt(statusIconIndex[statusIndex]);
    }
    public Icon getIcon(Status status) {
        return getIcon((null == status) ? Status.I_STATUS_OFFLINE : status.getStatusIndex());
    }
    public int getWidth(Status status) {
        if ((null == status) || (Status.offlineStatus == status)) {
            return 29;
        }
        return statusWidth[status.getStatusIndex()];
    }
    public final boolean isOffline(byte statusIndex, int type) { //it is changed
        switch (statusIndex) {
            case Status.I_STATUS_OFFLINE:
                return true;
        }
        return false;
    }
}
