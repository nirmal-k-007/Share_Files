import java.awt.*;
import java.awt.event.*;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;


public class Receive extends JFrame {

    Label serverStatus, clientStatus, ip, total, current, locLabel, nameLabel, progressLabel, progress, key;
    JProgressBar progressBar;
    TextField locField;
    Button saveLoc;
    ServerSocket serverSocket;
    Socket socket;
    DataInputStream dis;
    DataOutputStream dos;
    InetAddress localHost;
    boolean flag;
    boolean broadcastFlag = true;

    public Receive() {
        flag = false;
        setTitle("ShareFiles");
        setSize(500, 600);
        setLayout(new FlowLayout());

        locLabel = new Label("Save Location");
        add(locLabel);
        locField = new TextField(20);
        add(locField);
        locField.setEnabled(false);
        saveLoc = new Button("Choose Location");
        add(saveLoc);
        serverStatus = new Label("Server Status : Waiting...");
        add(serverStatus);
        clientStatus = new Label("Client Status : Not Resolved!");
        add(clientStatus);
        ip = new Label("IP Address : Not Resolved!");
        add(ip);
        total = new Label("Total : -");
        add(total);
        total.setVisible(false);
        current = new Label("Current : -");
        add(current);
        current.setVisible(false);
        key = new Label("Key : ");
        add(key);
        key.setVisible(false);
        nameLabel = new Label("File Name : ");
        add(nameLabel);
        nameLabel.setVisible(false);
        progressLabel = new Label("Progress : ");
        add(progressLabel);
        progressLabel.setVisible(false);
        // progress = new Label();
        // add(progress);
        // progress.setVisible(false);
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(400, 25));
        progressBar.setStringPainted(true); // shows % text
        add(progressBar);
        progressBar.setVisible(false);

        try {
            localHost = InetAddress.getLocalHost();
            ip.setText("IP Address : " + localHost.getHostAddress());
        } catch (UnknownHostException e) {
            System.out.println("Some Error!!");
        }

        

