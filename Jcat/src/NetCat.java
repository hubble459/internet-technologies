import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class NetCat {
    private static Socket socket;
    private static PrintWriter writer;

    public static void main(String[] args) throws IOException {
//        socket = new Socket("127.0.0.1", 1337);
//        socket = new Socket("86.87.206.20", 1337);
        socket = new Socket("64.227.68.106", 1337);
        writer = new PrintWriter(socket.getOutputStream());

        new Thread(new inputReader()).start();

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

                    System.out.println(line);
                    if (line.startsWith("DCST")) {
                        socket.close();
                    }
                }

                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
