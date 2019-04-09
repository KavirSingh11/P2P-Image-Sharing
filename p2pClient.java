/* Assigned port numbers: 20770 - 20779*/

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class p2pClient {

    static String dhtIP1;
    static int dhtPort1;
    static int p2pPort;

    public static void main(String[] args) {//User input of known variables and begin the main thread
        Scanner sc = new Scanner(System.in);

        System.out.println("\nEnter DHT 1 IP address \n");
        dhtIP1 = sc.nextLine();

        System.out.println("\nEnter DHT 1 port \n");
        dhtPort1 = sc.nextInt();

        System.out.println("\nEnter the client's port number \n");
        p2pPort = sc.nextInt();

        Client peerClient = new Client(dhtIP1, dhtPort1, p2pPort);
        //p2pServer peerServer = new p2pServer(p2pPort)

        int userInput = 0;
        System.out.println("1. Add an image to the network.\n2. Find a file in the network\n3. Exit and remove all references of this peer from the DHT\n");
        while(userInput != 3){

            userInput = Integer.parseInt(sc.nextLine());

            if(userInput == 1){
                System.out.println("Enter the name of the file:\n");
                String fileName = sc.nextLine();
                System.out.println("Enter the server ID of the file you wish to upload to:\n");
                int serverID = sc.nextInt();
                try {
                    peerClient.addFileToDHT(serverID, fileName);
                }
                catch (UnknownHostException e){
                    System.out.println("Server connection failed...");
                }
            }
            else if(userInput == 2){
                System.out.println("Enter file name: \n");
                String filename = sc.nextLine();
                System.out.println("Enter server ID you wish to query: \n");
                int serverID = sc.nextInt();
                try{
                    peerClient.findFile(filename, serverID);
                }
                catch(Exception e){
                    System.out.println("Failed to get file");
                }
            }
            else if(userInput == 3){
                    peerClient.exit();
            }else{
                System.out.println("Enter a number between 1 and 3");
            }
        }
    }

    @SuppressWarnings("unused")
    public static class Client{
        int serverPort;

        ArrayList<String> servers = new ArrayList<>();
        ArrayList<Integer> portNumbers = new ArrayList<>();
        DatagramSocket ds;

        //constructor
        public Client(String dhtIP1, int dhtPort1, int p2pPort) {
            //set the first server IP and port to DHT 1's information and
            this.serverPort = p2pPort;
            this.servers.add(0, dhtIP1);
            this.portNumbers.add(0, dhtPort1);
            try{
                ds = new DatagramSocket();
                init();
            }catch(Exception e){
                System.out.println("\nError occurred");
            }
        }

        //initialize connection with DHT pool and get all their IPs
        public void init() throws Exception{
            String status;
            String message;
            sendToDHT("Send DHT Pool IPs", servers.get(0), portNumbers.get(0));
            message = getFromDHT();
            Scanner sc = new Scanner(message);
            status = sc.next();
            sc.next();
            sc.next();
            sc.next();
            sc.next();
            sc.next();
            if(status.equals("200")){
                System.out.println("\nDHT Init Complete");
            }
            for(int i = 0; i < 4; i++){
                servers.add(i, sc.next());
                portNumbers.add(i, Integer.parseInt(sc.next()));
            }
        }

        public void sendToDHT(String message, String dhtIP, int port) throws Exception{
            byte[] pktBytes = message.getBytes();
            InetAddress adr = InetAddress.getByName(dhtIP);
            DatagramPacket sendPKT = new DatagramPacket(pktBytes, pktBytes.length, adr,port);
            ds.send(sendPKT);
        }

        public String getFromDHT() throws Exception{
            byte[] buffer = new byte[2048];
            DatagramPacket rcv = new DatagramPacket(buffer, buffer.length);
            ds.receive(rcv);
            return new String(rcv.getData());

        }

        public void findFile(String fileName, int n) throws Exception{
            String fileDestIP;
            String message = "Find" + fileName;

            sendToDHT(message, servers.get(n), portNumbers.get(n));
            message = getFromDHT();
            Scanner sc = new Scanner(message);
            String status = sc.next();

            if(status.equals("404")){
                System.out.println("\nSorry the file you are looking for does not exist. (Error 404)");
            }
            else if(status.equals("200")){
                System.out.println("File Found");
                sc = new Scanner(message);
                sc.next();
                fileDestIP = sc.next();

                //create the message in HTTP format
                String request = "GET" +" /" + fileName + ".jpeg" + " HTTP/1.1\r\n" + "Host: " + InetAddress.getByName(fileDestIP).getHostName() + "\r\n" + "Accept: image/jpeg\r\n" +"+Connection:  close\r\n" + "Accept-language: en";

                Socket connect = new Socket(fileDestIP, serverPort);
                OutputStream toServer = connect.getOutputStream();
                DataOutputStream out = new DataOutputStream(toServer);
                out.writeUTF("Open");
                DataInputStream in = new DataInputStream(connect.getInputStream());
                message = in.readUTF();
                connect.close();

                sc = new Scanner(message);
                status = sc.next();
                int portNumber = sc.nextInt();

                if (status.equals("200")){
                    //if peer with the file is found connect to it
                    Socket connectToPeer = new Socket (fileDestIP, serverPort);
                    OutputStream toPeer = connectToPeer.getOutputStream();
                    DataOutputStream out1 = new DataOutputStream(toPeer);
                    out.writeUTF(request);
                    InputStream in1 = connectToPeer.getInputStream();
                    DataInputStream dataInput = new DataInputStream(in1);
                    int length = dataInput.readInt();
                    byte[] data = new byte[length];
                    if(length > 0){
                        dataInput.readFully(data);
                    }
                    connectToPeer.close();

                    String file = new String(data);
                    sc = new Scanner(file);
                    status = sc.nextLine() + "\r\n";
                    String temp;

                    if(status.contains("200")){
                        status = getHTTP(sc, status);
                        File output = new File(fileName + "jpeg");
                        int fSize = data.length - status.getBytes().length;
                        byte[] convertToBytes = new byte[fSize];
                        for(int i = status.getBytes().length; i < data.length; i ++){
                            convertToBytes[i - status.getBytes().length] = data[i];
                        }

                        FileOutputStream fos = new FileOutputStream(output);
                        fos.write(convertToBytes);
                        fos.close();
                    }
                    else if(status.contains("400")){
                        status = getHTTP(sc, status);
                    }
                    else if(status.contains("404")){
                        status = getHTTP(sc, status);
                    }
                    else if(status.contains("505")){
                        status = getHTTP(sc, status);
                    }
                    System.out.println("Error Occurred, Error :" + status);
                }

            }
        }

        public String getHTTP(Scanner sc, String reply){
            String temp;
            while(sc.hasNext()){
                temp = sc.nextLine() + "\r\n";
                reply += temp;
                if(temp.equals("\r\n")){
                    break;
                }
            }
            return reply;
        }


        public void addFileToDHT(int dhtServer, String fileName) throws UnknownHostException{
            String status;
            String message = "Upload" + fileName+ " " + InetAddress.getLocalHost().getHostAddress();
            try {
                sendToDHT(message, servers.get(dhtServer), portNumbers.get(dhtServer));
            }
            catch (Exception e){
                System.out.println("Error sending the file");
            }
            try {
                message = getFromDHT();
            }
            catch(Exception e){
                System.out.println("Error receiving response from server");
            }
            Scanner sc = new Scanner(message);
            status = sc.nextLine();

            if(status.equals("200")){
                System.out.println("File successfully sent to DHT");
            }
        }

        //Send notice to DHT pool to remove all reference to this client
        public void exit(){
            byte[] receiveData = new byte[2048];
            String message = "Exit" + servers.get(0) + servers.get(1) + servers.get(2) +servers.get(3);

            try {
                sendToDHT(message,servers.get(0), portNumbers.get(0));

            }
            catch(Exception e){
                System.out.println("Failed to connect to base DHT");
            }
            try {
                message = getFromDHT();
            }
            catch(Exception e){
                System.out.println("Failed to get response from DHT");
            }

            ds.close();
            Scanner sc = new Scanner(message);
            String status = sc.nextLine();

            if(status.equals("200")){
                System.out.println("Peer successfully exited");
            }
            System.exit(0);
        }


    }

}
