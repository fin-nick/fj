/*
 * CachedRecord.java
 *
 * Created on 19 Апрель 2007 г., 15:00
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.history;

import jimm.comm.Util;

/**
 * Class to cache one line in messages list
 * All fields are public to easy and fast access
 */
// #sijapp cond.if modules_HISTORY is "true" #
public class CachedRecord {
    public String text;
    public String date;
    public String from;
	public byte type; // 0 - incoming message, 1 - outgoing message
    
	private String shortText;
    public String getShortText() {
        if (null != this.shortText) {
            return this.shortText;
        }
        final int maxLen = 20;
        shortText = text;
        if (text.length() > maxLen) {
            shortText = text.substring(0, maxLen) + "...";
        }
        shortText = shortText.replace('\n', ' ').replace('\r', ' ');
        return shortText;
    }
	// #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
	public boolean containsUrl() {
        return (null != Util.parseMessageForURL(text));
    }
	// #sijapp cond.end#
}
// #sijapp cond.end #