/*
 * StatusInfo.java
 *
 * Created on 24 Август 2010 г., 16:57
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.jabber;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import jimm.util.ResourceBundle;
import protocol.Status;
import protocol.StatusInfo;

/**
 *
 * @author Vladimir Kryukov
 */
public class JabberStatusInfo extends StatusInfo {
    public JabberStatusInfo(ImageList statuses, int[] width, int[] index, String[] names) {
        super(statuses, width, index, names);
    }
}
