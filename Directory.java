import java.io.*;
import java.util.*;
import java.net.*;

public class Directory{
    public static void main(String[] args) throws Exception{
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter port number\n");
        int port = Integer.parseInt(sc.nextLine());

        System.out.println("Enter IP address\n");
        String ip = sc.nextLine();

        System.out.println("Enter DHT ID");
        int ID = Integer.parseInt(sc.nextLine());

        System.out.println("Enter successor port number\n");
        int succPort = Integer.parseInt(sc.nextLine());

        System.out.println("Enter successor IP address");
        String succIp = sc.nextLine();

        Hashtable<String, String> content = new Hashtable<>();

        byte[] buffer = new byte[10000];
        DatagramSocket server = new DatagramSocket(port);
        DatagramPacket rcv = new DatagramPacket(buffer, buffer.length);

        while(true){

            System.out.println("Server is listening...\n");

            server.receive(rcv);



            System.out.println("Received" + rcv);
            String message = new String(rcv.getData(), 0 , rcv.getLength());
            sc = new Scanner(message);

            if(message.contains("STORE")){
                System.out.println("Upload requested");
                sc.nextLine();
                String newFile = sc.nextLine();
                String newIP = sc.nextLine();
                content.put(newFile, newIP);
                message = "200";
                byte[] responseBuff = message.getBytes();
                DatagramPacket response = new DatagramPacket(responseBuff, responseBuff.length, InetAddress.getByName(newIP), rcv.getPort());
                server.send(response);
            }
            else if(message.contains("INIT")){
            	System.out.println("Init in progress...\n");
                sc.nextLine();
                String srcIP = sc.nextLine();
                int srcPort = Integer.parseInt(sc.nextLine());

                byte[] initBuff = message.getBytes();
                if(!message.contains(ip)){
                	System.out.println("Sending Init message to successor...\n");
                    server.receive(rcv);
                    message = new String(rcv.getData(), 0, rcv.getLength());

                    message += ip + "\n";
                    message += port + "\n";
                    DatagramPacket initPkt = new DatagramPacket(initBuff, initBuff.length, InetAddress.getByName(succIp), succPort);
                    server.send(initPkt);
                }

                if(message.contains(ip)){
                	System.out.println("Returning completed init...\n");
                    DatagramPacket initRet = new DatagramPacket(initBuff, initBuff.length, InetAddress.getByName(srcIP), srcPort);
                    server.send(initRet);
                }
                else{
                	System.out.println("Returning incomplete init...\n");
                	DatagramPacket incompleteInit = new DatagramPacket(initBuff, initBuff.length, InetAddress.getByName(srcIP), srcPort);
                	server.send(incompleteInit);
                }
            }
            else if(message.contains("QUERY")){
                System.out.println("Query received");
                sc.nextLine();
                String file = sc.nextLine();
                String fileSource = content.get(file);
                String srcIP = sc.nextLine();
                int srcPort = Integer.parseInt(sc.nextLine());
                message = "200\n" + fileSource + "\n";
                byte[] responseBuff = message.getBytes();
                DatagramPacket response = new DatagramPacket(responseBuff, responseBuff.length, InetAddress.getByName(srcIP), srcPort);
                server.send(response);
            }
            else if(message.contains("EXIT")){
            	sc.nextLine();
            	String srcIP = sc.nextLine();
            	int srcPort = Integer.parseInt(sc.nextLine());
            	content.remove(srcIP);
            	if(message.contains(Integer.toString(ID))){
                	byte[] buff = message.getBytes();
            		DatagramPacket sendToSrc = new DatagramPacket(buff, buff.length, InetAddress.getByName(srcIP), srcPort);
            		server.send(sendToSrc);
            	}
            	else{
            		message += ID + "\n";
            		byte[] buff = message.getBytes();
            		DatagramPacket sendToSucc = new DatagramPacket(buff, buff.length, InetAddress.getByName(succIp), succPort);
            		server.send(sendToSucc);
            	}
            	
            }

        }
    }

}