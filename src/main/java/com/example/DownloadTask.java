package com.example;

import org.apache.commons.io.FileUtils;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class DownloadTask implements Runnable {

    private String fileUrl; // 文件的URL地址
    private String fileName; // 文件名
    private int row; // 表格中的行数

    private long totalSize; // 文件总大小，以字节为单位
    private long downloadedSize = 0; // 已下载的大小，以字节为单位

    private DefaultTableModel tableModel; // 表格模型，用于更新表格数据
    private JProgressBar progressBar; // 进度条，显示下载进度
    private String destDirectory; // 文件下载目录

    private volatile boolean paused = false; // 标记是否暂停下载
    private volatile boolean cancelled = false; // 标记是否取消下载

    public DownloadTask(String fileUrl, String fileName, int row, DefaultTableModel tableModel, JProgressBar progressBar, String destDirectory) {
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.row = row;
        this.tableModel = tableModel;
        this.progressBar = progressBar;
        this.destDirectory = destDirectory;
    }

    // 获取文件大小的方法
    public long getFileSize(String fileUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.getInputStream();
            return conn.getContentLengthLong(); // 获取文件大小
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // 出错时返回-1
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public void run() {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        File outputFile = null;

        long fileSizeBytes = getFileSize(this.fileUrl); // 获取文件大小
        String fileSizeDisplay = fileSizeBytes == -1 ? "未知" : humanReadableByteCountBin(fileSizeBytes); // 将文件大小转换为易读格式

        // 提取文件扩展名
        String fileExtension;
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            fileExtension = fileName.substring(i + 1);
        } else {
            fileExtension = "";
        }

        try {

            //核心下载部分
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            int fileSize = connection.getContentLength(); // 获取文件大小
            SwingUtilities.invokeLater(() -> {
                tableModel.setValueAt(fileExtension, row, 1); // 在表格中设置文件扩展名
                tableModel.setValueAt("下载中", row, 2); // 在表格中设置下载状态为“下载中”
                tableModel.setValueAt(fileSizeDisplay, row, 4); // 在表格中设置文件大小
            });

            inputStream = connection.getInputStream();
            outputFile = new File(destDirectory, fileName); // 创建要下载的文件
            //当执行完成后，outputStream会自动关闭，释放资源
            //这样可以避免忘记关闭资源而造成的资源泄露问题
            try (var outputStream = FileUtils.openOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1 && !cancelled) {
                    while (paused && !cancelled) {
                        Thread.sleep(1000); // 如果暂停下载，线程休眠一秒钟
                    }
                    outputStream.write(buffer, 0, bytesRead); // 将数据写入文件
                    totalBytesRead += bytesRead; // 更新已下载的大小
                    int progress = (int) ((totalBytesRead * 100) / fileSize); // 计算下载进度百分比

                    String remainingSizeDisplay = fileSizeBytes > 0 ? humanReadableByteCountBin(fileSizeBytes - totalBytesRead) : "未知";

                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress); // 更新进度条
                        tableModel.setValueAt(progress, row, 3); // 在表格中设置下载进度
                        tableModel.setValueAt(progress + "%", row, 2); // 在表格中设置下载状态为百分比形式
                        tableModel.setValueAt(remainingSizeDisplay, row, 5); // 在表格中设置剩余大小
                    });

                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException(); // 如果线程被中断，则抛出异常
                    }
                }

                if (cancelled && totalBytesRead < fileSize) {
                    // 如果任务被取消并且文件未完全下载，则删除文件
                    outputFile.delete();
                    SwingUtilities.invokeLater(() -> tableModel.setValueAt("已取消", row, 2)); // 在表格中设置下载状态为“已取消”
                } else {
                    SwingUtilities.invokeLater(() -> tableModel.setValueAt("已完成", row, 2)); // 在表格中设置下载状态为“已完成”
                }
            }

        } catch (InterruptedException e) {
            SwingUtilities.invokeLater(() -> tableModel.setValueAt("已暂停", row, 2)); // 在表格中设置下载状态为“已暂停”
            if (outputFile != null && outputFile.exists() && !outputFile.isDirectory()) {
                outputFile.delete(); // 如果文件存在且不是目录，则删除文件
            }
        } catch (Exception e) {
            if (!cancelled) {
                SwingUtilities.invokeLater(() -> tableModel.setValueAt("错误", row, 2)); // 在表格中设置下载状态为“错误”
            }
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception ignored) {
            }
        }
    }

    // 将字节大小转换为易读格式的方法
    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %cB", value / 1024.0, ci.current());
    }

    // 暂停下载的方法
    public void pause() {
        paused = true;
    }

    // 恢复下载的方法
    public void resume() {
        paused = false;
    }

    // 检查是否已暂停下载的方法
    public boolean isPaused() {
        return paused;
    }

    // 取消下载的方法
    public void cancel() {
        cancelled = true;
        Thread.currentThread().interrupt(); // 中断线程
    }

    // 获取文件路径的方法
    public String getFilePath() {
        return destDirectory + File.separator + fileName;
    }
}
