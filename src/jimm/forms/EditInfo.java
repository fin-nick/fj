/*******************************************************************************
Jimm - Mobile Messaging - J2ME ICQ clone
Copyright (C) 2003-06  Jimm Project

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
File: src/jimm/JimmUI.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Igor Palkin
*******************************************************************************/
// #sijapp cond.if protocols_ICQ is "true" | protocols_JABBER is "true" #
package jimm.forms;

import javax.microedition.lcdui.*;
import jimm.modules.*;
import jimm.modules.fs.*;
import protocol.Protocol;
import protocol.icq.*;
import jimm.search.*;

import jimm.ui.*;
import jimm.comm.*;
import jimm.util.ResourceBundle;

public class EditInfo implements CommandListener {

	private static final int _NickNameItem  = 1000;
	private static final int _FirstNameItem = 1001;
	private static final int _LastNameItem  = 1002;
	private static final int _EmailItem     = 1003;
	private static final int _BdayItem      = 1004;
    private static final int _CellPhoneItem = 1005;
    private static final int _AddrItem      = 1006;
	private static final int _CityItem      = 1007;
    private static final int _StateItem     = 1008;
	private static final int _SexItem       = 1009;
    private static final int _HomePageItem  = 1010;
    private static final int _WorkCompanyItem    = 1011;
    private static final int _WorkDepartmentItem = 1012;
    private static final int _WorkPositionItem   = 1013;
    private static final int _WorkPhoneItem      = 1014;
    private FormEx form;
    private Protocol protocol;
    private UserInfo userInfo;

                
	public EditInfo(Protocol p, UserInfo info)  {
        protocol = p;
        this.userInfo = info;
        form = new FormEx("editform", "save", "cancel", this);
        update();
    }
    private void update() {
        form.clearForm();
        form.addTextField(_NickNameItem, "nick", userInfo.nick, 64, TextField.ANY);
        form.addTextField(_FirstNameItem, "firstname", userInfo.firstName, 64, TextField.ANY);
        form.addTextField(_LastNameItem, "lastname", userInfo.lastName, 64, TextField.ANY);
        // #sijapp cond.if protocols_JABBER isnot "true"#
        form.addSelector(_SexItem, "gender", "-" + "|" + "female" + "|" + "male", userInfo.gender);
        // #sijapp cond.end#
        // #sijapp cond.if protocols_JABBER is "true"#
        form.addTextField(_EmailItem, "email", userInfo.email, 64, TextField.ANY);
        // #sijapp cond.else#
        form.addTextField(_EmailItem, "email", userInfo.email, 64, TextField.EMAILADDR);
        // #sijapp cond.end#
        form.addTextField(_BdayItem, "birth_day", userInfo.birthDay, 15, TextField.ANY);
        // #sijapp cond.if protocols_JABBER is "true"#
        form.addTextField(_CellPhoneItem, "cell_phone", userInfo.cellPhone, 64, TextField.ANY);
        // #sijapp cond.end#
        form.addTextField(_HomePageItem, "home_page", userInfo.homePage, 256, TextField.ANY);

        form.addString(ResourceBundle.getString("home_info"));
        // #sijapp cond.if protocols_JABBER is "true"#
        form.addTextField(_AddrItem, "addr", userInfo.homeAddress, 256, TextField.ANY);
        // #sijapp cond.end#
        form.addTextField(_CityItem, "city", userInfo.homeCity, 128, TextField.ANY);
        form.addTextField(_StateItem, "state", userInfo.homeState, 128, TextField.ANY);

        form.addString(ResourceBundle.getString("work_info"));
        form.addTextField(_WorkCompanyItem, "title", userInfo.workCompany, 256, TextField.ANY);
        form.addTextField(_WorkDepartmentItem, "depart", userInfo.workDepartment, 256, TextField.ANY);
        form.addTextField(_WorkPositionItem, "position", userInfo.workPosition, 256, TextField.ANY);
        // #sijapp cond.if protocols_JABBER is "true"#
        form.addTextField(_AddrItem, "phone", userInfo.workPhone, 64, TextField.ANY);
        // #sijapp cond.end#
        form.endForm();
    }
	public void show() {
		form.show();
	}
    private void destroy() {
        form.destroy();
        protocol = null;
        form = null;
        userInfo = null;
    }

	public void commandAction(Command c, Displayable d)  {
		if (form.backCommand == c) {
			form.back();
            destroy();
		
        } else if (form.saveCommand == c) {
			userInfo.nick      = form.getTextFieldValue(_NickNameItem);
			userInfo.email     = form.getTextFieldValue(_EmailItem);
			userInfo.birthDay  = form.getTextFieldValue(_BdayItem);
            // #sijapp cond.if protocols_JABBER is "true"#
            userInfo.cellPhone = form.getTextFieldValue(_CellPhoneItem);
            // #sijapp cond.end#
			userInfo.firstName = form.getTextFieldValue(_FirstNameItem);
			userInfo.lastName  = form.getTextFieldValue(_LastNameItem);
            // #sijapp cond.if protocols_JABBER isnot "true"#
			userInfo.gender  = (byte)form.getSelectorValue(_SexItem);
            // #sijapp cond.end#
            userInfo.homePage = form.getTextFieldValue(_HomePageItem);

            // #sijapp cond.if protocols_JABBER is "true"#
            userInfo.homeAddress = form.getTextFieldValue(_AddrItem);
            // #sijapp cond.end#
			userInfo.homeCity  = form.getTextFieldValue(_CityItem);
            userInfo.homeState = form.getTextFieldValue(_StateItem);

            userInfo.workCompany    = form.getTextFieldValue(_WorkCompanyItem);
            userInfo.workDepartment = form.getTextFieldValue(_WorkDepartmentItem);
            userInfo.workPosition   = form.getTextFieldValue(_WorkPositionItem);
            // #sijapp cond.if protocols_JABBER is "true"#
            userInfo.workPhone      = form.getTextFieldValue(_WorkPhoneItem);
            // #sijapp cond.end#

			userInfo.updateProfileView();
            protocol.saveUserInfo(userInfo);
			form.back();
            destroy();
		}
	}
}
// #sijapp cond.end #
