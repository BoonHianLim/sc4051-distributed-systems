package SimpleUDPProgram;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class Client {
    public static void main(String args[]) throws IOException {
        Scanner sc = new Scanner(System.in);
        // Step 1: Create the socket object for carrying the data
        DatagramSocket ds = new DatagramSocket();
        InetAddress ip = InetAddress.getLocalHost();
        byte buf[] = null;

        // loop while the user not enters "bye"
        while(true) 
        {
            String inp = sc.nextLine();

            // Convert the String input into a byte array
            buf = inp.getBytes();

            // Step 2: Create the datagramPacket for sending the data
            DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, 1234);

            // Step 3: Invoke th send call to actually send the data
            ds.send(DpSend);

            // break the loop if the user enters "bye"
            if (inp.equals("bye")) {
                break;
            }
        }
        sc.close();
        ds.close();
     }
}
