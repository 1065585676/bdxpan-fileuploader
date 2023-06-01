package com.baidu.xpan;

public class Error {
    public static final int ErrnoMD5NotMatch = -1; // 分片上传阶段，分片数据md5和预上传阶段对应分片md5值不一致
    public static final int ErrnoSuperfile2Upload = -2; // 分片上传阶段错误
    public static final int ErrnoBlockNumLimit = -3; // 超出最大分片数
}
