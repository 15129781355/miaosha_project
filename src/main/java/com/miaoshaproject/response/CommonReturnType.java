package com.miaoshaproject.response;

public class CommonReturnType {
    //根据结果返回success或fail
    private String status;
    //返回json数据或前端通用的错误码格式
    private Object data;

    public static CommonReturnType create(Object result){
        return create(result,"success");
    }

    public static CommonReturnType create(Object result, String status){
        CommonReturnType commonReturnType = new CommonReturnType();
        commonReturnType.setData(result);
        commonReturnType.setStatus(status);
        return commonReturnType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
