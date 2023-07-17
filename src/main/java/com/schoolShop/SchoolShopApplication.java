package com.schoolShop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.schoolShop.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class SchoolShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchoolShopApplication.class, args);
    }

}
