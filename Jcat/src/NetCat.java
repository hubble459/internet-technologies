import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

public class NetCat {
    private static Socket socket;
    private static PrintWriter writer;

    public static void main(String[] args) throws IOException, InterruptedException {
        socket = new Socket("127.0.0.1", 1337);
//       socket = new Socket("86.87.206.20", 1338);
        //        socket = new Socket("64.227.68.106", 1337);
        OutputStream out = socket.getOutputStream();
        writer = new PrintWriter(out);
        writer.println("CONN owo");

        new Thread(new inputReader()).start();

        Thread.sleep(1000);
        writer.println("DOWN 1_uwu.jpg");
        writer.flush();

        while (!socket.isClosed()) {
            Scanner sc = new Scanner(System.in);
            writer.println(sc.nextLine());
            writer.flush();
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

//                    else {
//                        File file = new File("uwu.jpg");
//                        long length = file.length();
//                        if (length > Integer.MAX_VALUE) {
//                            System.out.println("File is too large.");
//                            return;
//                        }
//
//                        byte[] array = Files.readAllBytes(file.toPath());
//                        String base64 = "FILE " + Base64.getEncoder().encodeToString(array);
//                        writer.println(base64);
//                        writer.flush();
//                    }

                    if (line.startsWith("PING")) {
                        writer.println("PONG");
                        writer.flush();
                    }
                    System.out.println(line);
                    if (line.startsWith("DCST")) {
                        socket.close();
                    }
                }

//                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
