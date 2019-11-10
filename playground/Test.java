public class Test {
    public static void main(String[] args) {
        byte[] b = new byte[2];
        int n = 40000;
        b[0] = (byte) (n%256);
        b[1] = (byte) ((n/256)%256);
        System.out.println("n = " + n + ", b = " + b + ", b[0] = " + b[0] + ", b[1] = " + b[1]);
        int nb = ((b[0]>=0?b[0]:256+b[0]) + ((b[1]>=0?b[1]:256+b[1]) << 8));
        System.out.println("nb = " + nb);
    }
}