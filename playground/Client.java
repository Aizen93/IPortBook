
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        String mess = "";
        byte[] message = new byte[1024];
        byte[] message2 = new byte[1024];
        try {
            Socket socket = new Socket("localhost", Integer.parseInt(args[0]));

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            Scanner sc = new Scanner(System.in);

            while (true) {
                System.out.println("Entrez un message");
                mess = sc.nextLine();
                message = mess.getBytes();
                os.write(message);
                String mess2 = mess.substring(0, 5);
                int bytesRead;
                switch (mess2) {
                    case "REGIS":
                        bytesRead = is.read(message);
                        Arrays.fill(message, bytesRead, message.length, (byte) 0);
                        System.out.println("Message reçu: " + new String(message));
                        break;
                    case "CONNE":
                        bytesRead = is.read(message);
                        Arrays.fill(message, bytesRead, message.length, (byte) 0);
                        System.out.println("Message reçu: " + new String(message));
                        break;
                    case "CONSU":
                        bytesRead = is.read(message2);
                        Arrays.fill(message2, bytesRead, message2.length, (byte) 0);
                        System.out.println("Message reçu: " + new String(message2));
                        break;
                    case "IQUIT":
                        bytesRead = is.read(message);
                        Arrays.fill(message, bytesRead, message.length, (byte) 0);
                        System.out.println("Message reçu: " + new String(message));
                        break;
                    case "FRIE?":
                        bytesRead = is.read(message2);
                        Arrays.fill(message2, bytesRead, message2.length, (byte) 0);
                        System.out.println("Message reçu: " + new String(message2));
                        break;
                    case "MESS?":
                        break;
                    case "MENUM":
                        bytesRead = is.read(message);
                        Arrays.fill(message, bytesRead, message.length, (byte) 0);
                        System.out.println("Message reçu: " + new String(message));
                        break;
                    case "FLOO?":
                        bytesRead = is.read(message2);
                        Arrays.fill(message2, bytesRead, message2.length, (byte) 0);
                        System.out.println("Message reçu: " + new String(message2));
                        break;
                    case "LIST?":
                        bytesRead = is.read(message2);
                        Arrays.fill(message2, bytesRead, message2.length, (byte) 0);
                        System.out.println("Message reçu: " + new String(message2));
                        
                        bytesRead = is.read(message2);
                        Arrays.fill(message2, bytesRead, message2.length, (byte) 0);
                        System.out.println("Message reçu: " + new String(message2));
                        break;
                    default:
                        bytesRead = is.read(message2);
                        Arrays.fill(message2, bytesRead, message2.length, (byte) 0);
                        System.out.println("Message reçu: " + new String(message2));
                        break;

                }
            }
        } catch (IOException e) {
            System.out.println("Il n'y a pas ou plus de réponse du Serveur, on déconnecte\nBye...");
        }
    }
}
