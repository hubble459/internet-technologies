package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Checksum {
    private static byte[] createChecksum(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream dis = new DigestInputStream(new FileInputStream(file), md);
        while (dis.read() != -1);
        return md.digest();
    }

    // a byte array to a HEX string
    public static String getMD5Checksum(File file) throws Exception {
        byte[] bytes = createChecksum(file);
        return bytesToString(bytes);
    }

    public static String bytesToString(byte[] bytes) throws Exception {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
