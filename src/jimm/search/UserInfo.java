/*
 * UserInfo.java
 *
 * Created on 25 Март 2008 г., 19:45
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.search;

import DrawControls.icons.*;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Image;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.forms.*;
import jimm.modules.fs.*;
import jimm.modules.photo.*;
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.ui.base.*;
import jimm.util.ResourceBundle;
import protocol.*;
import protocol.icq.*;
import protocol.jabber.*;
import protocol.mrim.*;

/**
 *
 * @author vladimir
 */
public class UserInfo implements
        // #sijapp cond.if protocols_JABBER is "true" #
        // #sijapp cond.if modules_FILES="true"#
        // #sijapp cond.if target isnot "MOTOROLA" #
        PhotoListener,
        // #sijapp cond.end #
        // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA" #
        FileBrowserListener,
        // #sijapp cond.end #
        // #sijapp cond.end #
        // #sijapp cond.end #
        SelectListener {
    private final Protocol protocol;
    private TextListEx profileView;
    
    /** Creates a new instance of UserInfo */
    public UserInfo(Protocol prot, String uin) {
        protocol = prot;
        realUin = uin;
    }
    public UserInfo(Protocol prot) {
        protocol = prot;
        realUin = null;
    }
    public void setProfileView(TextListEx view) {
        profileView = view;
    }
    public TextListEx getProfileView() {
        return profileView;
    }

    
    private static final int INFO_MENU_COPY     = 1040;
    private static final int INFO_MENU_COPY_ALL = 1041;
    private static final int INFO_MENU_BACK     = 1042;
    private static final int INFO_MENU_AVATAR   = 1043;
    private static final int INFO_MENU_EDIT     = 1044;
    private static final int INFO_MENU_REMOVE_AVATAR = 1045;
    private static final int INFO_MENU_ADD_AVATAR    = 1046;
    private static final int INFO_MENU_TAKE_AVATAR   = 1047;
    
    public void setOptimalName() {
        Contact contact = protocol.getItemByUIN(uin);
        if (null != contact) {
            contact.setOptimalName(this);
        }
    }
    public synchronized void updateProfileView() {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if ((null == profileView)) {
            jimm.modules.DebugLog.panic("profileView is null");
            return;
        }
        // #sijapp cond.end#
        profileView.lock();
        profileView.clear();
        
        profileView.setHeader("main_info");
        profileView.add(protocol.getUinName(),    uin);
        profileView.add("nick",   nick);
        profileView.add("name", getName());
        profileView.add("gender", getGenderAsString());
        if (0 < age) {
            profileView.add("age", Integer.toString(age));
        }
        profileView.add("email",  email);
        if (auth) {
            profileView.add("auth", ResourceBundle.getString("yes"));
        }
        // #sijapp cond.if protocols_ICQ is "true" #
        profileView.add("user_statuses", getStatusAsIcon());
        // #sijapp cond.end #
        profileView.add("birth_day",  birthDay);
        profileView.add("cell_phone", cellPhone);
        profileView.add("home_page",  homePage);
        profileView.add("interests",  interests);
        profileView.add("notes",      about);
        
        profileView.setHeader("home_info");
        profileView.add("addr",  homeAddress);
        profileView.add("city",  homeCity);
        profileView.add("state", homeState);
        profileView.add("phone", homePhones);
        profileView.add("fax",   homeFax);
        
        profileView.setHeader("work_info");
        profileView.add("title",    workCompany);
        profileView.add("depart",   workDepartment);
        profileView.add("position", workPosition);
        profileView.add("addr",     workAddress);
        profileView.add("city",     workCity);
        profileView.add("state",    workState);
        profileView.add("phone",    workPhone);
        profileView.add("fax",      workFax);
        
        profileView.setHeader("avatar");
        profileView.add(null, avatar);

        profileView.unlock();

        //profileView.setCaption(getName());
        MenuModel menu = new MenuModel();
        menu.addItem("copy_text",     INFO_MENU_COPY);
        menu.addItem("copy_all_text", INFO_MENU_COPY_ALL);
        if (isEditable()) {
            menu.addItem("edit",      INFO_MENU_EDIT);
            // #sijapp cond.if protocols_JABBER is "true" #
            // #sijapp cond.if modules_FILES="true"#
            if (protocol instanceof Jabber) {
                // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" #
                menu.addItem("take_photo", INFO_MENU_TAKE_AVATAR);
                // #sijapp cond.end #
                // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA" #
                if (jimm.modules.fs.FileSystem.isSupported()) {
                    menu.addItem("add_from_fs", INFO_MENU_ADD_AVATAR);
                }
                menu.addItem("remove", INFO_MENU_REMOVE_AVATAR);
                // #sijapp cond.end #
            }
            // #sijapp cond.end #
            // #sijapp cond.end #
        }
        // #sijapp cond.if (protocols_MRIM is "true") or (protocols_ICQ is "true") #
        if (null != uin) {
            boolean hasAvatarItem = false;
            // #sijapp cond.if protocols_MRIM is "true"#
            hasAvatarItem |= (protocol instanceof Mrim);
            // #sijapp cond.end #
            // #sijapp cond.if protocols_ICQ is "true"#
            hasAvatarItem |= (protocol instanceof Icq);
            // #sijapp cond.end #
            if (hasAvatarItem) {
                menu.addItem("get_avatar",    INFO_MENU_AVATAR);
            }
        }
        // #sijapp cond.end #
        menu.addItem("back", INFO_MENU_BACK);
        menu.setActionListener(this);
        profileView.setMenu(menu, INFO_MENU_BACK, INFO_MENU_COPY);
    }
    public void setProfileViewToWait() {
        MenuModel menu = new MenuModel();
        menu.addItem("back", INFO_MENU_BACK);
        profileView.clear();
        profileView.addBigText(ResourceBundle.getString("wait"),
                CanvasEx.THEME_TEXT, Font.STYLE_PLAIN, -1);
        menu.setActionListener(this);
        profileView.setMenu(menu, INFO_MENU_BACK, INFO_MENU_COPY);
    }

    public boolean isEditable() {
        // #sijapp cond.if protocols_ICQ is "true" #
        if (protocol instanceof Icq) {
            return protocol.getUserId().equals(uin) && protocol.isConnected();
        }
        // #sijapp cond.end #
        // #sijapp cond.if protocols_JABBER is "true" #
        if (protocol instanceof Jabber) {
            return protocol.getUserId().equals(uin) && protocol.isConnected();
        }
        // #sijapp cond.end #
        return false;
    }
    
    public void select(Select select, MenuModel model, int cmd) {
        switch (cmd) {
            case INFO_MENU_COPY:
            case INFO_MENU_COPY_ALL:
                profileView.copy(INFO_MENU_COPY_ALL == cmd);
                profileView.restore();
                break;

            case INFO_MENU_BACK:
                profileView.back();
                profileView.clear();
                break;

            // #sijapp cond.if (protocols_MRIM is "true") or (protocols_ICQ is "true") #
            case INFO_MENU_AVATAR:
                protocol.getAvatar(this);
                profileView.restore();
                break;
            // #sijapp cond.end #

            // #sijapp cond.if protocols_ICQ is "true" | protocols_JABBER is "true" #
            case INFO_MENU_EDIT:
                new EditInfo(protocol, this).show();
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_JABBER is "true" #
            // #sijapp cond.if modules_FILES="true"#
            // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" #
            case INFO_MENU_TAKE_AVATAR:
                ViewFinder vf = new ViewFinder();
                vf.setPhotoListener(this);
                vf.show();
                break;

            // #sijapp cond.end #
            // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA" #
            case INFO_MENU_REMOVE_AVATAR:
                removeAvatar();
                protocol.saveUserInfo(this);
                updateProfileView();
                profileView.restore();
                break;

            case INFO_MENU_ADD_AVATAR:
                FileBrowser fsBrowser = new FileBrowser(false);
                fsBrowser.setListener(this);
                fsBrowser.activate();
                break;
            // #sijapp cond.end #
            // #sijapp cond.end #
            // #sijapp cond.end #
        }
    }
    public final String realUin;

    public String uin;
    public String nick;
    public String email;
    public String homeCity;
    public String firstName;
    public String lastName;

    public String homeState;
    public String homePhones;
    public String homeFax;
    public String homeAddress;
    public String cellPhone;

    public String homePage;
    public String interests;
    
    public String about;

    public String workCity;
    public String workState;
    public String workPhone;
    public String workFax;
    public String workAddress;
    public String workCompany;
    public String workDepartment;
    public String workPosition;
    public String birthDay;
    
    public int age;
    public byte gender;
    //public String auth;
    public boolean auth; // required

    // #sijapp cond.if protocols_ICQ is "true" #
    public String status;
    private Icon getStatusAsIcon() {
        if (null == status) {
            return null;
        }
        byte statusIndex = Status.I_STATUS_NA;
        switch (Util.strToIntDef(status, 0)) {
            case 0: statusIndex = Status.I_STATUS_OFFLINE;   break;
            case 1: statusIndex = Status.I_STATUS_ONLINE;    break;
            case 2: statusIndex = Status.I_STATUS_INVISIBLE_; break;
        }
        return protocol.getStatusInfo().getIcon(statusIndex);
    }
    // #sijapp cond.end #
    // Convert gender code to string
    public String getGenderAsString() {
        switch (gender) {
            case 1: return ResourceBundle.getString("female");
            case 2: return ResourceBundle.getString("male");
        }
        return "";
    }

    private String packString(String str) {
        return (null == str) ? "" : str.trim();
    }
    public String getName() {
        return packString(packString(firstName) + " " + packString(lastName));
    }
    public String getOptimalName() {
        String optimalName = packString(nick);
        if (optimalName.length() == 0) {
            optimalName = packString(getName());
        }
        if (optimalName.length() == 0) {
            optimalName = packString(firstName);
        }
        if (optimalName.length() == 0) {
            optimalName = packString(lastName);
        }
        return optimalName;
    }
    
    public Icon avatar;
    public void setAvatar(Image img) {
        if (null == img) {
            avatar = null;
            return;
        }
        int height = NativeCanvas.getScreenHeight() * 2 / 3;
        int width = NativeCanvas.getScreenWidth() - 5;
        Image image = Util.createThumbnail(img, width, height);
        avatar = new Icon(image, 0, 0, image.getWidth(), image.getHeight());
    }

    // #sijapp cond.if protocols_JABBER is "true" #
    public protocol.jabber.XmlNode vCard;
    // #sijapp cond.if modules_FILES="true"#
    public void removeAvatar() {
        avatar = null;
        vCard.removeNode("PHOTO");
    }
    // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA" #
    private String getImageType(byte[] data) {
        if ("PNG".equals(StringConvertor.byteArrayToString(data, 1, 3))) {
            return "image/png";
        } 
        return "image/jpeg";
    }
    public void setAvatar(byte[] data) {
        try {
            
            setAvatar(Image.createImage(data, 0, data.length));
            
            vCard.setValue("PHOTO", null, "TYPE", getImageType(data));
            vCard.setValue("PHOTO", null, "BINVAL", Util.base64encode(data));
        } catch (Exception e) {
        }
    }
    public void onFileSelect(String filename) {
        try {
            FileSystem file = FileSystem.getInstance();
            file.openFile(filename);
            // FIXME resource leak
            java.io.InputStream fis = file.openInputStream();
            int size = (int)file.fileSize();
            if (size <= 30*1024*1024) {
                byte[] avatar = new byte[size];
                int readed = 0;
                while (readed < avatar.length) {
                    int read = fis.read(avatar, readed, avatar.length - readed);
                    if (-1 == read) break;
                    readed += read;
                }
                setAvatar(avatar);
                avatar = null;
            }

            fis.close();
            file.close();
            fis = null;
            file = null;
        } catch (OutOfMemoryError er) {
        } catch (Exception e) {
        }
        if (null != avatar) {
            protocol.saveUserInfo(this);
            updateProfileView();
        }
        profileView.restore();
    }

    public void onDirectorySelect(String directory) {
    }
    // #sijapp cond.end #
    // #sijapp cond.if target isnot "MOTOROLA" #
    public void processPhoto(byte[] data) {
        setAvatar(data);
        data = null;
        if (null != avatar) {
            protocol.saveUserInfo(this);
            updateProfileView();
        }
        profileView.restore();
    }
    // #sijapp cond.end #
    // #sijapp cond.end #
    // #sijapp cond.end #
}