        Thread cli = new Thread(new Runnable() {
            public void run() {
                try {
                    socket = serverSocket.accept();
                    dis = new DataInputStream(socket.getInputStream());
                    dos = new DataOutputStream(socket.getOutputStream());

                    

                    String saveDir = locField.getText().trim();
                    SwingUtilities.invokeLater(() -> {
                        clientStatus.setText("Client Status : Client Connected!");
                        saveLoc.setEnabled(false);

                        total.setVisible(true);
                        current.setVisible(true);
                        nameLabel.setVisible(true);
                        progressLabel.setVisible(true);
                        // progress.setVisible(true);
                        progressBar.setVisible(true);
                        key.setVisible(true);
                    });
                    broadcastFlag = false;

                    // DH key exchange (receiver starts)
                SecretKey aesKey = performDhAndDeriveAesKeyAsReceiver(dis, dos);
                System.out.println("Derived AES key (receiver): " + bytesToHex(aesKey.getEncoded()));
                key.setText("Key : " + bytesToHex(aesKey.getEncoded()));


                    // receive no of files
                    int n = Integer.parseInt(dis.readUTF());

                    System.out.println("n = " + n);

                    SwingUtilities.invokeLater(() -> {
                        total.setText("Total : " + n);
                        current.setText("Current : 0/" + n);
                    });

                    for (int i = 0; i < n; i++) {

                        int curr = i;
                        SwingUtilities.invokeLater(() -> {
                            current.setText("Current : " + (curr + 1) + "/" + n);
                        });
                        // receive file name
                        String fileName = dis.readUTF();

                        System.out.println("name = " + fileName);

                        System.out.println("Name : " + fileName);
                        SwingUtilities.invokeLater(() -> {
                            nameLabel.setText("File Name : " + sliceFirst30(fileName) + "...");
                        });
                        File file = new File(saveDir, fileName);

                        // receive file size
                        long size = dis.readLong();

                        System.out.println("Size : " + size);

                        // receive file bytes
                        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
                            byte[] buffer = new byte[8192];
                            long remaining = size;
                            int bytesRead;
                            long received = 0;
                            int lastPercent = 0;

                            while (remaining > 0 && (bytesRead = dis.read(buffer, 0,
                                    (int) Math.min(buffer.length, remaining))) != -1) {
                                bos.write(buffer, 0, bytesRead);
                                remaining -= bytesRead;
                                received += bytesRead;

                                int percent = (int) ((received * 100) / size);
                                if (percent != lastPercent && percent % 1 == 0) {
                                    lastPercent = percent;
                                    // progress.setText(prog(percent) + percent + "%");
                                    SwingUtilities.invokeLater(() -> {
                                        progressBar.setValue(percent);
                                    });
                                }
                            }
                            bos.flush();
                        }

                    }
                    System.out.println("Done!");
                    saveLoc.setEnabled(true);

                } catch (Exception e) {
                    System.out.println("Error Starting Server !!");
                }
            }
        });
        Thread broadCast = new Thread(new Runnable() {
            public void run(){
                DatagramSocket skt;
                try {
                    skt = new DatagramSocket();
                    skt.setBroadcast(true);
                    String message = localHost.getHostAddress();
                    byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
                    InetAddress broadCastAddress = InetAddress.getByName("255.255.255.255");
                    int port = 5000;

                    while(broadcastFlag){
                        DatagramPacket pkt = new DatagramPacket(buffer, buffer.length, broadCastAddress, port);
                        skt.send(pkt);
                        System.out.println("Broadcast Sent!!");
                        Thread.sleep(2000);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                
            }
        });

        saveLoc.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Select a Folder");

                int result = chooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFolder = chooser.getSelectedFile();
                    System.out.println("Selected folder: " + selectedFolder.getAbsolutePath());
                    locField.setText(selectedFolder.getAbsolutePath());

                    if (flag) {
                        locField.setText(selectedFolder.getAbsolutePath());
                    } else {
                        flag = true;
                        locField.setText(selectedFolder.getAbsolutePath());
                        try {
                            serverSocket = new ServerSocket(12345);
                            serverStatus.setText("Server Status : Server Running!");
                            clientStatus.setText("Client Status : Waiting for Client to connect!");
                        } catch (Exception ex) {
                            serverStatus.setText("Server Status : Failed to Start Server...");
                        }
                        broadCast.start();
                        cli.start();
                    }
                }
            });
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                } catch (IOException ignored) {
                }
                try {
                    if (socket != null && socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ignored) {
                }

                dispose();
            }
        });

        setVisible(true);
    }

    private SecretKey performDhAndDeriveAesKeyAsReceiver(DataInputStream in, DataOutputStream out) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        byte[] receiverPubEnc = kp.getPublic().getEncoded();
        out.writeInt(receiverPubEnc.length);
        out.write(receiverPubEnc);
        out.flush();

        int len = in.readInt();
        byte[] senderPubEnc = new byte[len];
        in.readFully(senderPubEnc);

        KeyFactory keyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(senderPubEnc);
        PublicKey senderPubKey = keyFac.generatePublic(x509KeySpec);

        KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
        keyAgree.init(kp.getPrivate());
        keyAgree.doPhase(senderPubKey, true);
        byte[] sharedSecret = keyAgree.generateSecret();

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(sharedSecret);
        byte[] aesKeyBytes = Arrays.copyOf(keyBytes, 16);
        return new SecretKeySpec(aesKeyBytes, "AES");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static String prog(int percent) {
        String s = new String();
        int stat = 100;
        int p = (percent * stat) / 100;
        s = "|".repeat(p);
        s += (".".repeat(stat - p));
        return s;
    }

    public static String sliceFirst30(String input) {
        if (input == null)
            return ""; // handle null input safely
        return input.length() > 30 ? input.substring(0, 30) : input;
    }

}
