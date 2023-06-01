package com.baidu.xpan;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Unit test for Utils.
 */
public class UtilsTest {

    @Rule
    public TestWatcher watcher = new TestWatcher() {
        long startTime;

        @Override
        protected void starting(Description description) {
            System.out.println("Starting test: " + description.getMethodName());
            startTime = System.currentTimeMillis();
        }

        @Override
        protected void finished(Description description) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            System.out
                    .println("Finished test: " + description.getMethodName() + " (Time taken: " + elapsedTime + "ms)");
        }
    };

    @Test
    public void calFileBlockMD5ListTest() throws Exception {
        // 获取文件block list
        ArrayList<String> blockMD5List = Utils.calFileBlockMD5List("./data/Postman-osx-8.12.4-x64.zip",
                Const.CHUNKSIZE_4M, Const.BUFFSIZE_256K);
        assertTrue(blockMD5List.size() > 0);
    }
}
