package com;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.io.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.net.*;
import java.text.*;

public class PublicChatWin extends JFrame {
    // RightWindow
    // 用户信息
    private Map user;
    // 好友信息
    private List<Map<String, String>> friends;
    // 好友Label
    private List<JLabel> lblFriendList;

    // MiddleWindow
    // 查看文本区
    private JTextArea txtMainInfo;
    // 发送文本区
    private JTextArea txtInfo;
    // 消息日志( 是查看文本区的冗余信息 )
    private StringBuffer infoLog;
    // 日期格式
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // LeftWindow
    // private SharePanel.DrawPanel drawPanel;
    private JPanel drawPanel; //啊啊啊，多态真香！
    private JSONObject jsonObjDraw = new JSONObject();

    // 获得当前屏幕的宽
    private double screenWidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    // 登录窗口宽和高
    private int frameWidth = 260;
    private int frameHeight = 600;

    public PublicChatWin(Map user) {

        // 初始化成员变量
        this.user = user;
        this.infoLog = new StringBuffer();

        // 获取具体的用户信息
        this.friends = (List<Map<String, String>>) user.get("friends");
        String userId = (String) user.get("user_id");

        /// 初始化当前Frame
        setBounds((int) screenWidth - 1200, 10, 4 * frameWidth, frameHeight);
        setIconImage(Toolkit.getDefaultToolkit().getImage(PublicChatWin.class.getResource("/images/QQ.png")));
        setLayout(null);

        JPanel panelRight = new InfoPanel();
        panelRight.setBounds(3 * frameWidth, 0, frameWidth, frameHeight);
        getContentPane().add(panelRight);

        JPanel panelMiddle = new TxtPanel();
        panelMiddle.setBounds(2 * frameWidth, 0, frameWidth, frameHeight);
        getContentPane().add(panelMiddle);

        JPanel panelLeft = new SharePanel();
        panelLeft.setBounds(0 * frameWidth, 0, 2 * frameWidth, frameHeight);
        getContentPane().add(panelLeft);

        // 注册窗口事件
        addWindowListener(new WindowAdapter() {
            // 单击窗口关闭按钮时调用
            public void windowClosing(WindowEvent e) {

                // 当前用户下线
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("command", Client.COMMAND_LOGOUT);
                jsonObj.put("user_id", userId);
                byte[] b = jsonObj.toString().getBytes();

                InetAddress address;
                try {
                    address = InetAddress.getByName(Client.SERVER_IP);
                    // 创建DatagramPacket对象
                    DatagramPacket packet = new DatagramPacket(b, b.length, address, Client.SERVER_PORT);
                    // 发送
                    Client.socket.send(packet);
                } catch (IOException e1) {
                }

                // 退出系统
                System.exit(0);
            }
        });

        new Thread(new leftThread()).start();
        new Thread(new middleThread()).start();
        new Thread(new rightThread()).start();
        new Thread(new fileThread()).start();
        

    }
    
    class InfoPanel extends JPanel {
        InfoPanel() {
            setLayout(new BorderLayout());

            String userName = (String) user.get("user_name");
            String userIcon = (String) user.get("user_icon");

            // User信息
            JLabel lblLabel = new JLabel(userName);
            lblLabel.setHorizontalAlignment(SwingConstants.CENTER);
            // TODO
            add(lblLabel, BorderLayout.NORTH);

            // Friends信息
            JPanel panel1 = new FriendsPanel();
            add(panel1, BorderLayout.CENTER);
        }

