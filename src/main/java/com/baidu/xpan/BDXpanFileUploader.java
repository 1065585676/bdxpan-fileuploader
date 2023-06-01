package com.baidu.xpan;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class Superfile2UploadResponse {
    @SerializedName("errno")
    private int errno;
    @SerializedName("request_id")
    private long requestID;
    @SerializedName("md5")
    private String md5;

    public int getErrno() {
        return errno;
    }

    public void setErrno(int errno) {
        this.errno = errno;
    }

    public long getRequestID() {
        return requestID;
    }

    public String getMd5() {
        return md5;
    }
}

class CreateResponse {
    @SerializedName("errno")
    private int errno;
    @SerializedName("fs_id")
    private long fsid;
    @SerializedName("path")
    private String path;
    @SerializedName("md5")
    private String md5;
    @SerializedName("category")
    private int category;
    @SerializedName("size")
    private long size;
    @SerializedName("isdir")
    private int isdir;
    @SerializedName("ctime")
    private long ctime;
    @SerializedName("mtime")
    private long mtime;

    public int getErrno() {
        return this.errno;
    }

    public long getFsid() {
        return this.fsid;
    }

    public String getPath() {
        return this.path;
    }

    public String getMd5() {
        return this.md5;
    }

    public int getCategory() {
        return this.category;
    }

    public long getSize() {
        return this.size;
    }

    public int getIsdir() {
        return this.isdir;
    }

    public long getCtime() {
        return this.ctime;
    }

    public long getMtime() {
        return this.mtime;
    }
}

class PreCreateResponse {
    @SerializedName("errno")
    private int errno;
    @SerializedName("request_id")
    private long requestID;
    @SerializedName("uploadid")
    private String uploadID;
    @SerializedName("block_list")
    private int[] blockList;
    @SerializedName("path")
    private String path;

    public int getErrno() {
        return this.errno;
    }

    public long getRequestID() {
        return this.requestID;
    }

    public String getUploadID() {
        return this.uploadID;
    }

    public int[] getBlockList() {
        int[] copy = new int[this.blockList.length];
        System.arraycopy(this.blockList, 0, copy, 0, copy.length);
        return copy;
    }

    public String getPath() {
        return this.path;
    }
}

public class BDXpanFileUploader {
    public static final Logger logger = LogManager.getLogger(BDXpanFileUploader.class);

    private String accessToken; // 授权信息
    private String localFilePath; // 本地文件路径
    private String remoteFilePath; // 远程（云端）上传路径

    private int chunkSize = Const.CHUNKSIZE_4M; // 分片上传大小，默认 4MB
    private int buffSize = Const.CHUNKSIZE_4M; // 文件IO缓冲区大小，默认 4MB
    private int rType = Const.RENAMETYPE_NORENAME; // 文件重命名类型，默认不重命名，返回冲突错误

    private ArrayList<String> blockMD5List = new ArrayList<>(); // 分片 MD5，预上传时计算，不会重复计算

    private int uploadConcurrency = 1; // 上传并发数
    private ExecutorService executor = Executors.newFixedThreadPool(this.uploadConcurrency); // 上传线程池

    public BDXpanFileUploader() {
    }

    public BDXpanFileUploader(String accessToken, String localFilePath, String remoteFilePath) {
        this.accessToken = accessToken;
        this.localFilePath = localFilePath;
        this.remoteFilePath = remoteFilePath;
    }

    public BDXpanFileUploader setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public BDXpanFileUploader setLocalFilePath(String localFilePath) {
        if (this.localFilePath.equals(localFilePath)) {
            return this;
        }

        this.localFilePath = localFilePath;
        this.blockMD5List.clear(); // reset
        return this;
    }

    public BDXpanFileUploader setRemoteFilePath(String remoteFilePath) {
        this.remoteFilePath = remoteFilePath;
        return this;
    }

    public BDXpanFileUploader setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    public BDXpanFileUploader setBuffSize(int buffSize) {
        this.buffSize = buffSize;
        return this;
    }

