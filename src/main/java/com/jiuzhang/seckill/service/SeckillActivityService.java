package com.jiuzhang.seckill.service;

import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.dao.SeckillCommodityDao;
import com.jiuzhang.seckill.db.po.Order;
import com.jiuzhang.seckill.db.po.SeckillActivity;
import com.jiuzhang.seckill.db.po.SeckillCommodity;
import com.jiuzhang.seckill.service.mq.RocketMQService;
import com.jiuzhang.seckill.util.SnowFlake;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Slf4j
@Service
public class SeckillActivityService {

    @Resource
    private RedisService service;

    @Resource
    private SeckillActivityDao seckillActivityDao;

    @Resource
    private RocketMQService rocketMQService;

    private SnowFlake snowFlake = new SnowFlake(1, 1);

    @Resource
    private OrderDao orderDao;

    @Resource
    private SeckillCommodityDao seckillCommodityDao;


    /**
     * 判断秒杀库存
     *
     * @param activityId
     * @return
     */
    public boolean seckillStockValidator(long activityId) {
        String key = "stock:" + activityId;
        return service.stockDeductValidation(key);
    }
    // 写完这个 service 层之后，我们需要写一个 controller 控制层，把这个方法给映射出去

    /**
     * 创建订单
     *
     * @param seckillActivityId
     * @param userId
     * @return
     * @throws Exception
     */
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

        // 在发送以上订单创建的消息的同时，也发送一个延迟消息 (在创建订单时，发送延迟消息)
        /*
         * 3.发送订单付款状态校验消息
         * 开源RocketMQ支持延迟消息，但是不支持秒级精度。默认支持18个level的延迟消息，这是通过broker端的messageDelayLevel配置项确定的，如下：
         * messageDelayLevel=1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
         */
        // rocketMQService.sendDelayMessage("pay_check",JSON.toJSONString(order),5);        // 延迟 1m

        return order;
    }

    /**
     * 订单支付完成处理
     * @param orderNo
     */
    public void payOrderProcess(String orderNo) {
        // 实际中，这里应该调用支付宝、微信的接口；这里就不做了，假设直接支付成功
        log.info("完成支付订单 订单号：" + orderNo);
        Order order = orderDao.queryOrder(orderNo);

        // 之前是先 锁定库存；现在缴费成功后，扣减库存
        boolean deductStockResult = seckillActivityDao.deductStock(order.getSeckillActivityId());

        if (deductStockResult) {
            order.setPayTime(new Date());
            // 0 没有可用库存，无效订单
            // 1 已创建并等待付款
            // 2 完成支付
            order.setOrderStatus(2);
            orderDao.updateOrder(order);
        }
    }

    /**
     * 将秒杀详情相关信息导入 redis     (秒杀开始前两天，可以进行预热，通过执行这个函数，将数据写到 redis 中去)
     * @param seckillActivityId
     */
    public void pushSeckillInfoToRedis(long seckillActivityId) {
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        service.setValue("seckillActivity:" + seckillActivityId, JSON.toJSONString(seckillActivity));

        // 缓存活动信息后，还有缓存商品信息
        SeckillCommodity seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        service.setValue("seckillCommodity:" + seckillActivity.getCommodityId(), JSON.toJSONString(seckillCommodity));
    }
}
