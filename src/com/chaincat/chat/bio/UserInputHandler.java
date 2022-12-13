package com.chaincat.chat.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 客户端用户输入任务
 * @author Chain
 */
public class UserInputHandler implements Runnable {

    /**
     * 客户端
     */
    private final ChatClient chatClient;

    /**
     * 初始化
     * @param chatClient 客户端
     */
    public UserInputHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        try {
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            // 循环可输入
            while (true) {
                String input = consoleReader.readLine();
                chatClient.send(input);
                // 输入退出指令则结束任务
                if (chatClient.inputQuit(input)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
