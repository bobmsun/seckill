package com.jiuzhang.seckill.web;

import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.dao.SeckillCommodityDao;
import com.jiuzhang.seckill.db.po.Order;
import com.jiuzhang.seckill.db.po.SeckillActivity;
import com.jiuzhang.seckill.db.po.SeckillCommodity;
import com.jiuzhang.seckill.service.RedisService;
import com.jiuzhang.seckill.service.SeckillActivityService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller         // 标示这个类是 controller
public class SeckillActivityController {

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private SeckillCommodityDao seckillCommodityDao;

    @Autowired
    private SeckillActivityService seckillActivityService;

    @Resource
    private OrderDao orderDao;

    @Resource
    RedisService redisService;


    /**
     * 跳转 发布活动页面
     * @return
     */
    @RequestMapping("/addSeckillActivity")         // 标示在哪个路径来访问它
    public String addSeckillActivity() {
        return "add_activity";
    }

    //@ResponseBody                       // 有了这个注释后，Spring Boot 就不会去找 template 了
    @RequestMapping("/addSeckillActivityAction")
    public String addSeckillActivityAction(
            @RequestParam("name") String name,
            @RequestParam("commodityId") long commodityId,
            @RequestParam("seckillPrice") BigDecimal seckillPrice,
            @RequestParam("oldPrice") BigDecimal oldPrice,
            @RequestParam("seckillNumber") long seckillNumber,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            Map<String, Object> resultMap                  // 把返回的内容放在 map 中，让 spring boot 进行渲染
                                                           // （试了一下，这里 resultMap 的名字可以改）
                                                           // （还试了一下，把 value 的类型 Object 改成 SeckilActivity 也是可以的）
    ) throws ParseException {
//        System.out.println(name);
//        System.out.println(startTime);
//        System.out.println(endTime);
        startTime = startTime.substring(0, 10) +  startTime.substring(11);
        endTime = endTime.substring(0, 10) +  endTime.substring(11);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddhh:mm");
        SeckillActivity seckillActivity = new SeckillActivity();
        seckillActivity.setName(name);
        seckillActivity.setCommodityId(commodityId);
        seckillActivity.setSeckillPrice(seckillPrice);
        seckillActivity.setOldPrice(oldPrice);
        seckillActivity.setTotalStock(seckillNumber);
        seckillActivity.setAvailableStock(new Integer("" + seckillNumber));
        seckillActivity.setLockStock(0L);
        seckillActivity.setActivityStatus(1);
        seckillActivity.setStartTime(format.parse(startTime));
        seckillActivity.setEndTime(format.parse(endTime));
        seckillActivityDao.insertSeckillActivity(seckillActivity);

        // return seckillActivity.toString();
        resultMap.put("seckillActivity", seckillActivity);
        return "add_success";
    }

    /**
     * 查询秒杀活动的列表
     * @param resultMap
     * @return
     */
    @RequestMapping("/seckills")
    public String activityList(
            Map<String, Object> resultMap
    ) {
        List<SeckillActivity> seckillActivities = seckillActivityDao.querySeckillActivitysByStatus(1);     // 得到所有没有下架的 activity
        resultMap.put("seckillActivities", seckillActivities);
        return "seckill_activity";
    }

