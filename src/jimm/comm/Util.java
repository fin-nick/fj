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
 File: src/jimm/comm/Util.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Sergey Chernov, Andrey B. Ivlev
            Artyomov Denis, Igor Palkin, Vladimir Kryukov
 *******************************************************************************/


package jimm.comm;

import DrawControls.*;
import java.io.*;
import java.util.*;
import javax.microedition.lcdui.*;

import jimm.*;
import jimm.cl.*;
import jimm.util.ResourceBundle;


public class Util {
    private ByteArrayOutputStream stream = new ByteArrayOutputStream();
    public Util() {
    }
    public byte[] toByteArray() {
        return stream.toByteArray();
    }
    public String toUtf8String() {
        byte[] data = toByteArray();
        return StringConvertor.utf8beByteArrayToString(data, 0, data.length);
    }
    public int size() {
        return stream.size();
    }
    public void reset() {
        try {
    	    stream.reset();
        } catch (Exception e) {
        }
    }
    
    public void writeWordBE(int value) {
        try {
            stream.write(((value & 0xFF00) >> 8) & 0xFF);
            stream.write(value & 0xFF);
        } catch (Exception e) {
        }
    }
    public void writeWordLE(int value) {
        try {
            stream.write(value & 0xFF);
            stream.write(((value & 0xFF00) >> 8) & 0xFF);
        } catch (Exception e) {
        }
    }
    public void writeWord(int value, boolean bigEndian) {
        if (bigEndian) {
            writeWordBE(value);
        } else {
            writeWordLE(value);
        }
    }
    
    public void writeByteArray(byte[] array) {
        try {
            stream.write(array);
        } catch (Exception e) {
        }
    }
    
    public void writeByteArray(byte[] array, int offset, int length) {
        try {
            stream.write(array, offset, length);
        } catch (Exception e) {
        }
    }
    
    public void writeDWord(long value, boolean bigEndian) {
        if (bigEndian) {
            writeDWordBE(value);
        } else {
            writeDWordLE(value);
        }
    }
    public void writeDWordBE(long longValue) {
        try {
            int value = (int)longValue;
            stream.write(((value & 0xFF000000) >> 24) & 0xFF);
            stream.write(((value & 0x00FF0000) >> 16) & 0xFF);
            stream.write(((value & 0x0000FF00) >> 8)  & 0xFF);
            stream.write(  value & 0x000000FF);
        } catch (Exception e) {
        }
    }
    public void writeDWordLE(long longValue) {
        try {
            int value = (int)longValue;
            stream.write(  value & 0x000000FF);
            stream.write(((value & 0x0000FF00) >> 8)  & 0xFF);
            stream.write(((value & 0x00FF0000) >> 16) & 0xFF);
            stream.write(((value & 0xFF000000) >> 24) & 0xFF);
        } catch (Exception e) {
        }
    }
    
    public void writeByte(int value) {
        try {
            stream.write(value);
        } catch (Exception e) {
        }
    }
    
    public void writeShortLenAndString(String value, boolean utf8) {
        byte[] raw = StringConvertor.stringToByteArray(value, utf8);
        writeByte(raw.length);
        try {
            stream.write(raw, 0, raw.length);
        } catch (Exception e) {
        }
    }
    public void writeLenAndString(String value, boolean utf8) {
        byte[] raw = StringConvertor.stringToByteArray(value, utf8);
        writeWordBE(raw.length);
        try {
            stream.write(raw, 0, raw.length);
        } catch (Exception e) {
        }
    }
    public void writeString(String value, boolean utf8) {
        byte[] raw = StringConvertor.stringToByteArray(value, utf8);
        try {
            stream.write(raw, 0, raw.length);
        } catch (Exception e) {
        }
    }
    public void writeAsciizTLV(int type, String value, boolean bigEndian) {
        writeWord(type, bigEndian);
        byte[] raw = StringConvertor.stringToByteArray(value);
        writeWordLE(raw.length + 3);
        writeWordLE(raw.length + 1);
        try {
            stream.write(raw, 0, raw.length);
            stream.write(0);
        } catch (Exception e) {
        }
    }
    public void writeUtfzTLV(int type, String value) {
        writeWordLE(type);
        byte[] raw = StringConvertor.stringToByteArray(value);
        writeWordLE(raw.length + 3);
        writeWordLE(raw.length + 1);
        try {
            stream.write(raw, 0, raw.length);
            stream.write(0);
        } catch (Exception e) {
        }
    }
    public void writeAsciizTLV(int type, String value) {
        writeAsciizTLV(type, value, true);
    }
    public void writeTLV(int type, byte[] data) {
        writeWordBE(type);
        int length = (null == data) ? 0 : data.length;
        writeWordBE(length);
        if (length > 0) {
            try {
                stream.write(data, 0, data.length);
            } catch (Exception e) {
            }
        }
    }
    public void writeTLVWord(int type, int wordValue) {
        writeWordBE(type);
        writeWordBE(2);
        writeWordBE(wordValue);
    }
    public void writeTLVDWord(int type, long wordValue) {
        writeWordBE(type);
        writeWordBE(4);
        writeDWordBE(wordValue);
    }
    public void writeTLVByte(int type, int wordValue) {
        writeWordBE(type);
        writeWordBE(1);
        writeByte(wordValue);
    }

    
    

