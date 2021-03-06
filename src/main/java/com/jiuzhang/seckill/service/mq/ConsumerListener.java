package com.jiuzhang.seckill.service.mq;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

// 这个类是实现 消费者
@Component
@RocketMQMessageListener(topic = "test-jiuzhang", consumerGroup = "consumerGroup-jiuzhang")
public class ConsumerListener implements RocketMQListener<MessageExt>{

    @Override
    public void onMessage(MessageExt messageExt) {
        try {
            String body = new String(messageExt.getBody(), "UTF-8");
            System.out.println("Receive message: " + body);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

}

// 这个类是测试用的，不是实现业务逻辑用的
