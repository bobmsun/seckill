package com.jiuzhang.seckill;

import com.jiuzhang.seckill.service.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class RedisDemoTest {

    @Resource
    private RedisService redisService;

    @Test
    public void stockTest() {
        // 因为我们之前写 code 的时候考虑了链式调用，所以这里就可以用 链式调用
        String value = redisService.setValue("stock:19", 10L).getValue("stock:19");
        // redis 里边存储的是 string，所以取出来的是 string
        System.out.println(value);
        // 执行 test 时会发现，spring 日志中会打印："JedisPool 注入成功"，"Redis 地址：localhost:6,379"
    }
}
