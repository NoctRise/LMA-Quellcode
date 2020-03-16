import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
public class Server {
    public static void main(String[] args) {

        try {
            String zeitString = "Zeit in Sekunden";
            String energieString = "Energie-Effizienz in km/h";
            String distanzString = "Distanz in Meter";
            String stillstandString = "Stillstandwert";

            System.out.println("Server start");
            ServerSocket serverSocket = new ServerSocket(5556);
            Socket clientSocket = serverSocket.accept();

            OutputStream socketOutStr = clientSocket.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socketOutStr);
            BufferedWriter bw = new BufferedWriter(outputStreamWriter);

            InputStream socketInpStr = clientSocket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(socketInpStr);
            BufferedReader br = new BufferedReader(inputStreamReader);

            String nachricht = "";
            FileWriter fw = null;

            while (!(nachricht = br.readLine()).equals("stop")) {
                System.out.println(nachricht);
                bw.write("clear");
                bw.newLine();
                bw.flush();
                //Speichert die Datei mit dem Namen der Strategie ab
                File f = new File(nachricht.substring(0, nachricht.indexOf(" ")) + ". txt");
                fw = new FileWriter(f, true);

                bw.newLine();
                fw.append("\n");
                fw.append(nachricht);

                fw.flush();
            }

            fw.close();
            bw.close();
            br.close();
            clientSocket.close();
            serverSocket.close();

        } catch (Exception e) {
            System.out.print(e);
        }
    }
}

