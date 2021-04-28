package com.example.conf;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@Service
public class PriceConf {
    private BigDecimal alarmHighLimitPrice = BigDecimal.valueOf(0.0000000199);
    private BigDecimal alarmLowLimitPrice = BigDecimal.valueOf(0.0000000040);
    private BigDecimal amountHolding = BigDecimal.valueOf(10123123);
    private DecimalFormat dfPrice = new DecimalFormat("#,###,##0.0000000000");
    private DecimalFormat dfAmount = new DecimalFormat("#,###,###.00");
    public BigDecimal bidPrice;
    public BigDecimal bidAmount;
    public BigDecimal askPrice;
    public BigDecimal askAmount;
    private String cryptoPair = "datxbtc";
    public BigDecimal bidPriceBtc;
    //watch 60 seconds for difference of 100
    public String statisticsConfig = null;
    public boolean huobiWebsocketServiceStarted;
    public int msgSentCounter=0;
    public static final int msgSentLoopCount=1;

    public BigDecimal getAmountHolding(){
        return this.amountHolding;
    }

    public void setAmountHolding(BigDecimal amountHolding){
        this.amountHolding = amountHolding;
    }

    public BigDecimal getAlarmHighLimitPrice(){
        return this.alarmHighLimitPrice;
    }

    public void setAlarmHighLimitPrice(BigDecimal alarmHighLimitPrice){
        this.alarmHighLimitPrice = alarmHighLimitPrice;
    }

    public BigDecimal getAlarmLowLimitPrice(){
        return this.alarmLowLimitPrice;
    }

    public void setAlarmLowLimitPrice(BigDecimal alarmLowLimitPrice){
        this.alarmLowLimitPrice = alarmLowLimitPrice;
    }

    public DecimalFormat getDfPrice(){
        return this.dfPrice;
    }

    public DecimalFormat getDfAmount(){
        return this.dfAmount;
    }

    public String getCryptoPair() {
        return cryptoPair;
    }

    public void setCryptoPair(String cryptoPair) {
        this.cryptoPair = cryptoPair;
    }

    public String getStatisticsConfig() {
        return statisticsConfig;
    }

    public synchronized void setStatisticsConfig(String statisticsConfig) {
        this.statisticsConfig = statisticsConfig;
    }

}
