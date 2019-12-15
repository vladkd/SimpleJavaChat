import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Chat Server, by Vladimir Martynenko<br>
 * ISTE 200.01<br>
 * Homework 13<br>
 * Date: 2019-12-12<br>
 * ChatServer.java contains the Server portion of the simple chat.
 * 
 * @author: Vladimir Martynenko
 */

public class ChatServer {
  private static final short LISTENING_PORT = 12345;
  private JFrame frame;
  private int port;
  private Thread listeningThread;
  private SocketThread tempThread;
  private ArrayList<SocketThread> threadList = new ArrayList<SocketThread>();
  private JTextArea taMessages = new JTextArea();
  private JScrollPane scrollPane = new JScrollPane(taMessages);
  private JButton btnClose = new JButton("Close");

  /**
   * Generate and show the window
   */
  public ChatServer(int passedPort) {
    port = passedPort == 0 ? LISTENING_PORT : passedPort;
    listeningThread = new Thread() {
      public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
          while (true) {
            tempThread = new SocketThread(serverSocket.accept());
            synchronized (threadList) {threadList.add(tempThread);}
            tempThread.start();
          }
        } catch (IOException ioe) {
          JOptionPane.showMessageDialog(null, "Something went wrong while opening a server socket on port " + port);
          System.exit(1);
        } catch (IllegalArgumentException iae) {
          JOptionPane.showMessageDialog(null, "Valid port numbers should be between 0 and  65535, inclusive. Attempt to open socket on port "
          + port + " failed.");
          System.exit(2);
        } catch (SecurityException se) {
          JOptionPane.showMessageDialog(null, "Don't have a permission to open socket listening on port " + port);
          System.exit(3);
        }
      }
    };
    listeningThread.start();
    frame = new JFrame("Vladimir's Chat Server on port " + port);
    btnClose.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        synchronized (threadList) {
          Iterator<SocketThread> threadListIterator = threadList.iterator();
          while (threadListIterator.hasNext()){
            SocketThread socketThread = threadListIterator.next();
            socketThread.send("Server is shutting down.");
            try {socketThread.s.close();} catch (IOException e1) {}
            threadListIterator.remove();
          }
        }
        System.exit(0);
      }
    });
    taMessages.setEditable(false);
    frame.add(scrollPane, BorderLayout.CENTER);
    frame.add(btnClose, BorderLayout.SOUTH);
    frame.setSize(400, 400);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  /**
   * Main entry point
   */
  public static void main(String[] args) {
    int passedPort = 0;
    if (args.length > 0) {
      try {
        passedPort = Integer.parseInt(args[0]);
        if (passedPort < 1 || passedPort > 65535) {
          System.out.println(args[0] + " not a valid listening port number");
          passedPort = 0;
        }
      } catch (NumberFormatException nfe) {
        System.out.println(args[0] + " not a valid listening port number");
      }
    }
    final int portNumber = passedPort;
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new ChatServer(portNumber);
      }
    });
  }

  /**
   * Listening socket thread. (One per client).
   */
  private class SocketThread extends Thread {
    private Socket s;
    private String line;
    private BufferedWriter bw;

    SocketThread(Socket s) {
      super();
      this.s = s;
    }

    // Send the message provided to individual client
    private void send(String line) {
      synchronized (bw) {
        try {
          bw.write(line);
          bw.newLine();
          bw.flush();
        } catch (IOException ioe) {
          display("Something went wrong with " + s.getRemoteSocketAddress());
          synchronized (threadList) {threadList.remove(this);}
          try {this.s.close();} catch (IOException e) {}
          synchronized (threadList){
            for (SocketThread thread : threadList) {
              thread.send("Client Lost");
            }
          }
        }
      }
    }

    // Notify everybody when client disconnects
    private void die() {
      display("Client Disconnected");
      synchronized (threadList) {threadList.remove(this);}
      try {this.s.close();} catch (IOException ioe) {}
      synchronized (threadList){
        for (SocketThread thread : threadList) {
          thread.send("Client Disconnected");
        }
      }
    }

    // Main incomming message listening loop
    public void run() {
      try {
        bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
      } catch (IOException ioe) {
        die();
      }
      try (BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
        display("Client connected");
        synchronized (threadList){
          for (SocketThread thread : threadList) {
            if (thread != this)
              thread.send("New client connected");
          }
        }
        while ((line = br.readLine()) != null) {
          synchronized (threadList) {
            threadList.forEach(thread -> {
              if (!thread.equals(this))
                thread.send(line);
            });
          }
          display(line);
        }
        die();
      } catch (SocketException se) {
        die();
      } catch (IOException ioe) {
        ioe.printStackTrace();
        display("Something happened while reading from socket " + s.getRemoteSocketAddress());
        synchronized (threadList) {threadList.remove(this);}
        try {this.s.close();} catch (IOException e) {}
        synchronized (threadList){
          for (SocketThread thread : threadList) {
            thread.send("Client Lost");
          }
        }
      }
    }

    // Helper method to update messages list in the UI thread safe way
    private void display(final String s) {
      EventQueue.invokeLater(new Runnable() {
          //@Override
          public void run() {
              taMessages.append(s + '\n');
          }
      });
    }
  }
}