package com.finalproject.lu.server.StaticThreads;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import POJO.Message;
import POJO.Nodification;

/**
 * Created by kaushikp on 27-11-2017.
 */

public class PackagingThread extends Thread {
    private static ConcurrentLinkedQueue<Message> packageList;
    private static Socket socket = null;
    private static ConcurrentLinkedQueue<Message> deliveryList;
    private static ConcurrentLinkedQueue<Object> messages;
    public static Socket getSocket() {
        return socket;
    }

    public static void setSocket(Socket socket) {
        PackagingThread.socket = socket;
    }

    public PackagingThread(ConcurrentLinkedQueue<Message> packageList, ConcurrentLinkedQueue<Message> deliveryList,
                           ConcurrentLinkedQueue<Object> messages){
        this.packageList = packageList;
        this.deliveryList = deliveryList;
        this.messages = messages;
    }

    @Override
    public void run() {
        while(true){
            if(socket == null || packageList.isEmpty()){
                continue;
            }
            Random r = new Random();
            int sleepTime= 2000 + r.nextInt(3000);
            try {
                Message msg  = packageList.poll();
                msg.getNodification().setNodification(Nodification.Status.PACKAGE.getStatus());
                messages.offer(msg);
                sleep(sleepTime);
                msg.getNodification().setNodification(Nodification.Status.DELIVERY.getStatus());
                messages.offer(msg);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
