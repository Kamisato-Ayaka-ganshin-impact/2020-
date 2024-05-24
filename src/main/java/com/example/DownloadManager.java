package com.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DownloadManager extends JFrame {

    private JTable table;
    private DefaultTableModel tableModel;
    private ExecutorService executorService;

    private Map<Integer, Future<?>> currentTasks = new HashMap<>();
    private Map<Integer, DownloadTask> downloadTasks = new HashMap<>();

    JLabel background = new JLabel(new ImageIcon("backgroundmain.png"));

    public boolean isDownloading() {
        return isDownloading;
    }

    private boolean isDownloading = false;

    private Future<?> currentTask;
    private DownloadTask downloadTask;

    public DownloadManager() {
        setTitle("下载器");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);


        // 设置窗口图标
        ImageIcon icon = new ImageIcon("icon.png"); // 替换为你的图标文件路径
        setIconImage(icon.getImage());


        tableModel = new DefaultTableModel(new Object[]{"文件名", "文件类型", "文件状态", "下载进度条", "文件总大小", "文件剩余大小"}, 0);
        table = new JTable(tableModel);
        table.getColumnModel().getColumn(3).setCellRenderer(new ProgressBarRenderer());

        // 添加鼠标监听器以处理双击事件
        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    JTable eventTable = (JTable) mouseEvent.getSource();
                    int row = eventTable.rowAtPoint(mouseEvent.getPoint());
                    if (row >= 0) {
                        String status = (String) tableModel.getValueAt(row, 2);
                        if ("已完成".equals(status)) {
                            openFileLocation(row);
                        }
                    }
                }
            }
        });

        setBackGround();

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel panel = new JPanel();
        JButton addButton = new JButton("新建下载");
        JButton startButton = new JButton("继续下载");
        JButton stopButton = new JButton("暂停下载");
        JButton deleteButton = new JButton("删除任务");

        addButton.addActionListener(e -> addDownload());
        startButton.addActionListener(e -> startButton());
        stopButton.addActionListener(e -> stopButton());
        deleteButton.addActionListener(e -> deleteButton());

        panel.add(addButton);
        panel.add(startButton);
        panel.add(stopButton);
        panel.add(deleteButton);


        add(panel, BorderLayout.SOUTH);

        executorService = Executors.newFixedThreadPool(4);
    }

    // 添加下载任务
    private void addDownload() {
        String fileUrl = JOptionPane.showInputDialog(this, "请输入文件的URL地址，仅支持HTTP链接");
        if (fileUrl != null && !fileUrl.trim().isEmpty()) {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);

            String fileSize = "未知";

            int row = tableModel.getRowCount();
            tableModel.addRow(new Object[]{fileName, "未知", "等待中", 0, fileSize});

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = fileChooser.showDialog(this, "选择存放的文件夹");
            if (option == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = fileChooser.getSelectedFile();
                String destDirectory = selectedFolder.getAbsolutePath();

                DownloadTask task = new DownloadTask(fileUrl, fileName, row, tableModel, progressBar, destDirectory);
                Future<?> future = executorService.submit(task);
                currentTasks.put(row, future);
                downloadTasks.put(row, task);
                isDownloading = true;
                //deleteImage();
            } else {
                tableModel.removeRow(row);
            }
        }
    }

    // 暂停下载任务
    public void stopButton() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            DownloadTask task = downloadTasks.get(selectedRow);
            if (task != null) {
                task.pause();
            }
        }
    }

    // 继续下载任务
    public void startButton() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            DownloadTask task = downloadTasks.get(selectedRow);
            if (task != null) {
                task.resume();
            }
        }
    }

    // 设置背景图片
    private void setBackGround() {
        background.setBounds(0, 0, 600, 400);
        this.getContentPane().add(background);
        getContentPane().repaint();
    }

    // 删除背景图片
    private void deleteImage() {
        this.getContentPane().remove(background);
        getContentPane().repaint();
    }

    // 删除下载任务
    public void deleteButton() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
            Future<?> future = currentTasks.get(selectedRow);
            if (future != null) {
                downloadTasks.get(selectedRow).cancel();
                future.cancel(true);
            }
            currentTasks.remove(selectedRow);
            downloadTasks.remove(selectedRow);
            tableModel.removeRow(selectedRow);
        }
    }



    // 打开文件所在位置
    private void openFileLocation(int row) {
        try {
            DownloadTask task = downloadTasks.get(row);
            if (task != null) {
                String filePath = task.getFilePath();
                File file = new File(filePath);

                if (!file.exists()) {
                    JOptionPane.showMessageDialog(this, "文件不存在", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(file.getParentFile());
                } else {
                    JOptionPane.showMessageDialog(this, "打开行为不支持", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "在打开本地文件时出现一个错误", "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void start() {
        SwingUtilities.invokeLater(() -> {
            DownloadManager manager = new DownloadManager();
            manager.setVisible(true);
        });
    }
}
