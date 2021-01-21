package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 Checksum utility class
 */
public class Checksum {
    private static byte[] createChecksum(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream dis = new DigestInputStream(new FileInputStream(file), md);
        //noinspection StatementWithEmptyBody
        while (dis.read() != -1) ;
        dis.close();
        return md.digest();
    }

    // a byte array to a HEX string
    public static String getMD5Checksum(File file) throws Exception {
        byte[] bytes = createChecksum(file);
        return bytesToString(bytes);
    }

    public static String getMD5Checksum(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        for (byte aByte : bytes) {
            md.update(aByte);
        }
        return bytesToString(md.digest());
    }

    public static String bytesToString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
