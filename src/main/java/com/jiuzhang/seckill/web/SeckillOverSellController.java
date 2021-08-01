package com.jiuzhang.seckill.web;

import com.jiuzhang.seckill.service.SeckillOverSellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SeckillOverSellController {

    @Autowired         // 注入 service 为 controller 提供服务
    private SeckillOverSellService seckillOverSellService;

    /**
     * 简单 处理抢购请求
     * @param seckillActivityId
     * @return
     */
    @ResponseBody          // 返回 ResponseBody，可以理解为是 Json 类型格式的
    @RequestMapping("/seckill/{seckillActivityId}")
    public String seckill(@PathVariable long seckillActivityId) {
        return seckillOverSellService.processSeckill(seckillActivityId);
    }

}
