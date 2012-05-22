// #sijapp cond.if modules_FILES="true"#
package jimm.modules.fs;

import DrawControls.*;
import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import DrawControls.tree.*;
import javax.microedition.lcdui.*;
import java.util.*;
import java.io.*;
import jimm.*;
import jimm.cl.*;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import jimm.util.ResourceBundle;



public class FileBrowser extends VirtualList implements Runnable {
    private final ImageList fsIcons = ImageList.createImageList("/fs.png");
    private static final int TYPE_FILE        = 1;
    private static final int TYPE_DIR         = 0;
    private static final int TYPE_PARENT_DIR  = 0;
    private static final int TYPE_DISK        = 0;

    private FileBrowserListener listener;
    private boolean needToSelectDirectory;
    private boolean selectFirst;
    
    private String currDir;

    private String nextDir = null;
    private Vector root = new Vector();
    
    private Icon[] getIcon(int type) {
        return new Icon[]{fsIcons.iconAt(type)};
    }

    public FileBrowser(boolean selectDir) {
        super(null);
        //setCapImages(getIcon(FileBrowser.TYPE_DIR));
        setCaption(selectDir ? "Dirs" : "Files");
        needToSelectDirectory = selectDir;
    }
    
    public void setListener(FileBrowserListener _listener) {
        this.listener = _listener;
    }

    public void activate() {
        if (jimm.modules.fs.FileSystem.isSupported()) {
            rebuildTree(FileSystem.ROOT_DIRECTORY);
            show();
        }
    }
    
    private FileNode getFile(String file) {
        int i = file.lastIndexOf('/', file.length() - 2);
        if (i <= 0) {
            return null;
        }
        return new FileNode(file.substring(0, i + 1), file.substring(i + 1));
    }
    public void run() {
        selectFirst = false;
        try {
            String currentPath = nextDir;
            FileSystem fs = FileSystem.getInstance();
            Vector newRoot = fs.getDirectoryContents(currentPath, needToSelectDirectory);
            
            Vector files = new Vector();
            for (int i = 0; i < newRoot.size(); ++i) {
                FileNode file = (FileNode)newRoot.elementAt(i);
                if (!FileSystem.PARENT_DIRECTORY.equals(file.getText())) {
                    files.addElement(file);
                }
            }
            Util.sort(files);
            if (needToSelectDirectory) {
                FileNode parent = getFile(currentPath);
                if (null != parent) {
                    files.insertElementAt(parent, 0);
                    selectFirst = true;
                }
            }
            lock();
            setCurrentItem(0);
            root = files;
            currDir = currentPath;
            restoring();
            unlock();
        } catch (JimmException e) {
            JimmException.handleException(e);
        } catch (Exception e) {
            JimmException.handleException(new JimmException(191, 2, true));
        }
        nextDir = null;
    }

    private void rebuildTree(String next) {
        if (null == nextDir) {
            nextDir = next;
            new Thread(this).start();
        }
    }
    
    protected void itemSelected() {
        FileNode file = getCurrentFile();
        if (null == file) {
            return;
        }
        String fullpath = file.getFullName();
        if (selectFirst && (0 == getCurrItem())) {
            listener.onDirectorySelect(fullpath);
            return;
        }

        if (file.isDir()) {
            rebuildTree(fullpath);
        
        } else {
            listener.onFileSelect(fullpath);
        }
    }
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if (type == CanvasEx.KEY_PRESSED) {
            switch (keyCode) {
                case NativeCanvas.LEFT_SOFT:
                    itemSelected();
                    return;

                case NativeCanvas.RIGHT_SOFT:
                case NativeCanvas.CLOSE_KEY:
                    if (!FileSystem.ROOT_DIRECTORY.equals(currDir)) {
                        int d = currDir.lastIndexOf('/', currDir.length() - 2);
                        rebuildTree(currDir.substring(0, d + 1));

                    } else {
                        ContactList.activate();
                    }
                    return;
            }
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }

    private FileNode getCurrentFile() {
        int num = getCurrItem();
        if ((0 <= num) && (num < root.size())) {
            return (FileNode)root.elementAt(num);
        }
        return null;
    }
    
    protected void onCursorMove() {
        restoring();
    }
    
    protected void restoring() {
        String cmd = "open";
        FileNode file = getCurrentFile();
        if (null != file) {
            if (selectFirst ? (0 == getCurrItem()) : file.isFile()) {
                cmd = "select";
            }
        }
        NativeCanvas.setCommands(cmd, "back");
    }

    protected int getSize() {
        return root.size();
    }

    protected int getItemHeight(int itemIndex) {
        return Math.max(CanvasEx.minItemHeight,
                Math.max(fsIcons.getHeight(), getDefaultFont().getHeight() + 1));
    }

    protected void drawItemData(GraphicsEx g, int index, int x1, int y1, int x2, int y2) {
        if (selectFirst && (0 == index)) {
            g.setThemeColor(CanvasEx.THEME_CAP_BACKGROUND);
            g.fillRect(x1, y1, x2 - x1, y2 - y1);
            g.setThemeColor(CanvasEx.THEME_CAP_TEXT);
        }else {
            g.setThemeColor(THEME_TEXT);
        }
        g.setFont(getDefaultFont());
        FileNode node = (FileNode)root.elementAt(index);
        boolean isDir = node.isDir() || node.isParentDir();
        g.drawString(getIcon(isDir ? TYPE_DIR : TYPE_FILE), node.getText(), null,
                x1, y1, x2 - x1, y2 - y1);
        if (selectFirst && (0 == index)) {
            g.drawLine(x1, y2, x2, y2);
        }
    }
}
// #sijapp cond.end#