    public BDXpanFileUploader setRType(int rType) {
        this.rType = rType;
        return this;
    }

    public BDXpanFileUploader setUploadConcurrency(int uploadConcurrency) {
        if (uploadConcurrency > 0 && uploadConcurrency != this.uploadConcurrency) {
            this.uploadConcurrency = uploadConcurrency;
            this.executor.shutdown(); // 关闭线程池
            this.executor = Executors.newFixedThreadPool(this.uploadConcurrency); // 重启线程池
        }
        return this;
    }

    public PreCreateResponse preCreate() throws Exception {
        // 获取文件block list
        if (this.blockMD5List.isEmpty()) {
            this.blockMD5List = Utils.calFileBlockMD5List(this.localFilePath, this.chunkSize, this.buffSize);
        }

        String url = "https://pan.baidu.com/rest/2.0/xpan/file?method=precreate"
                + "&access_token=" + URLEncoder.encode(this.accessToken, "UTF-8");

        String postData = "autoinit=1"
                + "&path=" + URLEncoder.encode(this.remoteFilePath, "UTF-8")
                + "&size=" + new File(this.localFilePath).length()
                + "&block_list=" + new Gson().toJson(this.blockMD5List)
                + "&rtype=" + this.rType;

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Host", "pan.baidu.com");

        String response = Utils.doHTTPPostRequest(url, postData, headers);
        PreCreateResponse resp = new Gson().fromJson(response, PreCreateResponse.class);
        if (resp.getErrno() != 0) {
            BDXpanFileUploader.logger.error("[msg: precreate fail] [errno: {}]", resp.getErrno());
            return resp;
        }
        return resp;
    }

    public ArrayList<Map.Entry<Integer, Superfile2UploadResponse>> superfile2Upload(String uploadID) throws Exception {
        return this.superfile2Upload(uploadID, new ArrayList<>());
    }

    public ArrayList<Map.Entry<Integer, Superfile2UploadResponse>> superfile2Upload(String uploadID, int retry)
            throws Exception {
        ArrayList<Map.Entry<Integer, Superfile2UploadResponse>> result = new ArrayList<>();
        for (int i = 0; i < retry; i++) {
            result = this.superfile2Upload(uploadID,
                    result.stream().map(Map.Entry::getKey).collect(Collectors.toCollection(ArrayList::new)));
            if (result.size() == 0) {
                return result;
            }
            BDXpanFileUploader.logger.warn("[msg: superfile2 upload need retry] [retryidx: {}] [retryblockcnt: {}]",
                    i + 1, result.size());
        }
        return result;
    }

    private ArrayList<Map.Entry<Integer, Superfile2UploadResponse>> superfile2Upload(String uploadID,
            ArrayList<Integer> withSeqs)
            throws Exception {
        ArrayList<Map.Entry<Integer, Superfile2UploadResponse>> failPartSeq = new ArrayList<>();

        String url = "https://d.pcs.baidu.com/rest/2.0/pcs/superfile2?method=upload"
                + "&access_token=" + URLEncoder.encode(this.accessToken, "UTF-8")
                + "&type=tmpfile"
                + "&path=" + URLEncoder.encode(this.remoteFilePath, "UTF-8")
                + "&uploadid=" + URLEncoder.encode(uploadID, "UTF-8");

        // 创建任务列表
        ArrayList<Callable<Map.Entry<Integer, Superfile2UploadResponse>>> tasks = new ArrayList<>();

        if (withSeqs.isEmpty()) {
            long fileSize = new File(this.localFilePath).length(); // 文件总大小
            int chunks = (int) Math.ceil((double) fileSize / this.chunkSize); // 分片数量
            withSeqs = IntStream.range(0, chunks).boxed().collect(Collectors.toCollection(ArrayList::new));
        }
        for (final Integer seq : withSeqs) {
            tasks.add(() -> superfile2UploadData(url, seq));
        }

        // 执行任务并等待完成
        List<Future<Map.Entry<Integer, Superfile2UploadResponse>>> results = this.executor.invokeAll(tasks);

        // 收集上传失败的分片序号
        for (int i = 0; i < results.size(); i++) {
            Future<Map.Entry<Integer, Superfile2UploadResponse>> result = results.get(i);
            if (result.get().getValue().getErrno() != 0) {
                failPartSeq.add(result.get());
            }
        }
        return failPartSeq;
    }

