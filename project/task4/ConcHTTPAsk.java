import java.net.*;
import java.io.*;


public class ConcHTTPAsk {
	 public static void main(String[] args) {
			try {
				ServerSocket HTTPSocket = new ServerSocket(Integer.parseInt(args[0]));
				while(true){
				Socket socket = HTTPSocket.accept();
				(new Thread(new MyRunnable(socket))).start();}
			}catch(IOException ex){
				System.out.println("error");
			}
    }
}

class MyRunnable implements Runnable{
    private boolean shutdown = false;             // True if client should shutdown connection
    private Integer timeout = null;			     // Max time to wait for data from server (null if no limit)
	private Integer limit = null;			     // Max no. of bytes to receive from server (null if no limit)
	private String hostname = null;			     // Domain name of server
	private String string = null;			     // Domain name of server
    private Integer port = null;					     // Server port number
	private Integer requestStatus = 0;
	private Socket socket;
	private String toDisplay = null;
	
	public MyRunnable(Socket socket){
		this.socket = socket;
	}

	public void run(){
		try{
			parseHTTPRequest(socket);
			if(requestStatus != 400 && requestStatus != 404){
				TCPClient tcpClient = new TCPClient(shutdown, timeout, limit);
				if (string == null){
					toDisplay = new String(tcpClient.askServer(hostname, port));
				}else{
					toDisplay = new String(tcpClient.askServer(hostname, port, string.getBytes()));
				}
				requestStatus = 200;
			}
		}catch(IOException e){
			requestStatus = 404;
		}
		try{
			outputHTTPResponse(socket);
		}catch(IOException e){}
	}



	private void outputHTTPResponse(Socket socket) throws IOException{
		String HTTPResponse = "";

		if(requestStatus == 404 ){
			HTTPResponse = "HTTP/1.1 404 Not Found\r\n" +
				"Content-Type: text/html\r\n" +
				"Content-Length: 0\r\n\r\n"
			;

		}else if(requestStatus == 400){	
			HTTPResponse = "HTTP/1.1 400 Bad Request\r\n" +
				"Content-Type: text/html\r\n" +
				"Content-Length: 0\r\n\r\n"
			;
		}else{
			HTTPResponse = "HTTP/1.1 200 OK\r\n" + 
			"Content-Type: text/html\r\n" + 
			"Content-Length: "+ toDisplay.length() + "\r\n"
			+ "\r\n" + 
			toDisplay;		

		}

		socket.getOutputStream().write(HTTPResponse.getBytes());

	}

    private void parseHTTPRequest(Socket socket) throws IOException {
		InputStream streamInFromServer = socket.getInputStream();
        byte[] serverResponseBuffer = new byte[1];
        ByteArrayOutputStream serverResponse = new ByteArrayOutputStream();

		int fromServerLength = streamInFromServer.read(serverResponseBuffer);
		while (serverResponseBuffer[0] != '\n' && fromServerLength != -1) {
			serverResponse.write(serverResponseBuffer,0,fromServerLength);
			streamInFromServer.read(serverResponseBuffer);
		}
		
		String[] query = serverResponse.toString().split("[?]",2);
		
		if(!query[0].contains("/ask") || !query[1].contains("HTTP/1.1") || !query[0].contains("GET")){
			requestStatus = 400;
			

		}else if (query[0].contains("GET /ask")){
			for (String str : query[1].split(" ", 2)[0].split("&",7)) {
				String[] parts = str.split("=",2);
				if(parts[0].equals("shutdown")){
					shutdown = Boolean.parseBoolean(parts[1]);
				}else if(parts[0].equals("hostname")){
					hostname = parts[1];
				}else if(parts[0].equals("port")){
					port = Integer.parseInt(parts[1]);
				}else if(parts[0].equals("string")){
					string = parts[1];
				}else if(parts[0].equals("limit")){
					limit = Integer.parseInt(parts[1]);
				}else if(parts[0].equals("timeout")){
					timeout = Integer.parseInt(parts[1]);
				}
			}
			if(port == null || hostname == null){
				requestStatus = 400;
			}
		}else{
			requestStatus = 404;
		}
	}
}

class TCPClient {
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
                while (fromServerLength != -1 && serverResponse.size() <= limit) 
				{
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
