package com.miaoshaproject.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdKeySerializers;
import com.miaoshaproject.serializer.JodaDataTimeJsonDeserializer;
import com.miaoshaproject.serializer.JodaDataTimeJsonSerializer;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.stereotype.Component;

@Component
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600) //一个小时过期
public class RedisConfig {


    /**
     *  解决redis集群环境没有开启Keyspace notifications导致的
     *  Error creating bean with name 'enableRedisKeyspaceNotificationsInitializer' defined in class path resource
     *
     * */
    @Bean
    public static ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }


    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //改变key的序列化方式--转化为字符串对象
        StringRedisSerializer stringKeySerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringKeySerializer);
        //改变value的序列化方式--转化为json
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(DateTime.class,new JodaDataTimeJsonSerializer());
        simpleModule.addDeserializer(DateTime.class,new JodaDataTimeJsonDeserializer());
        objectMapper.registerModule(simpleModule);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);//会包含类的信息
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);




        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        return redisTemplate;
    }
}
