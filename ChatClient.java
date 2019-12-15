import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * Chat Client, by Vladimir Martynenko<br>
 * ISTE 200.01<br>
 * Homework 13<br>
 * Date: 2019-12-12<br>
 * ChatClient.java contains the Client portion of the simple chat.
 * 
 * @author: Vladimir Martynenko
 */

public class ChatClient {
  private static final Color ERROR = new Color(255, 182, 193);
  private static final Color NORMAL = UIManager.getColor("TextField.background");
  private  JFrame frame;
  private  JTextArea taMessages = new JTextArea(5, 20);
  private  JTextField tfMessage = new JTextField();
  private  JButton sendButton = new JButton("Send");
  private  JTextField tfName = new JTextField("Unnamed One");
  private  JTextField tfHost = new JTextField("127.0.0.1");
  private  JTextField tfPort = new JTextField("12345");
  private  JButton connectButton = new JButton("Connect");
  private  String name, host;
  private  int port;
  private  Socket socket;
  private  PrintWriter osw;
  private  SocketThread socketThread;

  /**
   * Performes all the changes when connect button clicked while disconnected
   */
  private void connect() {
    // If no name provided, fighlight end do nothing.
    if ((name = tfName.getText()).length() == 0) {
      tfName.setBackground(ERROR);
      tfName.requestFocus();
      return;
    }
    // If no host address provided, highlight and do nothing.
    if ((host = tfHost.getText()).length() == 0) {
      tfHost.setBackground(ERROR);
      tfHost.requestFocus();
      return;
    }
    // If no correct port number provided. Highlight and do nothing
    try {
      port = Integer.parseInt(tfPort.getText());
      if (port > 65535 || port < 1) {
        tfPort.setBackground(ERROR);
        tfPort.requestFocus();
        return;
      }
    } catch (NumberFormatException nfe) {
      tfPort.setBackground(ERROR);
      tfPort.requestFocus();
      return;
    }
    try {
      // Attempt to open connection
      socket = new Socket(host, port);
      osw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
      // Create and start incomming listening thread
      socketThread = new SocketThread(socket);
      socketThread.start();
      // Make UI changes
      tfName.setEnabled(false);
      tfHost.setEnabled(false);
      tfPort.setEnabled(false);
      taMessages.setEnabled(true);
      tfMessage.setEnabled(true);
      sendButton.setEnabled(true);
      connectButton.setText("Disconnect");
      // Activate send button when enter is pressed anywhere
      frame.getRootPane().setDefaultButton(sendButton);
      // move focus to the messege entry field
      tfMessage.requestFocus();
    } catch (IllegalArgumentException iae) {
      // Highlight if socket does not like the port number
      tfPort.setBackground(ERROR);
    } catch (UnknownHostException uhe) {
      // Hghlight if socket does not like the host address
      tfHost.setBackground(ERROR);
    } catch (IOException ioe) {
      // Other socket exceptions
      JOptionPane.showMessageDialog(null, "Something went wrong while trying to connect");
    }
  }

  /**
   * Called when connec button clicked while connected
   */
  private void disconnect() {
    // Close the socket
    try {socket.close();} catch (IOException e) {}
    // Make UI changes
    tfName.setEnabled(true);
    tfHost.setEnabled(true);
    tfPort.setEnabled(true);
    taMessages.setEnabled(false);
    tfMessage.setEnabled(false);
    sendButton.setEnabled(false);
    connectButton.setText("Connect");
    // Make Enter key press activate the Connect button
    frame.getRootPane().setDefaultButton(connectButton);
  }

