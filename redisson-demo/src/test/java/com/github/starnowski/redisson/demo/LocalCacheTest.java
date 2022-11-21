package com.github.starnowski.redisson.demo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.redisson.Redisson;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Check https://github.com/redisson/redisson/wiki/7.-distributed-collections
 */
public class LocalCacheTest {

    private static RedissonClient client;
    private static RLocalCachedMap<String, TestObject> rLocalCachedMap1;
    private static RLocalCachedMap<String, TestObject> rLocalCachedMap2;

    @BeforeAll
    static void setup() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379");

        client = Redisson.create(config);
        rLocalCachedMap1 = client.getLocalCachedMap("anyMap", LocalCachedMapOptions.defaults());
        rLocalCachedMap2 = client.getLocalCachedMap("anyMap", LocalCachedMapOptions.defaults());
    }

    @Order(1)
    @org.junit.jupiter.api.Test
    public void shouldUpdateMap()
    {
        // GIVEN
        TestObject testObject = new TestObject("First value");

        // WHEN
        rLocalCachedMap1.put("key1", testObject);

        // THEN
        TestObject result = rLocalCachedMap1.get("key1");
        assertAll("numbers",
                () -> assertEquals(result.getValue(), "First value")
        );
    }

    private static class TestObject {

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