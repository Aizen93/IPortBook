import java.util.ArrayList;

/**
 * @author Oussama AOUESSAR, Mike AREZES et Chafik DALI.
 */
class Flux {
    private ArrayList<byte[]> data_List;
    private ArrayList<Message> message_List;
    private ArrayList<Message> message_List_Not_Communicated;
    private ArrayList<String> friend_Request_List;
    private ArrayList<String> message_Consulted_Incomplete;
    private ArrayList<fileMess> file_List;
    private int friendRequest;
    private int friendRequestAccepted;
    private int friendRequestRefused;
    private int promotorMessage;
    private int floodMessage;
    private int fileReceived;
    private int message;

    Flux() {
        this.data_List = new ArrayList<>();
        this.message_List = new ArrayList<>();
        this.message_List_Not_Communicated = new ArrayList<>();
        this.friend_Request_List = new ArrayList<>();
        this.message_Consulted_Incomplete = new ArrayList<>();
        this.file_List = new ArrayList<>();
        this.friendRequest = 0;
        this.friendRequestAccepted = 0;
        this.friendRequestRefused = 0;
        this.promotorMessage = 0;
        this.floodMessage = 0;
        this.fileReceived = 0;
        this.message = 0;
    }

    /**
     * Increment functions
     */

    private synchronized void incrementNbFriendRequest() {
        this.friendRequest++;
    }

    synchronized void incrementNbFriendRequestAccepted() {
        this.friendRequestAccepted++;
    }

    synchronized void incrementNbFriendRequestRefused() {
        this.friendRequestRefused++;
    }

    synchronized void incrementNbPromotorMessage() {
        this.promotorMessage++;
    }

    private synchronized void incrementNbFloodMessage() {
        this.floodMessage++;
    }

    private synchronized void incrementNbFileReceived() {
        this.fileReceived++;
    }

    synchronized void incrementNbMessage() {
        this.message++;
    }

    /**
     * Decrement functions
     */

    synchronized void decrementNbFriendRequest() {
        this.friendRequest--;
    }

    synchronized void decrementNbFriendRequestAccepted() {
        this.friendRequestAccepted--;
    }

    synchronized void decrementNbFriendRequestRefused() {
        this.friendRequestRefused--;
    }

    synchronized void decrementNbPromotorMessage() {
        this.promotorMessage--;
    }

    synchronized void decrementNbFloodMessage() {
        this.floodMessage--;
    }

    synchronized void decrementNbFileReceived() {
        this.fileReceived--;
    }

    synchronized void decrementNbMessage() {
        this.message--;
    }

    /**
     * Operation on arraylist
     */

    byte[] popEltArrayList(int i) {
        synchronized (this.data_List) {
            byte[] b = this.data_List.get(i);
            this.data_List.remove(i);
            return b;
        }
    }

