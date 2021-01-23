import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;

public class NetCat {
    private static Socket socket;
    private static PrintWriter writer;

    public static void main(String[] args) throws IOException {
        createKeys();

        socket = new Socket("127.0.0.1", 1337);
//        socket = new Socket("86.87.206.20", 1338);
//        socket = new Socket("64.227.68.106", 1337);
        OutputStream out = socket.getOutputStream();
        writer = new PrintWriter(out);
//        writer.println("CONN owo" + (int) (Math.random() * 100));
//        writer.flush();

        new Thread(new inputReader()).start();

        while (!socket.isClosed()) {
            Scanner sc = new Scanner(System.in);
            writer.println(sc.nextLine());
            writer.flush();
        }
    }

    private static void createKeys() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.genKeyPair();
            String puk = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
            System.out.println(puk);

            // MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs4oHQj2tY8Aos2E3Kb7BKJZazVqRueKT6ZX7lpHBBpLiiQtOLEVJVOGcCp615zhEzZOUY/9P99L7bdIygvnBhOXC6NPLDIgL5kP1Lp8Fc/JQo3cF1RhWfF8IiJ+q2MlsS1xM/e62CQx8ZfieMn8A2SYuDbo8TEUVPNkjLdMnDj5ENA0CbaVc/57KpIgtbkBnnEpZQUjWU8GpW6r4LY506HS8KDAXnaaPSjmkT2qJFdjdj3dmjUlxxjNI795A0fC/TARIpJ1NV6Iwl22ktV0lkmVaEpiEGHNc4FU+EpaCdlV555DfhjbXkw4moaQySlptWn0R3z/b3/3U8gGtfctmNwIDAQAB
            // MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAh5IuTFOEDzX/lZhg4DdopWJehpzec7NXjr93AmHCQsjgpdFeSDy9Gj6IWFTcG4GODlrsrleQ3YPJoFPljuAMyOxIzqKEISeOYWUYNnoStK0PblKtI3T4iaPIoA7NckR0H7/gHdgyL7Moe06L3FETh+Z0ws6vBI6v+XSgAxytsMCRSFKqWytGMriRb3Si8HeVzAsxSTVNe9+HFsQ5EEkdtd0X0ggHOJb6ZUqISEK2SO30JIX/eQGYrf6AFxDSpLAgpSah8UBuxFOLAlSTiWxNpfCP7uGuvNfG8DRcnvImzdn5mt818nbM3YUTNiCp7fXjRMJ07QcIPdhbOMy6C8jAKQIDAQAB

            KeyPairGenerator kpg2 = KeyPairGenerator.getInstance("RSA");
            kpg2.initialize(2048);
            KeyPair kp2 = kpg2.genKeyPair();
            String puk2 = Base64.getEncoder().encodeToString(kp2.getPublic().getEncoded());
            System.out.println(puk2);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static class inputReader implements Runnable {
        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while (!socket.isClosed()) {
                    String line = reader.readLine();
                    if (line == null) break;

                    if (line.startsWith("PING")) {
                        writer.println("PONG");
                        writer.flush();
                    } else {
                        System.out.println(line);
                    }
                    if (line.startsWith("DCST")) {
                        break;
                    }
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
