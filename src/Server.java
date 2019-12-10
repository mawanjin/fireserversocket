import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    /**
     * 要处理客户端发来的对象，并返回一个对象，可实现该接口。
     */
    public interface ObjectAction{
        Object doAction(Object rev, Server server);
    }

    public static final class DefaultObjectAction implements ObjectAction{
        public Object doAction(Object rev,Server server) {
            System.out.println("处理并返回："+rev);
            return rev;
        }
    }

    public static void main(String[] args) {
        int port = 65432;
        Server server = new Server(port);
        server.start();
    }

    private int port;
    private volatile boolean running=false;

    private ConcurrentHashMap<Class, ObjectAction> actionMapping = new ConcurrentHashMap<Class,ObjectAction>();
    private Thread connWatchDog;

    public Server(int port) {
        this.port = port;
    }

    public void start(){
        if(running)return;
        running=true;
        connWatchDog = new Thread(new ConnWatchDog());
        connWatchDog.start();
    }

    @SuppressWarnings("deprecation")
    public void stop(){
        if(running)running=false;
        if(connWatchDog!=null)connWatchDog.stop();
    }

    public void addActionMap(Class<Object> cls,ObjectAction action){
        actionMapping.put(cls, action);
    }

    class ConnWatchDog implements Runnable{
        public void run(){
            try {
                ServerSocket ss = new ServerSocket(port,5);
                while(running){
                    Socket s = ss.accept();
                    new Thread(new SocketAction(s)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Server.this.stop();
            }

        }
    }

    class SocketAction implements Runnable{
        Socket s;
        boolean run=true;

        public SocketAction(Socket s) {
            this.s = s;
        }
        public void run() {
            while(running && run){
                try {
                    InputStream in = s.getInputStream();
                    if(in.available()>0){
                        int len;

                        byte[] lenBytes = new byte[4];
                        in.read(lenBytes);
                        int contentLen = bytesToInt(lenBytes);
                        System.out.println("contentLen：\t"+contentLen);

                        byte[] bytes = new byte[contentLen];
                        in.read(bytes);
                        String str = new String(bytes,"utf8");

                        System.out.println("接收：\t"+str);

                        OutputStream out = s.getOutputStream();
                        out.write(getPlus());
                        out.flush();

                        if(str.contains("准备好了")){
                            out.write(new StartMsg().parse());
                            out.flush();
                        }else if(str.contains("准备开始识别")){
                            out.write(new RecgMsg().parse());
                            out.flush();
                            mp3Ready= true;
                        }

//                        if(mp3Ready){
//                            mp3Ready = false;
//                            out.write(new Mp3Msg(7).parse());
//                            out.flush();
//                            new Thread(){
//                                @Override
//                                public void run() {
//                                    super.run();
//                                    for(int i=0;i<7;i++){
//
//                                        try {
//                                            sleep(5000);
//                                            out.write(new Mp3Msg(i).parse());
//                                            out.flush();
//                                        } catch (Exception e) {
//                                            e.printStackTrace();
//                                        }
//
//                                    }
//                                }
//                            }.start();
//                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    overThis();
                }
            }
        }

        private boolean mp3Ready =false;

        private void overThis() {
            if(run)run=false;
            if(s!=null){
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("关闭："+s.getRemoteSocketAddress());
        }

    }
    public byte[] getPlus(){
        String str = "pulse";
        //Build the byte array according to the server's parsing rules
        byte[] body = str.getBytes(Charset.defaultCharset());
        ByteBuffer bb = ByteBuffer.allocate(4 + body.length);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(body.length);
        bb.put(body);
        return bb.array();
    }

    public static int bytesToInt(byte[] bs) {
        int a = 0;
        for (int i = bs.length - 1; i >= 0; i--) {
            a += bs[i] * Math.pow(255, bs.length - i - 1);
        }
        return a;
    }
}
