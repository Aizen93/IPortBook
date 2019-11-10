import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Oussama AOUESSAR, Mike AREZES et Chafik DALI.
 */
class User {
    private String id; // exactly 8 alphanumerical characters  (pseudo)
    private byte[] password; // a number between 0 et 65535
    private String udp_Address; // a string that represent the UDP address
    private int udp_Port; // inferior to 9999
    private boolean status; // true=connected, false=deconnected
    private Flux flux; // Here we put all the streams
    private ArrayList<String> friend_List;

    User(String id, byte[] password, String udp_Address, int udp_Port) {
        this.id = id;
        this.password = password;
        this.udp_Address = udp_Address;
        this.udp_Port = udp_Port;
        status = true;
        this.flux = new Flux();
        this.friend_List = new ArrayList<>();
    }

    synchronized void onLine() {
        status = true;
    }

    synchronized void offLine() {
        status = false;
    }

    String getUdp_Address() {
        return udp_Address;
    }

    int getUdp_Port() {
        return udp_Port;
    }

    synchronized boolean getStatus() {
        return status;
    }

    Flux getFlux() {
        return this.flux;
    }

    ArrayList<String> getFriendList() {
        return this.friend_List;
    }

    void addFriend(String friend) {
        this.friend_List.add(friend);
    }

    boolean isAFriend(String friend) {
        synchronized (friend_List) {
            return friend_List.contains(friend);
        }
    }

    boolean comparePwd(byte[] pwd) {
        return (this.password[0] == pwd[0] && this.password[1] == pwd[1]);
    }

    void getFriendsToFlood(User[] ul, HashMap<String, Integer> ru,
                           ArrayList<String> utsm, String userToIgnore) {
        synchronized (friend_List) {
            for (String username : friend_List) {
                if (!username.equals(userToIgnore)) {
                    if (utsm.isEmpty()) {
                        utsm.add(username);
                        ul[ru.get(username)].getFriendsToFlood(ul, ru, utsm, userToIgnore);
                    } else if (!utsm.contains(username)) {
                        utsm.add(username);
                        ul[ru.get(username)].getFriendsToFlood(ul, ru, utsm, userToIgnore);
                    }
                }
            }
        }
    }

}