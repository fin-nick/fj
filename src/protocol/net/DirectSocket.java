/*
 * DirectSocket.java
 *
 * Created on 23 ������ 2007 �., 19:11
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_ICQ is "true" #
package protocol.net;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import jimm.comm.*;
import jimm.modules.traffic.*;
import protocol.icq.*;
import protocol.icq.packet.*;
import jimm.modules.*;
import jimm.util.ResourceBundle;
import jimm.*;

public class DirectSocket {
    private StreamConnection sc;
    private InputStream is;
    private OutputStream os;
    
    public void connectTo(String url) throws JimmException {
        try {
            sc = (StreamConnection)Connector.open(url, Connector.READ_WRITE);
            //SocketConnection socket = (SocketConnection)sc;
            //socket.setSocketOption(SocketConnection.DELAY, 0);
            //socket.setSocketOption(SocketConnection.KEEPALIVE, 2*60);
            //socket.setSocketOption(SocketConnection.LINGER, 0);
            //socket.setSocketOption(SocketConnection.RCVBUF, 10*1024);
            //socket.setSocketOption(SocketConnection.SNDBUF, 10*1024);
            os = sc.openOutputStream();
            is = sc.openInputStream();
        } catch (ConnectionNotFoundException e) {
            throw new JimmException(121, 0);
        } catch (SecurityException e) {
            throw new JimmException(120, 9);
        } catch (IllegalArgumentException e) {
            throw new JimmException(122, 0);
        } catch (IOException e) {
            throw new JimmException(120, 0);
        } catch (Exception e) {
            throw new JimmException(120, 10);
        }
    }
    public final int read() throws JimmException {
        try {
            return is.read();
        } catch (Exception e) {
            close();
            throw new JimmException(120, 4);
        }
    }
    
    public final int readFully(byte[] data) throws JimmException {
        try {
            int bReadSum = 0;
            do {
                int bRead = is.read(data, bReadSum, data.length - bReadSum);
                if (-1 == bRead) {
                    throw new IOException();
                } else if (0 == bRead) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception ex) {
                    }
                }
                bReadSum += bRead;
            } while (bReadSum < data.length);
            return bReadSum;
        } catch (Exception e) {
            close();
            throw new JimmException(120, 1);
        }
    }
    public final void write(byte[] data) throws JimmException {
        try {
            os.write(data);
            os.flush();
        } catch (Exception e) {
            close();
            throw new JimmException(120, 2);
        }
        // #sijapp cond.if modules_TRAFFIC is "true" #
        Traffic.getInstance().addOutTraffic(data.length + 51); // 51 is the overhead for each packet
        // #sijapp cond.end#
    }
    public final int available() throws JimmException {
        try {
            return is.available();
        } catch (Exception e) {
            close();
            throw new JimmException(120, 3);
        }
    }

    // Sets the reconnect flag and closes the connection
    public final void close() {
        try {
            if (null != is) {
                is.close();
            }
        } catch (Exception e) { /* Do nothing */
        }
        is = null;
        try {
            if (null != os) {
                os.close();
            }
        } catch (Exception e) { /* Do nothing */
        }
        os = null;
        try {
            if (null != sc) {
                sc.close();
            }
        } catch (Exception e) { /* Do nothing */
        }
        sc = null;
    }
}
// #sijapp cond.end #
