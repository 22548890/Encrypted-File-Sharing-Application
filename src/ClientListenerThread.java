import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;


/**
 * This class is for the thread that waits for messages
 */
public class ClientListenerThread implements Runnable {

    private String username;

    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    private JFrame frame;
    private JTextArea enteredText;
    private DefaultListModel<String> listModelUsers;
    private DefaultListModel<String> listModelRooms;


    /**
     * Constructor sets up useful properties
     * 
     * @param socket      for connection to server
     * @param ois         for receiving messages
     * @param oos         for sending messages
     * @param enteredText for displaying received messages
     * @param listModel   for displaying list of users
     * @param frame       for displaying everything
     */
    public ClientListenerThread(String username, Socket socket, ObjectInputStream ois, ObjectOutputStream oos, JFrame frame,
            JTextArea enteredText, DefaultListModel<String> listModelUsers, DefaultListModel<String> listModelRooms) {
        this.username = username;
        this.socket = socket;
        this.ois = ois;
        this.oos = oos;
        this.frame = frame;
        this.enteredText = enteredText;
        this.listModelUsers = listModelUsers;
        this.listModelRooms = listModelRooms;
    }


    public String search(String str) {
        ArrayList<File> files = Client.uploadedFiles;
        boolean[] included = new boolean[files.size()];
        for (int i = 0; i < included.length; i++) {
            included[i] = false;
        }
        String result = "/results ";


        // add exact matches
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).getName().toLowerCase().equals(str.toLowerCase())) {
                result += files.get(i) + "@";
                included[i] = true;
            }
        }
        
        if (str.length() < 3) {
            // substring
            for (int i = 0; i < files.size(); i++) { // might be smaller than 3
                if (files.get(i).getName().toLowerCase().contains(str.toLowerCase()) && included[i] == false) {
                    result += files.get(i).getName() + "@";
                    included[i] = true;
                }
            }
        } else {
            // add smaller substring matches of both sides
            for (int i = str.length(); i >= 3; i--) {
                for (int j = 0; j < files.size(); j++) {
                    for (int k = 0; k <= str.length() - i; k++) {
                        String substr = str.substring(k, i + k);
                        if (files.get(j).getName().toLowerCase().contains(substr.toLowerCase()) && included[j] == false) {
                            result += files.get(j).getName() + "@";
                            included[j] = true;
                            System.out.println(files.get(j).getName().toLowerCase());
                            System.out.println(substr.toLowerCase());
                        }
                    }
                }
            }
        }
        return result;
    }


    /**
     * Handles the output of received messages
     * 
     * @param message The object received form server
     */
    public void handleMessage(Message message) {
        if (message.from().equals("SERVER")) {
            if (message.text().endsWith("has entered the chat!")) {
                listModelUsers.addElement(message.text().split(" has entered the chat", 2)[0]);
            } else if (message.text().endsWith("has entered the room!")) {
                if (message.text().split(" has entered the room!", 2)[0].equals(username)) {
                    frame.setTitle("Client: " + username + "    |    Room: " + Client.roomToJoin);
                    listModelUsers.clear();
                    listModelUsers.addElement("Online Users:");
                    ArrayList<String> usernames = null;
                    try {
                        usernames = (ArrayList<String>) ois.readObject();
                    } catch (Exception e) {
                        closeEverything();
                    }
                    for (String name : usernames) {
                        listModelUsers.addElement(name);
                    }
                } else {
                    listModelUsers.addElement(message.text().split(" has entered the room", 2)[0]);
                }
            } else if (message.text().endsWith("has left the chat!")) {
                listModelUsers.removeElement(message.text().split(" has left the chat!", 2)[0]);
            } else if (message.text().endsWith("has left the room!")) {
                listModelUsers.removeElement(message.text().split(" has left the room!", 2)[0]);
            } else if (message.text().startsWith("Room created - ")) {
                listModelRooms.addElement(message.text().split(" - ", 2)[1]);
            }
        } else if (message.text().startsWith("/search")) {
            if (message.from().equals(username)) {
                return;
            }
            String result = search(message.text().split("/search ", 2)[1]);
            if (result.equals("/results ")) {
                return;
            }
            try {
                oos.writeObject(new Message(result, username, message.from()));
                oos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        } else if (message.text().startsWith("/results ")) {
            String text = message.text().split("/results ")[1];
            String[] files = text.split("@");
            for(String file : files) {
                Client.searchFiles.add(file);
                Client.searchNames.add(message.from());
                Client.searchNum++;

                String line = Client.searchNum + " - " + file + "\n";
                enteredText.insert(line, enteredText.getText().length());
            }
            return;
        } else if (message.text().startsWith("/download ")) {
            String parts[] = message.text().split(" ");
            String host = parts[1];
            String filename = parts[2];
            Thread thread = new Thread(new SendThread(host, filename));
            thread.start();
            return;
        }
        String text = message.from() + ": " + message.text();
        enteredText.insert(text + "\n", enteredText.getText().length());
    }


    /**
     * Closes socket and streams neatly and exits
     */
    public void closeEverything() {
        try {
            if (ois != null) {
                ois.close();
            }
        } catch (IOException e) {
        }

        try {
            if (oos != null) {
                oos.close();
            }
        } catch (IOException e) {
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
        }

        enteredText.insert("SERVER: Shut down" + "\n",
                enteredText.getText().length());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        System.exit(0);
    }


    /**
     * Thread execution that waits for messages while connected to server
     */
    @Override
    public void run() {
        if (socket.isConnected()) {
            try {
                ArrayList<String> usernames = (ArrayList<String>) ois.readObject();
                for (String name : usernames) {
                    listModelUsers.addElement(name);
                }

                ArrayList<String> rooms = (ArrayList<String>) ois.readObject();
                for (String room : rooms) {
                    listModelRooms.addElement(room);
                }
            } catch (ClassNotFoundException e) {
                closeEverything();
            } catch (IOException e) {
                closeEverything();
            }
        }

        while (socket.isConnected()) {
            try {
                handleMessage((Message) ois.readObject());
            } catch (Exception e) {
                closeEverything();
            }
        }
    }
}