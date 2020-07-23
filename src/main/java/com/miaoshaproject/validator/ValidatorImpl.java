package com.miaoshaproject.validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import java.sql.ResultSet;
import java.util.Set;

@Component
public class ValidatorImpl implements InitializingBean {

    //初始化该对象的时候进行赋值！！！
    private Validator validator;

    //实现检验方法并返回校验结果
    public ValidationResult validate(Object bean){
        ValidationResult validationResult = new ValidationResult();
        //在加了注释的地方会进行验证！！！
        Set<ConstraintViolation<Object>> validateSet = validator.validate(bean);
        if(validateSet.size()>0){
            //有错误
            validationResult.setHasErrors(true);
            validateSet.forEach(validate->{
                String message = validate.getMessage();
                String propertyName = validate.getPropertyPath().toString();
                validationResult.getErrorMsgMap().put(propertyName,message);
            });
        }
        return validationResult;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //将hibernate validator实例化
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

}