    // Counter variable
    private static volatile int counter = 0;

    public static int getNextCounter() {
        return ++counter;
    }

    // Password encryption key
    private static final byte[] PASSENC_KEY = {(byte)0xF3, (byte)0x26, (byte)0x81, (byte)0xC4,
                                              (byte)0x39, (byte)0x86, (byte)0xDB, (byte)0x92,
                                              (byte)0x71, (byte)0xA3, (byte)0xB9, (byte)0xE6,
                                              (byte)0x53, (byte)0x7A, (byte)0x95, (byte)0x7C};

    // Extracts the byte from the buffer (buf) at position off
    public static int getByte(byte[] buf, int off) {
        return ((int) buf[off]) & 0x000000FF;
    }


    // Puts the specified byte (val) into the buffer (buf) at position off
    public static void putByte(byte[] buf, int off, int val) {
        buf[off] = (byte) (val & 0x000000FF);
    }


    // Extracts the word from the buffer (buf) at position off using the specified byte ordering (bigEndian)
    public static int getWord(byte[] buf, int off, boolean bigEndian) {
        int val;
        if (bigEndian) {
            val = (((int) buf[off]) << 8) & 0x0000FF00;
            val |= (((int) buf[++off])) & 0x000000FF;
        } else {
            // Little endian
            val = (((int) buf[off])) & 0x000000FF;
            val |= (((int) buf[++off]) << 8) & 0x0000FF00;
        }
        return val;
    }
    
    
    static public DataInputStream getDataInputStream(byte[] array, int offset) {
        return new DataInputStream(new ByteArrayInputStream(array, offset, array.length-offset));
    }
    
    static public int getWord(DataInputStream stream, boolean bigEndian) throws IOException {
        return bigEndian
                ? stream.readUnsignedShort()
                : ((int)stream.readByte() & 0x00FF) | (((int)stream.readByte() << 8) & 0xFF00);
    }
    
    static public String readAsciiz(DataInputStream stream) throws IOException {
        int len = Util.getWord(stream, false);
        if (len == 0) {
            return "";
        }
        byte[] buffer = new byte[len];
        stream.readFully(buffer);
        return StringConvertor.byteArrayToString(buffer); 
    }
    // Extracts the word from the buffer (buf) at position off using big endian byte ordering
    public static int getWord(byte[] buf, int off) {
        return Util.getWord(buf, off, true);
    }


    // Puts the specified word (val) into the buffer (buf) at position off using the specified byte ordering (bigEndian)
    public static void putWord(byte[] buf, int off, int val, boolean bigEndian) {
        if (bigEndian) {
            buf[off]   = (byte) ((val >> 8) & 0x000000FF);
            buf[++off] = (byte) ((val)      & 0x000000FF);
        } else {
            // Little endian
            buf[off]   = (byte) ((val)      & 0x000000FF);
            buf[++off] = (byte) ((val >> 8) & 0x000000FF);
        }
    }


    // Puts the specified word (val) into the buffer (buf) at position off using big endian byte ordering
    public static void putWord(byte[] buf, int off, int val) {
        Util.putWord(buf, off, val, true);
    }


    // Extracts the double from the buffer (buf) at position off using the specified byte ordering (bigEndian)
    public static long getDWord(byte[] buf, int off, boolean bigEndian) {
        long val;
        if (bigEndian) {
            val  = (((long) buf[off]) << 24)   & 0xFF000000;
            val |= (((long) buf[++off]) << 16) & 0x00FF0000;
            val |= (((long) buf[++off]) << 8)  & 0x0000FF00;
            val |= (((long) buf[++off]))       & 0x000000FF;
        } else {
            // Little endian
            val  = (((long) buf[off]))         & 0x000000FF;
            val |= (((long) buf[++off]) << 8)  & 0x0000FF00;
            val |= (((long) buf[++off]) << 16) & 0x00FF0000;
            val |= (((long) buf[++off]) << 24) & 0xFF000000;
        }
        return val;
    }
    public static long getDWordLE(byte[] buf, int off) {
        long val;
        // Little endian
        val  = (((long) buf[off]))         & 0x000000FF;
        val |= (((long) buf[++off]) << 8)  & 0x0000FF00;
        val |= (((long) buf[++off]) << 16) & 0x00FF0000;
        val |= (((long) buf[++off]) << 24) & 0xFF000000;
        return val;
    }


    // Extracts the double from the buffer (buf) at position off using big endian byte ordering
    public static long getDWord(byte[] buf, int off) {
        return Util.getDWord(buf, off, true);
    }


