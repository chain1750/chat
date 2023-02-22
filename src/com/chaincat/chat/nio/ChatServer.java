package com.chaincat.chat.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;

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
     * 定义buffer大小
     */
    private static final int BUFFER_SIZE = 1024;

    private Selector selector;

    private final ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    private final int port;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
    }

    @SuppressWarnings("all")
    private void start() {
        try {
            // 创建一个ServerSocketChannel，并设置为非阻塞
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            // 设置ServerSocket监听的端口
            serverSocketChannel.socket().bind(new InetSocketAddress(port));

            // 创建Selector
            selector = Selector.open();

            // 将ServerSocketChannel注册到Selector，注册事件为接受
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器，监听端口：" + port);

            while (true) {
                // 该方法会阻塞，直到至少有一个Channel发生了对应的注册事件
                selector.select();

                // 获取到所有事件对应Channel的SelectionKey
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                // 遍历处理发生的事件
                for (SelectionKey selectionKey : selectionKeys) {
                    handles(selectionKey);
                }

                // 清除已处理过的事件
                selectionKeys.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(selector);
        }
    }

    private void handles(SelectionKey selectionKey) throws IOException {
        // ACCEPT事件：和客户端建立连接
        if (selectionKey.isAcceptable()) {
            // 获取服务器Channel
            ServerSocketChannel serverChannel = (ServerSocketChannel) selectionKey.channel();
            // 获取客户端Channel，并设置为非阻塞
            SocketChannel clientChannel = serverChannel.accept();
            clientChannel.configureBlocking(false);
            // 将客户端Channel注册到Selector，注册事件为读取
            clientChannel.register(selector, SelectionKey.OP_READ);
            System.out.println(clientName(clientChannel) + "已连接");
        }
        // READ事件：客户端发送消息
        else if (selectionKey.isReadable()) {
            // 获取客户端Channel
            SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
            String fwdMsg = receive(clientChannel);

            // 如果读取信息为空，则表示客户端异常
            if (fwdMsg.isEmpty()) {
                // 让Selector取消监听该Channel
                selectionKey.cancel();
                selector.wakeup();
            } else {
                if (inputQuit(fwdMsg)) {
                    selectionKey.cancel();
                    selector.wakeup();
                    System.out.println(clientName(clientChannel) + "已断开");
                    return;
                }
                forwardMessage(clientChannel, fwdMsg);
            }
        }
    }

    @SuppressWarnings("all")
    private String receive(SocketChannel socketChannel) throws IOException {
        rBuffer.clear();
        // 将信息遍历写入Buffer
        while (socketChannel.read(rBuffer) > 0);
        // 转为读模式，并获取信息
        rBuffer.flip();
        return String.valueOf(StandardCharsets.UTF_8.decode(rBuffer));
    }

    private void forwardMessage(SocketChannel socketChannel, String fwdMsg) throws IOException {
        // 获取所有注册的Channel对应的SelectionKey
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            Channel channel = key.channel();
            // 跳过ServerSocketChannel，即服务器Channel
            if (channel instanceof ServerSocketChannel) {
                continue;
            }
            // 判断当前Channel是否保持连接，没有被关闭
            // 不是当前发送信息的客户端Channel
            if (key.isValid() && !socketChannel.equals(channel)) {
                wBuffer.clear();
                // 将信息写入Buffer
                wBuffer.put(StandardCharsets.UTF_8.encode(clientName(socketChannel) + "：" + fwdMsg));
                // 转为读模式，让Channel读取写入的信息
                wBuffer.flip();
                while (wBuffer.hasRemaining()) {
                    ((SocketChannel) channel).write(wBuffer);
                }
            }
        }
    }

    private String clientName(SocketChannel socketChannel) {
        return "客户端【" + socketChannel.socket().getPort() + "】";
    }

    private boolean inputQuit(String msg) {
        return QUIT.equals(msg);
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }
}
