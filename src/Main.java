import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Main {
    private Socket socket;

    public static void main(String[] args) throws IOException {
        new Main().run();
    }

    public void run() throws IOException {
        socket = new Socket("127.0.0.1", 1337);


        new Thread(inputReader).start();
//        new Thread(inputReader).start()

        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println("CONN test");
        writer.close();
    }

    private final Runnable inputReader = new Runnable() {
        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                while (!socket.isClosed()) {
                    System.out.println(reader.readLine());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}
