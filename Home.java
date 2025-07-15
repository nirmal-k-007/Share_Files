import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Home extends Frame{
    
    private Button send,receive;


    public Home(){
        setTitle("ShareFiles");
        setSize(500, 600);
        setLayout(null);
        send = new Button("Send");
        send.setBounds(150,250,200,40);
        add(send);
        receive = new Button("Receive");
        receive.setBounds(150,310,200,40);
        add(receive);
        setVisible(true);
        
        send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                // showNotification("NOTIFICATION", "BUTTON CLICKED!");
                dispose();
                new Send();
            }
        });

        receive.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                dispose();
                new Receive();
            }
        });
        
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    private void showNotification(String title, String message) {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray not supported");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("icon.png"); // optional: tray icon

            TrayIcon trayIcon = new TrayIcon(image, "Java Notification");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("Tray Icon");

            tray.add(trayIcon);
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);

            // Optional: remove icon after showing message
            Thread.sleep(3000); // wait so user sees the icon briefly
            tray.remove(trayIcon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        new Home();
    }
}
