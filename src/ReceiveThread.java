import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveThread implements Runnable {

    @Override
    public void run() {
        int port = 12346;

        ServerSocket serverSocket;
        Socket socket;
        ObjectInputStream ois;
        try {
            serverSocket = new ServerSocket(port);
            socket = serverSocket.accept();
            ois = new ObjectInputStream(socket.getInputStream());

            String fileName = (String) ois.readObject();
            byte[] bytes = (byte[]) ois.readObject();

            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                fos.write(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        
    }
    
}
