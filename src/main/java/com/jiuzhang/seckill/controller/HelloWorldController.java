package com.jiuzhang.seckill.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController       // 让 spring boot 把这个class 识别成一个 controller
public class HelloWorldController {       // HelloWorld 控制器

    @RequestMapping
    public String helloWorld() { return "Hello, World!"; }
}
