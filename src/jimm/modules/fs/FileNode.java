/*
 * FileNode.java
 *
 * Created on 7 Февраль 2008 г., 20:26
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if modules_FILES="true"#
package jimm.modules.fs;

import DrawControls.icons.*;
import DrawControls.text.ListItem;
import DrawControls.tree.TreeNode;
import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import javax.microedition.lcdui.Font;
import jimm.comm.Sortable;
import jimm.ui.base.*;

/**
 *
 * @author vladimir
 */
public class FileNode implements Sortable {
    final private String path;
    final private String filename;
    private long size = -1;

    /** Creates a new instance of FileNode */
    public FileNode(String path, String filename) {
        this.path = path;
        this.filename = filename;
    }

    public String getText() {
        return filename;
    }

    public int getNodeWeight() {
        if (isParentDir()) {
            return 0;
        }
        if (isDir()) {
            return 10;
        }
        return 20;
    }

    public boolean isDir() {
        return filename.endsWith("/");
    }
    public boolean isParentDir() {
        return filename.equals(FileSystem.PARENT_DIRECTORY);
    }
    public boolean isDisk() {
        return path.equals(FileSystem.ROOT_DIRECTORY);
    }
    public boolean isFile() {
        return !isDir();
    }

    public long size() {
        if (!isDisk() && !isFile()) {
            return 0;
        }
        if (-1 == size) {
            try {
                FileSystem fs = FileSystem.getInstance();
                fs.openFile(getFullName());
                size = isFile() ? fs.fileSize() : fs.totalSize();
                fs.close();
            } catch (Exception e) {
                return 0;
            }
        }
        return size;
    }
    
    public String getFullName() {
        if (isParentDir()) {
            int d = path.lastIndexOf('/', path.length() - 2);
            return (d != -1) ? path.substring(0, d + 1) : FileSystem.ROOT_DIRECTORY;
        }
        return path + filename;
    }
}
// #sijapp cond.end#