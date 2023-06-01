package com.baidu.xpan;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Unit test for BDXpanFileUploader.
 */
public class BDXpanFileUploaderTest {

        @Rule
        public TestWatcher watcher = new TestWatcher() {
                long startTime;

                @Override
                protected void starting(Description description) {
                        System.out.println(new Date() + " Starting test: " + description.getMethodName());
                        startTime = System.currentTimeMillis();
                }

                @Override
                protected void finished(Description description) {
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        System.out
                                        .println(new Date() + " Finished test: " + description.getMethodName()
                                                        + " (Time taken: "
                                                        + elapsedTime + "ms)");
                }
        };

        @Test
        public void uploadFileTest() throws Exception {
                BDXpanFileUploader bdxfu = new BDXpanFileUploader(
                                "123.4e4b483ab978a579d57a08df61b56b88.YlQsl952PMPiVpPGArcr20UQAK-J7jwO9RKwa3A.fcPzEQ",
                                "./data/Postman-osx-8.12.4-x64.zip", "/我的资源/Postman-osx-8.12.4-x64.zip");

                bdxfu.setRType(Const.RENAMETYPE_MD5CHECK).setUploadConcurrency(1);

                PreCreateResponse preCreateResponse = bdxfu.preCreate();
                assertTrue(preCreateResponse.getErrno() == 0);

                ArrayList<Entry<Integer, Superfile2UploadResponse>> failPartSeq = bdxfu
                                .superfile2Upload(preCreateResponse.getUploadID(), 3);
                assertTrue(failPartSeq.size() == 0);

                CreateResponse createResponse = bdxfu.create(0, preCreateResponse.getUploadID());
                assertTrue(createResponse.getErrno() == 0 || createResponse.getErrno() == -8);
        }

        @Test
        public void uploadFileConcurrencyTest() throws Exception {
                BDXpanFileUploader bdxfu = new BDXpanFileUploader();
                bdxfu.setAccessToken(
                                "123.4e4b483ab978a579d57a08df61b56b88.YlQsl952PMPiVpPGArcr20UQAK-J7jwO9RKwa3A.fcPzEQ")
                                .setLocalFilePath("./data/Postman-osx-8.12.4-x64.zip")
                                .setRemoteFilePath("/我的资源/Postman-osx-8.12.4-x64.zip")
                                .setRType(Const.RENAMETYPE_MD5CHECK)
                                .setUploadConcurrency(100);

                PreCreateResponse preCreateResponse = bdxfu.preCreate();
                assertTrue(preCreateResponse.getErrno() == 0);

                ArrayList<Entry<Integer, Superfile2UploadResponse>> failPartSeq = bdxfu
                                .superfile2Upload(preCreateResponse.getUploadID(), 3);
                assertTrue(failPartSeq.size() == 0);

                CreateResponse createResponse = bdxfu.create(0, preCreateResponse.getUploadID());
                assertTrue(createResponse.getErrno() == 0 || createResponse.getErrno() == -8);
        }

        @Test
        public void onestepUploadTest() throws Exception {
                BDXpanFileUploader bdxfu = new BDXpanFileUploader();
                bdxfu.setAccessToken(
                                "123.4e4b483ab978a579d57a08df61b56b88.YlQsl952PMPiVpPGArcr20UQAK-J7jwO9RKwa3A.fcPzEQ")
                                .setLocalFilePath("./data/Postman-osx-8.12.4-x64.zip")
                                .setRemoteFilePath("/我的资源/Postman-osx-8.12.4-x64.zip");

                int errno = bdxfu.onestepUpload();
                assertTrue(errno == 0 || errno == -8);
        }

        @Test
        public void onestepUploadWith256KBuffSizeSetTest() throws Exception {
                BDXpanFileUploader bdxfu = new BDXpanFileUploader();
                bdxfu.setAccessToken(
                                "123.4e4b483ab978a579d57a08df61b56b88.YlQsl952PMPiVpPGArcr20UQAK-J7jwO9RKwa3A.fcPzEQ")
                                .setLocalFilePath("./data/Postman-osx-8.12.4-x64.zip")
                                .setRemoteFilePath("/我的资源/Postman-osx-8.12.4-x64.zip")
                                .setRType(Const.RENAMETYPE_MD5CHECK)
                                .setChunkSize(Const.BUFFSIZE_32M)
                                .setUploadConcurrency(10);

                // 256K
                bdxfu.setBuffSize(Const.BUFFSIZE_256K);
                int errno = bdxfu.onestepUpload();
                assertTrue(errno == 0 || errno == -8);
        }

        @Test
        public void onestepUploadLageFileTest() throws Exception {
                BDXpanFileUploader bdxfu = new BDXpanFileUploader(
                                "123.4e4b483ab978a579d57a08df61b56b88.YlQsl952PMPiVpPGArcr20UQAK-J7jwO9RKwa3A.fcPzEQ",
                                "./data/output_file", "/我的资源/output_file");

                bdxfu.setRType(Const.RENAMETYPE_MD5CHECK).setUploadConcurrency(3)
                                .setChunkSize(Const.BUFFSIZE_4M).setBuffSize(Const.BUFFSIZE_256K);
                int errno = bdxfu.onestepUpload();
                assertTrue(errno == 0 || errno == -8);
        }

        @Test
        public void createDirTest() throws Exception {
                BDXpanFileUploader bdxfu = new BDXpanFileUploader();
                bdxfu.setAccessToken(
                                "123.4e4b483ab978a579d57a08df61b56b88.YlQsl952PMPiVpPGArcr20UQAK-J7jwO9RKwa3A.fcPzEQ")
                                .setLocalFilePath("./data/Postman-osx-8.12.4-x64.zip")
                                .setRemoteFilePath("/test/uploadapi/java/createdir");

                CreateResponse dirCreateResponse = bdxfu.create(1, "");
                assertTrue(dirCreateResponse.getErrno() == 0 || dirCreateResponse.getErrno() == -8);
        }
}