        class FriendsPanel extends JPanel {
            FriendsPanel() {
                setLayout(new BorderLayout(0, 0));

                JLabel label = new JLabel("我的好友");
                label.setHorizontalAlignment(SwingConstants.CENTER);
                add(label, BorderLayout.NORTH);

                JPanel friendListPanel = new JPanel();
                friendListPanel.setLayout(new GridLayout(10, 0, 0, 5));
                add(friendListPanel);

                // 将所有的好友都挂到面板上，只是通过在线情况设置[状态]——可用/不可用，而非生成实体
                lblFriendList = new ArrayList<JLabel>();
                for (int i = 0; i < friends.size(); i++) {
                    // 获得每个好友的信息
                    Map<String, String> friend = friends.get(i);
                    String friendUserId = friend.get("user_id");
                    String friendUserName = friend.get("user_name");
                    String friendUserIcon = friend.get("user_icon");
                    String friendUserOnline = friend.get("online"); // 获得好友在线状态

                    // 生成每个好友的label
                    JLabel lblFriend = new JLabel(friendUserName);
                    lblFriend.setToolTipText(friendUserId);
                    // TODO // lblFriend.setIcon(null);

                    // 根据在线情况，设置可用/不可用
                    if (friendUserOnline.equals(Client.OFFLINE)) {
                        lblFriend.setEnabled(false);
                    } else {
                        lblFriend.setEnabled(true);
                    }

                    // 设置双击好友的响应：打开私聊窗口
                    lblFriend.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            // 用户图标双击鼠标时显示对话框
                            if (e.getClickCount() == 2) {
                                // TODO
                                // PrivateChatWin chatFrame = new PrivateChatWin(FriendsFrame.this, user,
                                // friend);
                                // chatFrame.setVisible(true);
                                // isRunning = false;
                                new PrivateChatWin(user, friend).setVisible(true);
                            }
                        }
                    });
                    lblFriendList.add(lblFriend); // 添加到列表集合，本质上是一个接口，供外部操作[好友面板]使用
                    friendListPanel.add(lblFriend); // 添加到面板
                }
            }

        }
    }
    
    class TxtPanel extends JPanel {

        TxtPanel() {
            setLayout(null);

            JPanel panel1 = new PanLine1();
            add(panel1);

            JPanel panel2 = new PanLine2();
            add(panel2);
        }

        class PanLine1 extends JPanel {
            PanLine1() {
                setLayout(null);
                setBounds(new Rectangle(5, 5, 252, 410));
                setBorder(BorderFactory.createLineBorder(Color.blue, 1));

                txtMainInfo = new JTextArea();
                txtMainInfo.setEditable(false);
                JScrollPane scrollPane = new JScrollPane();
                scrollPane.setBounds(5, 5, 242, 400);
                scrollPane.setViewportView(txtMainInfo);

                add(scrollPane);
            }

        }

        class PanLine2 extends JPanel {
            PanLine2() {
                setLayout(null);
                setBounds(5, 420, 252, 138);
                setBorder(BorderFactory.createLineBorder(Color.blue, 1));

                txtInfo = new JTextArea();
                JScrollPane scrollPane = new JScrollPane();
                scrollPane.setBounds(5, 5, 160, 120);
                scrollPane.setViewportView(txtInfo);

                add(scrollPane);
                add(getSendButton());

            }

            // 函数：生成发送按钮
            private JButton getSendButton() {

                JButton btSend = new JButton("发送");
                btSend.setBounds(170, 10, 80, 110);

                btSend.addActionListener(e -> {

                    if (!txtInfo.getText().equals("")) {

                        String date = dateFormat.format(new Date()); // 获得当前时间，并格式化

                        String info = String.format("#%s#" + "\n" + "%s说：%s" + "\n", date, user.get("user_name"),
                                txtInfo.getText());
                        JSONObject jObj = new JSONObject();
                        jObj.put("message", info);// body
                        jObj.put("command", Client.COMMAND_TXTBROADCAST);// head

                        // 交给服务器广播
                        try {
                            InetAddress address = InetAddress.getByName(Client.SERVER_IP);
                            byte[] b = jObj.toString().getBytes();
                            DatagramPacket packet = new DatagramPacket(b, b.length, address, Client.SERVER_PORT);
                            Client.socket.send(packet);

                        } catch (IOException e1) {
                            System.out.println("消息广播失败...");
                            e1.printStackTrace();
                        }
                    }

                    txtInfo.setText("");
                });

                return btSend;
            }
        }
    }
    
    class SharePanel extends JPanel {
        SharePanel() {
            setLayout(new BorderLayout());

            // // Server信息
            // JLabel lblLabel = new JLabel("Server/Teacher");
            // // lblLabel.setHorizontalAlignment(SwingConstants.CENTER);
            // // TODO
            // add(lblLabel, BorderLayout.NORTH);

            // // Users信息
            drawPanel = new DrawPanel();
            add(drawPanel, BorderLayout.CENTER);
        }

        class DrawPanel extends JPanel {
            public void paint(Graphics g) {

                if (jsonObjDraw.isEmpty()) {
                    // System.out.println(jsonObjDraw.isEmpty());
                    // int[] x = { 25, 55, 60, 78, 126 };
                    // int[] y = { 126, 78, 60, 55, 25 };
                    // int size = 5;
                    // g.drawPolyline(x, y, size);
                    g.drawString("专心听课哦~", 20, 20);

                } else {
                    System.out.println(jsonObjDraw.isEmpty());
                    JSONArray xArray = jsonObjDraw.getJSONArray("xPoints");
                    JSONArray yArray = jsonObjDraw.getJSONArray("yPoints");
                    int size = xArray.length();
                    int[] x = new int[size];
                    int[] y = new int[size];
                    for (int i = 0; i < size; i++) {
                        x[i] = (xArray.getInt(i));
                        y[i] = (yArray.getInt(i));
                        System.out.println(x[i] + "," + y[i]);
                    }
                    g.drawPolyline(x, y, size);

                }

        }
        
        
            

        }
    }

    private class rightThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    JSONObject jsonObj = Client.dataToRefreshFriendList.read();

                    System.out.println("当前线程：" + Thread.currentThread().getName());

                    String userId = (String) jsonObj.get("user_id");
                    String online = (String) jsonObj.get("online");

                    for (JLabel lblFriend : lblFriendList) {
                        // 判断用户Id是否一致
                        if (userId.equals(lblFriend.getToolTipText())) {
                            if (online.equals(Client.ONLINE)) {
                                lblFriend.setEnabled(true);
                            } else {
                                lblFriend.setEnabled(false);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println(Thread.currentThread().getName() + "线程出错");
                    e.printStackTrace();
                }
            }
        }
    }

   
    private class middleThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    JSONObject jsonObj = Client.dataTxtForALL.read();

                    System.out.println("当前线程：" + Thread.currentThread().getName()); 

                    String info = (String) jsonObj.get("message");

                    // 显示在查看区
                    infoLog.append(info);
                    txtMainInfo.setText(infoLog.toString());

                } catch (InterruptedException e) {
                    System.out.println(Thread.currentThread().getName() + "线程出错");
                    e.printStackTrace();
                }
            }
        }
    }

    private class leftThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // System.out.println(Client.dataDrawForALL.read());
                    jsonObjDraw = Client.dataDrawForALL.read();

                    System.out.println("当前线程：" + Thread.currentThread().getName());

                    drawPanel.repaint();

                } catch (InterruptedException e) {
                    System.out.println(Thread.currentThread().getName() + "线程出错");
                    e.printStackTrace();
                }
            }
        }
    }
    
    private class fileThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    JSONObject jsonObj = Client.dataFileForALL.read();

                    System.out.println("当前线程：" + Thread.currentThread().getName());

                    String fileName = (String) jsonObj.get("fileName");
                    JSONArray bArray = jsonObj.getJSONArray("message");
                    int size = bArray.length();
                    byte[] buffer = new byte[size];
                    for (int i = 0; i < size; i++) {
                        buffer[i] = (byte)( ( (int)bArray.get(i) ) & 0xFF)  ;
                        System.out.println(buffer[i]);
                    }
                    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("./src/file/"+fileName))) {
                        out.write(buffer);
                    } catch (IOException e) {
                        System.out.println("文件写入失败...");
                        e.printStackTrace();
                    }

                    

                } catch (InterruptedException e) {
                    System.out.println(Thread.currentThread().getName() + "线程出错");
                    e.printStackTrace();
                }
            }
        }
    }
    
}
