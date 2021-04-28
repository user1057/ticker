package com.example.domain;

import com.example.services.TickerDBSaver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.SortedMap;
import java.util.TreeMap;

public class State {
    private BigDecimal minBid;
    private BigDecimal minBidSize;
    private BigDecimal maxBid;
    private BigDecimal maxBidSize;
    private BigDecimal minAsk;
    private BigDecimal minAskSize;
    private BigDecimal maxAsk;
    private BigDecimal maxAskSize;
    private long period;
    private int bidDiff;
    private int askDiff;
    private boolean stateTriggered;

    private DecimalFormat dfPrice = new DecimalFormat("#,###,##0.0000000000");
    private DecimalFormat dfAmount = new DecimalFormat("#,###,###,###.00");

    private final Logger log = LoggerFactory.getLogger(State.class);

    public boolean checkState(long startMilis, Tick tick, TickerDBSaver tickerDBSaver){
        if ((startMilis - tick.getTimestamp()) < period*1000) {

            // BIDS

            //log.info("tick.getBidPrice() = " + tick.getBidPrice() + ", this.minBid = " + this.minBid + ", this.maxBid = " + this.maxBid) ;
            //set min bid
            //log.info("tick.getBidPrice().compareTo(this.minBid) = " + tick.getBidPrice().compareTo(this.minBid));
            if(tick.getBidPrice().compareTo(this.minBid)<0) {
                this.minBid = tick.getBidPrice();
                this.minBidSize = tick.getBidSize();
            }
            //log.info("tick.getBidPrice().compareTo(this.maxBid) = " + tick.getBidPrice().compareTo(this.maxBid));
            if(tick.getBidPrice().compareTo(this.maxBid)>0) {
                this.maxBid = tick.getBidPrice();
                this.maxBidSize = tick.getBidSize();
            }
            //log.info("this.maxBid.subtract(this.minBid).multiply(BigDecimal.valueOf(10000000000L)).compareTo(BigDecimal.valueOf(bidDiff)) = " + this.maxBid.subtract(this.minBid).multiply(BigDecimal.valueOf(10000000000L)).compareTo(BigDecimal.valueOf(diff)));
            if(this.maxBid.subtract(this.minBid).multiply(BigDecimal.valueOf(10000000000L)).compareTo(BigDecimal.valueOf(bidDiff))>0 && !this.isStateTriggered()){

                log.info("trigger SET");
                this.setStateTriggered(true);
                log.info("STATE triggered: " + this);

                tick.setTrigger(true);
                tick.setState(this.build());

/*                Runnable runnable = tickerDBSaver.init(this).newRunnable();

                Thread thread = new Thread(runnable);
                thread.start();*/

                return true;
            }

            // ASKS

            if(tick.getAskPrice().compareTo(this.minAsk)<0) {
                this.minAsk = tick.getAskPrice();
                this.minAskSize = tick.getAskSize();
            }
            //log.info("tick.getBidPrice().compareTo(this.maxBid) = " + tick.getBidPrice().compareTo(this.maxBid));
            if(tick.getAskPrice().compareTo(this.maxAsk)>0) {
                this.maxAsk = tick.getAskPrice();
                this.maxAskSize = tick.getAskSize();
            }
            //log.info("this.maxBid.subtract(this.minBid).multiply(BigDecimal.valueOf(10000000000L)).compareTo(BigDecimal.valueOf(bidDiff)) = " + this.maxBid.subtract(this.minBid).multiply(BigDecimal.valueOf(10000000000L)).compareTo(BigDecimal.valueOf(diff)));
            if(this.maxAsk.subtract(this.minAsk).multiply(BigDecimal.valueOf(10000000000L)).compareTo(BigDecimal.valueOf(askDiff))>0 && !this.isStateTriggered()){

                this.setStateTriggered(true);

                tick.setTrigger(true);
                tick.setState(this.build());

                //new Thread(new TickerDBSaver().init(this, ticks)).start();

                return true;
            }

        }
        return false;
    }