    void removeMessageMess(int pos) {
        message_List.remove(pos);
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
     * Function that look in which position is the next element that can be poped and
     * verify if it is a message if it is complete or not and act according
     *
     * @return Return -1 if the list is empty or the only element present can't be poped, else it return the
     * position of the element that can be poped in the data_List
     */
    int elementPopableArrayList() {
        int i;
        int messPos = 0;
        String type;
        synchronized (this.data_List) {
            if (this.data_List.isEmpty()) return -1;
            for (i = 0; i < data_List.size(); i++) {
                type = getMessageType(data_List.get(i));
                switch (type) {
                    case "MESS?":
                        synchronized (message_List) {
                            if (message_List.get(messPos).isComplete) {
                                if (message_Consulted_Incomplete.contains(message_List.get(messPos).getSender())) {
                                    message_Consulted_Incomplete.remove(message_List.get(messPos).getSender());
                                }
                                return i;
                            } else {
                                message_Consulted_Incomplete.add(message_List.get(messPos).getSender());
                                messPos++;
                                break;
                            }
                        }
                    case "FRIE?":
                        return i;
                    case "FLOO?":
                        return i;
                    case "PUBL?":
                        return i;
                    case "FRIEN":
                        return i;
                    case "NOFRI":
                        return i;
                    case "FILE?":
                        return i;
                }
            }
        }
        return -1;
    }

    /**
     * GET
     */

    ArrayList<byte[]> getArrayList() {
        return this.data_List;
    }

    ArrayList<String> getFriendRequestList() {
        return this.friend_Request_List;
    }

    String getSenderMessageMess(int pos) {
        return message_List.get(pos).getSender();
    }

    int getMessageMessTotalLength(int pos) {
        return message_List.get(pos).getTotalLength();
    }

    ArrayList<byte[]> getFileListFile() {
        return file_List.get(0).getFile();
    }

    ArrayList<byte[]> getMessageMessParts(int pos) {
        return message_List.get(pos).getParts();
    }

    synchronized int getNbFriendRequest() {
        return this.friendRequest;
    }

    synchronized int getNbFriendRequestAccepted() {
        return this.friendRequestAccepted;
    }

    synchronized int getNbFriendRequestRefused() {
        return this.friendRequestRefused;
    }

    synchronized int getNbPromotorMessage() {
        return this.promotorMessage;
    }

    synchronized int getNbFloodMessage() {
        return this.floodMessage;
    }

    synchronized int getNbFileReceived() {
        return this.fileReceived;
    }

    synchronized int getNbMessage() {
        return this.message;
    }

    void createNewAskForAFriend(String senderId) {
        synchronized (data_List) {
            data_List.add(("FRIE? " + senderId + "+++").getBytes());
            friend_Request_List.add(senderId);
            incrementNbFriendRequest();
        }
    }

    void addFriendRequest(String id) {
        friend_Request_List.add(id);
    }

    void removeFriendRequest(String id) {
        friend_Request_List.remove(id);
    }

    void removeFileListFile() {
        file_List.remove(0);
    }

    void createNewMessage(String senderId, int nbParts, boolean isAFriend) {
        if (isAFriend) {
            synchronized (message_List) {
                synchronized (data_List) {
                    message_List.add(new Message(senderId, nbParts, true));
                    data_List.add("MESS?".getBytes());
                    if (ServerService.getVerbose())
                        System.out.println("Message MESS? for known user put at position "
                                + (message_List.size() - 1));
                }
            }
        } else {
            synchronized (message_List_Not_Communicated) {
                message_List_Not_Communicated.add(new Message(senderId, nbParts, false));
                if (ServerService.getVerbose())
                    System.out.println("Message MESS? for known user but not a friend put at position "
                            + (message_List_Not_Communicated.size() - 1));
            }
        }
    }

    /**
     * Function that return if there was an error or if the message is complete or not
     *
     * @param sender     The string that represent the id of the sender
     * @param numMessPos The int that represent the number order of the part of the message
     * @param mess       The part of the message only
     * @param isAFriend  The boolean that says if the part comes from a friend
     * @return Return 0 if everything is fine and we can continue,
     * 1 if the message is correct and complete and we can send a MESS>+++,
     * 2 if the message isn't correct and we can send a MESS<+++
     * and finally -1 if there is a problem like a wrong order of message received
     */
    synchronized int addPartMess(String sender, int numMessPos, byte[] mess, boolean isAFriend) {
        if (isAFriend) {
            int i;
            for (i = message_List.size() - 1; i >= 0; i--)
                if ((message_List.get(i).getSender()).equals(sender))
                    break;
            if ((message_List.get(i)).addMess(numMessPos, mess)) {
                if ((message_List.get(i)).isComplete()) return 1;
                return 0;
            }
            return -1;
        } else {
            int i;
            for (i = message_List_Not_Communicated.size() - 1; i >= 0; i--)
                if ((message_List_Not_Communicated.get(i).getSender()).equals(sender))
                    break;
            if ((message_List_Not_Communicated.get(i)).addMess(numMessPos, mess)) {
                if ((message_List_Not_Communicated.get(i)).isComplete()) {
                    synchronized (message_List_Not_Communicated) {
                        message_List_Not_Communicated.remove(i);
                    }
                    return 2;
                }
                return 0;
            }
            return -1;
        }
    }

    void createFileMess(String sender, String filename, byte[] mess) {
        byte[] s = sender.getBytes();
        System.arraycopy(s, 0, mess, 6, 8);
        synchronized (data_List) {
            file_List.add(new fileMess(sender, filename));
            data_List.add(mess);
            incrementNbFileReceived();
            if (ServerService.getVerbose())
                System.out.println("File added");
        }
    }

    void addFileMess(String sender, byte[] partFile) {
        synchronized (file_List) {
            int i;
            for (i = file_List.size() - 1; i >= 0; i--) {
                if (file_List.get(i).getSender().equals(sender)) {
                    break;
                }
            }
            file_List.get(i).addPartFile(partFile);
            if (ServerService.getVerbose())
                System.out.println("File part added");
        }
    }

    void flood(byte[] mess, String sender) {
        byte[] mess2 = new byte[mess.length + sender.length() + 1];
        System.arraycopy(mess, 0, mess2, 0, 6);
        System.arraycopy(sender.getBytes(), 0, mess2, 6, 8);
        System.arraycopy(" ".getBytes(), 0, mess2, 14, 1);
        System.arraycopy(mess, 6, mess2, 15, mess.length - 6);
        synchronized (data_List) {
            data_List.add(mess2);
        }
        incrementNbFloodMessage();
    }

    public class Message {
        private String sender;
        private boolean isComplete;
        private boolean isAFriend;
        private int totalLength;
        private int messReceived;
        private ArrayList<byte[]> parts;

        Message(String sender, int totalLength, boolean isAFriend) {
            this.sender = sender;
            this.isComplete = false;
            this.isAFriend = isAFriend;
            this.totalLength = totalLength;
            this.messReceived = 0;
            this.parts = new ArrayList<>();
        }

        boolean addMess(int numMessPos, byte[] mess) {
            if (isComplete) {
                if (isAFriend) System.out.println("The message received is already complete for a friend");
                else System.out.println("The message received is already complete not for a friend");
                return false;
            } else {
                if (numMessPos == messReceived) {
                    if (isAFriend) parts.add(mess);
                    messReceived++;
                    if (totalLength == messReceived) isComplete = true;
                    return true;
                } else {
                    if (isAFriend) System.out.println("The message isn't in the right order for a friend");
                    else System.out.println("The message isn't in the right order nor for a friend");
                    return false;
                }
            }
        }

        boolean isComplete() {
            return this.isComplete;
        }

        String getSender() {
            return this.sender;
        }

        ArrayList<byte[]> getParts() {
            return this.parts;
        }

        int getTotalLength() {
            return this.totalLength;
        }
    }

    public class fileMess {
        private String sender;
        private String filename;
        private ArrayList<byte[]> file;

        fileMess(String sender, String filename) {
            this.sender = sender;
            this.file = new ArrayList<>();
        }

        void addPartFile(byte[] part) {
            byte[] p = new byte[part.length];
            System.arraycopy(p, 0, part, 0, part.length);
            file.add(p);
        }

        String getSender() {
            return this.sender;
        }

        String getFilename() {
            return this.filename;
        }

        ArrayList<byte[]> getFile() {
            return this.file;
        }
    }
}