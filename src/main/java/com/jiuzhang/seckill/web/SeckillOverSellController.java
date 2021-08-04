package com.jiuzhang.seckill.web;

import com.jiuzhang.seckill.service.SeckillActivityService;
import com.jiuzhang.seckill.service.SeckillOverSellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

@Controller
public class SeckillOverSellController {

    @Autowired         // 注入 service 为 controller 提供服务
    private SeckillOverSellService seckillOverSellService;

    @Resource
    private SeckillActivityService seckillActivityService;

    /**
     * 简单 处理抢购请求
     * @param seckillActivityId
     * @return
     */
    @ResponseBody          // 返回 ResponseBody，可以理解为是 Json 类型格式的
    @RequestMapping("/seckill_old/{seckillActivityId}")
    public String seckill(@PathVariable long seckillActivityId) {
        return seckillOverSellService.processSeckill(seckillActivityId);
    }
    // 以上是无法防止超买的 controller

    @ResponseBody
    @RequestMapping("/seckill/{seckillActivityId}")
    public String seckillCommodity(@PathVariable long seckillActivityId) {     // @PathVariable 是从路径里面过去
        boolean stockValidateResult = seckillActivityService.seckillStockValidator(seckillActivityId);
        return stockValidateResult ? "恭喜你秒杀成功" : "商品已售光，请下次再来";
    }

}
