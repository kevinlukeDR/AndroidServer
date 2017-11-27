package com.finalproject.lu.server.StaticThreads;

/**
 * Created by kaushikp on 27-11-2017.
 */

import java.net.Socket;

import POJO.Message;

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

public class KitchenThread extends Thread {
    private static ConcurrentLinkedQueue<Message> orderList;
    private static Socket socket = null;
    private static ConcurrentLinkedQueue<Message> packageList;

    public static Socket getSocket() {
        return socket;
    }

    public static void setSocket(Socket socket) {
        KitchenThread.socket = socket;
    }

    public KitchenThread(ConcurrentLinkedQueue<Message> orderList, ConcurrentLinkedQueue<Message> packageList){
        this.packageList = packageList;
        this.orderList = orderList;
    }

    @Override
    public void run() {
        while(true){
            if(socket == null || orderList.isEmpty()){
                continue;
            }
            Random r = new Random();
            int sleepTime= 2000 + r.nextInt(3000);
            try {
                Message msg  = orderList.poll();
                msg.getNodification().setNodification(Nodification.Status.PREPARE.getStatus());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());;
                oos.writeObject(msg);
                oos.flush();
                sleep(sleepTime);
                packageList.offer(msg);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
