package com.wujie.server;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.InetAddress;
import java.net.UnknownHostException;



public class ServerWindow extends Frame {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private FileServer s = new FileServer(12345);
    private Label label;

    public ServerWindow(String title) {
        super(title);
        label = new Label();
        add(label, BorderLayout.PAGE_START);
        label.setText("服务器端已经启动");
        this.addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent arg0) {
                // TODO Auto-generated method stub
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        try {
                            s.start();
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                    }
                }).start();
            }

            @Override
            public void windowIconified(WindowEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowDeiconified(WindowEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowDeactivated(WindowEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowClosing(WindowEvent arg0) {
                // TODO Auto-generated method stub
                s.quit();
                System.exit(0);
            }

            @Override
            public void windowClosed(WindowEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowActivated(WindowEvent arg0) {
                // TODO Auto-generated method stub

            }
        });
    }
    public static void main(String[] args) throws UnknownHostException {
        // TODO Auto-generated method stub
        InetAddress address = InetAddress.getLocalHost();
        ServerWindow window = new ServerWindow("文件上传服务端：" + address.getHostAddress());
        window.setSize(400, 300);
        window.setVisible(true);
    }

}
