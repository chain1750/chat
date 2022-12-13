package com.chaincat.chat.bio;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 聊天室客户端
 * @author Chain
 */
public class ChatClient {

    /**
     * 聊天室服务端地址
     */
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";

    /**
     * 聊天室服务端端口
     */
    private static final int DEFAULT_SERVER_PORT = 8888;

    /**
     * 退出聊天指令
     */
    private static final String QUIT = "quit";

    /**
     * Socket
     */
    private Socket socket;

    /**
     * 输入流
     */
    private BufferedReader reader;

    /**
     * 输出流
     */
    private BufferedWriter writer;

    /**
     * 发送消息
     * @param msg 消息
     * @throws IOException 异常
     */
    public void send(String msg) throws IOException {
        if (!socket.isOutputShutdown()) {
            writer.write(msg + "\n");
            writer.flush();
        }
    }

    /**
     * 接受消息
     * @return 消息
     * @throws IOException 异常
     */
    public String receive() throws IOException {
        String msg = null;
        if (!socket.isInputShutdown()) {
            msg = reader.readLine();
        }
        return msg;
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
            socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            ExecutorService executorService = Executors.newFixedThreadPool(1);
            executorService.execute(new UserInputHandler(this));

            String msg;
            while ((msg = receive()) != null) {
                System.out.println(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }
}
