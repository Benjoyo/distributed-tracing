package tracing.backend.output.falcon;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;

/**
 * A WebSocket server to send data to a browser.
 */
public class WebSocketServer extends org.java_websocket.server.WebSocketServer {

    private CloseListener closeListener;
    private WebSocket webSocket;

    /**
     * Creates a server that listens on the specified port.
     * @param port the server port
     */
    public WebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        System.out.println("Websocket opened.");
        this.webSocket = webSocket;
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        System.out.println("Websocket closed.");
        webSocket.close();
        if (this.closeListener != null) {
            this.closeListener.onClose();
        }
    }

    public CloseListener getCloseListener() {
        return closeListener;
    }

    public void setCloseListener(CloseListener closeListener) {
        this.closeListener = closeListener;
    }

    interface CloseListener {
        void onClose();
    }

    /**
     * Client is not expected to send anything.
     */
    @Override
    public void onMessage(WebSocket webSocket, String s) { }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        System.out.println("Websocket error: " + e.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Websocket server starting...");
    }

    /**
     * Send the given String to the client via the WebSocket connection.
     * @param data the String to send
     */
    public void send(String data) {
        if (this.webSocket != null && webSocket.isOpen()) {
            this.webSocket.send(data);
        }
    }
}
