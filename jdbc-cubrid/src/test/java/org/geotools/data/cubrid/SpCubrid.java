package org.geotools.data.cubrid;

import java.io.ByteArrayOutputStream;

public class SpCubrid {
    public static String HelloCubrid() {
        return "Hello, Cubrid !!";
    }

    public static String ST_MakePoint(double x, double y) {
        return "POINT(" + x + " " + y + ")";
    }

    static int byteOrder = ByteOrderValues.BIG_ENDIAN; // BIG_ENDIAN = 1;

    public static byte[] ST_POINT(double x, double y) {
        
        int num = 0;
        if (num == 0) {
            String stringToConvert = "dddddddddddddddddd";
            return stringToConvert.getBytes();
        }
        
        byte[] buf = new byte[8];

        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        byteArrayOS.reset();

        // 1. write byte order
        if (byteOrder == ByteOrderValues.LITTLE_ENDIAN) {
            buf[0] = WKBConstants.wkbNDR;
        } else {
            buf[0] = WKBConstants.wkbXDR;
        }
        byteArrayOS.write(buf, 0, 1);

        // 2. write geometry type
        ByteOrderValues.putInt(WKBConstants.wkbPoint, buf, byteOrder);
        byteArrayOS.write(buf, 0, 4);

        // 3. write x, y coordinate
        ByteOrderValues.putDouble(x, buf, byteOrder);
        byteArrayOS.write(buf, 0, 8);
        ByteOrderValues.putDouble(y, buf, byteOrder);
        byteArrayOS.write(buf, 0, 8);

        return byteArrayOS.toByteArray();
    }

    public static final class ByteOrderValues {
        public static final int BIG_ENDIAN = 1;

        public static final int LITTLE_ENDIAN = 2;

        public static int getInt(byte[] buf, int byteOrder) {
            if (byteOrder == BIG_ENDIAN) {
                return ((int) (buf[0] & 0xff) << 24) | ((int) (buf[1] & 0xff) << 16)
                        | ((int) (buf[2] & 0xff) << 8) | ((int) (buf[3] & 0xff));
            } else {// LITTLE_ENDIAN
                return ((int) (buf[3] & 0xff) << 24) | ((int) (buf[2] & 0xff) << 16)
                        | ((int) (buf[1] & 0xff) << 8) | ((int) (buf[0] & 0xff));
            }
        }

        public static void putInt(int intValue, byte[] buf, int byteOrder) {
            if (byteOrder == BIG_ENDIAN) {
                buf[0] = (byte) (intValue >> 24);
                buf[1] = (byte) (intValue >> 16);
                buf[2] = (byte) (intValue >> 8);
                buf[3] = (byte) intValue;
            } else {// LITTLE_ENDIAN
                buf[0] = (byte) intValue;
                buf[1] = (byte) (intValue >> 8);
                buf[2] = (byte) (intValue >> 16);
                buf[3] = (byte) (intValue >> 24);
            }
        }

        public static long getLong(byte[] buf, int byteOrder) {
            if (byteOrder == BIG_ENDIAN) {
                return (long) (buf[0] & 0xff) << 56 | (long) (buf[1] & 0xff) << 48
                        | (long) (buf[2] & 0xff) << 40 | (long) (buf[3] & 0xff) << 32
                        | (long) (buf[4] & 0xff) << 24 | (long) (buf[5] & 0xff) << 16
                        | (long) (buf[6] & 0xff) << 8 | (long) (buf[7] & 0xff);
            } else {// LITTLE_ENDIAN
                return (long) (buf[7] & 0xff) << 56 | (long) (buf[6] & 0xff) << 48
                        | (long) (buf[5] & 0xff) << 40 | (long) (buf[4] & 0xff) << 32
                        | (long) (buf[3] & 0xff) << 24 | (long) (buf[2] & 0xff) << 16
                        | (long) (buf[1] & 0xff) << 8 | (long) (buf[0] & 0xff);
            }
        }

        public static void putLong(long longValue, byte[] buf, int byteOrder) {
            if (byteOrder == BIG_ENDIAN) {
                buf[0] = (byte) (longValue >> 56);
                buf[1] = (byte) (longValue >> 48);
                buf[2] = (byte) (longValue >> 40);
                buf[3] = (byte) (longValue >> 32);
                buf[4] = (byte) (longValue >> 24);
                buf[5] = (byte) (longValue >> 16);
                buf[6] = (byte) (longValue >> 8);
                buf[7] = (byte) longValue;
            } else { // LITTLE_ENDIAN
                buf[0] = (byte) longValue;
                buf[1] = (byte) (longValue >> 8);
                buf[2] = (byte) (longValue >> 16);
                buf[3] = (byte) (longValue >> 24);
                buf[4] = (byte) (longValue >> 32);
                buf[5] = (byte) (longValue >> 40);
                buf[6] = (byte) (longValue >> 48);
                buf[7] = (byte) (longValue >> 56);
            }
        }

        public static double getDouble(byte[] buf, int byteOrder) {
            long longVal = getLong(buf, byteOrder);
            return Double.longBitsToDouble(longVal);
        }

        public static void putDouble(double doubleValue, byte[] buf, int byteOrder) {
            long longVal = Double.doubleToLongBits(doubleValue);
            putLong(longVal, buf, byteOrder);
        }

    }

    public interface WKBConstants {
        int wkbXDR = 0;

        int wkbNDR = 1;

        int wkbPoint = 1;

        int wkbLineString = 2;

        int wkbPolygon = 3;

        int wkbMultiPoint = 4;

        int wkbMultiLineString = 5;

        int wkbMultiPolygon = 6;

        int wkbGeometryCollection = 7;
    }

}
