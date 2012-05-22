/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/ChatHistory.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Andreas Rossbacher, Artyomov Denis, Dmitry Tunin, Vladimir Kryukov
 *******************************************************************************/

/*
 * MessData.java
 *
 * Created on 19 Апрель 2007 г., 15:05
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.chat;

class MessData {
	private long time;
	private short rowData;
    private String nick;
	
	public MessData(boolean incoming, long time, int textOffset, String nick, boolean contains_url) {
        this.nick = nick;
		this.time    = time;
		this.rowData = (short)((textOffset & 0x00FF)
                | (contains_url ? 0x4000 : 0)
                | (incoming     ? 0x8000 : 0));
	}
		
	public long getTime() {
        return time;
    }
	public int getOffset() {
        return rowData & 0x00FF;
    }
    public String getNick() {
        return nick;
    }
	public boolean isIncoming() {
        return (rowData & 0x8000) != 0;
    }
	public boolean isURL() {
        return (rowData & 0x4000) != 0;
    }
	public boolean isMarked() {
        return (rowData & 0x4000) != 0;
    }
}