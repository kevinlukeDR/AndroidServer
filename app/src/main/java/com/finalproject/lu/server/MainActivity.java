package com.finalproject.lu.server;

import POJO.FoodsEnum;
import POJO.Message;
import POJO.Nodification;
import POJO.Order;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import java.io.*;
import java.net.*;
import java.util.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Bundle;
import android.app.Activity;
import android.widget.TextView;

import com.finalproject.lu.server.StaticThreads.KitchenThread;
import com.finalproject.lu.server.StaticThreads.PackagingThread;

public class MainActivity extends Activity {

    private final static Calendar date = Calendar.getInstance();
    private static ConcurrentLinkedQueue<Message> orderList = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<Message> packetList = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<Message> deliveryList = new ConcurrentLinkedQueue<>();
    private static ConcurrentHashMap<String, Integer> inventoryList = new ConcurrentHashMap<>();
    TextView info, infoip, msg;
    String message = "";
    ServerSocket serverSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        info = (TextView) findViewById(R.id.info);
        infoip = (TextView) findViewById(R.id.infoip);
        msg = (TextView) findViewById(R.id.msg);
        infoip.setText(getIpAddress());


        InventoryListThread inventoryListThread = new InventoryListThread();
        Thread socketServerThread = new Thread(new SocketServerThread());
        PackagingThread packagingThread = new PackagingThread(packetList, deliveryList);
        KitchenThread kitchenThread = new KitchenThread(orderList, packetList);
        inventoryListThread.start();
        kitchenThread.start();
        packagingThread.start();
        socketServerThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class SocketServerThread extends Thread {

        static final int SocketServerPORT = 8080;
        int count = 0;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        info.setText("I'm waiting here: "
                                + serverSocket.getLocalPort());
                    }
                });
                ExecutorService replyPool = Executors.newCachedThreadPool();
                ExecutorService listenPool = Executors.newCachedThreadPool();
                while (true) {
                    Socket socket = serverSocket.accept();
                    PackagingThread.setSocket(socket);
                    KitchenThread.setSocket(socket);
                    int currentHour = date.get(Calendar.HOUR_OF_DAY);
                    // TODO remove comment
//                    if (currentHour > 19 || currentHour < 11){
//                        replyPool.execute(new SocketServerReplyThread(
//                                socket, count, false));
//                        continue;
//                    }
                    count++;
                    message += "#" + count + " from " + socket.getInetAddress()
                            + ":" + socket.getPort() + "\n";

                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            msg.setText(message);
                        }
                    });

                    replyPool.execute(new SocketServerReplyThread(
                            socket, count, true));
                    listenPool.execute(new SocketServerListenThread(socket, count));
                    System.out.println("123");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    private class SocketServerReplyThread extends Thread {

        Socket hostThreadSocket;
        int cnt;
        boolean isOpen;

        SocketServerReplyThread(Socket socket, int c, boolean isOpen) {
            hostThreadSocket = socket;
            cnt = c;
            this.isOpen = isOpen;
        }

        @Override
        public void run() {
            OutputStream outputStream;
            String msgReply = "Hello from Android, you are #" + cnt;

            try {
                ObjectOutputStream oos = new ObjectOutputStream(hostThreadSocket.getOutputStream());;
                if (!isOpen){
                    oos.writeObject("Closed Now!");
                    oos.flush();
                    oos.close();
                }
                else {
                    oos.writeObject(cnt);
                    oos.flush();

                    message += "replayed: " + msgReply + "\n";

                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            msg.setText(message);
                        }
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
                message += "Something wrong! " + e.toString() + "\n";
            }

            MainActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    msg.setText(message);
                }
            });
        }

    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    private class SocketServerListenThread extends Thread {
        private Socket socket;
        int cnt;
        private String response;
        SocketServerListenThread(Socket socket, int c) {
            this.socket = socket;
            cnt = c;
        }

        @Override
        public void run() {
            while(true) {
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());;
                    response = "";
                    InputStream is = socket.getInputStream();
                    ObjectInputStream ois = new ObjectInputStream(is);
                    Object object = ois.readObject();
                    Message message = (Message) object;
                    Map<String, Boolean> res = new HashMap<>();
                    if (InventoryListThread.isFullyAvailable(message)){
                        orderList.offer(message);
                        Order order = message.getOrder();
                        Message reply = new Message(order, new Nodification(Nodification.Status.RECEIVE.getStatus()), false, null);
                        oos.writeObject(reply);
                        oos.flush();
                    }
                    else if ((res = InventoryListThread.isPartialAvailable(message)) != null){
                        Order order = message.getOrder();
                        // TODO handle partial order
                        Message reply = new Message(order, new Nodification(Nodification.Status.PARTIAL.getStatus()), false, res);
                        oos.writeObject(reply);
                        oos.flush();
                    }
                    else {
                        Order order = message.getOrder();
                        Message reply = new Message(order, new Nodification(Nodification.Status.NOTAVAILABLE.getStatus()), false, null);
                        oos.writeObject(reply);
                        oos.flush();
                    }
                    response = message.getNodification().getNodification();

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    response = "UnknownHostException: " + e.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                    response = "IOException: " + e.toString();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                message += response;
                if (!"".equals(response)) {
                    MainActivity.this.runOnUiThread(() -> msg.setText(message));
                }
                if ("88".equals(response)){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static class InventoryListThread extends Thread {
        @Override
        public void run(){
            while (true){
                updateList();
                try {
                    Thread.sleep(86400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // TODO update from inventory.txt
        private void updateList() {
            for (FoodsEnum key : FoodsEnum.values()){
                inventoryList.put(key.getName(), inventoryList.getOrDefault(key.getName(), 0) + 50);
            }
        }

        // TODO find a way to make it synchronized
        private static boolean isFullyAvailable(Message message) {
            Map<String, Integer> foods = message.getOrder().getFoods();
            for (String item : foods.keySet()){
                if (foods.get(item) > inventoryList.get(item)){
                    return false;
                }
            }
            return true;
        }

        public static Map<String, Boolean> isPartialAvailable(Message message) {
            Map<String, Integer> foods = message.getOrder().getFoods();
            Map<String, Boolean> res = new HashMap<>();
            int count = 0;
            for (String item : foods.keySet()){
                if (foods.get(item) <= inventoryList.get(item)){
                    res.put(item, true);
                    count++;
                }
            }
            return count == 0 ? null : res;
        }
    }
}
