/*
 * HistoryStorageList.java
 *
 * Created on 1 Май 2007 г., 15:06
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.history;

// #sijapp cond.if modules_HISTORY is "true" #

import java.util.*;
import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
import javax.microedition.midlet.*;
// #sijapp cond.if target="SIEMENS2" | target="MOTOROLA" | target="MIDP2"#
import javax.microedition.io.*;
// #sijapp cond.end#
import jimm.*;
import jimm.cl.*;
import jimm.modules.fs.*;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import jimm.modules.*;
import jimm.util.ResourceBundle;
import DrawControls.*;
import jimm.comm.*;
import protocol.Contact;

// Visual messages history list
public class HistoryStorageList extends VirtualFlatList implements
        Runnable, CommandListener, VirtualListCommands, SelectListener {
    
    // list UIN
    private HistoryStorage history;
    private Contact contact;
    
    // Controls for finding text
    private FormEx frmFind;
    private static final int tfldFind = 1000;
    private static final int chsFind = 1010;
    
    // Constructor
    public HistoryStorageList(HistoryStorage storage, Contact contact) {
        super(ResourceBundle.getString("history"));
        history = storage;
        this.contact = contact;
        history.openHistory();

        int size = getSize();
        if (0 != size) {
            setCurrentItem(size - 1);
        }
    }

    protected void restoring() {
        NativeCanvas.setCommands("menu", "back");
    }
    
    void closeHistoryView() {
        clearCache();
        history.closeHistoryView();
        searching = null;
    }

    public int getHistorySize() {
        return history.getHistorySize();
    }
    private static final int CACHE_SIZE = 50;
    private Hashtable cachedRecords = new Hashtable();
    private CachedRecord getCachedRecord(int num) {
        Integer key = new Integer(num);
        CachedRecord cachedRec = (CachedRecord)cachedRecords.get(key);
        if (null == cachedRec) {
            trimCache();
            cachedRec = history.getRecord(num);
            if (null != cachedRec) {
                cachedRecords.put(key, cachedRec);
            }
        }
        return cachedRec;
    }
    private void trimCache() {
        if (cachedRecords.size() > CACHE_SIZE) {
            cachedRecords.clear();
        }
    }
    // Clears cache before hiding history list
    private void clearCache() {
        cachedRecords.clear();
        System.gc();
    }

    protected void onCursorMove() {
        CachedRecord record = getCachedRecord(getCurrItem());
        
        if (record == null) return;
        setCaption(record.from + " " + record.date);
    }
    protected void itemSelected() {
        showMessText();
    }
    
    // VirtualList command impl.
    public boolean onKeyPress(VirtualList sender, int keyCode, int actionCode) {
        switch (actionCode) {
            case NativeCanvas.NAVIKEY_LEFT:
                moveInList(-1);
                return true;
                
            case NativeCanvas.NAVIKEY_RIGHT:
                moveInList(+1);
                return true;
                
            case NativeCanvas.NAVIKEY_FIRE:
                getMenu().exec(null, MENU_SELECT);
                return true;
        }
        return false;
    }
    
    private static final int MENU_SELECT     = 0;
    private static final int MENU_FIND       = 1;
    private static final int MENU_CLEAR      = 2;
    private static final int MENU_DEL_CURRENT        = 40;
    private static final int MENU_DEL_ALL_EXCEPT_CUR = 41;
    private static final int MENU_DEL_ALL            = 42;
    private static final int MENU_COPY_TEXT  = 3;
    private static final int MENU_INFO       = 4;
    private static final int MENU_EXPORT     = 5;
    private static final int MENU_EXPORT_ALL = 6;
    private static final int MENU_GOTO_URL   = 7;

    public static final int MMENU_NEXT = 30;
    public static final int MMENU_PREV = 31;
    public static final int MMENU_BACK = 33;
    private MenuModel menu = new MenuModel();
    protected MenuModel getMenu() {
        menu.clean();
        if (getSize() > 0) {
            menu.addItem("select",       MENU_SELECT);
            menu.addEllipsisItem("find",  null, MENU_FIND);
            menu.addEllipsisItem("clear", null, MENU_CLEAR);
            menu.addItem("copy_text",    MENU_COPY_TEXT);
            menu.addItem("history_info", MENU_INFO);
            // #sijapp cond.if modules_FILES="true"#
            if (jimm.modules.fs.FileSystem.isSupported()) {
                menu.addItem("export",       MENU_EXPORT);
                menu.addItem("exportall",    MENU_EXPORT_ALL);
            }
            // #sijapp cond.end#
        }
        menu.addItem("back", MenuModel.BACK_COMMAND_CODE);
        menu.setActionListener(this);
        return menu;
    }
    
    // Moves on next/previous message in list and shows message text
    private void moveInList(int offset) {
        moveCursor(offset);
        showMessText();
    }
    private Thread searching = null;
    public void run() {
        searching = Thread.currentThread();
        // search
        String text = frmFind.getTextFieldValue(tfldFind);
        int textIndex = find(text, getCurrItem(),
                frmFind.getCheckBoxValue(chsFind + 1),
                frmFind.getCheckBoxValue(chsFind + 0));
        
        if (textIndex >= 0) {
            setCurrentItem(textIndex);
            restore();
        } else {
            new PopupWindow("find", text + "\n" + ResourceBundle.getString("not_found")).show();
        }
    }
    
    private int find(String text, int fromIndex, boolean caseSens, boolean back) {
        Thread it = Thread.currentThread();
        int size = history.getHistorySize();
        if ((fromIndex < 0) || (fromIndex >= size)) return -1;
        if (!caseSens) text = StringConvertor.toLowerCase(text);
        
        int step = back ? -1 : +1;
        int updater = 100;
        for (int index = fromIndex; ; index += step) {
            if ((index < 0) || (index >= size)) break;
            CachedRecord record = history.getRecord(index);
            String searchText = caseSens
                    ? record.text
                    : StringConvertor.toLowerCase(record.text);
            if (searchText.indexOf(text) != -1) {
                return index;
            }
            
            if (0 != updater) {
                updater--;
            } else {
                updater = 100;
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
                if (it != searching) {
                    return -1;
                }
            }
        }
        return -1;
    }
    
    public void commandAction(Command c, Displayable d) {
        // user select ok command in find screen
        if (frmFind.saveCommand == c) {
            new Thread(this).start();

        } else if (frmFind.backCommand == c) {
            searching = null;
            restore();
        }
    } // end 'commandAction'
    
    private MenuModel msgMenu = new MenuModel();
    private TextListEx msgText = new TextListEx(null);
    
    // Show text message of current message of messages list
    private void showMessText() {
        if (getCurrItem() >= getSize()) return;
        CachedRecord record = history.getRecord(getCurrItem());
        msgMenu.clean();
        msgMenu.addItem("next",      MMENU_NEXT);
        msgMenu.addItem("prev",      MMENU_PREV);
        msgMenu.addItem("copy_text", MENU_COPY_TEXT);
        // #sijapp cond.if target is "SIEMENS2" | target is "MOTOROLA" | target is "MIDP2"#
        if (record.containsUrl()) {
            msgMenu.addItem("goto_url",  MENU_GOTO_URL);
        }
        // #sijapp cond.end#
        msgMenu.addItem("back",      MMENU_BACK);
        msgMenu.setActionListener(this);

        msgText.setMenu(msgMenu, MMENU_BACK);
        msgText.setVLCommands(this);
        msgText.lock();
        msgText.clear();
        msgText.addBigText(record.date + ":", THEME_TEXT, Font.STYLE_BOLD, -1);
        msgText.doCRLF(-1);
        
        msgText.addTextWithEmotions(record.text, THEME_TEXT, Font.STYLE_PLAIN, -1);
        
        msgText.doCRLF(-1);
        msgText.setCaption(record.from);
        msgText.unlock();
        msgText.show();
    }
    
    // returns size of messages history list
    protected final int getSize() {
        return getHistorySize();
    }
      
    protected void drawItemData(GraphicsEx g, int index,
            int x1, int y1, int x2, int y2) {
        CachedRecord record = getCachedRecord(index);
        if ((null == record) || (null == record.getShortText())) return;
        Font font = getDefaultFont();
        g.setFont(font);
        g.setThemeColor((record.type == 0) ? THEME_CHAT_INMSG : THEME_CHAT_OUTMSG);
        g.drawString(record.getShortText(), x1, (y1 + y2 - font.getHeight()) / 2, 
                Graphics.TOP | Graphics.LEFT);
    }

    protected int getItemHeight(int itemIndex) {
        return Math.max(CanvasEx.minItemHeight, getDefaultFont().getHeight());
    }


    public void select(Select select, MenuModel model, int cmd) {
        switch (cmd) {
            case MMENU_BACK:
                restore();
                break;
            case MMENU_NEXT:
            case MMENU_PREV:
                moveInList((cmd == MMENU_NEXT) ? +1 : -1);
                break;

            // #sijapp cond.if target is "SIEMENS2" | target is "MOTOROLA" | target is "MIDP2"#
            case MENU_GOTO_URL:
                TextListEx urls = new TextListEx(null).gotoURL(getCachedRecord(getCurrItem()).text);
                if (null != urls) {
                    urls.show(this);
                }
                break;
            // #sijapp cond.end#
                
            case MENU_SELECT:
                showMessText();
                break;

            case MENU_FIND:
                if (null == frmFind) {
                    frmFind = new FormEx("find", "find", "back", this);
                    frmFind.addTextField(tfldFind, "text_to_find", "", 64, TextField.ANY);
                    frmFind.addCheckBox(chsFind + 0, "find_backwards", true);
                    frmFind.addCheckBox(chsFind + 1, "find_case_sensitiv", false);
                    frmFind.endForm();
                }
                frmFind.show();
                break;
                
            case MENU_CLEAR:
                MenuModel menu = new MenuModel();
                menu.addItem("currect_contact",         null, MENU_DEL_CURRENT);
                menu.addItem("all_contact_except_this", null, MENU_DEL_ALL_EXCEPT_CUR);
                menu.addItem("all_contacts",            null, MENU_DEL_ALL);
                menu.setActionListener(this);
                new Select(menu).show();
                break;

            case MENU_DEL_CURRENT:
                history.removeHistory();
                clearCache();
                restore();
                break;
                
            case MENU_DEL_ALL_EXCEPT_CUR:
                history.clearAll(contact);
                restore();
                break;
                
            case MENU_DEL_ALL:
                history.clearAll(null);
                clearCache();
                restore();
                break;

            case MENU_COPY_TEXT:
                int index = getCurrItem();
                if (index == -1) return;
                CachedRecord record = getCachedRecord(index);
                if (record == null) return;
                JimmUI.setClipBoardText((record.type == 0),
                        record.date, record.from, record.text);
                restore();
                break;

            case MENU_INFO:
                RecordStore rs = history.getRS();
                try {
                    String sb = ResourceBundle.getString("hist_cur") + ": " + getSize()  + "\n"
                            + ResourceBundle.getString("hist_size") + ": " + (rs.getSize() / 1024) + "\n"
                            + ResourceBundle.getString("hist_avail") + ": " + (rs.getSizeAvailable() / 1024) + "\n";
                    new PopupWindow("history_info", sb).show();
                } catch (Exception e) {
                }
                break;

            // #sijapp cond.if modules_FILES="true"#
            case MENU_EXPORT:
                new HistoryExport(contact).export(contact, history);
                break;

            case MENU_EXPORT_ALL:
                new HistoryExport(contact).export(null, null);
                break;
            // #sijapp cond.end#

            case MenuModel.BACK_COMMAND_CODE:
                back();
                closeHistoryView();
                break;
        }
    }
}

// #sijapp cond.if modules_FILES="true"#
class HistoryExport implements Runnable, FileBrowserListener {
    private Contact contact;
    public HistoryExport(Contact someC) {
        contact = someC;
    }
    private HistoryStorage exportHistory;
    private Contact        exportContact;
    private String directory;

    public void export(Contact contact, HistoryStorage storage) {
        exportHistory = storage;
        exportContact = contact;
        FileBrowser fsBrowser = new FileBrowser(true);
        fsBrowser.setListener(this);
        fsBrowser.activate();
    }

    public void onFileSelect(String s0) {
    }

    public void onDirectorySelect(String dir) {
        directory = dir;
        new Thread(this).start();
    }

    public void run() {
        SplashCanvas.setMessage(ResourceBundle.getEllipsisString("exporting"));
        SplashCanvas.setProgress(0);
        SplashCanvas.showSplash();
        if (null == exportHistory) {
            exportAll();
        } else {
            exportContact(exportContact, exportHistory);
        }
        ContactList.activate();
        new PopupWindow(null, ResourceBundle.getString("export_complete")).show();
    }

    private void exportUinToStream(Contact c, HistoryStorage storage, OutputStream os) throws IOException {
        boolean cp1251 = Options.getBoolean(Options.OPTION_CP1251_HACK);
        if (!cp1251) {
            os.write(new byte[] { (byte) 0xef, (byte) 0xbb, (byte) 0xbf });
        }

        int messageCount = storage.getHistorySize();
        if (messageCount == 0) {
            return;
        }

        String uin = c.getUin();
        String nick = (c.getName().length() > 0) ? c.getName() : uin;
        SplashCanvas.setMessage(nick);
        SplashCanvas.setProgress(0);
        String str_buf = "\r\n\t" + ResourceBundle.getString("message_history_with")
        + nick + " (" + uin + ")\r\n\t"
                + ResourceBundle.getString("export_date")
                + Util.getDateString(false)
                + "\r\n\r\n";
        os.write(StringConvertor.stringToByteArray(str_buf, !cp1251));
        
        String me = ResourceBundle.getString("me");
        int guiStep = Math.max(messageCount / 100, 1) * 5;
        for (int i = 0, curStep = 0; i < messageCount; ++i) {
            CachedRecord record = storage.getRecord(i);
            os.write(StringConvertor.stringToByteArray(" " + ((record.type == 0) ? nick : me)
            + " (" + record.date + "):\r\n", !cp1251));
            String currMsgText = StringConvertor.restoreCrLf(record.text.trim()) + "\r\n";
            os.write(StringConvertor.stringToByteArray(currMsgText, !cp1251));
            curStep++;
            if (curStep > guiStep) {
                os.flush();
                SplashCanvas.setProgress((100 * i) / messageCount);
                curStep = 0;
            }
        }
        os.flush();
    }

    private void exportUinToFile(Contact c, HistoryStorage storage, String filename) {
        try {
            if (storage.getHistorySize() > 0) {
                FileSystem file = FileSystem.getInstance();
                file.openFile(filename);
                OutputStream out = file.openOutputStream();
                exportUinToStream(c, storage, out);
                out.close();
                file.close();
            }
        } catch (Exception e) {
            JimmException.handleException(new JimmException(191, 5, false));
        }
    }
    
    private void exportContact(Contact c, HistoryStorage storage) {
        StringBuffer sb = new StringBuffer();
        int[] loclaDate = Util.createDate(Util.createCurrentDate(false));

        sb.append(directory).append("hist_")
                .append(c.getUniqueUin())
                .append('_')
                .append(Util.makeTwo(loclaDate[Util.TIME_YEAR] % 100))
                .append(Util.makeTwo(loclaDate[Util.TIME_MON]))
                //.append(Util.makeTwo(loclaDate[Util.TIME_DAY]))
                .append(".txt");
        storage.openHistory();
        exportUinToFile(c, storage, sb.toString());
        storage.closeHistory();
    }
    private void exportAll() {
        Vector cItems = contact.getProtocol().getContactItems();
        for (int i = 0; i < cItems.size(); i++) {
            Contact cItem = (Contact)cItems.elementAt(i);
            exportContact(cItem, HistoryStorage.getHistory(cItem));
        }
    }
}
// #sijapp cond.end#
// #sijapp cond.end#