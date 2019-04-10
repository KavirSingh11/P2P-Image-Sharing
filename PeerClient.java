import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.net.*;

public class PeerClient {

    static String[] serverIPs = new String[4];
    static Integer[] serverPorts = new Integer[4];
    static  ArrayList<LocalRecord> records = new ArrayList<>();

    public static void main(String[] args) throws Exception {



        Scanner sc = new Scanner(System.in);

        System.out.println("Enter port number\n");
        int port = Integer.parseInt(sc.nextLine());
        System.out.println("Enter your IP");
        String ip = sc.nextLine();
        System.out.println("Enter DHT 1 port number\n");
        int dhtPort = Integer.parseInt(sc.nextLine());
        System.out.println("Enter DHT 1 IP address\n");
        String dhtIp = sc.nextLine();


        System.out.println("Initializing Servers...");

        init(dhtIp, dhtPort, ip, port);


        while (true) {
            System.out.println("Enter 1 to upload a file.\nEnter 2 to get a file.\nEnter 3 to exit and remove all references of this peer\n");
            int input = sc.nextInt();

            switch (input){
                case 1:
                    try {
                        System.out.println("Enter file name\n");
                        String fileName = sc.nextLine();
                        DatagramSocket server = new DatagramSocket(port);

                        int dhtID = hashCode(fileName);

                        String destIP = serverIPs[dhtID - 1];
                        int destPort = serverPorts[dhtPort - 1];



                        String message = "STORE\n" + ip + "\n" + fileName + "\n" + port;

                        byte[] buffer = message.getBytes();
                        DatagramPacket send = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(destIP), destPort);
                        LocalRecord rec = new LocalRecord(fileName, dhtID, destIP, destPort);
                        records.add(rec);
                        server.send(send);

                        server.receive(send);
                        message = new String(send.getData(), 0 , send.getLength());
                        if (message.contains("200")) {
                            System.out.println("File successfully uploaded");
                        }

                    }
                    catch (SocketException e){
                        errorMessage("Cannot connect to socket");
                    }

                    break;
                case 2:

                    System.out.println("Enter file name\n");

                    String fileName = sc.nextLine();
                    int dhtID = hashCode(fileName);
                    String destIP = serverIPs[dhtID - 1];
                    int destPort = serverPorts[dhtPort - 1];
                    String message = "QUERY\n" +fileName+ "\n" + ip + "\n" + port + "\n";

                    byte[] buffer = message.getBytes();

                    DatagramSocket server = new DatagramSocket(port);
                    DatagramPacket query = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(destIP), destPort);
                    server.send(query);

                    server.receive(query);

                    String conf = new String(query.getData(), 0 , query.getLength());
                    Scanner responseSC = new Scanner(conf);
                    if(conf.contains("200")){
                        //get the image
                        responseSC.nextLine();
                        String fileSourceIP = responseSC.nextLine();
                        System.out.println("File found, now beginning transmission...\n");
                        System.out.println("enter port number for this IP ");

                        int fileSourcePort = Integer.parseInt(sc.nextLine());

                        ServerSocket tcpServer = new ServerSocket(port);
                        Socket socket = new Socket(fileSourceIP, fileSourcePort);
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        out.writeUTF(message);

                        byte[] fileBuffer = new byte[400000];
                        InputStream in = socket.getInputStream();
                        FileOutputStream fileOut = new FileOutputStream(fileName);
                        BufferedOutputStream bufferOutput = new BufferedOutputStream(fileOut);
                        int bytesRead = in.read(fileBuffer, 0, fileBuffer.length);
                        int current = bytesRead;

                        do {
                            bytesRead = in.read(fileBuffer, current, (fileBuffer.length - current));
                            if (bytesRead >= 0) current += bytesRead;
                        } while (bytesRead > -1);

                        bufferOutput.write(fileBuffer, 0, current);
                        bufferOutput.flush();


                    }
                    System.out.println();
                    break;
            }
        }


    }



    public static int hashCode(String fileName) {
        int x = 0;
        for(int i = 0; i < fileName.length(); i++){
            x += (int)fileName.charAt(i);
        }

        int hash = x % 4;
        return hash;
    }

    public static void init(String dhtIp, int dhtPort, String ip, int port) throws Exception{

        Scanner sc;
        DatagramSocket server = new DatagramSocket(port);

        String message = "INIT\n" + ip + "\n" + port + "\n" + dhtIp;

        byte[] buffer = message.getBytes();
        DatagramPacket pkt = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(dhtIp), dhtPort);
        server.send(pkt);
        System.out.println("INIT sent");
        server.receive(pkt);
        message = new String(pkt.getData(), 0, pkt.getLength());
        if(message.contains("200")){
            System.out.println("INIT completed");
            sc = new Scanner(message);
            serverIPs[0] = sc.nextLine();
            serverPorts[0] = Integer.parseInt(sc.nextLine());
            for(int i = 1; i < 4; i++){

                serverIPs[i] = sc.nextLine();
                serverPorts[i] = Integer.parseInt(sc.nextLine());

            }
        }
    }


    public static void errorMessage(String message){
        System.out.println("Error: "+message);
    }

    public static class LocalRecord{

        String fileName;
        int dhtID;
        String dhtIP;
        int dhtPort;
        public LocalRecord(String fileName, int dhtID, String dhtIP, int dhtPort){
            this.fileName = fileName;
            this.dhtID  = dhtID;
            this.dhtIP = dhtIP;
            this.dhtPort = dhtPort;
        }

    }
}
