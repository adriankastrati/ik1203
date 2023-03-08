package tcpclient;
import java.net.*;
import java.io.*;

public class TCPClient {
    public TCPClient(){}

    public byte[] askServer(String hostname, int port, byte[] bytesToServer) throws IOException {

        Socket clientSocket = new Socket(hostname, port);

        send(clientSocket, bytesToServer);
        byte[] serverResponse = recv(clientSocket);

        clientSocket.close();

        return serverResponse;
    }

    public byte[] askServer(String hostname, int port) throws IOException{
        Socket clientSocket = new Socket(hostname, port);

        byte[] serverResponse = recv(clientSocket);
        clientSocket.close();

        return serverResponse;
    }

    private void send(Socket socket, byte[] bytesToServer) throws IOException {
        socket.getOutputStream().write(bytesToServer,0, bytesToServer.length);
    }

    private byte[] recv(Socket socket) throws IOException {
        InputStream streamInFromServer = socket.getInputStream();
        byte[] serverResponseBuffer = new byte[1];
        ByteArrayOutputStream serverResponse = new ByteArrayOutputStream();

        int fromServerLength = streamInFromServer.read(serverResponseBuffer);
        while (fromServerLength != -1) {
            serverResponse.write(serverResponseBuffer,0,fromServerLength);
            fromServerLength = streamInFromServer.read(serverResponseBuffer);
        }

        return serverResponse.toByteArray();
    }
}


