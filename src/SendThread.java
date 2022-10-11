import java.io.*;
import java.net.Socket;
import javax.swing.JProgressBar;

public class SendThread implements Runnable {
    private String host, fileName;
    // private boolean isPausedUploaded;
    private JProgressBar progressBar;

    public SendThread(String host, String fileName, JProgressBar progressBar) {
        this.host = host;
        this.fileName = fileName;
        this.progressBar = progressBar;

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
                    int bytes = 0;
                    byte[] buffer = new byte[4 * 1024];
                    oos.writeObject(file.getName());
                    oos.writeObject(file.length());
                    int fileSize = (int) file.length();
                    int sent = 0;
                    // progressBar.setIndeterminate(true);
                    while ((bytes = fileIn.read(buffer)) != -1) {
                        // calculate sending amount remaining and add to progress bar
                        sent += bytes;
                        progressBar.setValue((int) (sent * 100 / fileSize));

                        try {
                            // if file size is > 5MB, sleep for 1ms
                            if (fileSize > 5 * 1024 * 1024) {
                                Thread.sleep(0);
                            } else {
                                Thread.sleep(10);
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        oos.write(buffer, 0, bytes);
                    }
                    oos.flush();
                    fileIn.close();
                    // if clientliostener is paused, wait until false

                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    progressBar.setValue(0);
                    progressBar.setIndeterminate(false);

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