    private Map.Entry<Integer, Superfile2UploadResponse> superfile2UploadData(String url, int seq) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(new File(this.localFilePath), "r");
        long fileSize = raf.length();

        long offset = seq * (long) this.chunkSize; // 偏移量
        int size = (int) Math.min(this.chunkSize, fileSize - offset); // 分片大小

        // 读取当前分片数据
        byte[] chunk = new byte[size];
        raf.seek(offset);
        raf.read(chunk);

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Host", "d.pcs.baidu.com");

        String response = Utils.doHTTPUploadRequest(url + "&partseq=" + seq, chunk, headers);
        Superfile2UploadResponse resp = new Gson().fromJson(response, Superfile2UploadResponse.class);
        if (resp.getErrno() != 0) {
            BDXpanFileUploader.logger.error("[msg: superfile2upload fail] [errno: {}] [seq: {}]", resp.getErrno(), seq);
            return new HashMap.SimpleEntry<>(seq, resp);
        }
        if (!resp.getMd5().equals(this.blockMD5List.get(seq))) { // 新增分片md5数据后验
            BDXpanFileUploader.logger.error(
                    "[msg: superfile2upload fail, block md5 not match] [blockmd5: {}] [respmd5: {}] [seq: {}]",
                    this.blockMD5List.get(seq), resp.getMd5(), seq);
            resp.setErrno(Error.ErrnoMD5NotMatch);
            return new HashMap.SimpleEntry<>(seq, resp);
        }

        return new HashMap.SimpleEntry<>(seq, resp);
    }

    public CreateResponse create(int isDir, String uploadID) throws Exception {
        String url = "https://pan.baidu.com/rest/2.0/xpan/file?method=create"
                + "&access_token=" + URLEncoder.encode(this.accessToken, "UTF-8");

        String postData = "&isdir=" + isDir
                + "&path=" + URLEncoder.encode(this.remoteFilePath, "UTF-8")
                + "&size=" + new File(this.localFilePath).length()
                + "&block_list=" + new Gson().toJson(this.blockMD5List)
                + "&rtype=" + this.rType
                + "&uploadid=" + URLEncoder.encode(uploadID, "UTF-8");

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Host", "pan.baidu.com");

        String response = Utils.doHTTPPostRequest(url, postData, headers);
        CreateResponse resp = new Gson().fromJson(response, CreateResponse.class);
        if (resp.getErrno() != 0) {
            BDXpanFileUploader.logger.error("[msg: create fail] [errno: {}]", resp.getErrno());
            return resp;
        }
        return resp;
    }

    public int onestepUpload() throws Exception {
        PreCreateResponse preCreateResponse = this.preCreate();
        if (preCreateResponse.getErrno() != 0) {
            return preCreateResponse.getErrno();
        }
        BDXpanFileUploader.logger.info("[msg: precreate succ]");

        ArrayList<Entry<Integer, Superfile2UploadResponse>> failPartSeq = this
                .superfile2Upload(preCreateResponse.getUploadID(), 3);
        if (failPartSeq.size() > 0) {
            BDXpanFileUploader.logger.error("[msg: superfile2upload fail] [failpartseq: {}]", failPartSeq);
            return Error.ErrnoSuperfile2Upload;
        }
        BDXpanFileUploader.logger.info("[msg: superfile2upload succ]");

        CreateResponse createResponse = this.create(0, preCreateResponse.getUploadID());
        if (createResponse.getErrno() != 0) {
            return createResponse.getErrno();
        }
        BDXpanFileUploader.logger.info("[msg: create succ]");

        return 0;
    }
}
