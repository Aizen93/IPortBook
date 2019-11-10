import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Oussama AOUESSAR, Mike AREZES et Chafik DALI.
 */
public class ServerPromotorMessageService implements Runnable {
    private Socket tcpSocket;
    private User[] users_List;

    /**
     * Constructor that create the thread-object ServerPromotorMessageService
     *
     * @param tcpSocket  A Socket that is connected in TCP
     * @param users_List An array of object User
     */
    ServerPromotorMessageService(Socket tcpSocket, User[] users_List) {
        this.tcpSocket = tcpSocket;
        this.users_List = users_List;
    }

    /**
     * Function that just send the response in the OutputStream os
     *
     * @param os       The OutputStream where is connected the Socket
     * @param response The String where is the response to be sent
     */
    private void sendResponse(OutputStream os, String response) {
        try {
            os.write(response.getBytes());
        } catch (IOException e) {
            if (ServerService.getVerbose()) System.out.println("The message " + response + " couldn't be sent");
        }
    }

    /**
     * Function that send notification by connecting once in UDP
     *
     * @param UDPAddress The string that represent the address that go with the port
     * @param UDPport    The int that represent the port that go with the address
     * @param nb         The int that represent the number of some type of message that can be consulted
     */
    private void sendNotification(String UDPAddress, int UDPport, int nb) {
        DatagramSocket dso;
        byte[] notif = new byte[3];
        try {
            dso = new DatagramSocket();
            notif[0] = ("5".getBytes())[0];
            byte[] intToByte = intTo2bytes(nb);
            System.arraycopy(intToByte, 0, notif, 1, 2); // copy the message id in id
            InetSocketAddress ia = new InetSocketAddress(UDPAddress, UDPport);
            DatagramPacket paquet = new DatagramPacket(notif, notif.length, ia);
            dso.send(paquet);
            if (ServerService.getVerbose())
                System.out.println("Notification \"new promotor message\" sent");
            dso.close();
        } catch (IOException e) {
            if (ServerService.getVerbose())
                System.out.println("The notification \"new promotor message\" couldn't be sent");
        }
    }

    /**
     * Function that return in a 2 bytes array of an int passed in arguments
     *
     * @param n The number of notification that can be consulted
     * @return a byte array of size 2
     */
    private byte[] intTo2bytes(int n) {
        byte[] b = new byte[2];
        b[0] = (byte) (n % 256);
        b[1] = (byte) ((n / 256) % 256);
        return b;
    }

    /**
     * Function that read in the OutputStream and verify if it isn't to long or end with +++
     *
     * @param is   The InputStream wher we receive the message
     * @param mess The byte[] where we write the message
     * @return True if it end with +++ and is smaller than 214, False if not
     */
    private boolean readMessage(InputStream is, byte[] mess) {
        int pos = 0;
        int nbplus = 0;
        while (nbplus != 3 && pos <= 230) {
            try {
                mess[pos] = (byte) is.read();
                if (mess[pos] != 43 && nbplus != 0) nbplus = 0;
                else if (mess[pos] == 43) nbplus++;
                pos++;
            } catch (IOException ignored) {

            }
        }
        if (nbplus == 3 && pos >= 8) {
            if (pos < 214)
                Arrays.fill(mess, pos, mess.length, (byte) 0);
            return true;
        } else if (mess[0] == -1 || mess[pos - 1] == -1) {
            if (ServerService.getVerbose())
                System.out.println("The promotor has closed the connection without warning");
            return false;
        } else if (pos < 8) {
            if (ServerService.getVerbose()) {
                System.out.println("The message is too short or doesn't end with +++");
                System.out.println("We close the connection with this promotor");
            }
            return false;
        } else {
            if (ServerService.getVerbose()) {
                System.out.println("The message is too long or doesn't end with +++");
                System.out.println("We close the connection with this promotor");
            }
            return false;
        }
    }

    /**
     * Function that return the 5 first bytes in string form
     *
     * @param message A byte[] with the message
     * @return The String that correspond to the type of message
     */
    private String getMessageType(byte[] message) {
        byte[] type = new byte[5];
        System.arraycopy(message, 0, type, 0, 5);
        return new String(type);
    }

    /**
     * Function that take a byte array that is not completely filled and put it inside a new
     * byte array with a size adapted
     *
     * @param originalMessage The original byte[] where the message is
     * @return The new byte[] with an adapted size
     */
    private byte[] getAdaptedMessageArray(byte[] originalMessage) {
        int nbplus = 0;
        int len;

        for (len = 0; len < originalMessage.length; len++) {
            if (originalMessage[len] == -1) break;
            if (originalMessage[len] != 43 && nbplus != 0) nbplus = 0;
            else if (originalMessage[len] == 43) nbplus++;
            if (nbplus == 3) {
                len++;
                break;
            }
        }

        byte[] newByteArray = new byte[len];
        System.arraycopy(originalMessage, 0, newByteArray, 0, len);
        return newByteArray;
    }

