package com.jiuzhang.seckill.service.mq;

import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.po.Order;
import com.jiuzhang.seckill.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component         // 告诉 Spring，只是一个组件，要扫描，并加到 Spring 的容器中去
@RocketMQMessageListener(topic = "pay_check", consumerGroup = "pay_check_group")       // 标记这个类是有个消息的监听者
public class PayStatusCheckListener implements RocketMQListener<MessageExt> {
    @Autowired
    private OrderDao orderDao;

//    @Autowired
//    private SeckillActivityDao seckillActivityDao;

//    @Autowired
//    private RedisService redisService;

    @Override
    public void onMessage(MessageExt messageExt) {
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("接受到订单支付状态校验消息：" + message);
        Order order = JSON.parseObject(message, Order.class);       // 把字符串转化为对象（要穿入一个要转化的类型）
        // 1. 查询订单
        Order orderInfo = orderDao.queryOrder(order.getOrderNo());
        if (orderInfo == null) {
            log.info("订单为空 " + order.getOrderNo());
            return;
        }
        // 2. 判断订单是否完成支付
        if (orderInfo.getOrderStatus() != 2) {      // 2 是 已付款，等待发货
            // 3. 未完成支付关闭订单
            log.info("未完成支付关闭订单（订单超时，未付款 --> 订单被关闭），订单号：" + orderInfo.getOrderNo());
            orderInfo.setOrderStatus(99);       // 把状态改为已支付状态
            orderDao.updateOrder(orderInfo);
            // 4. 恢复库存
            // seckillActivityDao.revertStock(order.getSeckillActivityId());
            // 5. 将用户从购买名单中删除
            // redisService.removeLimitMember(order.getSeckillActivityId(), order.getUserId());   // 把用户从限购名单中移除
        }
    }
}
