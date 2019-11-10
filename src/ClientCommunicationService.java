import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Oussama AOUESSAR, Mike AREZES et Chafik DALI.
 */
public class ClientCommunicationService implements Runnable {
    private ServerService serv;

    ClientCommunicationService(ServerService serv) {
        this.serv = serv;
    }

    @Override
    public void run() {
        System.out.println("Opening client port...");
        ServerSocket servSocketClient = null;
        if (serv != null) {
            try {
                servSocketClient = new ServerSocket(serv.getClientPort());
            } catch (IOException e) {
                System.out.println("Can't open server socket");
            }
        }

        Thread client;

        Socket connexion = null;
        ServerMessageService sms = null;
        System.out.println("Waiting for client connection...");
        while (true) {
            try {
                if (servSocketClient != null) {
                    /* Server accepts connections from client. */
                    /* The Accept method blocks until a connection occurs. */
                    connexion = servSocketClient.accept();
                }
                System.out.println("\nConnection started with a client !!!");

                if (serv != null) {
                    /* Create a single-threaded service. */
                    /* This will handle the client. */
                    sms = new ServerMessageService(connexion, serv.getUsersList(),
                            serv.getRegisteredUsers());
                }
                client = new Thread(sms);
                client.start();
                System.out.println("Thread started for a client !!!");
            } catch (IOException e) {
                /* Handle potential exceptions. */
                /* If the creation of the single-threaded service fail, 
                communication with the client can not start, so the data socket is closed. */
                assert connexion != null;
                try {
                    connexion.close();
                } catch (IOException ex) {
                    System.err.println("Can't close connection");
                }
            }
        }
    }
}