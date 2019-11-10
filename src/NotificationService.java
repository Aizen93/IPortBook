import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * @author Oussama AOUESSAR, Mike AREZES et Chafik DALI.
 */
public class NotificationService implements Runnable {
    private DatagramSocket dso;

    NotificationService(DatagramSocket dso) {
        this.dso = dso;
    }

    @Override
    public void run() {
        byte[] notification = new byte[3];
        DatagramPacket paquet = new DatagramPacket(notification, notification.length);
        System.out.println("Notification service started...");
        while (true) {
            try {
                dso.receive(paquet);
            } catch (IOException ex) {
                System.out.println("Problem with the reception in the UDP port");
            }
            String notif = new String(paquet.getData(), 0, paquet.getLength());
            int nb;
            switch (notif.charAt(0)) {
                case '0':
                    System.out.println("\n----------------------------------------------------------------------");
                    System.out.println("New notification received:");
                    nb = ((notification[1] >= 0 ? notification[1] : 256 + notification[1])
                            + ((notification[2] >= 0 ? notification[2] : 256 + notification[2]) << 8));
                    if (nb > 1)
                        System.out.println("You have " + nb + " friend requests waiting to be read.");
                    else
                        System.out.println("You have 1 friend request to be read.");
                    System.out.println("----------------------------------------------------------------------");
                    System.out.print("Continue what you were typing: ");
                    break;
                case '1':
                    System.out.println("\n----------------------------------------------------------------------");
                    System.out.println("New notification received:");
                    nb = ((notification[1] >= 0 ? notification[1] : 256 + notification[1])
                            + ((notification[2] >= 0 ? notification[2] : 256 + notification[2]) << 8));
                    if (nb > 1)
                        System.out.println("You have " + nb + " friend requests accepted waiting to be read.");
                    else
                        System.out.println("You have 1 friend request accepted to be read.");
                    System.out.println("----------------------------------------------------------------------");
                    System.out.print("Continue what you were typing: ");
                    break;
                case '2':
                    System.out.println("\n----------------------------------------------------------------------");
                    System.out.println("New notification received:");
                    nb = ((notification[1] >= 0 ? notification[1] : 256 + notification[1])
                            + ((notification[2] >= 0 ? notification[2] : 256 + notification[2]) << 8));
                    if (nb > 1)
                        System.out.println("You have " + nb + " friend requests refused waiting to be read.");
                    else
                        System.out.println("You have 1 friend request refused to be read.");
                    System.out.println("----------------------------------------------------------------------");
                    System.out.print("Continue what you were typing: ");
                    break;
                case '3':
                    System.out.println("\n----------------------------------------------------------------------");
                    System.out.println("New notification received:");
                    nb = ((notification[1] >= 0 ? notification[1] : 256 + notification[1])
                            + ((notification[2] >= 0 ? notification[2] : 256 + notification[2]) << 8));
                    if (nb > 1)
                        System.out.println("You have " + nb + " messages waiting to be read.");
                    else
                        System.out.println("You have 1 message waiting to be read.");
                    System.out.println("----------------------------------------------------------------------");
                    System.out.print("Continue what you were typing: ");
                    break;
                case '4':
                    System.out.println("\n----------------------------------------------------------------------");
                    System.out.println("New notification received:");
                    nb = ((notification[1] >= 0 ? notification[1] : 256 + notification[1])
                            + ((notification[2] >= 0 ? notification[2] : 256 + notification[2]) << 8));
                    if (nb > 1)
                        System.out.println("You have " + nb + " flood messages waiting to be read.");
                    else
                        System.out.println("You have 1 flood message waiting to be read.");
                    System.out.println("----------------------------------------------------------------------");
                    System.out.print("Continue what you were typing: ");
                    break;
                case '5':
                    System.out.println("\n----------------------------------------------------------------------");
                    System.out.println("New notification received:");
                    nb = ((notification[1] >= 0 ? notification[1] : 256 + notification[1])
                            + ((notification[2] >= 0 ? notification[2] : 256 + notification[2]) << 8));
                    if (nb > 1)
                        System.out.println("You have " + nb + " promotor messages waiting to be read.");
                    else
                        System.out.println("You have 1 promotor message waiting to be read.");
                    System.out.println("----------------------------------------------------------------------");
                    System.out.print("Continue what you were typing: ");
                    break;
                default:
                    System.out.println("\n----------------------------------------------------------------------");
                    System.out.println("Bad notification received");
                    System.out.println("----------------------------------------------------------------------");
                    System.out.print("Continue what you were typing: ");
                    break;
            }
        }
    }
}
