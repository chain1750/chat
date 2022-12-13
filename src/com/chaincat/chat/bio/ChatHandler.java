package com.chaincat.chat.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * 服务端任务
 * @author Chain
 */
public class ChatHandler implements Runnable {

    /**
     * 服务端
     */
    private final ChatServer server;

    /**
     * Socket
     */
    private final Socket socket;

    /**
     * 初始化
     * @param server 服务端
     * @param socket Socket
     */
    public ChatHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            server.addClient(socket);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg;
            while ((msg = reader.readLine()) != null) {
                String fwgMsg = "客户端【" + socket.getPort() + "】：" + msg + "\n";
                System.out.print(fwgMsg);
                server.forwardMessage(socket, fwgMsg);
                if (server.inputQuit(msg)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                server.removeClient(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