    @Override
    public String toString() {
        return  "\nperiod=" + period + " bidDiff=" + bidDiff + ", T " + stateTriggered +
                "\nminBid=" + dfPrice.format(minBid) + " minBidSize=" + StringUtils.leftPad(dfAmount.format(minBidSize), 14, '.') +
                "\nmaxBid=" + dfPrice.format(maxBid) + " maxBidSize=" + StringUtils.leftPad(dfAmount.format(maxBidSize), 14, '.') +
                "\nminAsk=" + dfPrice.format(minAsk) + " minAskSize=" + StringUtils.leftPad(dfAmount.format(minAskSize), 14, '.') +
                "\nmaxAsk=" + dfPrice.format(maxAsk) + " maxAskSize=" + StringUtils.leftPad(dfAmount.format(maxAskSize), 14, '.') +
                "\n";
    }

    public String toTickString() {
        return  period + " " + bidDiff + " " + (stateTriggered?1:0) + " " +
                "minBid=" + dfPrice.format(minBid) + " minBidSize=" + StringUtils.leftPad(dfAmount.format(minBidSize), 14, '.') +
                " maxBid=" + dfPrice.format(maxBid) + " maxBidSize=" + StringUtils.leftPad(dfAmount.format(maxBidSize), 14, '.') +
                " minAsk=" + dfPrice.format(minAsk) + " minAskSize=" + StringUtils.leftPad(dfAmount.format(minAskSize), 14, '.') +
                " maxAsk=" + dfPrice.format(maxAsk) + " maxAskSize=" + StringUtils.leftPad(dfAmount.format(maxAskSize), 14, '.')
                ;
    }

    public State build(){
        State s = new State();
        s.setMinBid(new BigDecimal(minBid.floatValue()));
        s.setMinBidSize(new BigDecimal(minBidSize.floatValue()));
        s.setMaxBid(new BigDecimal(maxBid.floatValue()));
        s.setMaxBidSize(new BigDecimal(maxBidSize.floatValue()));

        s.setMinAsk(new BigDecimal(minAsk.floatValue()));
        s.setMinAskSize(new BigDecimal(minAskSize.floatValue()));
        s.setMaxAsk(new BigDecimal(maxAsk.floatValue()));
        s.setMaxAskSize(new BigDecimal(maxAskSize.floatValue()));

        s.setPeriod(period);
        s.setBidDiff(bidDiff);
        s.setAskDiff(askDiff);
        s.setStateTriggered(stateTriggered);

        return s;
    }

    public boolean isStateTriggered() {
        return stateTriggered;
    }

    public void setStateTriggered(boolean stateTriggered) {
        this.stateTriggered = stateTriggered;
    }

    public BigDecimal getMinBidSize() {
        return minBidSize;
    }

    public void setMinBidSize(BigDecimal minBidSize) {
        this.minBidSize = minBidSize;
    }

    public BigDecimal getMaxBidSize() {
        return maxBidSize;
    }

    public void setMaxBidSize(BigDecimal maxBidSize) {
        this.maxBidSize = maxBidSize;
    }

    public BigDecimal getMinAskSize() {
        return minAskSize;
    }

    public void setMinAskSize(BigDecimal minAskSize) {
        this.minAskSize = minAskSize;
    }

    public BigDecimal getMaxAskSize() {
        return maxAskSize;
    }

    public void setMaxAskSize(BigDecimal maxAskSize) {
        this.maxAskSize = maxAskSize;
    }

    public BigDecimal getMinBid() {
        return minBid;
    }

    public void setMinBid(BigDecimal minBid) {
        this.minBid = minBid;
    }

    public BigDecimal getMaxBid() {
        return maxBid;
    }

    public void setMaxBid(BigDecimal maxBid) {
        this.maxBid = maxBid;
    }

    public BigDecimal getMinAsk() {
        return minAsk;
    }

    public void setMinAsk(BigDecimal minAsk) {
        this.minAsk = minAsk;
    }

    public BigDecimal getMaxAsk() {
        return maxAsk;
    }

    public void setMaxAsk(BigDecimal maxAsk) {
        this.maxAsk = maxAsk;
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public int getBidDiff() {
        return bidDiff;
    }

    public void setBidDiff(int bidDiff) {
        this.bidDiff = bidDiff;
    }

    public int getAskDiff() {
        return askDiff;
    }

    public void setAskDiff(int askDiff) {
        this.askDiff = askDiff;
    }
}
