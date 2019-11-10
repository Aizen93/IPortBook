import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static java.lang.System.exit;

/**
 * @author Oussama AOUESSAR, Mike AREZES et Chafik DALI.
 */
public class Promotor {

    private static void promotionalMessage(InetAddress ia, int port, String promMess) {
        if (promMess.length() < 300) {
            StringBuilder promMessBuilder = new StringBuilder(promMess);
            for (int j = promMessBuilder.length(); j < 300; j++) {
                promMessBuilder.append("#");
            }
            promMess = promMessBuilder.toString();
        }
        String mess = "PROM " + promMess;
        byte[] data = mess.getBytes();
        DatagramPacket dp = new DatagramPacket(data, data.length, ia, port);

        try (MulticastSocket ms = new MulticastSocket()) {
            ms.joinGroup(ia);
            ms.send(dp);
            ms.leaveGroup(ia);
        } catch (IOException ex) {
            System.out.println("Failed to send the multicast message !");
        }
    }

    private static String askForAdvertising(Socket socket, InputStream is, OutputStream os, String dIp, int dPort, String promMess) {
        String mess = "PUBL? " + dIp + " " + (number_to_octet(dPort)) + " " + promMess + "+++";
        byte[] message = mess.getBytes();

        try {
            os.write(message);
        } catch (IOException e) {
            System.out.println("Couldn't write on the output stream.");
            closing(os, is, socket);
            exit(1);
        }

        byte[] messageIn = new byte[8];
        int i = 8;
        try {
            i = is.read(messageIn);
        } catch (IOException e) {
            System.out.println("Couldn't read on the input stream.");
            closing(os, is, socket);
            exit(1);
        }
        byte[] tmp = new byte[i];
        System.arraycopy(messageIn, 0, tmp, 0, i);
        String answer = new String(tmp);
        return answer;
    }

    private static Socket connectionToServer(String ip, int port) {
        Socket socket = null;
        try {
            socket = new Socket(ip, port);
        } catch (IOException ex) {
            System.out.println("The opening of the socket failed ! Please check the port or the ip of the host.");
            exit(1);
        }
        return socket;
    }

    private static void closing(OutputStream os, InputStream is, Socket socket) {
        try {
            os.close();
        } catch (IOException e) {
            System.out.println("Couldn't close the output stream.");
        }
        try {
            is.close();
        } catch (IOException e) {
            System.out.println("Couldn't close the intput stream.");
        }
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Couldn't close the tcp socket.");
        }
    }

    /**
     * A function that turns a number into String of 4 octet
     *
     * @param i integer to transform
     * @return a String with 4 octet format
     */
    private static String number_to_octet(int i) {
        String nm = "" + i;
        if (nm.length() == 1) nm = "000" + nm;
        if (nm.length() == 2) nm = "00" + nm;
        if (nm.length() == 3) nm = "0" + nm;
        return nm;
    }

    /**
     * A function that finds a free port for the client.
     *
     * @return The first free port found
     */
    private static int getFreePort() {
        int port;
        DatagramSocket server;

        for (port = 9999; port > 0; port--) {
            try {
                server = new DatagramSocket(port);
                server.close();
                break;
            } catch (IOException ex) {
            }
        }
        if (port == 10000) {
            return -1;
        }
        return port;
    }

    public static void main(String[] args) {
        InetAddress diffusionIp = null;
        int diffusionPort = 0;
        String tcpIp = args[0];
        int tcpPort = parseInt(args[1]);

        Socket connection = connectionToServer(tcpIp, tcpPort);
        InputStream is = null;
        try {
            is = connection.getInputStream();
        } catch (IOException e) {
            System.out.println("The opening of input stream failed !");
        }
        OutputStream os = null;
        try {
            os = connection.getOutputStream();
        } catch (IOException e) {
            System.out.println("The opening of output stream failed !");
        }

        Scanner sc = new Scanner(System.in);
        String mess, mess2, serverAnswer;
        boolean exit = true;

        System.out.println("Connection on: Ip: " + tcpIp + " / Port: " + tcpPort + "\n");

        StringBuilder multIP;
        System.out.println("Trying to connect on a multicast diffusion...");
        Pattern p;
        Matcher m;
        boolean b1;

        diffusionPort = getFreePort();
        // Regex for multicast 224.0.0.0 to 239.255.255.255
        p = Pattern.compile("2(?:2[4-9]|3\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d?|0)){3}");

        do {
            System.out.print("Enter a valid IP: ");
            multIP = new StringBuilder(sc.nextLine());
            m = p.matcher(multIP.toString());
            b1 = m.matches();
        } while (!b1);

        try {
            diffusionIp = InetAddress.getByName(multIP.toString());
        } catch (UnknownHostException e) {
            System.out.println("The multicast ip adddress is incorrect");
        }

        System.out.println("Multicast on: Ip: " + diffusionIp + " / Port: " + diffusionPort + "\n");

        if (multIP.length() < 15) {
            for (int i = multIP.length(); i < 15; i++)
                multIP.append("#");
        }

        System.out.println("********************************************");
        System.out.println("*     Welcome to the promotor service      *");
        System.out.println("* There is a list of commands you can use: *");
        System.out.println("*------------------------------------------*");
        System.out.println("*   To send a promotion request use PROM   *");
        System.out.println("********************************************");


        System.out.print("Enter the advertisement message[200 character max]: ");
        mess2 = sc.nextLine();
        while (mess2.length() > 200 || mess2.length() == 0) {
            System.out.print("Wrong length, enter the advertisement message[200 character max]: ");
            mess2 = sc.nextLine();
        }
        serverAnswer = askForAdvertising(connection, is, os, multIP.toString(), diffusionPort, mess2);
        if (serverAnswer.equals("PUBL>+++")) {
            System.out.println("The advertisement has been sent !");
        } else {
            System.out.println("The advertisement has failed !");
            closing(os, is, connection);
            exit(1);
        }

        while (true) {
            System.out.print("To send a promotion use [PROM]: ");
            mess2 = sc.nextLine();
            if (mess2.equals("PROM")) {
                System.out.print("Enter a message [300 character max]: ");
                mess2 = sc.nextLine();
                while (mess2.length() > 300 || mess2.length() == 0) {
                    System.out.print("Wrong length, enter a message[300 character max]: ");
                    mess2 = sc.nextLine();
                }
                promotionalMessage(diffusionIp, diffusionPort, mess2);
            } else {
                System.out.print("Please ");
            }

        }
    }
}