    // Puts the specified double (val) into the buffer (buf) at position off using the specified byte ordering (bigEndian)
    public static void putDWordLE(byte[] buf, int off, long val) {
        // Little endian
        buf[off]   = (byte) ((val)       & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 8)  & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 16) & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 24) & 0x00000000000000FF);
    }
    // Puts the specified double (val) into the buffer (buf) at position off using big endian byte ordering
    public static void putDWordBE(byte[] buf, int off, long val) {
        buf[off]   = (byte) ((val >> 24) & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 16) & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 8)  & 0x00000000000000FF);
        buf[++off] = (byte) ((val)       & 0x00000000000000FF);
    }

    // getTlv(byte[] buf, int off) => byte[]
    public static byte[] getTlv(byte[] buf, int off) {
        if (off + 4 > buf.length) {
            return null;   // Length check (#1)
        }
        int length = Util.getWord(buf, off + 2);
        if (off + 4 + length > buf.length) {
            return null;   // Length check (#2)
        }
        byte[] value = new byte[length];
        System.arraycopy(buf, off + 4, value, 0, length);
        return value;
    }


    // Converts the specific 4 byte max buffer to an unsigned long
    public static long byteArrayToLong(byte[] b) {
        long l = 0;
        l |= b[0] & 0xFF;
        l <<= 8;
        l |= b[1] & 0xFF;
        l <<= 8;
        if (b.length > 3) {
            l |= b[2] & 0xFF;
            l <<= 8;
            l |= b[3] & 0xFF;
        }
        return l;
    }

    // DeScramble password
    public static byte[] decipherPassword(byte[] buf) {
        byte[] ret = new byte[buf.length];
        for (int i = 0; i < buf.length; i++) {
            ret[i] = (byte) (buf[i] ^ PASSENC_KEY[i % PASSENC_KEY.length]);
        }
        return ret;
    }

    //  If the numer has only one digit add a 0
    public static String makeTwo(int number) {
        if (number < 10) {
            return "0" + String.valueOf(number);
        }
        return String.valueOf(number);
    }
    
    // String IP to byte array
    public static byte[] ipToByteArray(String ip) {
        byte[] arrIP = new byte[4];
        int i;

        for (int j = 0; j < 3; j++) {
            for (i = 0; i < 3; i++) {
                if (ip.charAt(i) == '.') break;
            }
    
            arrIP[j] = (byte)Integer.parseInt(ip.substring(0, i));
            ip = ip.substring(i + 1);   
        }
        arrIP[3] = (byte)Integer.parseInt(ip);
        return arrIP;
    }
    public static long ipToLong(String ip) {
        long res = 0;
        int val = 0;
        for (int i = 0; i < ip.length(); i++) {
            if (ip.charAt(i) == '.') {
                res = (res * 0x100) + val;
                val = 0;
            } else {
                val = (val * 10) + (ip.charAt(i) - '0');
            }
        }
        res = (res * 0x100) + val;
        return res & 0x00000000FFFFFFFFL;
    }

    // #sijapp cond.if modules_PROXY is "true"#
    // Try to parse string IP
    public static boolean isIP(String ip) {
        try {
            ipToByteArray(ip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // #sijapp cond.end #
    private static Random rand = new Random(System.currentTimeMillis());
    public static int nextRandInt() {
        return Math.abs(Math.max(Integer.MIN_VALUE + 1, rand.nextInt()));
    }

    // #sijapp cond.if modules_TRAFFIC is "true" #
    // Returns String value of cost value
    public static String intToDecimal(int value) {
        try {
            if (value != 0) {
                String costString = Integer.toString(value / 1000) + ".";
                String afterDot = Integer.toString(value % 1000);
                while (afterDot.length() != 3) {
                    afterDot = "0" + afterDot;
                }
                while ((afterDot.endsWith("0")) && (afterDot.length() > 2)) {
                    afterDot = afterDot.substring(0, afterDot.length() - 1);
                }
                return costString + afterDot;
            }
        } catch (Exception e) {
        }
        return "0.0";
    }

    // Extracts the number value form String
    public static int decimalToInt(String string) {
        try {
            int i = string.indexOf('.');
            if (i < 0) {
                return Integer.parseInt(string) * 1000;

            } else {
                int value = Integer.parseInt(string.substring(0, i)) * 1000;
                string = string.substring(i + 1, Math.min(string.length(), i + 1 + 3));
                while (string.length() < 3) {
                    string = string + "0";
                }
                return value + Integer.parseInt(string);
            }
        } catch (Exception e) {
            return 0;
        }
    }
    // #sijapp cond.end#

    /*/////////////////////////////////////////////////////////////////////////
    //                                                                       //
    //                 METHODS FOR DATE AND TIME PROCESSING                  //
    //                                                                       //    
    /////////////////////////////////////////////////////////////////////////*/

    final public static int TIME_SECOND = 0;
    final public static int TIME_MINUTE = 1;
    final public static int TIME_HOUR   = 2;
    final public static int TIME_DAY    = 3;
    final public static int TIME_MON    = 4;
    final public static int TIME_YEAR   = 5;
    
    final private static byte[] dayCounts = {
        31,28,31,30,31,30,31,31,30,31,30,31
    };
    
    private final static int[] calFields= {
            Calendar.YEAR,         Calendar.MONTH,     Calendar.DATE,
            Calendar.HOUR_OF_DAY,  Calendar.MINUTE,    Calendar.SECOND};
    
    private final static int[] ofsFieldsA = { 0, 4, 6, 9, 12, 15 } ; //XEP-0091 - DEPRECATED
    private final static int[] ofsFieldsB = { 0, 5, 8, 11, 14, 17 } ;//XEP-0203
    private final static String[] months = {"Jan", "Feb", "Mar", "Apr",
                        "May", "Jun", "Jul", "Aug",
                        "Sep", "Oct", "Nov", "Dec"};
    public static long createDate(String sdate, boolean returnGmt) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        try {
            sdate = sdate.trim();
            int[] ofs = sdate.endsWith("Z") ? ofsFieldsB : ofsFieldsA;
            if (Character.isDigit(sdate.charAt(0))) {
                int fieldLength = 4;    // yearlen
                for (int i = 0; i < calFields.length; ++i) {
                    int begIndex = ofs[i];
                    int field = strToIntDef(sdate.substring(begIndex, begIndex + fieldLength), 0);
                    if (1 == i) {
                        field += Calendar.JANUARY - 1;
                    }
                    fieldLength = 2;
                    c.set(calFields[i], field);
                }
            } else {
                String[] rfcDate = Util.explode(sdate, ' ');
                c.set(Calendar.YEAR, strToIntDef(rfcDate[3], 0));
                
                for (int i = 0; i < months.length; ++i) {
                    if (months[i].equals(rfcDate[2])) {
                        c.set(Calendar.MONTH, i);
                        break;
                    }
                }
                c.set(Calendar.DATE, strToIntDef(rfcDate[1], 0));
                c.set(Calendar.HOUR_OF_DAY, strToIntDef(rfcDate[4].substring(0, 2), 0));
                c.set(Calendar.MINUTE,      strToIntDef(rfcDate[4].substring(3, 5), 0));
                c.set(Calendar.SECOND,      strToIntDef(rfcDate[4].substring(6), 0));
                
            }
            long result = Math.max(0, c.getTime().getTime() / 1000);
            return returnGmt ? result : gmtTimeToLocalTime(result);
        } catch (Exception e) {
        }
        return 0;
    }


    /* Creates current date (GMT or local) */
    public static long createCurrentDate(boolean gmt) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        long result = calendar.getTime().getTime() / 1000
                + Options.getInt(Options.OPTIONS_LOCAL_OFFSET) * 3600;
        /* returns GMT or local time */
        return gmt ? result : gmtTimeToLocalTime(result);
    }
    
    /* Show date string */
    public static String getDateString(long date, boolean onlyTime) {
        if (date == 0) return "***error***";
        
        int[] loclaDate = createDate(date);
        
        StringBuffer sb = new StringBuffer();
        
        if (!onlyTime) {
            sb.append(Util.makeTwo(loclaDate[TIME_DAY]))
              .append('.')
              .append(Util.makeTwo(loclaDate[TIME_MON]))
              .append('.')
              .append(loclaDate[TIME_YEAR])
              .append(' ');
        }
        
        sb.append(Util.makeTwo(loclaDate[TIME_HOUR]))
          .append(':')
          .append(Util.makeTwo(loclaDate[TIME_MINUTE]));
        
        return sb.toString();
    }
    
    /* Show date string */
    public static String getUtcDateString(long date) {
        int[] loclaDate = createDate(date);
        
        StringBuffer sb = new StringBuffer();
        
        sb.append(loclaDate[TIME_YEAR])
          .append('-');
        sb.append(Util.makeTwo(loclaDate[TIME_MON]))
          .append('-');
        sb.append(Util.makeTwo(loclaDate[TIME_DAY]))
          .append('T');
        
        sb.append(Util.makeTwo(loclaDate[TIME_HOUR]))
          .append(':')
          .append(Util.makeTwo(loclaDate[TIME_MINUTE]))
          .append(':')
          .append(Util.makeTwo(loclaDate[TIME_SECOND]));
        
        sb.append('Z');
        return sb.toString();
    }
    /* Generates seconds count from 1st Jan 1970 till mentioned date */ 
    public static long createLongTime(int year, int mon, int day,
            int hour, int min, int sec) {
        int day_count, i, febCount;

        day_count = (year - 1970) * 365 + day;
        day_count += (year - 1968) / 4;
        if (year >= 2000) day_count--;

        if ((year % 4 == 0) && (year != 2000)) {
            day_count--;
            febCount = 29;
        } else {
            febCount = 28;
        }

        for (i = 0; i < mon - 1; i++) {
            day_count += (i == 1) ? febCount : dayCounts[i];
        }

        long result = day_count * 24L * 3600L + hour * 3600L + min * 60L + sec;
        /* returns GMT or local time */
        return result;
    }
    
    // Creates array of calendar values form value of seconds since 1st jan 1970 (GMT)
    public static int[] createDate(long value) {
        int total_days, last_days, i;
        int sec, min, hour, day, mon, year;

        sec = (int) (value % 60);

        min = (int) ((value / 60) % 60); // min
        value -= 60 * min;

        hour = (int) ((value / 3600) % 24); // hour
        value -= 3600 * hour;

        total_days = (int) (value / (3600 * 24));

        year = 1970;
        for (;;) {
            last_days = total_days - ((year % 4 == 0) && (year != 2000) ? 366 : 365);
            if (last_days <= 0) break;
            total_days = last_days;
            year++;
        } // year

        int febrDays = ((year % 4 == 0) && (year != 2000)) ? 29 : 28;

        mon = 1;
        for (i = 0; i < 12; i++) {
            last_days = total_days - ((i == 1) ? febrDays : dayCounts[i]);
            if (last_days <= 0) break;
            mon++;
            total_days = last_days;
        } // mon

        day = total_days; // day

        return new int[] { sec, min, hour, day, mon, year };
    }
    
    public static String getDateString(boolean onlyTime) {
        return getDateString( createCurrentDate(false) , onlyTime);
    }
    
    public static long gmtTimeToLocalTime(long gmtTime) {
        long diff = Options.getInt(Options.OPTIONS_GMT_OFFSET) * 3600L;
        return gmtTime + diff;
    }    
    
    public static String longitudeToString(long seconds) {
        int days = (int)(seconds / 86400);
        seconds %= 86400;
        int hours = (int)(seconds / 3600);
        seconds %= 3600;
        int minutes = (int)(seconds / 60);
        
        StringBuffer buf = new StringBuffer();
        if (days != 0) {
            buf.append(days).append(' ').append( ResourceBundle.getString("days") ).append(' ');
        }
        if (hours != 0) {
            buf.append(hours).append(' ').append( ResourceBundle.getString("hours") ).append(' ');
        }
        if (minutes != 0) {
            buf.append(minutes).append(' ').append( ResourceBundle.getString("minutes") );
        }
        
        return buf.toString();
    }
    public static String getCurrentDay() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.setTime(new Date(createCurrentDate(false) * 1000));
        String day = "";
        
        switch (cal.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                day = "monday";
                break;

            case Calendar.TUESDAY:
                day = "tuesday";
                break;
                
            case Calendar.WEDNESDAY:
                day = "wednesday";
                break;
                
            case Calendar.THURSDAY:
                day = "thursday";
                break;
                
            case Calendar.FRIDAY:
                day = "friday";
                break;
                
            case Calendar.SATURDAY:
                day = "saturday";
                break;
                
            case Calendar.SUNDAY:
                day = "sunday";
                break;
        }
        return ResourceBundle.getString(day);
    }

    static private void Encode(byte[] output, long[] input, int len) {
        int i, j;
        for (i = 0, j = 0; j < len; i++, j += 4) {
            output[j] = (byte)(input[i] & 0xffL);
            output[j + 1] = (byte)((input[i] >>> 8) & 0xffL);
            output[j + 2] = (byte)((input[i] >>> 16) & 0xffL);
            output[j + 3] = (byte)((input[i] >>> 24) & 0xffL);
        }
    }
    static private void Decode(long[] output, byte[] input, int len) {
        int i, j;
        for (i = 0, j = 0; j < len; i++, j += 4) {
            output[i] = b2iu(input[j]) |
                (b2iu(input[j + 1]) << 8) |
                (b2iu(input[j + 2]) << 16) |
                (b2iu(input[j + 3]) << 24);
        }
    }
    public static long b2iu(byte b) {
        return b < 0 ? b & 0x7F + 128 : b;
    }

    
    private static final int URL_CHAR_PROTOCOL = 0;
    private static final int URL_CHAR_PREV    = 1;
    private static final int URL_CHAR_OTHER   = 2;
    private static final int URL_CHAR_DIGIT   = 3;
    private static final int URL_CHAR_NONE    = 4;

    private static boolean isURLChar(char chr, int mode) {
        if (mode == URL_CHAR_PROTOCOL) {
            return ((chr >= 'A') && (chr <= 'Z')) ||
                    ((chr >= 'a') && (chr <= 'z'));
        }

        if (mode == URL_CHAR_PREV) {
            return ((chr >= 'A') && (chr <= 'Z'))
                    || ((chr >= 'a') && (chr <= 'z'))
                    || ((chr >= '0') && (chr <= '9'))
                    || ('@' == chr) || ('-' == chr)
                    || ('_' == chr)|| ('%' == chr);
        }
        if (URL_CHAR_DIGIT == mode) return Character.isDigit(chr);
        if (URL_CHAR_NONE == mode) return (' ' == chr) || ('\n' == chr);

        if ((chr <= ' ') || (chr == '\n')) return false;
        return true;
    }

    private static final void putUrl(Vector urls, String url) {
        final String skip = "?!;:,.";
        final String openDelemiters = "{[(«";
        final String delemiters = "}])»";
        int cutIndex = url.length() - 1;
        for (; cutIndex >= 0; --cutIndex) {
            char lastChar = url.charAt(cutIndex);
            if (-1 != skip.indexOf(lastChar)) {
                continue;
            }
            int delemiterIndex = delemiters.indexOf(lastChar);
            if (-1 != delemiterIndex) {
                if (-1 == url.indexOf(openDelemiters.charAt(delemiterIndex))) {
                    continue;
                }
            }
            break;
        }

        if (cutIndex <= 0) {
            return;

        } else if (cutIndex != url.length() - 1) {
            url = url.substring(0, cutIndex + 1);
        }

        if (-1 == url.indexOf(':')) {
            boolean isPhone = ('+' == url.charAt(0));
            boolean hasDot = false;
            boolean nonDigit = false;
            for (int i = isPhone ? 1 : 0; i < url.length(); ++i) {
                char ch = url.charAt(i);
                if ('.' == ch) {
                    hasDot = true;
                } else if (!Character.isDigit(ch)) {
                    nonDigit = true;
                    break;
                }
            }
            if (isPhone) {
                if (!nonDigit && !hasDot && (7 <= url.length())) {
                    url = "tel:" + url;
                } else {
                    return;
                }
            } else {
                if (nonDigit) {
                    if (-1 == url.indexOf('/')) {
                        if (-1 == url.indexOf('@')) return;
                        // jid or email
                    } else {
                        url = "http:\57\57" + url;
                    }
                } else {
                    return;
                }
            }
        }
        int protoEnd = url.indexOf(':');
        if (-1 != protoEnd) {
            if (url.length() <= protoEnd + 5) {
                return;
            }
            for (int i = 0; i < protoEnd; ++i) {
                if (!isURLChar(url.charAt(i), URL_CHAR_PROTOCOL)) {
                    return;
                }
            }
        }
        if (!urls.contains(url)) {
            urls.addElement(url);
        }
    }
    private static void parseForUrl(Vector result, String msg, char ch, int before, int after, int limit) {
        if (limit <= result.size()) {
            return;
        }
        int size = msg.length();
        int findIndex = 0;
        int beginIdx;
        int endIdx;
        for (;;) {
            if (findIndex >= size) break;
            int ptIndex = msg.indexOf(ch, findIndex);
            if (ptIndex == -1) break;
            
            for (endIdx = ptIndex + 1; endIdx < size; ++endIdx) {
                if (!isURLChar(msg.charAt(endIdx), after)) {
                    break;
                }
            }
        
            findIndex = endIdx;
            if (endIdx - ptIndex < 2) continue;

            if  (URL_CHAR_NONE != before) {
                for (beginIdx = ptIndex - 1; beginIdx >= 0; --beginIdx) {
                    if (!isURLChar(msg.charAt(beginIdx), before)) {
                        break;
                    }
                }
                if ((beginIdx == -1) || !isURLChar(msg.charAt(beginIdx), before)) {
                    beginIdx++;
                }
                if (ptIndex == beginIdx) continue;

            } else {
                beginIdx = ptIndex;
                if ((0 < beginIdx) && !isURLChar(msg.charAt(beginIdx - 1), before)) {
                    continue;
                }
            }
            if (endIdx - beginIdx < 5) continue;
            putUrl(result, msg.substring(beginIdx, endIdx));
            if (limit < result.size()) {
                return;
            }
        }
    }
    public static String getUrlWithoutProtocol(String url) {
        int index = url.indexOf(':');
        if (-1 != index) {
            url = url.substring(index + 1);
            if (url.startsWith("\57\57")) {
                url = url.substring(2);
            }
        }
        return url;
    }
    public static String notUrls(String str) {
        str = StringConvertor.notNull(str);
        return (-1 != str.indexOf("http://")) ? "" : str;
    }
    public static boolean hasURL(String msg) {
        Vector result = new Vector();
        parseForUrl(result, msg, '.', URL_CHAR_PREV, URL_CHAR_OTHER, 1);
        parseForUrl(result, msg, ':', URL_CHAR_PROTOCOL, URL_CHAR_OTHER, 1);
        parseForUrl(result, msg, '+', URL_CHAR_NONE, URL_CHAR_DIGIT, 1);
        parseForUrl(result, msg, '@', URL_CHAR_PREV, URL_CHAR_OTHER, 1);
        return !result.isEmpty();
    }
    public static Vector parseMessageForURL(String msg) {
        // we are parsing 100 links only
        final int MAX_LINK_COUNT = 100;
        Vector result = new Vector();
        parseForUrl(result, msg, '.', URL_CHAR_PREV, URL_CHAR_OTHER, MAX_LINK_COUNT);
        parseForUrl(result, msg, ':', URL_CHAR_PROTOCOL, URL_CHAR_OTHER, MAX_LINK_COUNT);
        parseForUrl(result, msg, '+', URL_CHAR_NONE, URL_CHAR_DIGIT, MAX_LINK_COUNT);
        parseForUrl(result, msg, '@', URL_CHAR_PREV, URL_CHAR_OTHER, MAX_LINK_COUNT);
        return result.isEmpty() ? null : result;
    }
    
    public static int strToIntDef(String str, int defValue) {
        if (null == str) {
            return defValue;
        }
        try {
            while ((1 < str.length()) && ('0' == str.charAt(0))) {
                str = str.substring(1);
            }
            return Integer.parseInt(str);
        } catch (Exception e) {
        }
        return defValue;
    }
    

    public static final String replace(String text, String from, String to) {
        int fromSize = from.length();
        int toSize = to.length();
        int pos = 0;
        for (;;) {
            pos = text.indexOf(from, pos);
            if (pos == -1) break;
            text = text.substring(0, pos) + to
                    + text.substring(pos + fromSize, text.length());
            pos += toSize;
        }
        return text;
    }
    
    public static final String replace(String text, String[] from, String[] to, String keys) {
        // keys - is first chars of from
        StringBuffer result = new StringBuffer();
        int pos = 0;
        while (pos < text.length()) {
            char ch = text.charAt(pos);

            int index = keys.indexOf(ch);
            while (-1 != index) {
                if (text.startsWith(from[index], pos)) {
                    pos += from[index].length();
                    result.append(to[index]);
                    break;
                }
                index = keys.indexOf(text.charAt(pos), index + 1);
            }

            if (-1 == index) {
                result.append(ch);
                pos++;
            }
        }
        
        return result.toString();
    }
    
    /* Divide text to array of parts using serparator charaster */
    static public String[] explode(String text, char separator) {
        if (StringConvertor.isEmpty(text)) {
            return new String[0];
        }
        Vector tmp = new Vector();
        int start = 0;
        int end = text.indexOf(separator, start);
        while (end >= start) {
            tmp.addElement(text.substring(start, end));
            start = end + 1;
            end = text.indexOf(separator, start);
        }
        tmp.addElement(text.substring(start));
        String[] result = new String[tmp.size()];
        tmp.copyInto(result);
        return result; 
    }
    static public String implode(String[] text, String separator) {
	StringBuffer result = new StringBuffer();
	for (int i = 0; i < text.length; i++) {
	    if (null != text[i]) {
		if (0 != result.length()) {
		    result.append(separator);
		}
		result.append(text[i]);
	    }
	}
	return result.toString();
    }

    private static final String base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    private static final int base64GetNextChar(String str, int index) {
        if (-1 == index) return -2;
        char ch = str.charAt(index);
        if ('=' == ch) {
            return -1;
        }
        return base64.indexOf(ch);
    }
    private static final int base64GetNextIndex(String str, int index) {
        for (; index < str.length(); ++index) {
            char ch = str.charAt(index);
            if ('=' == ch) {
                return index;
            }
            int code = base64.indexOf(ch);
            if (-1 != code) {
                return index;
            }
        }
        return -1;
    }
    
    public static final byte[] base64decode(String str) {
        if (null == str) str = "";
        Util out = new Util();
        for (int strIndex = 0; strIndex < str.length(); ++strIndex) {
    	    strIndex = base64GetNextIndex(str, strIndex);
            if (-1 == strIndex) break;
            int ch1 = base64GetNextChar(str, strIndex);
            if (-1 == ch1) break;

            strIndex = base64GetNextIndex(str, strIndex + 1);
            if (-1 == strIndex) break;
            int ch2 = base64GetNextChar(str, strIndex);
            if (-1 == ch2) break;
            out.writeByte((byte)(0xFF & ((ch1 << 2) | (ch2 >>> 4))));

            strIndex = base64GetNextIndex(str, strIndex + 1);
            if (-1 == strIndex) break;
            int ch3 = base64GetNextChar(str, strIndex);
            if (-1 == ch3) break;
            out.writeByte((byte)(0xFF & ((ch2 << 4) | (ch3 >>> 2))));

            strIndex = base64GetNextIndex(str, strIndex + 1);
            if (-1 == strIndex) break;
            int ch4 = base64GetNextChar(str, strIndex);
            if (-1 == ch4) break;
            out.writeByte((byte)(0xFF & ((ch3 << 6) | (ch4 >>> 0))));
        }
        return out.toByteArray();
    }
    public static final String base64encode( final byte[] data ) {
        char[] out = new char[((data.length + 2) / 3) * 4];
        for (int i = 0, index = 0; i < data.length; i += 3, index += 4) {
            boolean quad = false;
            boolean trip = false;
            
            int val = (0xFF & data[i]);
            val <<= 8;
            if ((i + 1) < data.length) {
                val |= (0xFF & data[i + 1]);
                trip = true;
            }
            val <<= 8;
            if ((i + 2) < data.length) {
                val |= (0xFF & data[i + 2]);
                quad = true;
            }
            out[index+3] = base64.charAt(quad ? (val & 0x3F) : 64);
            val >>= 6;
            out[index+2] = base64.charAt(trip ? (val & 0x3F) : 64);
            val >>= 6;
            out[index+1] = base64.charAt(val & 0x3F);
            val >>= 6;
            out[index+0] = base64.charAt(val & 0x3F);
        }
        return new String(out);
    }

    private static final String[] escapedChars = {"&quot;", "&apos;", "&gt;", "&lt;", "&amp;"};
    private static final String[] unescapedChars = {"\"", "'", ">", "<", "&"};
    public static String xmlEscape(String text) {
        text = StringConvertor.notNull(text);
        return Util.replace(text, unescapedChars, escapedChars, "\"'><&");
    }
    
    public static String xmlUnescape(String text) {
        if (-1 == text.indexOf('&')) {
            return text;
        }
        return Util.replace(text, escapedChars, unescapedChars, "&&&&&");
    }

    public static final Image createThumbnail(Image image, int width, int height) {
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();

        if ((width > sourceWidth) && (height > sourceHeight)) {
            return image;
        }
        int thumbWidth = width;
        int thumbHeight = thumbWidth * sourceHeight / sourceWidth;
        if (thumbHeight > height) {
            thumbHeight = height;
            thumbWidth = thumbHeight * sourceWidth / sourceHeight;
        }

        Image thumb = Image.createImage(thumbWidth, thumbHeight);
        Graphics g = thumb.getGraphics();

        for (int y = 0; y < thumbHeight; ++y) {
            for (int x = 0; x < thumbWidth; ++x) {
                g.setClip(x, y, 1, 1);
                int dx = x * sourceWidth / thumbWidth;
                int dy = y * sourceHeight / thumbHeight;
                g.drawImage(image, x - dx, y - dy, Graphics.LEFT | Graphics.TOP);
            }
        }
        return thumb;
    }


    //////////////////////////////////////////////////////////////////////////////////
    private static int compareNodes(Sortable node1, Sortable node2) {
        int result = node1.getNodeWeight() - node2.getNodeWeight();
        if (0 == result) {
            result = StringConvertor.stringCompare(node1.getText(), node2.getText());
        } 
        return result;
    }

    public static void sort(Vector subnodes) {
        for (int i = 1; i < subnodes.size(); ++i) {
            Sortable currNode = (Sortable)subnodes.elementAt(i);
            int j = i - 1;
            for (; j >= 0; --j) {
                Sortable itemJ = (Sortable)subnodes.elementAt(j);
                if (compareNodes(itemJ, currNode) <= 0) {
                    break;
                }
                subnodes.setElementAt(itemJ, j + 1);
            }
            if (j + 1 != i) {
                subnodes.setElementAt(currNode, j + 1);
            }
        }
    }
    
    //////////////////////////////////////////////////////////////////////////////////
    private static void putCh(StringBuffer sb, int ch) {
        String s = Integer.toHexString(ch);
        sb.append("%");
        if (1 == s.length()) sb.append('0');
        sb.append(s);
    }
    public static String urlEscape(String param) {
        String urlOK = "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < param.length(); ++i) {
            char ch = param.charAt(i);
            char lowerCh = Character.toLowerCase(ch);
            if (Character.isDigit(ch) || (-1 != "qwertyuiopasdfghjklzxcvbnm@.-".indexOf(lowerCh))) {
                sb.append(ch);
            
            } else if (' ' == ch) {
                sb.append('+');
            
            } else if ((0x7F & ch) == ch) {
                putCh(sb, ch);
                
            } else if ((0xFFF & ch) == ch) {
                putCh(sb, 0xD0 | (ch >> 6));
                putCh(sb, 0x80 | (0x3F & ch));
                
            } else {
                putCh(sb, 0xE8 | (ch >> 12));
                putCh(sb, 0x80 | (0x3F & (ch >> 6)));
                putCh(sb, 0x80 | (0x3F & ch));
            }
        }
        return sb.toString();
    }
    //////////////////////////////////////////////////////////////////////////////////
    public static final int getIndex(Vector v, Object o) {
        int size = v.size();
        for (int i = 0; i < size; ++i) {
            if (v.elementAt(i) == o) {
                return i;
            }
        }
        return -1;
    }
}
