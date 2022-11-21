package com.github.starnowski.redisson.demo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.redisson.Redisson;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Check https://github.com/redisson/redisson/wiki/7.-distributed-collections
 *
 * https://github.com/redisson/redisson/issues/2220
 * https://medium.com/turtlemint-engineering-blog/handling-distributed-cache-and-synchronisation-locks-using-redisson-c1838331b0b2
 * https://redisson.org/glossary/cache-miss.html
 */
public class LocalCacheTest {

    public static final String KEY_1 = "key1";
    private static RLocalCachedMap<String, TestObject> rLocalCachedMap1;
    private static RLocalCachedMap<String, TestObject> rLocalCachedMap2;

    @BeforeAll
    static void setup() {
        Config config = new Config();
        // sudo docker run --name some-redis -p 6379:6379 redis
        // sudo docker run --name some-redis -p 6379:6379 -d redis
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379");

        RedissonClient client1 = Redisson.create(config);
        RedissonClient client2 = Redisson.create(config);
        LocalCachedMapOptions<String, TestObject> rLocalCachedMap1Options = LocalCachedMapOptions.defaults();
        rLocalCachedMap1Options.syncStrategy(LocalCachedMapOptions.SyncStrategy.UPDATE);
        LocalCachedMapOptions<String, TestObject> rLocalCachedMap2Options = LocalCachedMapOptions.defaults();
//        rLocalCachedMap2Options
//                .timeToLive(10, TimeUnit.SECONDS) // Available only for paid version
//        ;
        rLocalCachedMap1 = client1.getLocalCachedMap("anyMap", rLocalCachedMap1Options);
        rLocalCachedMap2 = client2.getLocalCachedMap("anyMap", rLocalCachedMap2Options);
    }

    @Order(1)
    @org.junit.jupiter.api.Test
    public void shouldUpdateMap()
    {
        // GIVEN
        TestObject testObject = new TestObject("First value");

        // WHEN
        rLocalCachedMap1.put(KEY_1, testObject);

        // THEN
        TestObject result = rLocalCachedMap1.get(KEY_1);
        assertAll("stored key",
                () -> assertEquals(result.getValue(), "First value")
        );
    }

    @Order(2)
    @org.junit.jupiter.api.Test
    public void shouldReadFromMap() throws InterruptedException {
        // GIVEN
        int maxRetries = 60;
        int delayInMilliseconds = 1000;
        TestObject result = null;

        // WHEN
        for (int i = 0; i < maxRetries; i++){
            System.out.println("shouldReadFromMap attempt: " + i);
            result = rLocalCachedMap2.get(KEY_1);
            System.out.println(String.format("Size : %d Size in memory: %d", rLocalCachedMap2.size(), rLocalCachedMap2.sizeInMemory()));
            if (result != null) {
                break;
            }
            Thread.sleep(delayInMilliseconds);
        }

        // THEN
        TestObject getOperationResult = result;
        assertAll("stored key from different map",
                () -> assertNotNull(getOperationResult, "key is null"),
                () -> assertEquals(getOperationResult.getValue(), "First value")
        );
    }

    private static class TestObject implements Serializable {

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public TestObject(String value) {
            this.value = value;
        }

        private String value;
    }

}
