import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SendThread implements Runnable {
    private String host, fileName;

    public SendThread(String host, String fileName) {
        this.host = host;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        int port = 12346;

        Socket socket = null;
        ObjectOutputStream oos = null;
        try {
            socket = new Socket(host, port);
            oos = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (File file : Client.uploadedFiles) {
            if (file.getName().equals(fileName)) {
                try {
                    FileInputStream fileIn = new FileInputStream(file); 
                    byte[] bytes = new byte[(int)file.length()];
                    fileIn.read(bytes);
                    fileIn.close();



                    oos.writeObject(fileName);
                    oos.flush();
                    
                    oos.writeObject(bytes);
                    oos.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            if (oos != null) {
                oos.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {

        }
    }
    
}
