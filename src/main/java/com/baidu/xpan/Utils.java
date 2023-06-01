package com.baidu.xpan;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Utils {
    public static final Logger logger = LogManager.getLogger(Utils.class);

    public static String doHTTPPostRequest(String url, String postData, Map<String, String> headers) throws Exception {
        StringBuffer response = new StringBuffer();

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        conn.getOutputStream().write(postData.getBytes("UTF-8"));

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        Utils.logger.debug(
                "[msg: log request info] [url: {}] [headers: {}] [respheaders: {}] [response: {}] [postdata: {}]",
                url, headers, conn.getHeaderFields(), response, postData);
        return response.toString();
    }

    public static String doHTTPUploadRequest(String url, byte[] postData, Map<String, String> headers)
            throws Exception {
        StringBuffer response = new StringBuffer();

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        String boundary = Long.toHexString(System.currentTimeMillis());
        String CRLF = "\r\n";
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        try (OutputStream outputStream = conn.getOutputStream()) {
            outputStream.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));

            outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"tmpfile\"" + CRLF)
                    .getBytes(StandardCharsets.UTF_8));

            outputStream.write((CRLF).getBytes(StandardCharsets.UTF_8));
            outputStream.write(postData);
            outputStream.write((CRLF).getBytes(StandardCharsets.UTF_8));

            outputStream.write(("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        Utils.logger.debug("[msg: log request info] [url: {}] [headers: {}] [respheaders: {}] [response: {}]", url,
                headers, conn.getHeaderFields(), response);
        return response.toString();
    }

    public static String calBytesMD5(byte[] data) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(data, 0, data.length);
        byte[] chunkMd5 = md5.digest();
        return bytesToHex(chunkMd5);
    }

    public static List<String> calFileBlockMD5List(String filePath, int chunkSize) throws Exception {
        List<String> blockMD5List = new ArrayList<>();

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buffer = new byte[chunkSize];
            int bytesRead = 0;
            while ((bytesRead = bis.read(buffer)) != -1) {
                md5.update(buffer, 0, bytesRead);
                byte[] chunkMd5 = md5.digest();
                blockMD5List.add(bytesToHex(chunkMd5));
                md5.reset();
            }
        }

        Utils.logger.debug("[msg: call file block md5 finish] [blockcnt: {}] [blockmd5list: {}]", blockMD5List.size(),
                blockMD5List);
        return blockMD5List;
    }

    public static ArrayList<String> calFileBlockMD5List(String filePath, int chunkSize, int bufferSize)
            throws Exception {
        ArrayList<String> blockMD5List = new ArrayList<>();

        long totalLeftSize = new File(filePath).length();
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buffer = new byte[bufferSize];
            int bytesRead = 0;
            int chunkLeftSize = chunkSize;
            while ((bytesRead = bis.read(buffer, 0, Math.min(bufferSize, chunkLeftSize))) != -1) {
                md5.update(buffer, 0, bytesRead);
                chunkLeftSize -= bytesRead;
                totalLeftSize -= bytesRead;
                if (chunkLeftSize <= 0 || totalLeftSize <= 0) { // read one chunk finish or read file finish
                    byte[] chunkMd5 = md5.digest();
                    blockMD5List.add(bytesToHex(chunkMd5));
                    md5.reset();
                    chunkLeftSize = chunkSize; // reset left read size for next chunk
                }
            }
        }

        Utils.logger.debug("[msg: call file block md5 finish] [blockmd5list: {}] [blockcnt: {}]", blockMD5List.size(),
                blockMD5List);
        return blockMD5List;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
