package com.miaoshaproject;

import com.miaoshaproject.RabbitMQ.MqProducer;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;


//默认扫描App的所有子目录，但需要加注解
@SpringBootApplication
@MapperScan("com.miaoshaproject.dao")
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class);

    }

}
