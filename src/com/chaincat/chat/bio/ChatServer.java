package com.chaincat.chat.bio;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 聊天室服务端
 * @author Chain
 */
public class ChatServer {

    /**
     * 默认端口
     */
    private static final int DEFAULT_PORT = 8888;

    /**
     * 退出聊天指令
     */
    private static final String QUIT = "quit";

    /**
     * ServerSocket
     */
    private ServerSocket serverSocket;

    /**
     * 保存连接的客户端字符输出流
     */
    private final Map<Integer, Writer> connectedClients;

    /**
     * 线程池
     */
    private final ExecutorService executorService;

    /**
     * 初始化
     */
    public ChatServer() {
        connectedClients = new ConcurrentHashMap<>(16);
        executorService = Executors.newFixedThreadPool(10);
    }

    /**
     * 添加客户端
     * @param socket Socket
     * @throws IOException 异常
     */
    public void addClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            connectedClients.put(port, writer);
            System.out.println("客户端【" + port + "】已连接");
        }
    }

    /**
     * 删除客户端
     * @param socket Socket
     * @throws IOException 异常
     */
    public void removeClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            if (connectedClients.containsKey(port)) {
                connectedClients.get(port).close();
            }
            connectedClients.remove(port);
            System.out.println("客户端【" + port + "】已断开");
        }
    }

    /**
     * 转发消息
     * @param socket Socket
     * @param msg 消息
     * @throws IOException 异常
     */
    public void forwardMessage(Socket socket, String msg) throws IOException {
        for (Map.Entry<Integer, Writer> entry : connectedClients.entrySet()) {
            int port = entry.getKey();
            Writer writer = entry.getValue();
            if (port != socket.getPort()) {
                writer.write(msg);
                writer.flush();
            }
        }
    }

    /**
     * 判断是否输入了退出指令
     * @param msg 消息
     * @return 是否退出
     */
    public boolean inputQuit(String msg) {
        return QUIT.equals(msg);
    }

    /**
     * 启动方法
     */
    private void start() {
        try {
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("启动服务器，监听端口：" + DEFAULT_PORT);

            // 死循环等待客户端连接
            while (true) {
                Socket socket = serverSocket.accept();
                executorService.execute(new ChatHandler(this, socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }
}
