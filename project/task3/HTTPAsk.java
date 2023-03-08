import java.net.*;
import java.net.http.HttpResponse;
import java.io.*;


public class HTTPAsk {
    static boolean shutdown = false;             // True if client should shutdown connection
    static Integer timeout = null;			     // Max time to wait for data from server (null if no limit)
	static Integer limit = null;			     // Max no. of bytes to receive from server (null if no limit)
	static String hostname = null;			     // Domain name of server
	static String string = null;			     // Domain name of server
    static Integer port = null;					     // Server port number
	static Integer requestStatus = 0;
    public static void main(String[] args) {
		try {
			ServerSocket HTTPSocket = new ServerSocket(Integer.parseInt(args[0]));
			
			while(true){
				String response = "";
				Socket socket = HTTPSocket.accept();
				
				parseHTTPRequest(socket);
				print();

				try{
					if(requestStatus != 400 && requestStatus != 404){
						TCPClient tcpClient = new TCPClient(shutdown, timeout, limit);
						if (string == null){
							response = new String(tcpClient.askServer(hostname, port));
						}else{
							response = new String(tcpClient.askServer(hostname, port, string.getBytes()));
						}
						requestStatus = 200;
					}
				}catch(IOException e){
					reset();
					requestStatus = 404;
				}

				outputHTTPResponse(response, socket);
			}
		}catch(IOException ex){}
    }

	private static void reset(){
		shutdown = false; 
		timeout = null;			
		limit = null;			  
	   	hostname = null;			
	   	string = null;			  
		port = null;					    
		requestStatus = 0;
	}

	private static void outputHTTPResponse(String toDisplay, Socket socket) throws IOException{
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

    public static void parseHTTPRequest(Socket socket) throws IOException {
		InputStream streamInFromServer = socket.getInputStream();
        byte[] serverResponseBuffer = new byte[1];
        ByteArrayOutputStream serverResponse = new ByteArrayOutputStream();

		int fromServerLength = streamInFromServer.read(serverResponseBuffer);
		while (serverResponseBuffer[0] != '\n' && fromServerLength != -1) {
			serverResponse.write(serverResponseBuffer,0,fromServerLength);
			streamInFromServer.read(serverResponseBuffer);
		}
		
		String[] query = serverResponse.toString().split("[?]",2);
		reset();
		
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

	private static void print(){
		System.out.println("\nRequestStatus: "+ "'" + + requestStatus+ "'" +"\nShutdown " + "'" +shutdown + "'" +
		"\nTimeout: " + "'" +timeout + "'" + "\nLimit: " + "'" +limit + "'" + 
		"\nHostname: " + "'" +hostname + "'" + "\nPort: "+ "'" +port + "'" + "\nString: "+ "'" +string + "'\n");
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