  /**
   * Generates and adds sub panel to the container
   */
  private void addAddressPanel(Container container) {
    tfName.getDocument().addDocumentListener(new MyDocumentListener());
    tfName.addActionListener(new FieldActionListener());
    tfHost.getDocument().addDocumentListener(new MyDocumentListener());
    tfHost.addActionListener(new FieldActionListener());
    tfPort.getDocument().addDocumentListener(new MyDocumentListener());
    tfPort.addActionListener(new FieldActionListener());

    JLabel nameLabel = new JLabel("Name: ");
    nameLabel.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
    JLabel hostLabel = new JLabel("Host: ");
    hostLabel.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
    JLabel portLabel = new JLabel("Port: ");
    portLabel.setAlignmentX(JComponent.RIGHT_ALIGNMENT);

    JPanel labelsPanel = new JPanel();
    labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));
    labelsPanel.add(Box.createVerticalGlue());
    labelsPanel.add(nameLabel);
    labelsPanel.add(Box.createVerticalGlue());
    labelsPanel.add(hostLabel);
    labelsPanel.add(Box.createVerticalGlue());
    labelsPanel.add(portLabel);
    labelsPanel.add(Box.createVerticalGlue());

    JPanel fieldsPanel = new JPanel();
    fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
    fieldsPanel.add(tfName);
    fieldsPanel.add(tfHost);
    fieldsPanel.add(tfPort);

    JPanel addressPanel = new JPanel(new BorderLayout());
    addressPanel.add(labelsPanel, BorderLayout.WEST);
    addressPanel.add(fieldsPanel, BorderLayout.CENTER);
    container.add(addressPanel);
  }

  /**
   * Genereates and adds controls panel
   */
  private void addControlsPanel(Container container) {
    JPanel controlsPanel = new JPanel();
    controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
    JPanel sendPanel = new JPanel(new BorderLayout());

    tfMessage.setEnabled(false);
    sendPanel.add(tfMessage, BorderLayout.CENTER);

    sendButton.setEnabled(false);
    // Action listener for the send button. Sends the message message
    // over the net and displays on the message list. Moves focus to 
    // the messeg entry field.
    sendButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String line;
        if ((line = tfMessage.getText()).length() > 0) {
          taMessages.append("Me: " + line + '\n');
          tfMessage.setText("");
          osw.println(name + ": " + line);
          osw.flush();
        }
        tfMessage.requestFocus();
      }

    });
    sendPanel.add(sendButton, BorderLayout.EAST);
    controlsPanel.add(sendPanel);

    addAddressPanel(controlsPanel);
    connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    // Action listener for the connect button. calls ether 
    // connect or disconnect depending on the current state
    connectButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (connectButton.getText().equals("Connect")) {
          connect();
        } else {
          disconnect();
        }
      }
    });
    controlsPanel.add(connectButton);
    container.add(controlsPanel, BorderLayout.SOUTH);
  }

  /**
   * Generates and shows the complete UI
   */
  public void createAndShowGUI() {
    frame = new JFrame("Vladimir's chat client");

    taMessages.setEnabled(false);
    taMessages.setEditable(false);
    JScrollPane sp = new JScrollPane(taMessages);
    frame.add(sp, BorderLayout.CENTER);
    addControlsPanel(frame);

    frame.getRootPane().setDefaultButton(connectButton);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setMinimumSize(new Dimension(300, 200));
    frame.setSize(new Dimension(350, 300));
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  /**
   * Main entry point
   */
  public static void main(String[] args) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new ChatClient().createAndShowGUI();
      }
    });
  }

  /**
   * Action listener for the field elements. Implements the commection when enter key is pressed inside on of the text fields
   */
  private class FieldActionListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      connect();
    }
  }

  /**
   * Removes the error highlight from the text field once text inside is changed
   */
  private  class MyDocumentListener implements DocumentListener {
    public void changedUpdate(DocumentEvent e) {
      warn(e);
    }

    public void removeUpdate(DocumentEvent e) {
      warn(e);
    }

    public void insertUpdate(DocumentEvent e) {
      warn(e);
    }

    public void warn(DocumentEvent e) {
      Document doc = e.getDocument();
      if (doc == tfName.getDocument()) {
        tfName.setBackground(NORMAL);
      } else if (doc == tfHost.getDocument()) {
        tfHost.setBackground(NORMAL);
      } else if (doc == tfPort.getDocument()) {
        tfPort.setBackground(NORMAL);
      }
    }
  }

  /**
   * Thread to listen for incomming messages
   */
  private class SocketThread extends Thread{
    private Socket socket;
    public SocketThread(Socket s){
      super();
      this.socket = s;
    }

    public void run(){
      try(BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
        String line;
        while ((line = br.readLine()) != null)
          display(line);
        } catch (SocketException se){
        } catch (IOException ioe){
          ioe.printStackTrace();
        }
        disconnect();
    }

    /**
     * Heleper function to update the list of the messages in the UI-thread safe way
     */
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