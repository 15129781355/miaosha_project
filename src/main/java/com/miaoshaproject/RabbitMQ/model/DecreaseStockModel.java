package com.miaoshaproject.RabbitMQ.model;

import org.springframework.stereotype.Service;

import java.io.Serializable;

public class DecreaseStockModel implements Serializable {

    private Integer amount;

    private Integer itemId;

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    @Override
    public String toString() {
        return "DecreaseStockModel{" +
                "amount=" + amount +
                ", itemId=" + itemId +
                '}';
    }
}
