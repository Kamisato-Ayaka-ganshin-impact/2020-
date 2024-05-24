package com.example;


import javax.swing.*;

import java.util.Timer;
import java.util.TimerTask;

public class BackgroundImage {
    public static void main(String[] args) {


        // SwingUtilities的静态方法invokeLater，其作用是将指定的Runnable任务在事件处理线程中延迟执行
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        // 创建 JFrame
        JFrame frame = new JFrame("下载器");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        //图标
        ImageIcon icon = new ImageIcon("icon.png");

        // 设置图标
        frame.setIconImage(icon.getImage());



        // 设置背景图
        ImageIcon backgroundImage = new ImageIcon("backgroundlaunch.png");
        JLabel backgroundLabel = new JLabel(backgroundImage);
        backgroundLabel.setBounds(0, 0, 800, 600);

        // 将背景图添加到 content pane
        frame.getContentPane().add(backgroundLabel);

        // 使窗口居中显示
        frame.setLocationRelativeTo(null);
        frame.setLayout(null);
        frame.setResizable(false);
        frame.setVisible(true);

        // 等待1.5秒后关闭窗口并启动 DownloadManager 类中的 start 函数
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                frame.dispose();
                DownloadManager.start();
                timer.cancel(); // 关闭定时器
            }
        }, 1800);
    }
}
