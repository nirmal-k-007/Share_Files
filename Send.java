import java.awt.*;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.*;

public class Send extends JFrame {
    Button select, send;
    TextField ip_text;
    Label ip_label, nameLabel, progressLabel, progress, total, current;
    JProgressBar progressBar;
    Socket socket;
    DataInputStream dis;
    DataOutputStream dos;

    ArrayList<FileData> files = new ArrayList<>();

    public Send() {
        setTitle("ShareFiles");
        setSize(500, 600);
        setLayout(new FlowLayout());

        // === Button: Choose Files ===
        select = new Button("Choose Files");
        add(select);

        select.addActionListener(e -> openFileDialog());

        // === Label: IP Address ===
        ip_label = new Label("IP Address:");
        add(ip_label);

        // === TextField: IP Address ===
        ip_text = new TextField("192.168.",10);
        add(ip_text);
        ip_text.setEnabled(false);

        // === Button: Send ===
        send = new Button("Send");
        add(send);
        send.setEnabled(false);

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
        total = new Label("Total : -");
        add(total);
        total.setVisible(false);
        current = new Label("Current : -");
        add(current);
        current.setVisible(false);

        send.addActionListener(e -> {
    SwingWorker<Void, Void> worker = new SwingWorker<>() {
        @Override
        protected Void doInBackground() throws Exception {
            try {
                socket = new Socket(ip_text.getText(), 12345);
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                System.out.println("Connected to server: " + ip_text.getText());

                SwingUtilities.invokeLater(() -> {
                    nameLabel.setVisible(true);
                    progressLabel.setVisible(true);
                    progressBar.setVisible(true);
                    total.setVisible(true);
                    current.setVisible(true);
                });

                int n = files.size();
                SwingUtilities.invokeLater(() -> {
                    total.setText("Total : " + n);
                    current.setText("Current : 0/" + n);
                });

                dos.writeInt(n);

                for (int i = 0; i < n; i++) {
                    FileData f = files.get(i);
                    int curr = i;

                    SwingUtilities.invokeLater(() -> {
                        current.setText("Current : " + (curr + 1) + "/" + n);
                        nameLabel.setText("File Name : " + sliceFirst30(f.getFilename()) + "...");
                    });

                    dos.writeUTF(f.getFilename());
                    dos.writeLong(f.getFileSize());

                    try (
                        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f.getFileBytes()))
                    ) {
                        long fileSize = f.getFileSize();
                        byte[] buffer = new byte[8192];
                        long sent = 0;
                        int lastPercent = -1;

                        int bytesRead;
                        while ((bytesRead = bis.read(buffer)) != -1) {
                            dos.write(buffer, 0, bytesRead);
                            sent += bytesRead;

                            int percent = (int) ((sent * 100L) / fileSize);
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                SwingUtilities.invokeLater(() -> progressBar.setValue(percent));
                            }
                        }

                        dos.flush();
                        System.out.println("Sent file: " + f.getFilename());

                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(Send.this, "Failed to connect to server", "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ignored) {}
            }
            return null;
        }
    };

    worker.execute();
});


        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ignored) {
                }
                dispose();
            }
        });

        setVisible(true);
    }

    private void openFileDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Files");
        fileChooser.setMultiSelectionEnabled(true); // Allow multiple file selection
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY); // Files only

        int result = fileChooser.showOpenDialog(this); // `this` refers to the parent component (e.g., JFrame)

        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();

            if (selectedFiles.length > 0) {
                files.clear();
                for (File file : selectedFiles) {
                    files.add(new FileData(file.getName(), file.length(), file));
                }

                ip_text.setEnabled(true);
                send.setEnabled(true); // Enable send button once files are selected
            } else {
                System.out.println("No files selected.");
            }
        } else {
            System.out.println("File selection canceled.");
        }
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
