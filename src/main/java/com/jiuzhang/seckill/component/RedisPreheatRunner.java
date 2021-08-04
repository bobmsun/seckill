package com.jiuzhang.seckill.component;

import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.po.SeckillActivity;
import com.jiuzhang.seckill.service.RedisService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

// 这个类，是为了，让我们在项目启动的时候，就把库存信息写到 redis 里面；把秒杀活动的信息，从数据库中，读到 redis 里边去
@Component     // @Component 告诉 Spirng Boot，这个东西由它来管理，有生命周期
public class RedisPreheatRunner implements ApplicationRunner {
    // implements ApplicationRunner 是为了让 Spring Boot 在启动的时候，自动执行这个类里边的方法

    @Resource
    private SeckillActivityDao seckillActivityDao;

    @Resource
    private RedisService redisService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<SeckillActivity> seckillActivities = seckillActivityDao.querySeckillActivitysByStatus(1);

        for (SeckillActivity seckillActivity : seckillActivities) {
            redisService.setValue("stock:" + seckillActivity.getId(), (long) seckillActivity.getAvailableStock());
        }
    }
}