    /**
     * 秒杀商品详情
     * @param resultMap
     * @param seckillActivityId
     * @return
     */
    @RequestMapping("/item/{seckillActivityId}")      // 这里用了 path variable 路径变量       // 7.3缓存预热实现（第6章）中 是 /seckill/detail/{seckillActivity}
    public String itemPage(
            @PathVariable long seckillActivityId,
            Map<String, Object> resultMap
    ) {
        // （以下为 7.3缓存预热实现 (第六章) 视频中的）以下为缓存预热 的 逻辑 （提前把 秒杀活动信息 和 秒杀商品信息 缓存到 redis 中，查询时，先查缓存，如果缓存有，就不到数据库中查了）
        SeckillActivity seckillActivity;
        SeckillCommodity seckillCommodity;

        String seckillActivityInfo = redisService.getValue("seckillActivity:" + seckillActivityId);
        if (StringUtils.isNotEmpty(seckillActivityInfo)) {
            log.info("redis缓存数据:" + seckillActivityInfo);        // 从 redis 缓存中读出数据，打开页面的速度会快一点
            seckillActivity = JSON.parseObject(seckillActivityInfo, SeckillActivity.class);
        } else {
            seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        }

        String seckillCommodityInfo = redisService.getValue("seckillCommodity:" + seckillActivity.getCommodityId());
        if (StringUtils.isNotEmpty(seckillCommodityInfo)) {
            log.info("redis缓存数据:" + seckillCommodityInfo);
            seckillCommodity = JSON.parseObject(seckillActivityInfo, SeckillCommodity.class);
        } else {
            seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        }

//        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
//        SeckillCommodity seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        resultMap.put("seckillActivity", seckillActivity);
        resultMap.put("seckillCommodity", seckillCommodity);
        resultMap.put("seckillPrice", seckillActivity.getSeckillPrice());
        resultMap.put("oldPrice", seckillActivity.getOldPrice());
        resultMap.put("commodityId", seckillActivity.getCommodityId());
        resultMap.put("commodityName", seckillCommodity.getCommodityName());
        resultMap.put("commodityDesc", seckillCommodity.getCommodityDesc());
        return "seckill_item";
    }

    /**
     * 处理抢购请求
     * @param userId
     * @param seckillActivityId
     * @return
     */
    @ResponseBody
    @RequestMapping("/seckill/buy/{userId}/{seckillActivityId}")
    public ModelAndView seckillCommodity(
            @PathVariable long userId,
            @PathVariable long seckillActivityId
    ) {
        boolean stockValidateResult = false;
        ModelAndView modelAndView = new ModelAndView();       // 稍后要返回给浏览器的一个渲染界面

        try {
            /**
             * 判断用品是否在已购买名单中
             */
//            if (redisService.inInLimitMember(seckillActivityId, userId)) {
//                // 提示用户已经在购买名单中
//                modelAndView.addObject("resultInfo", "对不起，您已经在购买名单中");
//                modelAndView.setViewName("seckill_result");
//                return modelAndView;
//            }
            /**
             * 确认是否能够进行秒杀
             */
            stockValidateResult = seckillActivityService.seckillStockValidator(seckillActivityId);    // 去 redis 里边看
            if (stockValidateResult) {
                Order order = seckillActivityService.createOrder(seckillActivityId, userId);
                modelAndView.addObject("resultInfo","秒杀成功，订单创建中，订单 ID：" + order.getOrderNo());
                modelAndView.addObject("orderNo", order.getOrderNo());

                // 本次秒杀成功后，就要把用户加到限购名单当中去，来表明 "用户买了"
                // redisService.addLimitMember(seckillActivityId, userId);
            } else {
                modelAndView.addObject("resultInfo", "对不起，商品库存不足");
            }
        } catch (Exception exception) {
            log.error("秒杀活动异常：", exception.toString());
            modelAndView.addObject("resultInfo", "秒杀失败");
        }

        modelAndView.setViewName("seckill_result");
        return modelAndView;
    }

    /**
     * 订单查询 详情
     * @param orderNo
     * @return   这个 function 同上面一样，同样是返回一个 modelAndView 对象
     */
    @RequestMapping("/seckill/orderQuery/{orderNo}")        // 这里 specify 一个路径
    public ModelAndView orderQuery(
            @PathVariable String orderNo
    ) {
        log.info("订单查询，订单号：" + orderNo);
        Order order = orderDao.queryOrder(orderNo);
        ModelAndView modelAndView = new ModelAndView();            // ModelAndView 是渲染页面用的，Model 数据 和 页面视图（view）的转化

        if (order != null) {
            modelAndView.setViewName("order");                     // .setViewName()  是指定我们要跳转到哪个页面里去
            modelAndView.addObject("order", order);
            SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(order.getSeckillActivityId());
            modelAndView.addObject("seckillActivity", seckillActivity);
        } else {
            modelAndView.setViewName("order_wait");        // 我们这里找不到一个 order_wait.html, thymeleaf 就会 redirect 到 error.html
        }

        return modelAndView;
    }

    /**
     * 订单支付
     * @return
     */
    @RequestMapping("/seckill/payOrder/{orderNo}")
    public String payOrder(
            @PathVariable String orderNo
    ) {
        seckillActivityService.payOrderProcess(orderNo);
        return "redirect:/seckill/orderQuery/" + orderNo;
    }
}
