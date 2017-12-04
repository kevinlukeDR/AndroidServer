package com.finalproject.lu.server.StaticThreads;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageThread extends Thread {
    private static ConcurrentLinkedQueue<Object> messages;
    private static Socket socket = null;
    private static Object object = null;
    public MessageThread(ConcurrentLinkedQueue<Object> messages){
        this.messages = messages;
    }

    public static void setSocket(Socket socket) {
        MessageThread.socket = socket;
    }

    @Override
    public void run() {
        while(true){
            if(socket == null || messages.isEmpty() || object != null){
                continue;
            }
            try {
                object = messages.poll();
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(object);
                oos.flush();
                object = null;
            }  catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
