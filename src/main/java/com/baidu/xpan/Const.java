package com.baidu.xpan;

public class Const {
    // 重命名类型
    public static final int RENAMETYPE_NORENAME = 0; // 不重命名，返回冲突错误
    public static final int RENAMETYPE_PATHCHECK = 1;// 只要 path 冲突即重命名
    public static final int RENAMETYPE_MD5CHECK = 2; // path 冲突且 block_list 不同才重命名
    public static final int RENAMETYPE_OVERWRITE = 3;// 覆盖

    // 分片大小
    public static final int CHUNKSIZE_4M = 4 * 1024 * 1024;
    public static final int CHUNKSIZE_16M = 16 * 1024 * 1024;
    public static final int CHUNKSIZE_32M = 32 * 1024 * 1024;
    // 分片最大数量
    public static final int MAX_CHUNKNUM = 1024;

    // 缓冲区大小
    public static final int BUFFSIZE_256K = 256 * 1024;
    public static final int BUFFSIZE_1M = 1 * 1024 * 1024;
    public static final int BUFFSIZE_4M = 4 * 1024 * 1024;
    public static final int BUFFSIZE_16M = 16 * 1024 * 1024;
    public static final int BUFFSIZE_32M = 32 * 1024 * 1024;
}
