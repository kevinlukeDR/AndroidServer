package com.finalproject.lu.server;

import POJO.FoodsEnum;
import POJO.Message;
import POJO.Nodification;
import POJO.Order;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.widget.TextView;
import com.finalproject.lu.server.StaticThreads.KitchenThread;
import com.finalproject.lu.server.StaticThreads.MessageThread;
import com.finalproject.lu.server.StaticThreads.PackagingThread;

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

public class MainActivity extends Activity {

    private final static Calendar date = Calendar.getInstance();
    private static ConcurrentLinkedQueue<Message> orderList = new ConcurrentLinkedQueue<>();
    private static ConcurrentHashMap<String, Integer> inventoryList = new ConcurrentHashMap<>();
    private static ConcurrentLinkedQueue<Message> packetList = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<Message> deliveryList = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<Object> messages = new ConcurrentLinkedQueue<>();

    TextView info, infoip, msg;
    String message = "";
    ServerSocket serverSocket;
    ServerSocket replySocket;
    String resourceName = "inventory.txt";
    String pathSDCard = Environment.getExternalStorageDirectory() + "/Android/data/" + resourceName;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        info = (TextView) findViewById(R.id.info);
        infoip = (TextView) findViewById(R.id.infoip);
        msg = (TextView) findViewById(R.id.msg);
        infoip.setText(getIpAddress());
        verifyStoragePermissions(this);
        loadInventory();
        InventoryListThread inventoryListThread = new InventoryListThread();
        inventoryListThread.start();
        PackagingThread packagingThread = new PackagingThread(packetList, deliveryList, messages);
        KitchenThread kitchenThread = new KitchenThread(orderList, packetList, messages);
        MessageThread messageThread = new MessageThread(messages);
        messageThread.start();
        kitchenThread.start();
        packagingThread.start();
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();

    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
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
        if (replySocket != null) {
            try {
                replySocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public void loadInventory(){

//String path ="/sdcard/ip.txt";

        System.out.println(pathSDCard);

//Get the text file


//Read text from file
  //      StringBuilder text = new StringBuilder();

        try {
            System.out.println("in read");
            InputStream inputStream = getResources().openRawResource(R.raw.inventory);
            ///*R.raw.inve*/getResources().getIdentifier("inventory", "raw",getPackageName()));
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line="";

            File file=new File(pathSDCard);
            OutputStream stream=new FileOutputStream(file);
            OutputStreamWriter os=new OutputStreamWriter(stream);
            BufferedWriter ou=new BufferedWriter(os);

            while ((line = br.readLine()) != null) {
                String items[] = line.split(",");

                //System.out.println(line);
                inventoryList.put(items[0], 5);
               items[1]= String.valueOf(Integer.valueOf(items[1])-50);
                ou.write(items[0] +"," + items[1]);
                ou.newLine();
                //System.out.println(items[0]);
                //System.out.println(line);
            }
            ou.flush();
            stream.close();
            br.close();


            //Read the values from the internal storage file
            InputStream fileInputStream=new FileInputStream(file);
            InputStreamReader is=new InputStreamReader(fileInputStream);
            BufferedReader in=new BufferedReader(is);
            String readLine="";
            while((readLine= in.readLine()) != null){
                System.out.println("Reading File" + readLine);
            }
            //End Reading

        }
        catch (IOException e) {
           e.printStackTrace(); //You'll need to add proper error handling here
        }
//
//        for(FoodsEnum foods : FoodsEnum.values()){
//            inventoryList.put(foods.getName(),5);
//        }
    }

    public void loadData(){
        System.out.println("inside function");
        String path = "inventory.txt";
        String line = "";
        // StringBuilder text = new StringBuilder();
        InputStream stream = getResources().openRawResource(R.raw.inventory);
        String pathSDCard = Environment.getExternalStorageDirectory() + "/data/" + path;
        InputStreamReader is = new InputStreamReader(stream);
        // FileOutputStream out =  new FileOutputStream(pathSDCard);


        try {
            OutputStream outstream=new FileOutputStream(pathSDCard);
            OutputStreamWriter os=new OutputStreamWriter(outstream);
            BufferedWriter bw=new BufferedWriter(os);
            BufferedReader br = new BufferedReader(is);

            System.out.println("in read");
            while ((line = br.readLine()) != null) {
                String items[] = line.split(",");
                System.out.println(line);
                inventoryList.put(items[0], 50);
                System.out.println(items[0]+ "," +items[1]);
                bw.write(items[0]+","+ items[1]);
            }
            bw.flush();
            os.close();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private class SocketServerThread extends Thread {

        static final int SocketAPORT = 8080;
        static final int SocketBPORT = 8081;
        int count = 0;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SocketAPORT);
                replySocket = new ServerSocket(SocketBPORT);
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
                    Socket reply = replySocket.accept();
                    MessageThread.setSocket(reply);
                    int currentHour = date.get(Calendar.HOUR_OF_DAY);
                    // TODO remove comment
                    if (currentHour >= 24 || currentHour < 10){
                        replyPool.execute(new SocketServerReplyThread(
                                reply, count, false));
                        continue;
                    }
                    count++;
                    message += "#" + count + " from " + reply.getInetAddress()
                            + ":" + reply.getPort() + "\n";

                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            msg.setText(message);
                        }
                    });

                    replyPool.execute(new SocketServerReplyThread(
                            reply, count, true));
                    PackagingThread.setSocket(reply);
                    KitchenThread.setSocket(reply);
                    Socket socket = serverSocket.accept();
                    listenPool.execute(new SocketServerListenThread(socket, reply, count));
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


            if (!isOpen) {
                messages.offer("Closed");
            } else {
                messages.offer(cnt);

                message += "replayed: " + msgReply + "\n";

                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        msg.setText(message);
                    }
                });
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
        private Socket socket, reply;
        int cnt;
        private String response;
        SocketServerListenThread(Socket socket, Socket reply, int c) {
            this.socket = socket;
            this.reply = reply;
            cnt = c;
        }

        @Override
        public void run() {
            while(true) {
                try {
                    response = "";
                    InputStream is = socket.getInputStream();
                    ObjectInputStream ois = new ObjectInputStream(is);
                    Object object = ois.readObject();
                    Message message = (Message) object;

                    // TODO decrease the amount of inventoryList

                    Map<String, Integer> res = new HashMap<>();
                    if (isFullyAvailable(message)){
                        Order order = message.getOrder();
                        Map<String, Integer> foods = message.getOrder().getFoods();
                        for (String item : foods.keySet()) {
                            inventoryList.put(item, (inventoryList.get(item) - foods.get(item)));
                        }
                        Message reply = new Message(order, new Nodification(Nodification.Status.RECEIVE.getStatus()), false, null);
                        messages.offer(reply);
                        orderList.offer(message);
                    }
                    else if ((res = isPartialAvailable(message)) != null){
                        Order order = message.getOrder();
                        Message reply = new Message(order, new Nodification(Nodification.Status.PARTIAL.getStatus()), false, res);
                        messages.offer(reply);
                    }
                    else {
                        Order order = message.getOrder();
                        Message reply = new Message(order, new Nodification(Nodification.Status.NOTAVAILABLE.getStatus()), false, null);
                        messages.offer(reply);
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
                try {
                    Thread.sleep(86400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                updateList();
            }
        }

        // TODO update from inventory.txt
        private void updateList() {
            String resourceName = "inventory.txt";

            ///*R.raw.inve*/getResources().getIdentifier("inventory", "raw",getPackageName()));

            String line="";
            String pathSDCard = Environment.getExternalStorageDirectory() + "/Android/data/" + resourceName;
            File file=new File(pathSDCard);
            Map<String, Integer> temp = new ConcurrentHashMap<>();
            InputStream fileInputStream= null;
            try {
                fileInputStream = new FileInputStream(file);
                InputStreamReader is=new InputStreamReader(fileInputStream);
                BufferedReader in=new BufferedReader(is);
                String readLine="";
                while((readLine= in.readLine()) != null){
                    String items[] = readLine.split(",");
                    int inVal = inventoryList.get(items[0]);
                    inventoryList.put(items[0], inVal + 50);
                    items[1]= String.valueOf(Integer.valueOf(items[1])-50);
                    temp.put(items[0], Integer.valueOf(items[1]));
                }
                in.close();
                is.close();
                //Write the File to Internal Storage

                OutputStream stream=new FileOutputStream(file, false);
                OutputStreamWriter os=new OutputStreamWriter(stream);
                BufferedWriter ou=new BufferedWriter(os);

                for(String item : temp.keySet()){
                    ou.write(item + "," + temp.get(item));
                    ou.newLine();
                }
                ou.flush();
                os.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    public static Map<String, Integer> isPartialAvailable(Message message) {
        Map<String, Integer> foods = message.getOrder().getFoods();
        Map<String, Integer> res = new HashMap<>();
        int count = 0;
        for (String item : foods.keySet()){
            if (foods.get(item) <= inventoryList.get(item)){
                res.put(item, foods.get(item));
                count++;
            }
        }
        return count == 0 ? null : res;
    }
}
