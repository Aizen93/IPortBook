
import java.net.*;

public class UDPListener {

    public static void main(String[] args) {
        try {
            DatagramSocket dso = new DatagramSocket(1024);
            byte[] data = new byte[100];
            DatagramPacket paquet = new DatagramPacket(data, data.length);
            while (true) {
                dso.receive(paquet);
                String st = new String(paquet.getData(), 0, paquet.getLength());
                System.out.println("J'ai reçu: " + st.charAt(0) + "-" 
                        + ((data[1]>=0?data[1]:256+data[1]) + ((data[2]>=0?data[2]:256+data[2]) << 8)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