    /**
     * Function that verify if the PUBL? message respect the protocol
     *
     * @param mess The message received that start with PUBL?
     * @return Return true if the message is correct, if not it return false
     */
    private boolean advertisement(byte[] mess) {
        byte[] ip_diff;
        byte[] port = new byte[4];
        byte[] byteMess;
        if (ServerService.getVerbose()) {
            System.out.println("Message length [Byte Array]: " + mess.length);
            System.out.println("Text [Byte Format]: " + Arrays.toString(mess));
        }

        // Check if the message length is between 31 and 230 included
        if (mess.length < 31 && mess.length > 230) {
            if (ServerService.getVerbose()) System.out.println("Bad length of the PUBL? message received");
            return false;
        }
        // Check if the spaces are at the right place
        if (mess[5] != 32 || mess[21] != 32 || mess[26] != 32) {
            if (ServerService.getVerbose()) System.out.println("Bad space in between the PUBL? message received");
            return false;
        }
        int pos = 27;
        int nbplus = 0;
        while (nbplus != 3) {
            if (mess[pos] != 43 && nbplus != 0) nbplus = 0;
            else if (mess[pos] == 43) nbplus++;
            pos++;
        }
        byteMess = new byte[pos - 30];

        int i = 20;
        while (mess[i] == 35) {
            i--;
        }
        ip_diff = new byte[i - 5];

        System.arraycopy(mess, 6, ip_diff, 0, ip_diff.length); // copy the message ip_diff in ip_diff
        System.arraycopy(mess, 22, port, 0, 4); // copy the message port in port
        System.arraycopy(mess, 27, byteMess, 0, byteMess.length); // copy the message mess in byteMess

        int portInt = 0;
        try {
            portInt = Integer.parseInt(new String(port));
        } catch (NumberFormatException e) {
            if (ServerService.getVerbose()) System.out.println("The port given by the promotor isn't a integer");
            return false;
        }
        if (portInt < 1) {
            if (ServerService.getVerbose()) System.out.println("The port is inferior or equals to 0");
            return false;
        }

        // We verify if the ip is correctly formed
        Pattern p = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        Matcher m = p.matcher(new String(ip_diff));

        if (!m.matches()) {
            if (ServerService.getVerbose()) System.out.println("The ip-diff received from promotor isn't correct");
            return false;
        }
        return true;
    }

    /**
     * Function that return the global Socket
     *
     * @return Return the global Socket
     */
    private Socket getSocket() {
        return this.tcpSocket;
    }

    @Override
    public void run() {
        if (ServerService.getVerbose()) System.out.println("I'm in a promotor thread");
        InputStream is = null;
        try {
            is = getSocket().getInputStream();
        } catch (IOException e) {
            System.out.println("The opening of the promotor input stream failed !");
        }
        OutputStream os = null;
        try {
            os = getSocket().getOutputStream();
        } catch (IOException e) {
            System.out.println("The opening of the promotor output stream failed !");
        }
        byte[] messReceived = new byte[250];
        if (ServerService.getVerbose()) {
            System.out.println("--------------------------------------------");
            System.out.println("Waiting promotor message...\n");
        }
        if (readMessage(is, messReceived)) {
            if (ServerService.getVerbose()) System.out.println("Message received: " + new String(messReceived));
            System.out.println("Text [Byte Format]: " + Arrays.toString(messReceived));

            if (ServerService.getVerbose()) {
                System.out.println("--------------------------------------------");
                System.out.println("Processing promotor message...\n");
            }

            switch (getMessageType(messReceived)) {
                case "PUBL?":
                    if (ServerService.getVerbose())
                        System.out.println("A promotor is sending a message PUBL?");
                    if (!advertisement(getAdaptedMessageArray(messReceived))) {
                        if (ServerService.getVerbose())
                            System.out.println("A promotor couldn't send a PUBL? message");
                    }
                    Flux flux;
                    synchronized (users_List) {
                        for (User u : users_List) {
                            if (u == null) break;
                            flux = u.getFlux();
                            flux.getArrayList().add(getAdaptedMessageArray(messReceived));
                            flux.incrementNbPromotorMessage();
                            sendNotification(u.getUdp_Address(), u.getUdp_Port(), flux.getNbPromotorMessage());
                        }
                        System.out.println("Notifications for promotor finished for every users");
                    }
                    sendResponse(os, "PUBL>+++");
                    if (ServerService.getVerbose())
                        System.out.println("The promotor message has correctly been sent");
                    break;
                default:
                    if (ServerService.getVerbose())
                        System.out.println("The type of message received from promotor isn't correct");
                    break;
            }
        }
        try {
            getSocket().close();
        } catch (IOException e) {
            System.out.println("Couldn't close the tcp socket.");
        }
        if (ServerService.getVerbose()) {
            System.out.println("The promotor thread has stopped\n");
        }
    }
}
