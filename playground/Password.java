
public class Password {

    public static boolean pwdIsCorrect(byte[] pwd) {
        int nb = ((pwd[0] >= 0 ? pwd[0] : 256 + pwd[0]) + ((pwd[1] >= 0 ? pwd[1] : 256 + pwd[1]) << 8));
        System.out.println("ça c'est nb -> " + nb);
        return (nb >= 0 && nb <= 65535);
    }

    public static boolean comparePwd(byte[] password, byte[] pwd) {
        return (password[0] == pwd[0] && password[1] == pwd[1]);
    }

    public static void main(String[] args) {
        // Fonction de int vers 2-bytes array et inverse
        byte[] b = new byte[2];
        int n = 67535;
        int n2 = 2310;
        b[0] = (byte) (n % 256);
        b[1] = (byte) ((n / 256) % 256);

        //data[0] = (byte) (width & 0xFF);
        //data[1] = (byte) ((width >> 8) & 0xFF);

        //data[0] = (byte) width;
        //data[1] = (byte) (width >>> 8);

        //int result = ByteBuffer.wrap(bytes).getInt();

        int nb = ((b[0] >= 0 ? b[0] : 256 + b[0]) + ((b[1] >= 0 ? b[1] : 256 + b[1]) << 8));
        System.out.println("n = " + n + ", nb = " + nb);
    }
}
