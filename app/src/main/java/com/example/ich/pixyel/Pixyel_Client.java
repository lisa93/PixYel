package com.example.ich.pixyel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;



/**
 *
 * @author Josua Frank
 */
public class PixYel_Client {

    Socket socket;//Der "Kanal" zum Server
    ServerInputListener listener;//Ein eigener Thread, der auf eingehende Nachrichten vom Server horcht
    String serverIP = "localhost";//IP-Adresse des Servers, zum testes localhost (Server und Client auf dem selben Computer), wird später "sharknoon.de" sein!
    //Der öffentliche Key des Servers
    String serverPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmG8OfhJrkN9/rXLh7auyUPcq7UxmYModYswChY8hIMgZO4m+cxOWopxOptUAYedjA4ZAKGp/P1g6n6YaXvtPQqIbi7G5oCT4vbh0zYFgI3wNCJlKtUX1gb6uCQW3rPinANcPtlZoIyegAsn/OW0FMZtc1x8PN0H1MQTlcCctXdJdotuljeYriO1lkRfb3GsotLIYjciMqIMKGQRQ2Rhj81bnxP9FybdNuVIjlS6Rfx9fzaZ2BKIdm7O7/Dzn9TcSZEOZdOSS7CHMMKr14O26g+bR2HiGWx8AbOH2zP3DMpR9/Y8GUrjO6QPqA+GorICGYWxIlrcm4iYx8740FsDaQQIDAQAB";
    //Der private Key des Clients
    String clientPrivateKey;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        PixYel_Client demoClient = new PixYel_Client();
    }

    public PixYel_Client() {
        //Verbindung zum Server herstellen
        connect();
        //Beispiel: sendet ein xml mit dem node "echo" an den Server, der server schickt daraufhin selbiges zurück
        sendToServer(XML.createNewXML("echo").toXMLString());
        //Wenn man die App schließt oder ähnliches, einfach die disconnect Methode aufrufen
    }

    public void connect() {
        //Falls der Server unerreichbar ist, versucht er es 'attemps' mal
        int attempts = 10;
        while (attempts > 0 && (socket == null || !socket.isConnected())) {
            attempts--;
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverIP, 7331), 500);
                System.out.println("Erfolgreich verbunden");
                listener = new ServerInputListener();
                new Thread(listener).start();
                finishEncryptionStuff();
            } catch (UnknownHostException e) {
                System.err.println("Unbekannter Host: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Server konnte nicht erreicht werden: " + e.getMessage());
            }
        }
    }

    public void finishEncryptionStuff() {
        //Erzeuge Client Private und Public Key
        String[] keyPair = Encryption.generateKeyPair();
        //Speichere den Private Key für andere Methoden sichtbar
        clientPrivateKey = keyPair[1];
        //Erzeuge ein XML mit einem Tag namens publickey und als Inhalt der public key
        XML publicKeyAsXML = XML.createNewXML("publickey").setContent(keyPair[0]);
        //Übermittle dem Server meinen Public Key
        sendToServer(publicKeyAsXML.toXMLString());
    }

    public void disconnect() {
        //Unwahrscheinlicher Fall, dass der Socket sich unerwartet beendet hat
        if (socket == null) {
            System.out.println("Server unerreichbar, beende daher sofort...");
            System.exit(0);
        } else {
            try {
                listener.stop();
                //"Kanal" zum Server schließen
                socket.close();
                System.out.println("Habe mich beim Server abgemeldet und beende mich jetzt...");
                System.exit(0);
            } catch (IOException ex) {
                System.out.println("Konnte Socket nicht schliessen!");
            }

        }
    }

    public void sendToServer(String toSend) {
        try {
            String compressed = Compression.compress(toSend);
            String encrypted = Encryption.encrypt(compressed, serverPublicKey);
            PrintWriter raus = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            raus.println(encrypted);
            raus.flush();
            System.out.println("Erfolgreich \"" + toSend + "\" gesendet!");
        } catch (Exception e) {
            if (e.toString().contains("Socket is closed")) {
                System.err.println("Could not send String beacuase the socket is closed, closing the connection now: " + e);
                disconnect();
            } else if (e.toString().contains("socket write error")) {
                System.err.println("Could not write on Socket: " + e);
            } else {
                System.err.println("String(" + toSend + ") could not be send: " + e);
            }
        }
    }

    public class ServerInputListener implements Runnable {

        //Dient zum einfachen Beenden dieses Threads
        public boolean run = true;

        @Override
        public void run() {
            System.out.println("Listener für eingehende Nachrichten vom Server in eigenem Thread erfolgreich gestartet!");
            BufferedReader rein;
            String string;
            while (!socket.isClosed() && socket.isConnected() && socket.isBound() && run) {
                try {
                    rein = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    string = rein.readLine();
                    if (run) {
                        onStringReceived(string);
                    }
                } catch (IOException exe) {
                    if (run) {
                        switch (exe.toString()) {
                            case "java.net.SocketException: Connection reset":
                            case "java.net.SocketException: Socket closed":
                                System.err.println("Server unreachable: " + exe + ", shuting down the connection to the Server");
                                disconnect();
                                break;
                            case "invalid stream header":
                                //Jemand sendet zu lange Strings
                                System.err.println("Steam header too long, received String too long??!?: " + exe);
                                disconnect();
                                break;
                            default:
                                System.err.println("Could not read incomming message: " + exe);
                                break;
                        }
                    }
                }
            }
        }

        public void stop() {
            run = false;
            System.out.println("Listener für Nachrichten vom Server erfolgreich gestoppt!");
        }
    }

    public void onStringReceived(String string) {
        //Decomprimiere den String
        String decrypted = Encryption.decrypt(string, clientPrivateKey);
        String decompressed = Compression.decompress(decrypted);
        try {
            //Parse den String in ein XML
            XML receivedXML = XML.openXML(decompressed);
            //Beispielvorgehen: Zeige den XML Baum in der Ausgabe an
            System.out.println("Command received: \n" + receivedXML.toString());
        } catch (Exception e) {
        }
    }

}