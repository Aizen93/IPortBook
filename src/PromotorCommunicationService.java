import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Oussama AOUESSAR, Mike AREZES et Chafik DALI.
 */
public class PromotorCommunicationService implements Runnable {
    private ServerService serv;

    PromotorCommunicationService(ServerService serv) {
        this.serv = serv;
    }

    @Override
    public void run() {
        System.out.println("Opening promotor port...");
        ServerSocket servSocketPromotor = null;
        if (serv != null) {
            try {
                servSocketPromotor = new ServerSocket(serv.getPromotorPort());
            } catch (IOException e) {
                System.out.println("Can't open server socket");
            }
        }

        Thread client;

        Socket connexion = null;
        ServerPromotorMessageService spms = null;
        System.out.println("Waiting for promotor connection...");
        while (true) {
            try {
                if (servSocketPromotor != null) {
                    /* Server accepts connections from promotor. */
                    /* The Accept method blocks until a connection occurs. */
                    connexion = servSocketPromotor.accept();
                }
                System.out.println("\nConnection started with a promotor !!!");

                if (serv != null) {
                    /* Create a single-threaded service. */
                    /* This will handle the promotor. */
                    spms = new ServerPromotorMessageService(connexion, serv.getUsersList());
                }
                client = new Thread(spms);
                client.start();
                System.out.println("Thread started for a promotor !!!");
            } catch (IOException e) {
                /* Handle potential exceptions. */
                /* If the creation of the single-threaded service fail, 
                communication with the promotor can not start, so the data socket is closed. */
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