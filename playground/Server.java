import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class Server {
    private int client_Port; // port inférieur à 9999
    private int promotor_Port; // port inférieur à 9999
    private User[] users_List = new User[100];
    private boolean verbose = false;

    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(4242);
            while (true) {
                Socket s = server.accept();
                InputStream is = s.getInputStream();
                OutputStream os = s.getOutputStream();
                byte[] bytes2 = new byte[100];
                is.read(bytes2);
                String mess = new String(bytes2);
                System.out.println("Message reçu: " + mess);
                mess = "Message pour dire coucou";
                byte[] bytes = mess.getBytes();
                System.out.println("Text : " + mess);
                System.out.println("Size Text : " + mess.length());
                System.out.println("Text [Byte Format] : " + Arrays.toString(bytes));
                System.out.println("Size Text [Byte Format] : " + bytes.length);
                os.write(bytes);
                is.close();
                os.close();
                s.close();
            }
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }
}
