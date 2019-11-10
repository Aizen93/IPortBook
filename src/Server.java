import java.io.IOException;

import static java.lang.Integer.parseInt;

/**
 * @author Oussama AOUESSAR, Mike AREZES et Chafik DALI.
 */
public class Server {
    public static void main(String[] args) throws IOException {
        ServerService serv = null;

        if (args.length == 0) {
            serv = new ServerService(false);
            if (!serv.findFreePorts()) {
                System.out.println("There is no free port or not enough.");
                System.out.println("Bye...");
                return;
            }
            serv.afficheServerPorts();
        } else if (args[0].equals("-v") && args.length == 1) {
            serv = new ServerService(true);
            if (!serv.findFreePorts()) {
                System.out.println("There is no free port or not enough.");
                System.out.println("Bye...");
                return;
            }
            serv.afficheServerPorts();
        } else if (args.length == 2) {
            try {
                int clientP = parseInt(args[0]);
                int promotorP = parseInt(args[1]);
                if ((clientP > 0 && clientP < 9999) && (promotorP > 0 && promotorP < 9999)) {
                    serv = new ServerService(false, clientP, promotorP);
                    serv.afficheServerPorts();
                }
            } catch (NumberFormatException nfe) {
                System.out.println("The promotor or client port aren't Integer.");
                System.out.println("Bye...");
                return;
            }
        } else if (args[0].equals("-v") && args.length == 3) {
            try {
                int clientP = parseInt(args[1]);
                int promotorP = parseInt(args[2]);
                if ((clientP > 0 && clientP < 9999) && (promotorP > 0 && promotorP < 9999)) {
                    serv = new ServerService(true, clientP, promotorP);
                    serv.afficheServerPorts();
                }
            } catch (NumberFormatException nfe) {
                System.out.println("The promotor or client port aren't Integer.");
                System.out.println("Bye...");
                return;
            }
        } else {
            System.out.println("There is too much arguments.");
            System.out.println("Bye...");
            return;
        }

        System.out.println("Initializing client communication service...");
        ClientCommunicationService ccs = new ClientCommunicationService(serv);
        Thread clients = new Thread(ccs);
        clients.start();

        System.out.println("Initializing promotor communication service...");
        PromotorCommunicationService pcs = new PromotorCommunicationService(serv);
        Thread promotors = new Thread(pcs);
        promotors.start();
    }
}
