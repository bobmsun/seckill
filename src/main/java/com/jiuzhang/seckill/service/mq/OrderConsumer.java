package com.jiuzhang.seckill.service.mq;

import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.po.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component        // 告诉 Sping Boot 是一个由他来管理的类
@RocketMQMessageListener(topic = "seckill_order", consumerGroup = "seckill_order_group")
public class OrderConsumer implements RocketMQListener<MessageExt> {

    @Resource
    private OrderDao orderDao;

    @Resource
    private SeckillActivityDao seckillActivityDao;

    @Override
    @Transactional
    public void onMessage(MessageExt messageExt) {
        // 解析创建订单的请求消息
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("接收到了创建订单的请求： " + message);     // 这里的 log.info() 就是我们通过 @Slf4j 这个注解注入进来的一个日志的实例
        Order order = JSON.parseObject(message, Order.class);      // 解析，反序列化
        order.setCreateTime(new Date());

        // 2. 锁定库存
        boolean lockStockResult = seckillActivityDao.lockStock(order.getSeckillActivityId());       // lockStock 在 mapper 中的 sql 用到了数据库的乐观锁
        if (lockStockResult) {
            // 锁定成功
            // 1 = 已创建，等待付款
            order.setOrderStatus(1);
        } else {
            // 0 = 创建失败，没有可用库存
            order.setOrderStatus(0);
        }

        // 3. 插入订单
        orderDao.insertOrder(order);
    }
}
