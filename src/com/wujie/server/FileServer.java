package com.wujie.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileServer {

    private ExecutorService executorService;//线程池
    private int port;//监听端口
    private boolean quit = false;//退出
    private ServerSocket server;
    private HashMap<Long, FileLog> datas = new HashMap<Long, FileLog>();

    public FileServer(int port) {
        this.port = port;
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 50);
    }

    public void quit() {
        this.quit = true;
        try {
            server.close();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public void start() throws Exception{
        server = new ServerSocket(port);
        while (!quit) {
            try {
                Socket socket = server.accept();
                executorService.execute(new SocketTask(socket));
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    private final class SocketTask implements Runnable {
        private Socket socket = null;
        public SocketTask (Socket socket) {
            this.socket = socket;
        }
        @Override
        public void run() {
            // TODO Auto-generated method stub
            try {
                System.out.println("accepted connection "+ socket.getInetAddress() + socket.getPort());
                PushbackInputStream inputStream = new PushbackInputStream(socket.getInputStream());
                //得到客户端发来的第一行协议数据：Content-Length=143253434;filename=xxx.3gp;sourceid=
                //如果用户初次上传文件，sourceid的值为空。
                String head = StreamTool.reaLine(inputStream);
                System.out.println(head);

                if(head != null) {
                    String [] items = head.split(";");
                    String filelength = items[0].substring(items[0].indexOf("=") + 1);
                    String filename = items[1].substring(items[1].indexOf("=") + 1);
                    String sourceid = items[2].substring(items[2].indexOf("=") + 1);
                    long id  = System.currentTimeMillis();//生产资源Id,如果需要唯一性，可以采用UUID;
                    FileLog log = null;
                    if (sourceid != null && !"".equals(sourceid)) {
                        id = Long.valueOf(sourceid);
                        log = find(id);
                    }
                    File file = null;
                    int position  = 0;

                    if(log == null) {
                        String path = new SimpleDateFormat("yyyy/MM/dd/HH/mm").format(new Date());
                        File dir = new File("file/" + path);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        file = new File(dir, filename);
                        if (file.exists()) {
                            filename = filename .substring(0, filename.indexOf(".") -1) +
                                    dir.listFiles().length + filename.substring(filename.indexOf("."));
                            file = new File(dir, filename);
                        }
                        save(id, file);
                    } else {
                        file = new File(log.getPath());
                        if (file.exists()) {
                            File logFile = new File(file.getParentFile(), file.getName()+".log");
                            if(logFile.exists()) {
                                Properties properties = new Properties();
                                properties.load(new FileInputStream(logFile));
                                position = Integer.valueOf(properties.getProperty("length"));
                            }
                        }

                        OutputStream outstream = socket.getOutputStream();
                        String respose = "sourceid="+ id+";position="+ position+ "\r\n";
                        //服务器收到客户端的请求信息后，给客户端返回响应信息：sourceid=1274773833264;position=0
                        //sourceid由服务器端生成，唯一标识上传的文件，position指示客户端从文件的什么位置开始上传
                        outstream.write(respose.getBytes());
                        RandomAccessFile fileOutStram = new RandomAccessFile(file, "rwd");
                        if (position == 0) {
                            fileOutStram.setLength(Integer.valueOf(filelength));
                            fileOutStram.seek(position);
                            byte[] buffer = new byte[1024];
                            int len = -1;
                            int length = position;
                            while ((len = inputStream.read(buffer)) != -1) {
                                fileOutStram.write(buffer, 0, len);
                                length += len;
                                Properties properties = new Properties();
                                properties.put("length", String.valueOf(length));
                                FileOutputStream logFile = new FileOutputStream(
                                        file.getName() + ".log");
                                properties.store(logFile, null);
                                logFile.close();
                            }
                            if (length == fileOutStram.length()) {
                                delete(id);
                            }
                            fileOutStram.close();
                            inputStream.close();
                            file = null;
                        }
                    }
                }
            } catch (Exception e) {
                // TODO: handle exception
            } finally {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (Exception e2) {
                    // TODO: handle exception
                }
            }

        }
    }

    public FileLog find(Long sourceid) {
        return datas.get(sourceid);
    }

    public void save(Long id , File saveFile) {
        datas.put(id, new FileLog(id, saveFile.getAbsolutePath()));
    }

    public void delete (long sourceid) {
        if (datas.containsKey(sourceid)) {
            datas.remove(sourceid);
        }
    }


    private class FileLog {
        private Long id;
        private String path;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getPath() {
            return path;
        }
        public void setPath (String path) {
            this.path = path;
        }

        public FileLog(Long id, String path) {
            this.id = id;
            this.path = path;
        }
    }
}
