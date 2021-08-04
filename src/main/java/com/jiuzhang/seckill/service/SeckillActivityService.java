package com.jiuzhang.seckill.service;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class SeckillActivityService {

    @Resource
    private RedisService service;

    public boolean seckillStockValidator(long activityId) {
        String key = "stock:" + activityId;
        return service.stockDeductValidation(key);
    }
    // 写完这个 service 层之后，我们需要写一个 controller 控制层，把这个方法给映射出去

}
