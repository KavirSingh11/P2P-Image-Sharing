import java.io.*;
import java.util.*;
import java.net.*;

public class PeerServer {
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        final String fileName = "image.jpeg";
        System.out.println("Enter port number");
        int port = Integer.parseInt(sc.nextLine());
        System.out.println("Enter IP address");
        String ip = sc.nextLine();
        try {
            ServerSocket server = new ServerSocket(port);
            Socket socket = server.accept();
            DataInputStream in = new DataInputStream(socket.getInputStream());
            String message = in.readUTF();
            System.out.println(message);
            if(message.contains("QUERY")) {
                File file = new File(fileName);
                byte[] fileBuffer = new byte[(int) file.length()];
                OutputStream out = socket.getOutputStream();
                out.write(fileBuffer, 0 , fileBuffer.length);
                out.flush();
                socket.close();
                System.out.println("File Sent, closed all sockets");
            }

        }
        catch(IOException e){
            System.out.println("Error");
        }

    }
}