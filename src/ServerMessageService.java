import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Oussama AOUESSAR, Mike AREZES et Chafik DALI.
 */
public class ServerMessageService implements Runnable {
    private Socket tcpSocket;
    private User[] users_List;
    private LinkedHashMap<String, Integer> registered_Users;
    private String userId;
    private int userPosition;
    private TempMessage tmpMess;
    private String isSendingMessage; // Correspond to the id of the person who's going to receive the message

    /**
     * Constructor that create the thread-object ServerMessageService
     *
     * @param tcpSocket        A Socket that is connected in TCP
     * @param users_List       An array of object User
     * @param registered_Users An LinkedHashMap that contains the string id, and the integer is the position in the user list
     */
    ServerMessageService(Socket tcpSocket, User[] users_List,
                         LinkedHashMap<String, Integer> registered_Users) {
        this.tcpSocket = tcpSocket;
        this.users_List = users_List;
        this.registered_Users = registered_Users;
        this.tmpMess = null;
    }

    /**
     * Add a user to the registred list.
     *
     * @param ul      The User[]
     * @param pos     The int that represent the position in the user list
     * @param id      The string that represent the id of the new user
     * @param pwd     The byte[] that represent the password of the new user
     * @param address A string that represent the address that goes with the port
     * @param port    The int that represent the port UDP to connect to the user
     * @return true if the operation is a success else return false
     */
    private boolean addUser(User[] ul, int pos, String id, byte[] pwd, String address, int port) {
        synchronized (ul) {
            if (ul[pos] != null) return false;
            ul[pos] = new User(id, pwd, address, port);
        }
        return true;
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
     * @param UDPAddress  The string that represent the address that go with the port
     * @param UDPport     The int that represent the port that go with the address
     * @param typeOfNotif The string that represent the type of notification
     * @param nb          The int that represent the number of some type of message that can be consulted
     */
    private void sendNotification(String UDPAddress, int UDPport, String typeOfNotif, int nb) {
        DatagramSocket dso;
        byte[] notif = new byte[3];
        try {
            dso = new DatagramSocket();
            notif[0] = (typeOfNotif.getBytes())[0];
            byte[] intToByte = intTo2bytes(nb);
            System.arraycopy(intToByte, 0, notif, 1, 2); // copy the message id in id
            InetSocketAddress ia = new InetSocketAddress(UDPAddress, UDPport);
            DatagramPacket paquet = new DatagramPacket(notif, notif.length, ia);
            dso.send(paquet);
            if (ServerService.getVerbose()) {
                switch ((int) notif[0]) {
                    case 0:
                        System.out.println("Notification \"friend request\" sent");
                        break;
                    case 1:
                        System.out.println("Notification \"accepted the friend request\" sent");
                        break;
                    case 2:
                        System.out.println("Notification \"refused the friend request\" sent");
                        break;
                    case 3:
                        System.out.println("Notification \"new message received\" sent");
                        break;
                    case 4:
                        System.out.println("Notification \"flood message received\" sent");
                        break;
                    case 5:
                        System.out.println("Notification \"new message from a promotor\" sent");
                        break;
                }
            }
            dso.close();
        } catch (IOException e) {
            if (ServerService.getVerbose()) {
                switch ((int) notif[0]) {
                    case 0:
                        System.out.println("The notification \"friend request\" couldn't be sent");
                        break;
                    case 1:
                        System.out.println("The notification \"the friend request accepted\" couldn't be sent");
                        break;
                    case 2:
                        System.out.println("The notification \"the friend request refused\" couldn't be sent");
                        break;
                    case 3:
                        System.out.println("The notification \"new message received\" couldn't be sent");
                        break;
                    case 4:
                        System.out.println("The notification \"flood message received\" couldn't be sent");
                        break;
                    case 5:
                        System.out.println("The notification \"new message from a promotor\" couldn't be sent");
                        break;
                }
            }
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
     * Function that take an int and return it in a 4 bytes string
     *
     * @param n The int we want to convert
     * @return Return a string in 4 bytes length
     */
    private String intTo4bytes(int n) {
        String num = "" + n;
        switch (num.length()) {
            case 1:
                return ("000" + num);
            case 2:
                return ("00" + num);
            case 3:
                return ("0" + num);
            default:
                break;
        }
        return num;
    }

    /**
     * Function that read in the OutputStream and verify if it isn't too long or end with +++
     *
     * @param is   The InputStream wher we receive the message
     * @param mess The byte[] where we write the message
     * @return True if it end with +++ and is smaller than 214, False if not
     */
    private boolean readMessage(InputStream is, byte[] mess) {
        int pos = 0;
        int nbplus = 0;
        while (nbplus != 3 && pos <= 214) {
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
            if (ServerService.getVerbose()) System.out.println("The client has closed the connection without warning");
            return false;
        } else if (pos < 8) {
            if (ServerService.getVerbose()) {
                System.out.println("The message is too short or doesn't end with +++");
                System.out.println("We close the connection with this client");
            }
            return false;
        } else {
            if (ServerService.getVerbose()) {
                System.out.println("The message is too long or doesn't end with +++");
                System.out.println("We close the connection with this client");
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
     * Function that register a new client if the conditions required are fulfilled
     *
     * @param ul   The user list
     * @param mess The message received that start with REGIS
     * @param ru   The hashmap that contains the list of users that has already subscribed
     * @return Return true if the function has successfully registered a new client, if not it return false
     */
    private boolean register(User[] ul, byte[] mess, LinkedHashMap<String, Integer> ru) {
        byte[] id = new byte[8];
        byte[] port = new byte[4];
        byte[] pwd = new byte[2];
        if (ServerService.getVerbose()) {
            System.out.println("Message length [Byte Array]: " + mess.length);
            System.out.println("Text [Byte Format]: " + Arrays.toString(mess));
        }

        // Check if the message length is 25
        if (mess.length != 25) {
            if (ServerService.getVerbose()) System.out.println("Bad length of the REGIS message received");
            return false;
        }
        // Check if the spaces are at the right place
        if (mess[5] != 32 || mess[14] != 32 || mess[19] != 32) {
            if (ServerService.getVerbose()) System.out.println("Bad space in between the REGIS message received");
            return false;
        }

        System.arraycopy(mess, 6, id, 0, 8); // copy the message id in id
        System.arraycopy(mess, 15, port, 0, 4); // copy the message port in port
        System.arraycopy(mess, 20, pwd, 0, 2); // copy the message password in pwd

        int intPwd = ((pwd[0] >= 0 ? pwd[0] : 256 + pwd[0]) + ((pwd[1] >= 0 ? pwd[1] : 256 + pwd[1]) << 8));
        if (intPwd < 0 || intPwd > 65535) {
            if (ServerService.getVerbose()) System.out.println("Bad password of the REGIS message received");
            return false;
        }
        if (ServerService.getVerbose())
            System.out.println("Password received: " + intPwd);

        String strId = new String(id);
        int portInt = Integer.parseInt(new String(port));
        if (portInt < 1) {
            if (ServerService.getVerbose()) System.out.println("The port is inferior or equals to 0");
            return false;
        }

        // verify that the id is correcty formed
        Pattern p = Pattern.compile("[a-zA-Z_0-9]*");
        Matcher m = p.matcher(strId);
        if (!m.matches()) {
            if (ServerService.getVerbose()) System.out.println("The id received isn't correctly formed");
            return false;
        }
        synchronized (ru) {
            if (ru.isEmpty()) {
                synchronized (ul) {
                    if (!addUser(ul, 0, strId, pwd, (getSocket().getInetAddress()).getHostAddress(), portInt)) {
                        if (ServerService.getVerbose())
                            System.out.println("We couldn't create a new user because there were already someone in the array argument passed in the function");
                    }
                    userPosition = 0;
                }
                ru.put(strId, 0);
            } else if (!ru.containsKey(strId)) {
                if (ru.size() == 100) {
                    if (ServerService.getVerbose()) System.out.println("The user list is full");
                    return false;
                } else {
                    synchronized (ul) {
                        int i;
                        for (i = 1; i < 100; i++) {
                            if (ul[i] == null) break;
                        }
                        if (!addUser(ul, i, strId, pwd, (getSocket().getInetAddress()).getHostAddress(), portInt)) {
                            if (ServerService.getVerbose())
                                System.out.println("We couldn't create a new user because there were already someone in the array argument passed in the function");
                        }
                        userPosition = i;
                    }
                    ru.put(strId, userPosition);
                }
            } else {
                if (ServerService.getVerbose()) System.out.println("The following user already exist: " + strId);
                return false;
            }
        }
        userId = strId;
        return true;
    }

    /**
     * Function that verify if the client can be connected
     *
     * @param ul   The user list
     * @param mess The message received that start with CONNE
     * @param ru   The argument hashmap that contains the registered users
     * @return Return true if the function can successfully connect the client, if not it return false
     */
    private boolean connection(User[] ul, byte[] mess, LinkedHashMap<String, Integer> ru) {
        byte[] id = new byte[8];
        byte[] pwd = new byte[2];
        if (ServerService.getVerbose()) {
            System.out.println("Message length [Byte Array]: " + mess.length);
            System.out.println("Text [Byte Format]: " + Arrays.toString(mess));
        }

        // Check if the message length is 20
        if (mess.length != 20) {
            if (ServerService.getVerbose()) System.out.println("Bad length of the CONNE message received");
            return false;
        }
        // Check if the spaces are at the right place
        if (mess[5] != 32 || mess[14] != 32) {
            if (ServerService.getVerbose()) System.out.println("Bad space in between the CONNE message received");
            return false;
        }

        System.arraycopy(mess, 6, id, 0, 8); // copy the message id in id
        System.arraycopy(mess, 15, pwd, 0, 2); // copy the message password in pwd
        if (ServerService.getVerbose())
            System.out.println("Password received: " + ((pwd[0] >= 0 ? pwd[0] : 256 + pwd[0]) + ((pwd[1] >= 0 ? pwd[1] : 256 + pwd[1]) << 8)));

        String strId = new String(id);

        // verify that the id is correcty formed
        Pattern p = Pattern.compile("[a-zA-Z_0-9]*");
        Matcher m = p.matcher(strId);
        if (!m.matches()) {
            if (ServerService.getVerbose()) System.out.println("The id received isn't correctly formed");
            return false;
        }

        synchronized (ru) {
            if (ru.isEmpty()) {
                if (ServerService.getVerbose()) System.out.println("There is no users registered");
                return false;
            } else if (ru.containsKey(strId)) {
                userPosition = ru.get(strId);
                synchronized (ul[userPosition]) {
                    if (ul[userPosition].getStatus()) {
                        if (ServerService.getVerbose()) {
                            System.out.println("The following user is already connected: " + strId);
                            System.out.println("This protocol isn't correct");
                        }
                        return false;
                    }
                    if (!ul[userPosition].comparePwd(pwd)) {
                        if (ServerService.getVerbose()) {
                            System.out.println("The following user didn't used the right password to connect himself: " + strId);
                            System.out.println("This protocol isn't correct");
                        }
                        return false;
                    }
                }
                if (ServerService.getVerbose())
                    System.out.println("There following user is going to connect: " + strId);
            } else {
                if (ServerService.getVerbose()) System.out.println("The following user doesn't exist: " + strId);
                return false;
            }
        }
        userId = strId;
        return true;
    }

    /**
     * Function that make the consultation of the array flux
     *
     * @param ul   The user list
     * @param mess The message received that start with CONSU
     * @param ru   The hashmap that contains the list of users that has already subscribed
     * @param os   The OutputStream to send messages if necessary
     * @return Return true if the consultation ended succefully, if not it return false
     */
    private boolean consultation(User[] ul, byte[] mess, LinkedHashMap<String, Integer> ru, OutputStream os, InputStream is) {
        if (ServerService.getVerbose()) {
            System.out.println("Message length [Byte Array]: " + mess.length);
            System.out.println("Text [Byte Format]: " + Arrays.toString(mess));
        }

        // Check if the message length is 8
        if (mess.length != 8) {
            if (ServerService.getVerbose()) System.out.println("Bad length of the CONSU message received");
            return false;
        }
        synchronized (ul[userPosition]) {
            int consPosToPop;
            synchronized (ul[userPosition].getFlux()) {
                consPosToPop = ul[userPosition].getFlux().elementPopableArrayList();
            }
            if (consPosToPop == -1) {
                if (ServerService.getVerbose()) System.out.println("There is nothing to consult");
                sendResponse(os, "NOCON+++");
                return true;
            }
            byte[] messagePoped = ul[userPosition].getFlux().popEltArrayList(consPosToPop);
            switch (getMessageType(messagePoped)) {
                case "MESS?":
                    sendResponse(os, ("SSEM> " + ul[userPosition].getFlux().getSenderMessageMess(consPosToPop) + " "
                            + intTo4bytes(ul[userPosition].getFlux().getMessageMessTotalLength(consPosToPop)) + "+++"));
                    ArrayList<byte[]> messParts = ul[userPosition].getFlux().getMessageMessParts(consPosToPop);
                    for (int i = 0; i < messParts.size(); i++) {
                        sendResponse(os, "MUNEM " + intTo4bytes(i) + " "
                                + (new String(messParts.get(i))) + "+++");
                    }
                    synchronized (ul[userPosition].getFlux()) {
                        ul[userPosition].getFlux().decrementNbMessage();
                        ul[userPosition].getFlux().removeMessageMess(consPosToPop);
                    }
                    if (ServerService.getVerbose()) System.out.println("A message MESS? has been consulted");
                    break;

                case "FRIE?":
                    System.arraycopy("EIRF>".getBytes(), 0, messagePoped, 0, 5);
                    sendResponse(os, (new String(messagePoped)));
                    byte[] response = new byte[250];
                    if (ServerService.getVerbose()) System.out.println("Waiting for response to FRIE?...");
                    if (!readMessage(is, response)) {
                        if (ServerService.getVerbose())
                            System.out.println("A problem appeared while waiting for the response to FRIE? message");
                        System.arraycopy("FRIE?".getBytes(), 0, messagePoped, 0, 5);
                        synchronized (ul[userPosition].getFlux()) {
                            ul[userPosition].getFlux().getArrayList().add(consPosToPop, messagePoped);
                        }
                        return false;
                    }
                    byte[] messageAdapted = getAdaptedMessageArray(response);
                    byte[] userRequestToSend = new byte[8];
                    System.arraycopy(messagePoped, 6, userRequestToSend, 0, 8);
                    int pos;
                    switch (getMessageType(messageAdapted)) {
                        case "OKIRF":
                            if (messageAdapted.length != 8) {
                                if (ServerService.getVerbose())
                                    System.out.println("Bad response OKIRF received");
                                return false;
                            }
                            synchronized (ru) {
                                pos = ru.get(new String(userRequestToSend));
                            }
                            synchronized (ul[pos].getFlux()) {
                                ul[pos].getFlux().incrementNbFriendRequestAccepted();
                                ul[pos].getFlux().removeFriendRequest(userId);
                                ul[pos].getFlux().getArrayList().add(("FRIEN " + userId + "+++").getBytes());
                            }
                            synchronized (ul[pos]) {
                                ul[pos].addFriend(userId);
                            }
                            synchronized (ul[userPosition].getFlux()) {
                                ul[userPosition].getFlux().decrementNbFriendRequest();
                                ul[userPosition].getFlux().removeFriendRequest(new String(userRequestToSend));
                            }
                            synchronized (ul[userPosition]) {
                                ul[userPosition].addFriend(new String(userRequestToSend));
                            }
                            sendResponse(os, "ACKRF+++");
                            sendNotification(ul[pos].getUdp_Address(),
                                    ul[pos].getUdp_Port(), "1", ul[pos].getFlux().getNbFriendRequestAccepted());
                            if (ServerService.getVerbose())
                                System.out.println("The messages FRIE? -> OKIRF has been consulted");
                            break;
                        case "NOKRF":
                            if (messageAdapted.length != 8) {
                                if (ServerService.getVerbose())
                                    System.out.println("Bad response NOKRF received");
                                return false;
                            }

                            synchronized (ru) {
                                pos = ru.get(new String(userRequestToSend));
                            }
                            synchronized (ul[pos].getFlux()) {
                                ul[pos].getFlux().incrementNbFriendRequestRefused();
                                ul[pos].getFlux().removeFriendRequest(userId);
                                ul[pos].getFlux().getArrayList().add(("NOFRI " + userId + "+++").getBytes());
                            }
                            synchronized (ul[userPosition].getFlux()) {
                                ul[userPosition].getFlux().decrementNbFriendRequest();
                                ul[userPosition].getFlux().removeFriendRequest(new String(userRequestToSend));
                            }
                            sendResponse(os, "ACKRF+++");
                            sendNotification(ul[pos].getUdp_Address(),
                                    ul[pos].getUdp_Port(), "2", ul[pos].getFlux().getNbFriendRequestRefused());
                            if (ServerService.getVerbose())
                                System.out.println("The messages FRIE? -> NOKRF has been consulted");
                            break;
                        default:
                            if (ServerService.getVerbose())
                                System.out.println("A wrong message appeared while waiting for the response to FRIE? message");
                            System.arraycopy("FRIE?".getBytes(), 0, messagePoped, 0, 5);
                            synchronized (ul[userPosition].getFlux()) {
                                ul[userPosition].getFlux().getArrayList().add(consPosToPop, messagePoped);
                            }
                            return false;
                    }
                    break;

                case "FLOO?":
                    System.arraycopy("OOLF>".getBytes(), 0, messagePoped, 0, 5);
                    sendResponse(os, (new String(messagePoped)));
                    synchronized (ul[userPosition].getFlux()) {
                        ul[userPosition].getFlux().decrementNbFloodMessage();
                    }
                    if (ServerService.getVerbose()) System.out.println("A message FLOO? has been consulted");
                    break;

                case "PUBL?":
                    System.arraycopy("LBUP>".getBytes(), 0, messagePoped, 0, 5);
                    sendResponse(os, (new String(messagePoped)));
                    synchronized (ul[userPosition].getFlux()) {
                        ul[userPosition].getFlux().decrementNbPromotorMessage();
                    }
                    if (ServerService.getVerbose()) System.out.println("A message PUBL? has been consulted");
                    break;

                case "FRIEN":
                    sendResponse(os, (new String(messagePoped)));
                    synchronized (ul[userPosition].getFlux()) {
                        ul[userPosition].getFlux().decrementNbFriendRequestAccepted();
                    }
                    if (ServerService.getVerbose()) System.out.println("A message FRIEN has been consulted");
                    break;

                case "NOFRI":
                    sendResponse(os, (new String(messagePoped)));
                    synchronized (ul[userPosition].getFlux()) {
                        ul[userPosition].getFlux().decrementNbFriendRequestRefused();
                    }
                    if (ServerService.getVerbose()) System.out.println("A message NOFRIE has been consulted");
                    break;

                case "FILE?":
                    System.out.println("Entré dans le case de consu file?");
                    System.arraycopy("ELIF>".getBytes(), 0, messagePoped, 0, 5);
                    sendResponse(os, (new String(messagePoped)));
                    ArrayList<byte[]> file = new ArrayList<>();
                    System.out.println("Avant le synchro ul[userPosition].getFlux()");
                    synchronized (ul[userPosition].getFlux()) {
                        System.out.println("Dans le synchro ul[userPosition].getFlux()");
                        file = ul[userPosition].getFlux().getFileListFile();
                        System.out.println("Après le synchro ul[userPosition].getFlux()");
                    }
                    DataOutputStream dos = null;
                    try {
                        dos = new DataOutputStream(getSocket().getOutputStream());
                    } catch (IOException ex) {
                        System.out.println("Failed to open DataOutputStream, FILE type format");
                        System.arraycopy("FILE?".getBytes(), 0, messagePoped, 0, 5);
                        System.out.println("avant synchro ul[userPosition].getFlux() dataoutput dans catch");
                        synchronized (ul[userPosition].getFlux()) {
                            ul[userPosition].getFlux().getArrayList().add(0, messagePoped);
                        }
                        break;
                    }
                    try {
                        for (byte[] buffer : file) {
                            dos.write(buffer);
                        }
                    } catch (IOException ex) {
                        System.out.println("Failed to send the file");
                        System.arraycopy("FILE?".getBytes(), 0, messagePoped, 0, 5);
                        System.out.println("Avant synchro ul[userPosition].getFlux() dans catch dos write");
                        synchronized (ul[userPosition].getFlux()) {
                            ul[userPosition].getFlux().getArrayList().add(0, messagePoped);
                        }
                        break;
                    }
                    System.out.println("Fin du case");
                    synchronized (ul[userPosition].getFlux()) {
                        ul[userPosition].getFlux().removeFileListFile();
                        ul[userPosition].getFlux().decrementNbFileReceived();
                    }
                    break;
            }
        }
        return true;
    }

    /**
     * Function that verify if the client can be disconnected
     *
     * @param ul   The user list
     * @param mess The message received that start with IQUIT
     * @param ui   The userId that represents the id
     * @param up   The position of the user (userPosition) in the user list
     * @return Return true if the function can successfully connect the client, if not it return false
     */
    private boolean disconnection(User[] ul, byte[] mess, String ui, int up) {
        if (ServerService.getVerbose()) {
            System.out.println("Message length [Byte Array]: " + mess.length);
            System.out.println("Text [Byte Format]: " + Arrays.toString(mess));
        }

        // Check if the message length is 8
        if (mess.length != 8) {
            if (ServerService.getVerbose()) System.out.println("Bad length of the IQUIT message received");
            return false;
        }

        synchronized (ul[up]) {
            if (!ul[up].getStatus()) {
                if (ServerService.getVerbose()) {
                    System.out.println("The following user is already disconnected: " + ui);
                    System.out.println("This protocol isn't correct");
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Function ask for a friend
     *
     * @param ul   The user list
     * @param mess The message received that start with FRIE?
     * @param ru   The hashmap that contains the list of users that has already subscribed
     * @param os   The OutputStream to send messages
     * @return Return true if the function has successfully sent the response to the FRIE? message, if not it return false
     */
    private boolean askToBeAFriend(User[] ul, byte[] mess, LinkedHashMap<String, Integer> ru, OutputStream os) {
        byte[] id = new byte[8];
        if (ServerService.getVerbose()) {
            System.out.println("Message length [Byte Array]: " + mess.length);
            System.out.println("Text [Byte Format]: " + Arrays.toString(mess));
        }

        // Check if the message length is 17
        if (mess.length != 17) {
            if (ServerService.getVerbose()) System.out.println("Bad length of the FRIE? message received");
            return false;
        }
        // Check if the space is at the right place
        if (mess[5] != 32) {
            if (ServerService.getVerbose()) System.out.println("Bad space in between the FRIE? message received");
            return false;
        }

        System.arraycopy(mess, 6, id, 0, 8); // copy the message id in id

        String strId = new String(id);

        // verify that the id is correcty formed
        Pattern p = Pattern.compile("[a-zA-Z_0-9]*");
        Matcher m = p.matcher(strId);
        if (!m.matches()) {
            if (ServerService.getVerbose()) System.out.println("The id received isn't correctly formed");
            return false;
        }

        if (strId.equals(userId)) {
            sendResponse(os, "FRIE<+++");
            if (ServerService.getVerbose())
                System.out.println("Someone tried to ask himself has a friend which is not possible");
        } else {
            synchronized (ru) {
                if (ru.containsKey(strId)) {
                    int posUsr = ru.get(strId);
                    boolean alreadyAsked;
                    synchronized (ul[userPosition].getFlux()) {
                        alreadyAsked = ul[userPosition].getFlux().getFriendRequestList().contains(strId);
                    }
                    if (!alreadyAsked) {
                        boolean isAFriend;
                        synchronized (ul[posUsr]) {
                            isAFriend = ul[posUsr].isAFriend(this.userId);
                        }
                        if (isAFriend) {
                            sendResponse(os, "FRIE<+++");
                            if (ServerService.getVerbose())
                                System.out.println("Someone tried to ask someone for a friend who is already a friend, which is not possible");
                        } else {
                            synchronized (ul[posUsr].getFlux()) {
                                (ul[posUsr].getFlux()).createNewAskForAFriend(this.userId);
                            }
                            synchronized (ul[userPosition].getFlux()) {
                                (ul[userPosition].getFlux()).addFriendRequest(strId);
                            }
                            sendResponse(os, "FRIE>+++");
                            sendNotification(ul[posUsr].getUdp_Address(),
                                    ul[posUsr].getUdp_Port(), "0", ul[posUsr].getFlux().getNbFriendRequest());
                        }
                    } else {
                        sendResponse(os, "FRIE<+++");
                        if (ServerService.getVerbose())
                            System.out.println("Someone tried to ask for a friend when the other part or himself already asked");
                    }
                } else {
                    sendResponse(os, "FRIE<+++");
                    if (ServerService.getVerbose())
                        System.out.println("Someone tried to ask for a friend that is not registered (doesn't exist)");
                }
            }
        }
        return true;
    }

    /**
     * Function prepare the arrival of future messages MENUM
     *
     * @param ul   The user list
     * @param mess The message received that start with MESS?
     * @param ru   The hashmap that contains the list of users that has already subscribed
     * @return Return true if the function has successfully received the MESS? message, if not it return false
     */
    private boolean announcement(User[] ul, byte[] mess, LinkedHashMap<String, Integer> ru) {
        if (isSendingMessage != null) {
            if (ServerService.getVerbose())
                System.out.println("Someone tried to send another message MESS? when he's already sending one");
            return false;
        }
        byte[] id = new byte[8];
        byte[] num_mess = new byte[4];
        if (ServerService.getVerbose()) {
            System.out.println("Message length [Byte Array]: " + mess.length);
            System.out.println("Text [Byte Format]: " + Arrays.toString(mess));
        }

        // Check if the message length is 22
        if (mess.length != 22) {
            if (ServerService.getVerbose()) System.out.println("Bad length of the MESS? message received");
            return false;
        }
        // Check if the spaces are at the right place
        if (mess[5] != 32 || mess[14] != 32) {
            if (ServerService.getVerbose()) System.out.println("Bad space in between the MESS? message received");
            return false;
        }

        System.arraycopy(mess, 6, id, 0, 8); // copy the message id in id
        System.arraycopy(mess, 15, num_mess, 0, 4); // copy the message num_mess in num_mess

        String strId = new String(id);
        int numMessInt = Integer.parseInt(new String(num_mess));
        if (numMessInt < 1) {
            if (ServerService.getVerbose()) System.out.println("Bad num message received");
            return false;
        }

        // verify that the id is correcty formed
        Pattern p = Pattern.compile("[a-zA-Z_0-9]*");
        Matcher m = p.matcher(strId);
        if (!m.matches()) {
            if (ServerService.getVerbose()) System.out.println("The id received isn't correctly formed");
            return false;
        }

        if (strId.equals(userId)) {
            synchronized (ul[userPosition].getFlux()) {
                (ul[userPosition].getFlux()).createNewMessage(strId, numMessInt, true);
            }
        } else {
            int posUsr = 0;
            boolean containsKey;
            synchronized (ru) {
                containsKey = ru.containsKey(strId);
                if (containsKey)
                    posUsr = ru.get(strId);
            }
            if (containsKey) {
                boolean isAFriend;
                synchronized (ul[posUsr]) {
                    isAFriend = ul[posUsr].isAFriend(this.userId);
                }
                synchronized (ul[posUsr].getFlux()) {
                    (ul[posUsr].getFlux()).createNewMessage(this.userId, numMessInt, isAFriend);
                }
            } else {
                if (this.tmpMess == null) {
                    if (ServerService.getVerbose())
                        System.out.println("We received a mess MESS? for a client that doesn't exist");
                    this.tmpMess = new TempMessage(numMessInt);
                    return true;
                } else {
                    if (ServerService.getVerbose())
                        System.out.println("We received a mess MESS? for a client that doesn't exist and tmpMess is not null");
                    return false;
                }
            }
        }
        isSendingMessage = strId;
        return true;
    }

    /**
     * Function that adds the parts of the messages MENUM
     *
     * @param ul   The user list
     * @param mess The message received that start with MENUM
     * @param ru   The hashmap that contains the list of users that has already subscribed
     * @param os   The OutputStream to send messages if necessary
     * @return Return true if the function has successfully added the part, if not it return false
     */
    private boolean messageParts(User[] ul, byte[] mess, LinkedHashMap<String, Integer> ru, OutputStream os) {
        byte[] numPos = new byte[4];
        byte[] messPart;
        if (ServerService.getVerbose()) {
            System.out.println("Message length [Byte Array]: " + mess.length);
            System.out.println("Text [Byte Format]: " + Arrays.toString(mess));
        }

        // Check if the message length is between 15 and 214 included
        if (mess.length < 15 && mess.length > 214) {
            if (ServerService.getVerbose()) System.out.println("Bad length of the MESS? message received");
            return false;
        }
        // Check if the spaces are at the right place
        if (mess[5] != 32 || mess[10] != 32) {
            if (ServerService.getVerbose()) System.out.println("Bad space in between the MESS? message received");
            return false;
        }
        int pos = 11;
        int nbplus = 0;
        while (nbplus != 3) {
            if (mess[pos] != 43 && nbplus != 0) nbplus = 0;
            else if (mess[pos] == 43) nbplus++;
            pos++;
        }
        messPart = new byte[pos - 14];

        System.arraycopy(mess, 6, numPos, 0, 4); // copy the message id in id
        System.arraycopy(mess, 11, messPart, 0, messPart.length); // copy the message num_mess in num_mess

        int numMessInt = Integer.parseInt(new String(numPos));
        if (numMessInt < 0) {
            if (ServerService.getVerbose()) System.out.println("Bad num message MENUM received, inferior to 0");
            return false;
        }
        if (tmpMess != null) {
            if (tmpMess.isTmpMess(numMessInt)) {
                if (tmpMess.isComplete()) {
                    sendResponse(os, "MESS<+++");
                    tmpMess = null;
                    isSendingMessage = null;
                }
                return true;
            }
        }
        if (isSendingMessage == null) {
            if (ServerService.getVerbose())
                System.out.println("We received a MENUM message without receiving a MESS? message");
            return false;
        } else {
            if (isSendingMessage.equals(userId)) {
                synchronized (ul[userPosition]) {
                    switch ((ul[userPosition].getFlux()).addPartMess(userId, numMessInt, messPart, true)) {
                        case 0:
                            break;
                        case 1:
                            ul[userPosition].getFlux().incrementNbMessage();
                            sendResponse(os, "MESS>+++");
                            sendNotification(ul[userPosition].getUdp_Address(),
                                    ul[userPosition].getUdp_Port(), "3",
                                    ul[userPosition].getFlux().getNbMessage());
                            isSendingMessage = null;
                            break;
                        case 2:
                            sendResponse(os, "MESS<+++");
                            isSendingMessage = null;
                            break;
                        case -1:
                            return false;
                    }
                }
            } else {
                synchronized (ru) {
                    int posUsr = ru.get(isSendingMessage);
                    synchronized (ul[posUsr]) {
                        boolean isAFriend;
                        synchronized (ul[posUsr]) {
                            isAFriend = ul[posUsr].isAFriend(this.userId);
                        }
                        switch ((ul[posUsr].getFlux()).addPartMess(userId, numMessInt, messPart, isAFriend)) {
                            case 0:
                                break;
                            case 1:
                                ul[posUsr].getFlux().incrementNbMessage();
                                sendResponse(os, "MESS>+++");
                                sendNotification(ul[posUsr].getUdp_Address(),
                                        ul[posUsr].getUdp_Port(), "3",
                                        ul[posUsr].getFlux().getNbMessage());
                                isSendingMessage = null;
                                break;
                            case 2:
                                sendResponse(os, "MESS<+++");
                                isSendingMessage = null;
                                break;
                            case -1:
                                return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Function that flood the friends of the user
     *
     * @param ul   The user list
     * @param mess The message received that start with FLOO?
     * @param ru   The hashmap that contains the list of users that has already subscribed
     * @return Return true if the function has successfully flooded, if not it return false
     */
    private boolean flood(User[] ul, byte[] mess, LinkedHashMap<String, Integer> ru) {
        if (ServerService.getVerbose()) {
            System.out.println("Message length [Byte Array]: " + mess.length);
            System.out.println("Text [Byte Format]: " + Arrays.toString(mess));
        }

        // Check if the message length is between 15 and 214 included
        if (mess.length < 10 && mess.length > 209) {
            if (ServerService.getVerbose()) System.out.println("Bad length of the FLOO? message received");
            return false;
        }
        // Check if the space is at the right place
        if (mess[5] != 32) {
            if (ServerService.getVerbose()) System.out.println("Bad space in between the FLOO? message received");
            return false;
        }
        int pos = 6;
        int nbplus = 0;
        while (nbplus != 3) {
            if (mess[pos] != 43 && nbplus != 0) nbplus = 0;
            else if (mess[pos] == 43) nbplus++;
            pos++;
        }

        if (ul[userPosition].getFriendList().isEmpty()) {
            if (ServerService.getVerbose()) System.out.println("There is no friend");
        } else {
            ArrayList<String> userToSendMess = new ArrayList<>();
            ul[userPosition].getFriendsToFlood(ul, ru, userToSendMess, userId);
            for (String utsm : userToSendMess) {
                synchronized (ru) {
                    pos = ru.get(utsm);
                }
                synchronized (ul[pos]) {
                    ul[pos].getFlux().flood(mess, userId);
                    sendNotification(ul[pos].getUdp_Address(),
                            ul[pos].getUdp_Port(), "4", ul[pos].getFlux().getNbFloodMessage());
                }
            }
        }
        return true;
    }

    /**
     * Function that send the list of the registered users
     *
     * @param mess The message received that start with MENUM
     * @param ru   The hashmap that contains the list of users that has already subscribed
     * @param os   The OutputStream to send messages if necessary
     * @return Return true if the function has successfully sent the list, if not it return false
     */
    private boolean sendRegisteredUserList(byte[] mess, LinkedHashMap<String, Integer> ru, OutputStream os) {
        if (ServerService.getVerbose()) {
            System.out.println("Message length [Byte Array]: " + mess.length);
            System.out.println("Text [Byte Format]: " + Arrays.toString(mess));
        }

        // Check if the message length is 8
        if (mess.length != 8) {
            if (ServerService.getVerbose()) System.out.println("Bad length of the MESS? message received");
            return false;
        }

        synchronized (ru) {
            sendResponse(os, ("RLIST " + intTo4bytes(ru.size()) + "+++"));
            for (String user : ru.keySet()) {
                sendResponse(os, ("LINUM " + user + "+++"));
            }
        }
        return true;
    }

    /**
     * Function add the file User
     *
     * @param ul   The user list
     * @param mess The message received that start with FILE?
     * @param ru   The hashmap that contains the list of users that has already subscribed
     * @return Return 0 if the function has been successfully added a file and is a friend, 1 if not a user and not sent, -1 if error
     */
    private int file(User[] ul, byte[] mess, LinkedHashMap<String, Integer> ru, InputStream is) {
        if (ServerService.getVerbose())
            System.out.println("Dans le file");
        byte[] receiver = new byte[8];
        byte[] filename;
        if (ServerService.getVerbose()) {
            System.out.println("Message length [Byte Array]: " + mess.length);
            System.out.println("Text [Byte Format]: " + Arrays.toString(mess));
        }


        // Check if the message length is between 19 and 214 included
        if (mess.length < 19 && mess.length > 214) {
            if (ServerService.getVerbose()) System.out.println("Bad length of the FILE? message received");
            return -1;
        }
        // Check if the spaces are at the right places
        if (mess[5] != 32 || mess[14] != 32) {
            if (ServerService.getVerbose()) System.out.println("Bad space in between the FILE? message received");
            return -1;
        }
        int pos = 15;
        int nbplus = 0;
        while (nbplus != 3) {
            if (mess[pos] != 43 && nbplus != 0) nbplus = 0;
            else if (mess[pos] == 43) nbplus++;
            pos++;
        }

        filename = new byte[pos - 18];

        System.arraycopy(mess, 6, receiver, 0, 8); // copy the message id in id
        System.arraycopy(mess, 15, filename, 0, pos - 18); // copy the message num_mess in num_mess

        DataInputStream dis = null;
        try {
            dis = new DataInputStream(getSocket().getInputStream());
        } catch (IOException ex) {
            System.out.println("Failed to open the DataInputStream");
            return -1;
        }

        boolean isAUser = false;
        boolean isAFriend = false;
        int posUser = 0;
        synchronized (ru) {
            isAUser = ru.containsKey(new String(receiver));
        }

        if (isAUser) {
            synchronized (ul[userPosition]) {
                isAFriend = ul[userPosition].isAFriend(new String(receiver));
            }

            synchronized (ru) {
                posUser = ru.get(new String(receiver));
            }
            if (isAFriend) {
                synchronized (ul[posUser].getFlux()) {
                    ul[posUser].getFlux().createFileMess(userId, (new String(filename)), mess);
                }
            }
        }

        byte[] buffer = new byte[4096];

        int filesize = 15123; // Send file size in separate msg
        int read = 0;
        int totalRead = 0;
        int remaining = filesize;
        try {
            if (ServerService.getVerbose())
                System.out.println("Dans le try");
            //while(true) {
            dis.readFully(buffer);
            if (ServerService.getVerbose())
                System.out.println("buffer => " + new String(buffer) + ", read = " + read);
            totalRead += read;
            remaining -= read;
            if (isAFriend) {
                synchronized (ul[posUser].getFlux()) {
                    ul[posUser].getFlux().addFileMess(userId, (buffer));
                }
            }
            if (ServerService.getVerbose())
                System.out.println("buffer => " + new String(buffer) + ", read = " + read);
            //System.out.println("a lu " + totalRead + " bytes.");
            //fos.write(buffer, 0, read);
            //System.out.println(""+new String(buffer));
            //}
            if (ServerService.getVerbose())
                System.out.println("Fin du try");
        } catch (IOException ex) {
        }
        if (ServerService.getVerbose())
            System.out.println("Après envoi du fichier");

        if (isAUser && !isAFriend) {
            return 1;
        }
        //sendNotification(ul[posUser].getUdp_Address(), ul[posUser].getUdp_Port(), "", ul[posUser].getFlux().getNbFileReceived());
        return 0;
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
        boolean goodMessage = true; // A boolean to close the loop if a message is wrong in the switch.
        if (ServerService.getVerbose()) System.out.println("I'm in a client thread");
        InputStream is = null;
        try {
            is = getSocket().getInputStream();
        } catch (IOException e) {
            System.out.println("The opening of the client input stream failed !");
        }
        OutputStream os = null;
        try {
            os = getSocket().getOutputStream();
        } catch (IOException e) {
            System.out.println("The opening of the client output stream failed !");
        }
        byte[] messReceived = new byte[250];
        while (goodMessage) {
            if (ServerService.getVerbose()) {
                System.out.println("--------------------------------------------");
                System.out.println("Waiting client message...\n");
            }
            if (readMessage(is, messReceived)) {
                if (ServerService.getVerbose()) System.out.println("Message received: " + new String(messReceived));
                //System.out.println("Text [Byte Format]: " + Arrays.toString(messReceived));
            } else {
                break;
            }
            if (ServerService.getVerbose()) {
                System.out.println("--------------------------------------------");
                System.out.println("Processing client message...\n");
            }

            switch (getMessageType(messReceived)) {
                case "REGIS":
                    if (userId == null) {
                        if (ServerService.getVerbose()) System.out.println("Someone wants to register REGIS");
                        if (register(users_List, getAdaptedMessageArray(messReceived), registered_Users)) {
                            if (ServerService.getVerbose())
                                System.out.println("The user " + userId + " has registered and is in position " + userPosition);
                            sendResponse(os, "WELCO+++");
                        } else {
                            if (ServerService.getVerbose())
                                System.out.println("Someone couldn't be registered has a new user");
                            sendResponse(os, "GOBYE+++");
                            goodMessage = false;
                        }
                    } else {
                        if (ServerService.getVerbose())
                            System.out.println("Someone tried to register when he has been already registered");
                        goodMessage = false;
                    }
                    break;
                case "CONNE":
                    if (userId == null) {
                        if (ServerService.getVerbose()) System.out.println("Someone wants to connect CONNE");
                        if (connection(users_List, getAdaptedMessageArray(messReceived), registered_Users)) {
                            synchronized (users_List[userPosition]) {
                                users_List[userPosition].onLine();
                            }
                            if (ServerService.getVerbose())
                                System.out.println("The following user just connected: " + userId);
                            sendResponse(os, "HELLO+++");
                        } else {
                            if (ServerService.getVerbose()) System.out.println("Someone couldn't be connected");
                            sendResponse(os, "GOBYE+++");
                            goodMessage = false;
                        }
                    } else {
                        if (ServerService.getVerbose())
                            System.out.println("Someone tried to connect when it is already connected");
                        goodMessage = false;
                    }
                    break;
                case "CONSU":
                    if (ServerService.getVerbose()) System.out.println("Someone wants to consult CONSU");
                    if (userId != null) {
                        if (!consultation(users_List, getAdaptedMessageArray(messReceived), registered_Users, os, is)) {
                            if (ServerService.getVerbose())
                                System.out.println("The consultation didn't ended well");
                            goodMessage = false;
                        }
                    } else {
                        if (ServerService.getVerbose())
                            System.out.println("Someone tried to consult without being connected");
                        goodMessage = false;
                    }
                    break;
                case "IQUIT":
                    if (userId != null) {
                        if (ServerService.getVerbose()) System.out.println("Someone wants to disconnect IQUIT");
                        if (disconnection(users_List, getAdaptedMessageArray(messReceived), userId, userPosition)) {
                            synchronized (users_List[userPosition]) {
                                users_List[userPosition].offLine();
                            }
                            if (ServerService.getVerbose())
                                System.out.println("The following user just disconnected: " + userId);
                            sendResponse(os, "GOBYE+++");
                            goodMessage = false;
                        } else {
                            if (ServerService.getVerbose()) System.out.println("Someone couldn't be connected");
                            goodMessage = false;
                        }
                    } else {
                        System.out.println("Someone tried to quit without being connected");
                        goodMessage = false;
                    }
                    break;
                case "FRIE?":
                    if (ServerService.getVerbose()) System.out.println("Someone wants to ask for a friend FRIE?");
                    if (userId != null) {
                        if (!askToBeAFriend(users_List, getAdaptedMessageArray(messReceived), registered_Users, os)) {
                            if (ServerService.getVerbose()) System.out.println("The message FRIE? wasn't correct");
                            goodMessage = false;
                        }
                    } else {
                        System.out.println("Someone tried to add another as his friend without being connected");
                        goodMessage = false;
                    }
                    break;
                case "MESS?":
                    if (ServerService.getVerbose()) System.out.println("Someone wants to send a message MESS?");
                    if (userId != null) {
                        if (!announcement(users_List, getAdaptedMessageArray(messReceived), registered_Users)) {
                            if (ServerService.getVerbose()) System.out.println("Someone sent a bad message MESS?");
                            goodMessage = false;
                        }
                    } else {
                        System.out.println("Someone tried to send a message without being connected");
                        goodMessage = false;
                    }
                    break;
                case "MENUM":
                    if (ServerService.getVerbose()) System.out.println("Someone is sending a message MENUM");
                    if (userId != null) {
                        if (!messageParts(users_List, getAdaptedMessageArray(messReceived), registered_Users, os)) {
                            if (ServerService.getVerbose()) System.out.println("Someone couldn't send a MENUM message");
                            goodMessage = false;
                        }
                    } else {
                        System.out.println("Someone tried to send parts of a message without being connected");
                        goodMessage = false;
                    }
                    break;
                case "FLOO?":
                    if (ServerService.getVerbose()) System.out.println("Someone wants to flood FLOO?");
                    if (userId != null) {
                        if (!flood(users_List, getAdaptedMessageArray(messReceived), registered_Users)) {
                            if (ServerService.getVerbose()) System.out.println("The message FLOO? wasn't correct");
                            goodMessage = false;
                        }
                        sendResponse(os, "FLOO>+++");
                    } else {
                        System.out.println("Someone tried to flood without being connected");
                        goodMessage = false;
                    }
                    break;
                case "LIST?":
                    if (ServerService.getVerbose())
                        System.out.println("Someone wants to ask for a list of client LIST?");
                    if (userId != null) {
                        if (!sendRegisteredUserList(getAdaptedMessageArray(messReceived), registered_Users, os)) {
                            if (ServerService.getVerbose()) System.out.println("Someone sent a bad message LIST?");
                            goodMessage = false;
                        }
                    } else {
                        System.out.println("Someone tried to see the list of clients without being connected");
                        goodMessage = false;
                    }
                    break;

                case "FILE?":
                    if (ServerService.getVerbose())
                        System.out.println("Someone wants to send a file FILE?");
                    if (userId != null) {
                        if (ServerService.getVerbose())
                            System.out.println("Dans la boucle du FILE? userId != null");
                        switch (file(users_List, messReceived, registered_Users, is)) {
                            case 0:
                                sendResponse(os, "FILE>+++");
                                if (ServerService.getVerbose())
                                    System.out.println("Someone sent a message FILE>+++");
                                break;
                            case 1:
                                sendResponse(os, "FILE<+++");
                                if (ServerService.getVerbose())
                                    System.out.println("Someone sent a bad message FILE<+++");
                                break;
                            case -1:
                                if (ServerService.getVerbose())
                                    System.out.println("Someone tried to send a bad message FILE?");
                                goodMessage = false;
                                break;
                        }
                    } else {
                        System.out.println("Someone tried to send a file without being connected");
                        goodMessage = false;
                    }
                    break;

                default:
                    if (ServerService.getVerbose()) System.out.println("The type of message received isn't correct");
                    goodMessage = false;
                    break;
            }
        }
        try {
            getSocket().close();
        } catch (IOException e) {
            System.out.println("Couldn't close the tcp socket.");
        }
        if (ServerService.getVerbose()) {
            if (userId != null)
                System.out.println("The thread of user " + userId + " has stopped");
            else System.out.println("The client thread has stopped\n");
        }
        if (userId != null) {
            synchronized (users_List[userPosition]) {
                users_List[userPosition].offLine();
            }
        }
    }

    public class TempMessage {
        private int totalLength;
        private int messReceived;

        TempMessage(int totalLength) {
            this.totalLength = totalLength;
            this.messReceived = 0;
        }

        boolean isTmpMess(int posMessReceived) {
            if (this.messReceived == posMessReceived) {
                if (ServerService.getVerbose()) System.out.println("The MENUM message received is a TempMessage");
                messReceived++;
                return true;
            }
            if (ServerService.getVerbose()) System.out.println("The MENUM message received isn't a TempMessage");
            return false;
        }

        boolean isComplete() {
            return (totalLength == messReceived);
        }
    }
}
