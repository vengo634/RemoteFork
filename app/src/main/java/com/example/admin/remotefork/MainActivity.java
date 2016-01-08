package com.example.admin.remotefork;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import android.app.Activity;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import android.net.Uri;

import 	java.net.HttpURLConnection;
import 	java.net.URL;
import 	java.io.UnsupportedEncodingException;

import 	java.util.regex.Pattern;
import 	java.util.regex.Matcher;

import android.content.Intent;
import com.example.admin.remotefork.ForkService;

public class MainActivity extends Activity {

    public final static String BROADCAST_ACTION = "com.example.admin.remotefork.ForkService";
    ServerSocket serverSocket;
    String message = "",ss="";
    String resp = "",last_ip="";
    TextView info,msg,topText;
    String[] xs;
    private static final String DEBUG_TAG = "HttpExample";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        info = (TextView) findViewById(R.id.info);
        msg = (TextView) findViewById(R.id.msg);
        topText = (TextView) findViewById(R.id.topText);



        IntentFilter f=new IntentFilter(BROADCAST_ACTION);
        registerReceiver(br, f);

        final Button button = (Button) findViewById(R.id.buttonSync);
        final Button buttonUpdate = (Button) findViewById(R.id.buttonUpdate);
        // parse("/parserlink?http%3A%2F%2Fya.ru%2F");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Thread socketServerThread = new Thread(new SocketServerThread());
                socketServerThread.start();
                topText.setText(getIpAddress());

                // Perform action on click
            }
        });
        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                topText.setText("http://getlist2.obovse.ru/remote/index.php?v=a1.01&appl=android&do=list&localip=" + getIpAddress() + ":8028");
                try {
                    downloadUrl("http://getlist2.obovse.ru/remote/index.php?v=a1.01&appl=android&do=list&localip=" + getIpAddress() + ":8028");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    topText.setText(topText.getText() + "\nError:" + e.toString());
                    e.printStackTrace();
                }
                button.performClick();
            }
        });
        //buttonUpdate.performClick();

    }
    public void onClickStart(View v) {

        startService(new Intent(this, ForkService.class));
    }

    public void onClickStop(View v) {

        stopService(new Intent(this, ForkService.class));
    }
    BroadcastReceiver br= new BroadcastReceiver() {
        // действия при получении сообщений
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");
            info.setText(status + "\n" + info.getText());

        }

    };
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
        unregisterReceiver(br);
    }
    @Override
    public void onResume() {
        super.onResume();

    }


    @Override
    public void onPause() {


        super.onPause();
    }

    private class SocketServerThread extends Thread {

        static final int SocketServerPORT = 8028;
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

                            resp += "<html><h1>ForkPlayer DLNA Android Work!</h1></html>";
                        } else if (get.indexOf("/parserlink") != -1) {
                            get = get.substring(12).replace("|", "_FRAGMENT_").replace("|", "_FRAGMENT_");

                            xs = get.split("_FRAGMENT_");
                            if (xs.length == 1) {
                                xs = new String[]{xs[0], "", ""};
                            } else if (xs.length == 2) {
                                xs = new String[]{xs[0], xs[1], ""};
                            }
                            String rs ="";
                            try {
                                rs = downloadUrl(xs[0]);
                            }catch (IOException e) {
                                rs=e.toString();
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
                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            topText.setText("parse " + xs[0]+"\n"+xs[1]+"-"+xs[2]);
                            info.setText(resp);
                            msg.setText(message);
                        }
                    });
                    SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(socket, resp);
                    socketServerReplyThread.run();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
               message=e.toString();
            }
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    info.setText(message.toString());
                }
            });
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
                        "Server:Android\n\n"+msgReply);
                printStream.close();

                //message += "replayed: " + msgReply + "\n";

                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        msg.setText(message);
                    }
                });

            } catch (IOException e) {
                // TODO Auto-generated catch block
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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            Log.d(DEBUG_TAG, "The response is: " + response);
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
    public String readIt(InputStream stream) throws IOException, UnsupportedEncodingException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        String s;
        String contentAsString = "";
        while (true) {
            s = br.readLine();
            if (s == null || s.trim().length() == 0) {
                break;
            }
            contentAsString += s;
        }
        return contentAsString;
        /*
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);*/
    }
    private String GET(String url){
        if(!isConnected()) return "No internet!";
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            msg.setText("Inp Err:"+url+"\n"+e.toString());
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    // convert inputstream to String
    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    // check network connection
    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }
}
