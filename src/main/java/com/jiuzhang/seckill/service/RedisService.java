package com.jiuzhang.seckill.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;

@Service
public class RedisService {

    // @Autowired      // 让 Spring Boot 自己来注入
    @Resource
    private JedisPool jedisPool;

    /**
     * 设置值
     *
     * @param key
     * @param value
     * @return
     */
    // public void setValue(String key, Long value) {
    public RedisService setValue(String key, Long value) {
        Jedis client = jedisPool.getResource();         // 获取一个 客户端
        client.set(key, value.toString());
        client.close();
        return this;     //
    }
    // service.setValue(..).getValue(). ...   想要可以 链式调用


    /**
     * 获取值
     *
     * @param key
     * @return
     */
    public String getValue(String key) {
        Jedis client = jedisPool.getResource();      // 获取一个 客户端
        String value = client.get(key);
        client.close();
        return value;
    }
}
