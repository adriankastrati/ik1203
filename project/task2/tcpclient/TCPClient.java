package tcpclient;
import java.net.*;
import java.io.*;

public class TCPClient {
    private boolean shutdown;
    private Integer timeout;
    private Integer limit;

    public TCPClient(boolean shutdown, Integer timeout, Integer limit) {
        this.limit = limit;
        this.shutdown = shutdown;
        this.timeout = timeout;
    }

    public byte[] askServer(String hostname, int port, byte[] bytesToServer) throws IOException {

        Socket clientSocket = new Socket(hostname, port);

        send(clientSocket, bytesToServer);
        byte[] serverResponse = recv(clientSocket);


        return serverResponse;
    }

    public byte[] askServer(String hostname, int port) throws IOException{
        Socket clientSocket = new Socket(hostname, port);
        byte[] serverResponse = recv(clientSocket);

        return serverResponse;
    }

    private void send(Socket socket, byte[] bytesToServer) throws IOException {
        socket.getOutputStream().write(bytesToServer,0, bytesToServer.length);
        if(this.shutdown){
            socket.shutdownOutput();
        }
    }

    private byte[] recv(Socket socket) throws IOException {
        InputStream streamInFromServer = socket.getInputStream();
        byte[] serverResponseBuffer = new byte[1];
        ByteArrayOutputStream serverResponse = new ByteArrayOutputStream();
        
        if(this.timeout != null){
            socket.setSoTimeout(this.timeout);
        }
    
        try{
        int fromServerLength = streamInFromServer.read(serverResponseBuffer);

            if(this.limit != null){
                while (fromServerLength != -1 || serverResponse.size() <= limit) {
                    serverResponse.write(serverResponseBuffer,0,fromServerLength);
                    fromServerLength = streamInFromServer.read(serverResponseBuffer);
                }
            }else{
                while (fromServerLength != -1) {
                    serverResponse.write(serverResponseBuffer,0,fromServerLength);
                    fromServerLength = streamInFromServer.read(serverResponseBuffer);
                }
            }
        }catch(java.io.InterruptedIOException e){
            System.err.println("Time out: " + this.timeout );
        } 
        
        return serverResponse.toByteArray();
    }
}

