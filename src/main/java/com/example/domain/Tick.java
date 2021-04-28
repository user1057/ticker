package com.example.domain;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Tick {

    private Long timestamp;
    private BigDecimal bidPrice;
    private BigDecimal bidSize;
    private BigDecimal askPrice;
    private BigDecimal askSize;
    private State state;

    private boolean trigger;

    private DecimalFormat dfPrice = new DecimalFormat("#,###,##0.0000000000");
    private DecimalFormat dfAmount = new DecimalFormat("#,###,###,###.00");

    @Override
    public String toString() {
        return  "(" + timestamp + ")" + Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.of(ZoneOffset.UTC.getId())).format(DateTimeFormatter.ofPattern("hh:mm:ss.SSS")) +
                " bid " + dfPrice.format(bidPrice) +
                " " + StringUtils.leftPad(dfAmount.format(bidSize), 14, '.') +
                " ask " + dfPrice.format(askPrice) +
                " " + StringUtils.leftPad(dfAmount.format(askSize), 14, '.') +
                (trigger?" state on trigger: " + this.getState().toTickString():"");
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getAskPrice() {
        return askPrice;
    }

    public void setAskPrice(BigDecimal askPrice) {
        this.askPrice = askPrice;
    }

    public BigDecimal getAskSize() {
        return askSize;
    }

    public void setAskSize(BigDecimal askSize) {
        this.askSize = askSize;
    }

    public BigDecimal getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(BigDecimal bidPrice) {
        this.bidPrice = bidPrice;
    }

    public BigDecimal getBidSize() {
        return bidSize;
    }

    public void setBidSize(BigDecimal bidSize) {
        this.bidSize = bidSize;
    }

    public boolean isTrigger() {
        return trigger;
    }

    public void setTrigger(boolean trigger) {
        this.trigger = trigger;
    }
}
