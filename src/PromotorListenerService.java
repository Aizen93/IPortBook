import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import static java.lang.System.exit;

/**
 * @author Oussama AOUESSAR, Mike AREZES et Chafik DALI.
 */
public class PromotorListenerService implements Runnable {
    private MulticastSocket mso;
    private String Address;
    private int port;

    public PromotorListenerService(MulticastSocket mso, String Address, int port) {
        this.mso = mso;
        this.Address = Address;
        this.port = port;
    }

    @Override
    public void run() {
        String message;
        byte[] mess = new byte[306];
        try {
            mso.joinGroup(InetAddress.getByName(this.Address));
        } catch (IOException ex) {
            System.out.println("Problem with the join of the address " + this.Address + ":" + this.port);
            exit(1);
        }
        DatagramPacket paquet = new DatagramPacket(mess, mess.length);
        while (true) {
            try {
                mso.receive(paquet);
            } catch (IOException ex) {
                System.out.println("Problem in receiving");
                exit(1);
            }
            message = new String(paquet.getData(), 0, paquet.getLength());
            if (message.length() > 305) {
                System.out.println("Incorrect message received (message too long), closing connection with "
                        + this.Address + ":" + this.port);
                exit(1);
            } else {
                if ((message.substring(0, 5)).equals("PROM ")) {
                    int i;
                    for (i = 304; i > 4; i--) {
                        if (message.charAt(i) != '#') break;
                    }
                    System.out.println();
                    System.out.println("----------------------------------------------------------------------");
                    System.out.println("Message received from promotor: " + this.Address + ":" + this.port + " :");
                    System.out.println(message.substring(5, i + 1));
                    System.out.println("----------------------------------------------------------------------");
                    System.out.print("Continue what you were typing: ");
                } else {
                    System.out.println("Incorrect message received, closing connection with "
                            + this.Address + ":" + this.port);
                    exit(1);
                }
            }
        }
    }
}
