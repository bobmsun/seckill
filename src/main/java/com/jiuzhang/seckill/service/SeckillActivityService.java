package com.jiuzhang.seckill.service;

import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.po.Order;
import com.jiuzhang.seckill.db.po.SeckillActivity;
import com.jiuzhang.seckill.service.mq.RocketMQService;
import com.jiuzhang.seckill.util.SnowFlake;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class SeckillActivityService {

    @Resource
    private RedisService service;

    @Resource
    private SeckillActivityDao seckillActivityDao;

    @Resource
    private RocketMQService rocketMQService;

    private SnowFlake snowFlake = new SnowFlake(1, 1);

    public boolean seckillStockValidator(long activityId) {
        String key = "stock:" + activityId;
        return service.stockDeductValidation(key);
    }
    // 写完这个 service 层之后，我们需要写一个 controller 控制层，把这个方法给映射出去


    public Order createOrder(long seckillActivityId, long userId) throws Exception {
        /*
         * 1.创建订单
         */
        SeckillActivity activity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        Order order = new Order();

        // 采用雪花算法生成订单 ID
        order.setOrderNo(String.valueOf(snowFlake.nextId()));
        order.setSeckillActivityId(activity.getId());
        order.setUserId(userId);
        order.setOrderAmount(activity.getSeckillPrice().longValue());

        /*
         *2.发送创建订单消息
         */
        // 向消息队列中发送创建订单消息
        rocketMQService.sendMessage("seckill_order", JSON.toJSONString(order));

        return order;
    }

}
