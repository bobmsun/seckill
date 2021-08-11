package com.jiuzhang.seckill.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.util.Collections;

@Slf4j

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

    /**
     * 缓存中库存判断和扣减
     * @param key
     * @return
     * @throws Exception
     */
    // 以下方法实现了库存扣减的原子操作
    public boolean stockDeductValidation(String key) {
        try (Jedis client = jedisPool.getResource()) {
            // 先判断 key 是否存在，再判断库存余量；如果存在，就扣减
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
            System.out.println("恭喜，抢购成功 (from redis 扣减)");
            return true;
        } catch (Throwable throwable) {
            System.out.println("库存扣减失败: " + throwable.toString());
            return false;
        }
    }

    /**
     * 添加限购名单
     * @param activityId
     * @param userId
     */
    public void addLimitMember(long activityId, long userId) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.sadd("seckillActivity_users:" + activityId, String.valueOf(userId));      // 这个活动中有哪些人参与了
    }

    /**
     * 判断是否在限购名单当中
     * @param activityId
     * @param userId
     * @return
     */
    // 判断一个用户是否在限购名单里面
    public boolean isInLimitMember(long activityId, long userId) {
        Jedis jedisClient = jedisPool.getResource();
        boolean sismember = jedisClient.sismember("seckillActivity_users:" + activityId, String.valueOf(userId));
        log.info("userId:{} activityId:{} 在已购名单中:{}", activityId, userId, sismember);
        return sismember;
    }

    /**
     * 移除限购名单
     * @param activityId
     * @param userId
     */
    // 订单超时未付款后，订单会被关闭，这时要把 user 从限购名单中移除，因为毕竟这次购买没有成功
    public void removeLimitMember(long activityId, long userId) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.srem("seckillActivity_users:" + activityId, String.valueOf(userId));
    }

}
