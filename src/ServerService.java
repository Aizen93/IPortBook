import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;

/**
 * @author Oussama AOUESSAR, Mike AREZES et Chafik DALI.
 */
class ServerService {
    private int client_Port; // port number inferior to 9999
    private int promotor_Port; // port number inferior to 9999
    private User[] users_List;
    private LinkedHashMap<String, Integer> registered_Users;
    private static boolean verbose;

    /**
     * Constructor of ServerService if the program is called the ports in argument
     *
     * @param verbose       Boolean if we are in verbose mode
     * @param client_Port   Integer between 0 and 9999 that represent the port to cummunicate with the Clients
     * @param promotor_Port Integer between 0 and 9999 that represent the port to cummunicate with the Promotors
     */
    ServerService(boolean verbose, int client_Port, int promotor_Port) {
        this.client_Port = client_Port;
        this.promotor_Port = promotor_Port;
        this.users_List = new User[100];
        for (int i = 0; i < 100; i++)
            this.users_List[i] = null;
        this.registered_Users = new LinkedHashMap<>();
        ServerService.verbose = verbose;
    }

    /**
     * Constructor of the ServerService if the program is called without arguments
     *
     * @param verbose Boolean if we are in verbose mode
     */
    ServerService(boolean verbose) {
        this.users_List = new User[100];
        for (int i = 0; i < 100; i++)
            this.users_List[i] = null;
        this.registered_Users = new LinkedHashMap<>();
        ServerService.verbose = verbose;
    }

    /**
     * That function find 2 port free TCP port for clients and promotors
     *
     * @return True if 2 free ports have been found, else False
     */
    boolean findFreePorts() {
        int port;
        ServerSocket server;
        int portFound = 0;

        for (port = 1; port < 9999; port++) {
            try {
                server = new ServerSocket(port);
                server.close();
                this.client_Port = port;
                portFound++;
                break;
            } catch (IOException ex) {
                //if (verbose)
                //System.out.println("The following port is not free " + port + ".");
            }
        }
        for (port = this.client_Port + 1; port < 9999; port++) {
            try {
                server = new ServerSocket(port);
                server.close();
                this.promotor_Port = port;
                portFound++;
                break;
            } catch (IOException ex) {
                //if (verbose)
                //System.out.println("The following port is not free " + port + ".");
            }
        }
        return (portFound == 2);
    }

    /**
     * Function that print the address and the ports to communicate with the Server
     *
     * @throws UnknownHostException Throw an exception if the host in unknown
     */
    void afficheServerPorts() throws UnknownHostException {
        System.out.println("----------------------------------------------------------------------");
        System.out.println("                      #########################");
        System.out.println("                      # Welcome to the Server #");
        System.out.println("                      #########################");
        if (verbose) System.out.println("                        We are in verbose mode");
        System.out.println();
        System.out.println("Searching Server information...");
        System.out.println("Server IP Address: " + (InetAddress.getLocalHost()).getHostAddress());
        System.out.println("Client TCP Port Listener: " + this.client_Port);
        System.out.println("Promotor TCP Port Listener: " + this.promotor_Port);
        System.out.println();
        System.out.println("----------------------------------------------------------------------");
    }

    /**
     * Function that return the argument client_Port
     *
     * @return Return the argument client_Port
     */
    int getClientPort() {
        return this.client_Port;
    }

    /**
     * Function that return the argument promotor_Port
     *
     * @return Return the argument promotor_Port
     */
    int getPromotorPort() {
        return this.promotor_Port;
    }

    /**
     * Function that get the User list argument
     *
     * @return A User list that correspond to the argument users_List
     */
    User[] getUsersList() {
        return this.users_List;
    }

    /**
     * Function that get the LinkedHashMap<String, Integer> argument
     *
     * @return An LinkedHashMap that correspond to the argument users_List
     */
    LinkedHashMap<String, Integer> getRegisteredUsers() {
        return this.registered_Users;
    }

    /**
     * Function that get the verbose argument
     *
     * @return A boolean that correspond to the argument verbose
     */
    static boolean getVerbose() {
        return verbose;
    }
}