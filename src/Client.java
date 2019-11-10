import java.io.*;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.exit;

public class Client {

    /**
     * A function that finds a free port for the client.
     *
     * @return The first free port found
     */
    private static int getFreePort() {
        int port;
        DatagramSocket server;

        for (port = 1; port <= 9999; port++) {
            try {
                server = new DatagramSocket(port);
                server.close();
                break;
            } catch (IOException ex) {
                // System.out.println("The following port is not free " + port + ".");
            }
        }
        if (port == 10000) {
            return -1;
        }
        return port;
    }

    /**
     * A function that handles the reception of the username and its validty.
     *
     * @return The valid username as a string.
     */
    private static String username() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter a username of 8 alphanumerical characters: ");
        String pseudo = sc.nextLine();
        Pattern p = Pattern.compile("[a-zA-Z_0-9]*");
        Matcher m = p.matcher(pseudo);
        boolean b1 = m.matches();
        while (pseudo.length() != 8 || !b1) {
            System.out.print("Enter a username of 8 alphanumerical characters: ");
            pseudo = sc.nextLine();
            m = p.matcher(pseudo);
            b1 = m.matches();
        }
        return pseudo;
    }


    /**
     * A function that handles the reception of the password and its validty.
     *
     * @return The valid password as a byte array.
     */
    private static byte[] password() {
        String password;
        boolean is_numeric = false;
        Scanner sc = new Scanner(System.in);
        int pwd = 0;
        Pattern p;
        Matcher m;
        boolean b1;
        do {
            System.out.print("Enter a password between 0 and 65535: ");
            password = sc.nextLine();
            p = Pattern.compile("[0-9]*");
            m = p.matcher(password);
            b1 = m.matches();
            if (b1) is_numeric = true;
            while (is_numeric) {
                try {
                    pwd = Integer.parseInt(password);
                } catch (NumberFormatException e) {
                    b1 = false;
                    is_numeric = false;
                    break;
                }
                if ((pwd < 0) || (pwd > 65535)) {
                    b1 = false;
                    is_numeric = false;
                    break;
                }
                break;
            }
        } while (!b1);


        byte[] b = new byte[2];
        b[0] = (byte) (pwd % 256);
        b[1] = (byte) ((pwd / 256) % 256);
        return b;
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
     * A function that tests if a String is alphanumerical and has exactly 8 character
     *
     * @param a the String to test
     * @return boolean
     */
    private static boolean string_is_octet(String a) {
        Pattern p = Pattern.compile("[a-zA-Z_0-9]*");
        Matcher m = p.matcher(a);
        boolean b1 = m.matches();
        return !(a.length() != 8 || !b1);
    }

    /**
     * A function that closes the Socket, InputStream and the Outputstream
     *
     * @param os     the output stream to close
     * @param is     the input stream to close
     * @param socket the socket to close
     */
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
     * Function that take a byte array that is not completely filled and put it inside a new
     * byte array with a size adapted
     *
     * @param originalMessage The original byte[] where the message is
     * @return The new byte[] with an adapted size
     */
    private static byte[] getAdaptedMessageArray(byte[] originalMessage) {
        int nbplus = 0;
        int len;

        for (len = 0; len < originalMessage.length; len++) {
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
     * Function that read in the OutputStream and verify if it isn't too long or end with +++
     *
     * @param is   The InputStream wher we receive the message
     * @param mess The byte[] where we write the message
     * @return True if it end with +++ and is smaller than 214, False if not
     */
    private static boolean readMessage(InputStream is, byte[] mess) {
        int pos = 0;
        int nbplus = 0;
        while (nbplus != 3 && pos <= 230) {
            try {
                mess[pos] = (byte) is.read();
                if (mess[pos] != 43 && nbplus != 0) nbplus = 0;
                else if (mess[pos] == 43) nbplus++;
                pos++;
            } catch (IOException ignored) {
                System.out.println("Couldn't read anything from the server");
                return false;
            }
        }
        if (nbplus == 3 && pos >= 8) {
            if (pos < 230)
                Arrays.fill(mess, pos, mess.length, (byte) 0);
            return true;
        } else if (mess[0] == -1 || mess[pos - 1] == -1) {
            System.out.println("The server has closed the connection without warning");
            return false;
        } else if (pos < 8) {
            System.out.println("The message is too short or doesn't end with +++");
            System.out.println("Request aborted, BYE...");

            return false;
        } else {
            System.out.println("The message is too long or doesn't end with +++");
            System.out.println("Request aborted, BYE...");

            return false;
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("2 arguments needed");
            exit(1);
        }
        Pattern p = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        Matcher match = p.matcher(args[0]);
        if (!match.matches()) {
            System.out.println("Incorrect IP address format");
            exit(1);
        }

        String myIp = args[0];
        int myPort = 0;
        p = Pattern.compile("[0-9]*");
        match = p.matcher(args[1]);
        if (match.matches()) {
            myPort = Integer.parseInt(args[1]);
            if (myPort < 1 || myPort > 9999) {
                System.out.println("Incorrect port format");
                exit(1);
            }
        }
        String mess, mess2, serverAnswer, recipient, sender, file, filename;
        ArrayList<String> subscribed_promotor = new ArrayList<>();
        ArrayList<String> friend_request = new ArrayList<>();
        ArrayList<String> friend_list = new ArrayList<>();
        int num_mess = 0;
        boolean exit = true;
        byte[] message = new byte[230];
        Socket socket = null;
        try {
            socket = new Socket(myIp, myPort);
        } catch (IOException ex) {
            System.out.println("The opening of the socket failed ! Please check the port or the ip of the host.");
            exit(1);
        }
        InputStream is = null;
        try {
            is = socket.getInputStream();
        } catch (IOException e) {
            System.out.println("The opening of input stream failed !");
        }
        OutputStream os = null;
        try {
            os = socket.getOutputStream();
        } catch (IOException e) {
            System.out.println("The opening of output stream failed !");
        }

        Scanner sc = new Scanner(System.in);
        System.out.println("Connection on: Ip: " + myIp + " / Port: " + myPort + "\n");

        String username = username();
        byte[] password = password();
        int UDPport = getFreePort();
        if (UDPport == -1) {
            System.out.println("No UDP port found, sorry bye...");
            closing(os, is, socket);
            exit(1);
        }
        try {
            NotificationService ns = new NotificationService(new DatagramSocket(UDPport));
            Thread notification = new Thread(ns);
            notification.start();
        } catch (SocketException e) {
            System.out.println("The opening of the datagram socket failed ! Please check the port of the host.");
            closing(os, is, socket);
            exit(1);
        }

        System.out.println("UDP Port assigned: " + UDPport + "\n");

        mess2 = "REGIS " + username + " " + number_to_octet(UDPport) + " ";
        byte[] message2 = new byte[25];
        System.arraycopy(mess2.getBytes(), 0, message2, 0, 20);
        message2[20] = password[0];
        message2[21] = password[1];
        message2[22] = (byte) 43;
        message2[23] = (byte) 43;
        message2[24] = (byte) 43;

        try {
            assert os != null;
            os.write(message2);
        } catch (IOException e) {
            System.out.println("Couldn't write on the output stream.");
        }
        if (!readMessage(is, message)) {
            System.out.println("Error: Bad server response, connection aborted...BYE");
            closing(os, is, socket);
            exit(1);
        }
        serverAnswer = new String(getAdaptedMessageArray(message));

        if (serverAnswer.equals("GOBYE+++")) {
            System.out.println("Connection closed, registration aborted!\n");
            closing(os, is, socket);
            exit(1);
        } else if (serverAnswer.equals("WELCO+++")) {

            System.out.println("**********************************************");
            System.out.println("* There is a list of commands you can use:   *");
            System.out.println("*--------------------------------------------*");
            System.out.println("* To send a message: MESS                    *");
            System.out.println("* To check your notifications: CONSU         *");
            System.out.println("* To send a friend request: FRIE             *");
            System.out.println("* To request the users list: LIST            *");
            System.out.println("* To flood: FLOO                             *");
            System.out.println("* To connect: CONNE                          *");
            System.out.println("* To disconnect: IQUIT                       *");
            System.out.println("**********************************************");

            while (exit) {
                System.out.print("Enter a command: ");
                mess = sc.nextLine();
                switch (mess) {
                    case "MESS":
                        System.out.print("Recipier: ");
                        recipient = sc.nextLine();
                        while (!string_is_octet(recipient)) {
                            System.out.print("Please enter a registered recipier: ");
                            recipient = sc.nextLine();
                        }
                        System.out.print("Enter your message: ");
                        mess = sc.nextLine();
                        num_mess = (int) Math.ceil(((double) mess.length()) / 200.0);
                        String nm = number_to_octet(num_mess);
                        mess2 = "MESS? " + recipient + " " + nm + "+++";
                        try {
                            os.write(mess2.getBytes());
                        } catch (IOException e) {
                            System.out.println("Couldn't write on the output stream.");
                            closing(os, is, socket);
                            exit(1);
                        }
                        //decoupage du message
                        if (num_mess > 1) {
                            int j = 200;
                            for (int i = 0; i < num_mess; i++) {
                                nm = number_to_octet(i);

                                mess2 = "MENUM " + nm + " " + (mess.substring(j - 200, j)) + "+++";
                                try {
                                    os.write(mess2.getBytes());
                                } catch (IOException e) {
                                    System.out.println("Couldn't write on the output stream.");
                                    closing(os, is, socket);
                                    exit(1);
                                }
                                if ((mess.length()) - j < 200 && (mess.length()) - j > 0) {
                                    nm = number_to_octet(i + 1);
                                    mess2 = "MENUM " + nm + " " + (mess.substring(j, mess.length())) + "+++";
                                    try {
                                        os.write(mess2.getBytes());
                                    } catch (IOException e) {
                                        System.out.println("Couldn't write on the output stream.");
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                    break;
                                } else {
                                    j += 200;
                                }
                            }
                        } else {
                            mess2 = "MENUM 0000 " + mess + "+++";
                            try {
                                os.write(mess2.getBytes());
                            } catch (IOException e) {
                                System.out.println("Couldn't write on the output stream.");
                                closing(os, is, socket);
                                exit(1);
                            }
                        }
                        if (!readMessage(is, message)) {
                            closing(os, is, socket);
                            exit = false;
                            break;
                        }
                        serverAnswer = new String(getAdaptedMessageArray(message));
                        if (serverAnswer.equals("MESS>+++")) {
                            System.out.println("Message sent to [" + recipient + "] !");
                        } else if (serverAnswer.equals("MESS<+++")) {
                            System.out.println("Message not transmitted");
                        } else {
                            System.out.println("Bad server response, format MESS> or MESS<");
                            closing(os, is, socket);
                            exit = false;
                            break;
                        }
                        break;

                    case "IQUIT":
                        mess2 = "IQUIT+++";
                        try {
                            os.write(mess2.getBytes());
                        } catch (IOException e) {
                            System.out.println("Couldn't write on the output stream.");
                            closing(os, is, socket);
                            exit(1);
                        }
                        if (!readMessage(is, message)) {
                            closing(os, is, socket);
                            exit(1);
                        }
                        serverAnswer = new String(getAdaptedMessageArray(message));
                        if (serverAnswer.equals("GOBYE+++")) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                System.out.println("Couldn't close the tcp socket.");
                                exit(1);
                            }
                            System.out.println("################");
                            System.out.println("# Disconnected #");
                            System.out.println("################");
                            System.out.println("Now you can only see notifications but can't interact with the server");
                            while (!mess.equals("CONNE")) {
                                System.out.print("To login use [CONNE]: ");
                                mess = sc.nextLine();
                                if (mess.equals("CONNE")) {
                                    username = username();
                                    password = password();
                                    try {
                                        socket = new Socket(myIp, myPort);
                                    } catch (IOException e) {
                                        System.out.println("The opening of the socket failed ! Please check the port or the ip of the host.");
                                        exit(1);
                                    }
                                    try {
                                        is = socket.getInputStream();
                                    } catch (IOException e) {
                                        System.out.println("The opening of input stream failed !");
                                        exit(1);
                                    }
                                    try {
                                        os = socket.getOutputStream();
                                    } catch (IOException e) {
                                        System.out.println("The opening of output stream failed !");
                                        exit(1);
                                    }

                                    mess2 = "CONNE " + username + " ";
                                    message2 = new byte[20];
                                    System.arraycopy(mess2.getBytes(), 0, message2, 0, 15);
                                    message2[15] = password[0];
                                    message2[16] = password[1];
                                    message2[17] = (byte) 43;
                                    message2[18] = (byte) 43;
                                    message2[19] = (byte) 43;

                                    try {
                                        os.write(message2);
                                    } catch (IOException e) {
                                        System.out.println("Couldn't write on the output stream.");
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                    if (!readMessage(is, message)) {
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                    serverAnswer = new String(getAdaptedMessageArray(message));
                                    if (serverAnswer.equals("HELLO+++")) {
                                        System.out.println("############################");
                                        System.out.println("# Connection successfull ! #");
                                        System.out.println("############################");

                                    } else if (serverAnswer.equals("GOBYE+++")) {
                                        System.out.println("Connection aborted, BYE...!");
                                        closing(os, is, socket);
                                        exit(1);

                                    } else {
                                        System.out.println("Bad server response, login aborted");
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                }
                            }
                        } else {
                            System.out.println("ERROR: Bad server response, couldn't disconnect");
                            closing(os, is, socket);
                            exit(1);
                        }
                        break;

                    case "CONSU":
                        mess2 = "CONSU+++";
                        try {
                            os.write(mess2.getBytes());
                        } catch (IOException e) {
                            System.out.println("Couldn't write on the output stream.");
                            closing(os, is, socket);
                            exit(1);
                        }
                        if (!readMessage(is, message)) {
                            closing(os, is, socket);
                            exit = false;
                            break;
                        }
                        serverAnswer = new String(getAdaptedMessageArray(message));
                        switch (serverAnswer.substring(0, 5)) {
                            case "SSEM>":
                                mess = serverAnswer.substring(19, serverAnswer.length());
                                if (mess.equals("+++")) {
                                    sender = serverAnswer.substring(6, 14);
                                    System.out.print("[" + sender + "] said: ");
                                    try {
                                        num_mess = Integer.parseInt(serverAnswer.substring(16, serverAnswer.length() - 3));
                                    } catch (NumberFormatException e) {
                                        System.out.println("Bad message server, SSEM> incoming type format (not an int)");
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                    if (num_mess < 1) {
                                        System.out.println("Bad message server, SSEM> incoming type format (<1)");
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                    int num_mess2 = 0;
                                    for (int k = 0; k < num_mess; k++) {
                                        if (!readMessage(is, message)) {
                                            closing(os, is, socket);
                                            exit(1);
                                        }
                                        mess = new String(getAdaptedMessageArray(message));
                                        mess2 = mess.substring(0, 5);
                                        if (mess2.equals("MUNEM") && (mess.substring(mess.length() - 3, mess.length())).equals("+++")) {
                                            try {
                                                num_mess2 = Integer.parseInt(mess.substring(6, 10));
                                            } catch (NumberFormatException e) {
                                                closing(os, is, socket);
                                                exit(1);
                                            }
                                            if (num_mess2 == k) {
                                                mess = mess.substring(11, mess.length() - 3);
                                                System.out.print(mess);
                                            } else {
                                                System.out.println("Bad server response, MUNEM type format (messy order)");
                                                closing(os, is, socket);
                                                exit(1);
                                            }
                                        } else {
                                            System.out.println("Warning: Bad server response: MUNEM type from SSEM> type format");
                                            closing(os, is, socket);
                                            exit(1);
                                        }
                                    }
                                    System.out.println();

                                } else {
                                    System.out.print("Warning: Bad server response, SSEM> type format");
                                    closing(os, is, socket);
                                    exit(1);
                                }
                                break;

                            case "OOLF>":
                                mess = serverAnswer.substring(serverAnswer.length() - 3, serverAnswer.length());
                                if (mess.equals("+++")) {
                                    sender = serverAnswer.substring(6, 14);
                                    System.out.println("Flood message in coming from [" + sender + "]...");
                                    mess = serverAnswer.substring(15, serverAnswer.length() - 3);
                                    System.out.println("############# Flood message #############");
                                    System.out.println(mess);
                                    System.out.println("#########################################");
                                } else {
                                    System.out.println("Warning: Bad server response, OOLF> type format");
                                    closing(os, is, socket);
                                    exit(1);
                                }
                                break;

                            case "EIRF>":
                                mess = serverAnswer.substring(14, serverAnswer.length());
                                if (mess.equals("+++")) {
                                    sender = serverAnswer.substring(6, 14);
                                    if (friend_list.contains(sender)) {
                                        System.out.println("You are connected to a bad server, connection aborted...");
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                    System.out.println("[" + sender + "] sent you a friend request");
                                    while (!(mess2.equals("YES") || mess2.equals("Y")) && !(mess2.equals("NO") || mess2.equals("N"))) {
                                        System.out.print("Would you like to accept [YES(Y)/NO(N)]: ");
                                        mess2 = sc.nextLine();
                                    }
                                    if (mess2.equals("YES") || mess2.equals("Y")) {
                                        friend_list.add(sender);
                                        mess2 = "OKIRF+++";
                                        try {
                                            os.write(mess2.getBytes());
                                        } catch (IOException e) {
                                            System.out.println("Couldn't write on the output stream.");
                                            closing(os, is, socket);
                                            exit(1);
                                        }
                                        if (!readMessage(is, message)) {
                                            closing(os, is, socket);
                                            exit(1);
                                        }
                                        serverAnswer = new String(getAdaptedMessageArray(message));
                                        if (serverAnswer.equals("ACKRF+++")) {
                                            System.out.println("Your answer has been sent to your new friend [" + sender + "] !");
                                        } else {
                                            System.out.println("Bad server response, ACKRF type format");
                                            closing(os, is, socket);
                                            exit(1);
                                        }
                                    } else if (mess2.equals("NO") || mess2.equals("N")) {
                                        mess2 = "NOKRF+++";
                                        try {
                                            os.write(mess2.getBytes());
                                        } catch (IOException e) {
                                            System.out.println("Couldn't write on the output stream.");
                                            closing(os, is, socket);
                                            exit(1);
                                        }
                                        if (!readMessage(is, message)) {
                                            closing(os, is, socket);
                                            exit(1);
                                        }
                                        serverAnswer = new String(getAdaptedMessageArray(message));
                                        if (serverAnswer.equals("ACKRF+++")) {
                                            System.out.println("Your answer has been sent to [" + sender + "] !");
                                        } else {
                                            System.out.println("Bad server response, ACKRF type format (refuse)");
                                            closing(os, is, socket);
                                            exit(1);
                                        }
                                    }
                                } else {
                                    System.out.println("Warning: Bad server response, EIRF> type format");
                                    closing(os, is, socket);
                                    exit(1);
                                }
                                break;

                            case "FRIEN":
                                mess = serverAnswer.substring(14, serverAnswer.length());
                                sender = serverAnswer.substring(6, 14);
                                if (mess.equals("+++")) {
                                    if (!friend_request.contains(sender)) {
                                        System.out.println("You are connected to a bad server, connection aborted...");
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                    friend_request.remove(sender);
                                    friend_list.add(sender);
                                    System.out.println("[" + sender + "] has accepted your friendship request !");
                                } else {
                                    System.out.println("Warning: Bad server response, FRIEN type format");
                                    closing(os, is, socket);
                                    exit(1);
                                }
                                break;

                            case "NOFRI":
                                mess = serverAnswer.substring(14, serverAnswer.length());
                                sender = serverAnswer.substring(6, 14);
                                if (mess.equals("+++")) {
                                    if (!friend_request.contains(sender)) {
                                        System.out.println("You are connected to a bad server, connection aborted...");
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                    friend_request.remove(sender);
                                    System.out.println("[" + sender + "] has refused your friendship request !");
                                } else {
                                    System.out.println("Warning: Bad server response, NOFRIE type format");
                                    closing(os, is, socket);
                                    exit(1);
                                }
                                break;

                            case "NOCON":
                                mess = serverAnswer.substring(5, serverAnswer.length());
                                if (mess.equals("+++")) {
                                    System.out.println("You have no new notification, try to become more popular ;)");
                                } else {
                                    System.out.println("Warning: Bad server response, NOCON type format");
                                    closing(os, is, socket);
                                    exit(1);
                                }
                                break;

                            case "LBUP>":
                                mess = serverAnswer.substring(serverAnswer.length() - 3, serverAnswer.length());
                                if (mess.equals("+++")) {
                                    int port = 0;
                                    int i = 20;
                                    while (serverAnswer.charAt(i) == '#') {
                                        i--;
                                    }
                                    String ip_diff = serverAnswer.substring(6, i + 1);
                                    p = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                                            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                                            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                                            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
                                    match = p.matcher(ip_diff);
                                    if (!match.matches()) {
                                        System.out.println("Bad server response, LBUP> type format (Promotor IP diff is incorrect)");
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                    try {
                                        port = Integer.parseInt(serverAnswer.substring(22, 26));
                                    } catch (NumberFormatException e) {
                                        System.out.println("Bad server response, LBUP> type format (bad promotor port integer)");
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                    if (port < 1 || port > 9999) {
                                        System.out.println("Bad server response, LBUP> type format (bad promotor port)");
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                    if (subscribed_promotor.contains(ip_diff + ":" + serverAnswer.substring(22, 26))) {
                                        System.out.println("Promotor advertising incoming...");
                                        System.out.println("No need to register, you are already subscribed");
                                    } else {
                                        subscribed_promotor.add(ip_diff + ":" + serverAnswer.substring(22, 26));
                                        mess2 = serverAnswer.substring(27, serverAnswer.length() - 3);
                                        System.out.println("########## Promotor Advertising ##########");
                                        System.out.println(mess2);
                                        System.out.println("##########################################\n");


                                        while (!(mess2.equals("YES") || mess2.equals("Y")) && !(mess2.equals("NO") || mess2.equals("N"))) {
                                            System.out.print("Would you like to subscribe to the newsletter [YES(Y)/NO(N)]: ");
                                            mess2 = sc.nextLine();
                                        }
                                        if (mess2.equals("YES") || mess2.equals("Y")) {
                                            MulticastSocket mso = null;
                                            try {
                                                mso = new MulticastSocket(port);
                                            } catch (IOException e) {
                                                System.out.println("Multicast port already in use");
                                                subscribed_promotor.remove(ip_diff + ":" + serverAnswer.substring(22, 26));
                                                break;
                                            }
                                            PromotorListenerService nsprom = new PromotorListenerService(mso, ip_diff, port);
                                            try {
                                                Thread notificationProm = new Thread(nsprom);
                                                notificationProm.start();
                                            } catch (Exception e) {
                                                System.out.println("ERROR: Thread couldn't start... LPUB> type format");
                                            }
                                        }
                                        if (mess2.equals("NO") || mess2.equals("N")) {
                                            System.out.println("Newsletter refused, you will not get notification from this promotor !");
                                        }
                                    }

                                } else {
                                    System.out.println("Warning: Bad server response, LBUP> type format");
                                    closing(os, is, socket);
                                    exit(1);
                                }
                                break;

                            case "ELIF>":
                                mess = serverAnswer.substring(serverAnswer.length() - 3, serverAnswer.length());
                                if (mess.equals("+++")) {
                                    sender = serverAnswer.substring(6, 14);
                                    filename = sender + "_" + serverAnswer.substring(15, serverAnswer.length() - 3);

                                    DataInputStream dis = null;
                                    try {
                                        dis = new DataInputStream(socket.getInputStream());
                                    } catch (IOException ex) {
                                        System.out.println("Couldn't read on the DataInputStream !");
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                    FileOutputStream fos = null;
                                    try {
                                        fos = new FileOutputStream(filename);
                                    } catch (FileNotFoundException ex) {
                                        System.out.println("Couldn't read on the FileOutputStream !");
                                        closing(os, is, socket);
                                        exit(1);
                                    }
                                    byte[] buffer = new byte[4096];

                                    try {
                                        dis.readFully(buffer);
                                        fos.write(buffer);
                                    } catch (IOException ex) {
                                        System.out.println("Couldn't read the file received... operation aborted");
                                        break;
                                    }
                                    try {
                                        fos.close();
                                    } catch (IOException ex) {
                                    }
                                    System.out.println("You received a file [" + filename + "] from [" + sender + "]");

                                } else {
                                    System.out.println("Bad server response, ELIF> type format");
                                    closing(os, is, socket);
                                    exit(1);
                                }
                                break;

                            default:
                                System.out.println("Warning: Bad Server response, please check the format");
                                closing(os, is, socket);
                                exit(1);
                        }
                        break;

                    case "FRIE":
                        System.out.print("Friendship with: ");
                        recipient = sc.nextLine();
                        while (!string_is_octet(recipient)) {
                            System.out.print("Please enter a registered friend: ");
                            recipient = sc.nextLine();
                        }
                        if (!friend_list.contains(recipient)) {
                            friend_request.add(recipient);

                            mess2 = "FRIE? " + recipient + "+++";
                            try {
                                os.write(mess2.getBytes());
                            } catch (IOException e) {
                                System.out.println("Couldn't write on the output stream.");
                                closing(os, is, socket);
                                exit(1);
                            }
                            if (!readMessage(is, message)) {
                                closing(os, is, socket);
                                exit = false;
                                break;
                            }
                            serverAnswer = new String(getAdaptedMessageArray(message));
                            if (serverAnswer.equals("FRIE>+++")) {
                                System.out.println("Friendship request transmited to [" + recipient + "]'s stream");
                                System.out.println("Once he accepts the request you will be notified !");
                            } else if (serverAnswer.equals("FRIE<+++")) {
                                System.out.println("the recipier [" + recipient + "] doesn't exist or he/she's already your friend!");
                                System.out.println("Friendship request aborted !");
                            } else {
                                System.out.println("ERROR: Bad server response, FRIE> or FRIE< type format...BYE");
                                closing(os, is, socket);
                                exit = false;
                            }
                        } else {
                            System.out.println("You already asked [" + recipient + "] for a friendship");
                        }

                        break;

                    case "LIST":
                        mess2 = "LIST?+++";
                        try {
                            os.write(mess2.getBytes());
                        } catch (IOException e) {
                            System.out.println("Couldn't write on the output stream.");
                            closing(os, is, socket);
                            exit(1);
                        }
                        if (!readMessage(is, message)) {
                            closing(os, is, socket);
                            exit = false;
                            break;
                        }
                        serverAnswer = new String(getAdaptedMessageArray(message));
                        mess = serverAnswer.substring(0, 5);
                        mess2 = serverAnswer.substring(10, serverAnswer.length());

                        if (mess.equals("RLIST") && mess2.equals("+++")) {
                            System.out.println("Message RLIST incoming...");
                            try {
                                num_mess = Integer.parseInt(serverAnswer.substring(6, 10));
                            } catch (NumberFormatException e) {
                                System.out.println("Bad message server, RLIST incoming type format (not an int)");
                                closing(os, is, socket);
                                exit = false;
                                break;
                            }
                            if (num_mess < 1) {
                                System.out.println("Bad message server, RLIST incoming type format (<1)");
                                closing(os, is, socket);
                                exit = false;
                                break;
                            }
                            System.out.println("################");
                            System.out.println("# User List:   #");

                            for (int m = 0; m < num_mess; m++) {
                                if (!readMessage(is, message)) {
                                    closing(os, is, socket);
                                    exit = false;
                                    break;
                                }
                                serverAnswer = new String(getAdaptedMessageArray(message));
                                mess = serverAnswer.substring(0, 5);
                                mess2 = serverAnswer.substring(14, serverAnswer.length());

                                if (mess.equals("LINUM") && mess2.equals("+++")) {
                                    recipient = serverAnswer.substring(6, 14);
                                    System.out.println("# " + (m + 1) + ") " + recipient);

                                } else {
                                    System.out.println("BAD message LINUM type in coming...");
                                    System.out.println("Error: Bad message format, either +++ or LINUM type doesn't exist");
                                    closing(os, is, socket);
                                    exit = false;
                                    break;
                                }
                            }
                            System.out.println("################");

                        } else {
                            System.out.println("BAD message RLIST type in coming...");
                            System.out.println("Error: Bad message format, either +++ or RLIST type doesn't exist");
                            closing(os, is, socket);
                            exit = false;
                        }
                        break;

                    case "FLOO":
                        System.out.print("Enter a message[200 character max]: ");
                        mess = sc.nextLine();
                        while (mess.length() > 200 || mess.length() == 0) {
                            System.out.print("Wrong length, enter a message[200 character max]: ");
                            mess = sc.nextLine();
                        }
                        mess2 = "FLOO? " + mess + "+++";
                        try {
                            os.write(mess2.getBytes());
                        } catch (IOException e) {
                            System.out.println("Couldn't write on the output stream.");
                            closing(os, is, socket);
                            exit(1);
                        }
                        if (!readMessage(is, message)) {
                            closing(os, is, socket);
                            exit = false;
                            break;
                        }
                        serverAnswer = new String(getAdaptedMessageArray(message));
                        if (serverAnswer.equals("FLOO>+++")) {
                            System.out.println("#########################################");
                            System.out.println("# Flood message transmited successfully #");
                            System.out.println("#########################################\n");
                        } else {
                            System.out.println("Error: Bad server response, FLOO> type format");
                            closing(os, is, socket);
                            exit = false;
                        }
                        break;

                    case "FILE":
                        System.out.print("Recipier: ");
                        recipient = sc.nextLine();
                        while (!string_is_octet(recipient)) {
                            System.out.print("Please enter a registered recipier: ");
                            recipient = sc.nextLine();
                        }
                        System.out.print("Enter a file name: ");
                        file = sc.nextLine();
                        if (file.length() > 196) {
                            System.out.println("File name too long");
                            break;
                        }
                        DataOutputStream dos = null;
                        try {
                            dos = new DataOutputStream(socket.getOutputStream());
                        } catch (IOException ex) {
                            System.out.println("Failed to open DataOutputStream, FILE type format");
                            closing(os, is, socket);
                            exit(1);
                        }
                        FileInputStream fis = null;
                        try {
                            fis = new FileInputStream(file);
                        } catch (FileNotFoundException ex) {
                            System.out.println("Failed to open the file, doesn't exist");
                            break;
                        }
                        byte[] buffer = new byte[4096];
                        mess = "FILE? " + recipient + " " + file + "+++";
                        try {
                            dos.write(mess.getBytes());
                            while (fis.read(buffer) > 0) {
                                dos.write(buffer);
                            }
                        } catch (IOException ex) {
                            System.out.println("Failed to read the file, corrupted file");
                            closing(os, is, socket);
                            exit(1);
                        }
                        if (!readMessage(is, message)) {
                            closing(os, is, socket);
                            exit = false;
                            break;
                        }
                        serverAnswer = new String(getAdaptedMessageArray(message));
                        if (serverAnswer.equals("FILE>+++")) {
                            System.out.println("File sent succefully to [" + recipient + "]");
                        } else if (serverAnswer.equals("FILE<+++")) {
                            System.out.println("Failed to send the file, either [" + recipient + "] doesn't exist or he/she's not your friend");
                        }

                        break;

                    default:
                        System.out.print("Please ");
                        break;
                }
            }
        } else {
            System.out.println("Error: Bad server response, registration aborted...BYE");
            closing(os, is, socket);
            exit(1);
        }
    }
}