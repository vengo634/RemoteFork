package com.example.admin.remotefork;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import android.widget.TextView;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import 	java.net.HttpURLConnection;
import 	java.net.URL;

import 	java.util.regex.Pattern;
import 	java.util.regex.Matcher;


public class ForkService extends Service {
    final String LOG_TAG = "myLogs";
    ServerSocket serverSocket;
    String message = "",ss="";
    String resp = "",last_ip="";
    TextView info,msg,topText;
    String[] xs;

    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand" + getIpAddress());
        sendStatus("Start " + getIpAddress() + ":8028");
        last_ip="";
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        sendStatus("Stop service");
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                sendStatus(e.toString());
            }
        }
    }

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }



    private class SocketServerThread extends Thread {

        static final int SocketServerPORT = 8028;
        int count = 0;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SocketServerPORT);

                while (true) {
                    if(last_ip!=getIpAddress()+":8028"){
                        last_ip=getIpAddress()+":8028";
                        String t =downloadUrl("http://getlist2.obovse.ru/remote/index.php?v=1.01&do=list&appl=android&localip="+last_ip);
                    }
                    Socket socket = serverSocket.accept();
                    count++;
                    InputStream is = socket.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String s, ms = "", get = "",text="";
                    ss="";
                    while (true) {
                        s = br.readLine();
                        if (s == null || s.trim().length() == 0) {
                            break;
                        }
                        int x1 = s.indexOf("GET ");
                        if (x1 != -1) {
                            int x2 = s.indexOf(" HTTP");
                            get = s.substring(x1 + 4, x2);
                            message += get + "|\n";
                        }
                        ms += s;
                    }
                    if (get != "") {
                        get = Uri.decode(get);
                        resp = "";
                        if (get.indexOf("/test") == 0) {
                            String[] initial=get.substring(6).replace("|", "_FRAGMENT_").replace("|", "_FRAGMENT_").split("_FRAGMENT_");
                            resp += "<html><h1>ForkPlayer DLNA Android Work!</h1></html>";
                            sendStatus("Connect "+initial[2]+" ("+initial[0]+")");
                        } else if (get.indexOf("/parserlink") != -1) {
                            get = get.substring(12).replace("|", "_FRAGMENT_").replace("|", "_FRAGMENT_");

                            xs = get.split("_FRAGMENT_");
                            if (xs.length == 1) {
                                xs = new String[]{xs[0], "", ""};
                            } else if (xs.length == 2) {
                                xs = new String[]{xs[0], xs[1], ""};
                            }
                            String rs ="";
                            sendStatus("GET "+xs[0].substring(0,30));
                            try {
                                rs = downloadUrl(xs[0]);
                            }catch (IOException e) {
                                rs=e.toString();
                                sendStatus(e.toString());
                            }

                            if(xs[1]==""&&xs[2]=="") text=rs;
                            else if(xs[1].indexOf(".*?")>=0||xs[2].indexOf(".*?")>=0){
                                Pattern p = Pattern.compile(xs[1]+"(.*?)"+xs[2]);
                                Matcher m = p.matcher(rs);
                                m.find();
                                text = m.group(1);


                            }
                            else{
                                int x1=rs.indexOf(xs[1])+xs[1].length();
                                int x2=rs.indexOf(xs[2],x1);
                                text=rs.substring(x1,x2);
                            }
                            resp=text;
                        }
                        // message += "#" + count + " from " + socket.getInetAddress()+ ":" + socket.getPort() + "\n";
                    }
                    SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(socket, resp);
                    socketServerReplyThread.run();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                sendStatus(e.toString());
            }

        }

    }
    public void sendStatus(String s) {
        Intent intent = new Intent(MainActivity.BROADCAST_ACTION);
        try {
            // сообщаем об старте задачи
            intent.putExtra("status", s);
            sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private class SocketServerReplyThread extends Thread {

        private Socket hostThreadSocket;
        int cnt;
        String res;

        SocketServerReplyThread(Socket socket, String c) {
            hostThreadSocket = socket;
            res = c;
        }

        @Override
        public void run() {
            OutputStream outputStream;
            String msgReply = res;

            try {
                outputStream = hostThreadSocket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print("HTTP/1.1 200 Ok\n" +
                        "Access-Control-Allow-Origin:*\n" +
                        "Connection:close\n" +
                        "Content-Length:" + msgReply.length() + "\n" +
                        "Content-Type:text/html\n" +
                        "Server:Android\n\n" + msgReply);
                printStream.close();

                //message += "replayed: " + msgReply + "\n";



            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                message += "Something wrong! " + e.toString() + "\n";
            }
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
                        ip = inetAddress.getHostAddress();
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            //ip = "";
        }

        return ip;
    }
    private String downloadUrl(String myurl) throws IOException {
        if(!isConnected()) return "No internet!";
        InputStream is = null;
        // Only display the first 500 characters of the retrieved
        // web page content.
        int len = 500;
        String contentAsString="Empty";
        URL url = new URL(myurl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setReadTimeout(40000 /* milliseconds */);
            conn.setConnectTimeout(40000 /* milliseconds */);
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(LOG_TAG, "The response is: " + response);
            is = conn.getInputStream();
            if(response==200) contentAsString =  readStream(is);
            else contentAsString="Ошибка "+response;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
            conn.disconnect();
        }
        return contentAsString;
    }
    private static String readStream(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        return sb.toString();
    }

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }
}
