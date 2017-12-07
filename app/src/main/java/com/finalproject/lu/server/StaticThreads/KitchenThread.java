package com.finalproject.lu.server.StaticThreads;

/**
 * Created by kaushikp on 27-11-2017.
 */

import java.net.Socket;

import POJO.FoodsEnum;
import POJO.Message;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import POJO.Message;
import POJO.Nodification;

/**
 * Created by kaushikp on 27-11-2017.
 */

public class KitchenThread extends Thread {
    //Kitchen Thread Code
    private static int burgerTime = 500;
    private static int chickenTime = 700;
    private static int friesTime = 500;
    private static int onionTime = 100;
    private static int sleepTime = 0;
    private static ConcurrentLinkedQueue<Message> orderList;
    private static Socket socket = null;
    private static ConcurrentLinkedQueue<Message> packageList;
    private static ConcurrentLinkedQueue<Object> messages;
    public static Socket getSocket() {
        return socket;
    }

    public static void setSocket(Socket socket) {
        KitchenThread.socket = socket;
    }

    public KitchenThread(ConcurrentLinkedQueue<Message> orderList, ConcurrentLinkedQueue<Message> packageList,
                         ConcurrentLinkedQueue<Object> messages){
        this.packageList = packageList;
        this.orderList = orderList;
        this.messages = messages;
    }

    @Override
    public void run() {
        while(true){
            if(socket == null || orderList.isEmpty()){
                continue;
            }
           // Random r = new Random();
            //int sleepTime= 2000 + r.nextInt(3000);
            Message msg  = orderList.poll();
            Map<String, Integer> temp = msg.getOrder().getFoods();

            for ( String item : temp.keySet()) {
                if(item.equals(FoodsEnum.FRENCHFRIES.getName())){
                    sleepTime += friesTime * temp.get(item);
                }else if(item.equals(FoodsEnum.CHICHENS.getName())){
                    sleepTime += chickenTime * temp.get(item);
                }else if(item.equals(FoodsEnum.BURGERS.getName())){
                    sleepTime += burgerTime * temp.get(item);
                }else {
                    sleepTime += onionTime * temp.get(item);
                }
            }
            System.out.println("SleepTime: " + sleepTime);
            try {
               // Message msg  = orderList.poll();
                msg.getNodification().setNodification(Nodification.Status.PREPARE.getStatus());
                messages.offer(msg);
                sleep(sleepTime*2);
                packageList.offer(msg);
                sleepTime=0;

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
