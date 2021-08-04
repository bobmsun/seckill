package com.jiuzhang.seckill.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.util.Collections;

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


    // 以下方法实现了库存扣减的原子操作
    public boolean stockDeductValidation(String key) {
        try (Jedis client = jedisPool.getResource()) {
            String script = "if redis.call('exists', KEYS[1]) == 1 then\n" +
                    "    local stock = tonumber(redis.call('get', KEYS[1]))\n" +
                    "    if (stock <= 0) then\n" +
                    "        return -1\n" +
                    "    end;\n" +
                    "\n" +
                    "    redis.call('decr', KEYS[1]);\n" +
                    "    return stock - 1;\n" +
                    "end;\n" +
                    "\n" +
                    "return -1;";
            Long stock = (Long) client.eval(script, Collections.singletonList(key), Collections.emptyList());

            if (stock < 0) {
                System.out.println("库存不足");
                return false;
            }
            System.out.println("恭喜，抢购成功");
            return true;
        } catch (Throwable throwable) {
            System.out.println("库存扣减失败: " + throwable.toString());
            return false;
        }
    }


}
