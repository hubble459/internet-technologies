import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final int TIMEOUT = 5000;
    private long last;
    private boolean loggedIn = false;
    private PrintWriter writer;

    public static void main(String[] args) throws IOException {
        new Main().run();
    }

    public void run() throws IOException {
        socket = new Socket("86.87.206.20", 1337);
//        socket = new Socket("127.0.0.1", 1337);
        socket.setKeepAlive(true);
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();

        Thread t1 = new Thread(inputReader);
        t1.start();

        try {
            t1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        socket.close();
    }

    private final Runnable outputWriter = () -> {
        Scanner reader = new Scanner(System.in);
        writer = new PrintWriter(outputStream);

        while (!socket.isClosed()) {
            String command;

            if (!loggedIn) {
                System.out.print("Login: ");
                command = "CONN";
            } else {
                command = "BCST";
            }

            String payload = reader.nextLine();

            last = System.currentTimeMillis();

            if (loggedIn && payload.equals("-q")) {
                command = "QUIT";
                payload = "";
            }

            writer.println(command + " " + payload);
            writer.flush();

            if (command.equals("CONN")) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        writer.close();
        System.out.println("NO OUTPUT");
    };

    private final Runnable inputReader = () -> {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            while (!socket.isClosed()) {
                String line = reader.readLine();

                if (socket.isClosed() || line == null) break;

                if (line.equals("PING")) {
                    if (System.currentTimeMillis() - last < TIMEOUT) {
                        writer.println("PONG");
                        writer.flush();
                    } else {
                        System.out.println("Connection timed out");
                        socket.close();
                        System.exit(0);
                        return;
                    }
                } else {
                    System.out.println(line);
                }

                if (line.contains("200")) {
                    if (line.toLowerCase().contains("goodbye")) {
                        socket.close();
                        System.exit(0);
                        return;
                    }
                    loggedIn = true;
                }

                if (line.contains("INFO")) {
                    Thread t2 = new Thread(outputWriter);
                    t2.start();
                }
            }

            System.out.println("Connection CLOSED");
        } catch (IOException e) {
            e.printStackTrace();
        }
    };
}
