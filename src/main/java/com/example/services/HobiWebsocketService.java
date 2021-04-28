package com.example.services;

import com.example.conf.PriceConf;
import com.example.domain.Tick;
import com.huobi.client.MarketClient;
import com.huobi.client.req.market.SubMarketBBORequest;
import com.huobi.client.req.market.SubMarketDepthRequest;
import com.huobi.client.req.market.SubMbpRefreshUpdateRequest;
import com.huobi.constant.HuobiOptions;
import com.huobi.constant.enums.DepthLevels;
import com.huobi.model.market.MarketBBO;
import com.huobi.model.market.MarketBBOEvent;
import com.huobi.service.huobi.connection.HuobiWebSocketConnection;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneOffset;

import org.apache.commons.lang3.RandomStringUtils;

@Service
public class HobiWebsocketService {

    private final Logger log = LoggerFactory.getLogger(HobiWebsocketService.class);

    @Autowired
    private PriceConf priceConf;

    @Autowired
    private StatisticsService statisticsService;

    private MarketClient marketClient;

    private HuobiWebSocketConnection cryptoPairConnection;

    private boolean subscribed = false;

    public void subscribeAll() {
        String btcSymbol = "btcusdt";
        String threadID = RandomStringUtils.randomAlphanumeric(5).toUpperCase();
        priceConf.msgSentCounter = 0;
        marketClient = MarketClient.create(new HuobiOptions());

        log.info("Starting marketClient.subMarketDepth BTC subscription["+threadID+"]");
        marketClient.subMarketDepth(SubMarketDepthRequest.builder().symbol(btcSymbol).build(), (marketDetailEvent) -> {
            priceConf.bidPriceBtc = marketDetailEvent.getDepth().getBids().get(0).getPrice();
        });

        log.info("Starting subMbpRefreshUpdateV2 {} subscription["+threadID+"]", priceConf.getCryptoPair());
        cryptoPairConnection = marketClient.subMarketBBOv2(SubMarketBBORequest.builder().symbol(priceConf.getCryptoPair()).build(), (event) -> {
            processCryptoPairEvent(event);
        });
    }

    public void subscribeCryptoPair() {
        String threadID = RandomStringUtils.randomAlphanumeric(5).toUpperCase();
        priceConf.msgSentCounter = 0;
        marketClient = MarketClient.create(new HuobiOptions());

        log.info("Starting subMbpRefreshUpdateV2 {} subscription["+threadID+"]", priceConf.getCryptoPair());
        cryptoPairConnection = marketClient.subMarketBBOv2(SubMarketBBORequest.builder().symbol(priceConf.getCryptoPair()).build(), (event) -> {
            processCryptoPairEvent(event);
        });
    }

    public void processCryptoPairEvent(MarketBBOEvent event){
        MarketBBO eventData = event.getBbo();
        priceConf.bidAmount = eventData.getBidSize();
        priceConf.bidPrice = eventData.getBid();

        priceConf.askAmount = eventData.getAskSize();
        priceConf.askPrice = eventData.getAsk();

        long now = System.currentTimeMillis();
        //log.info("getQuoteTime = " + eventData.getQuoteTime() + "("+ Instant.ofEpochMilli(eventData.getQuoteTime()).atZone(ZoneOffset.of(ZoneOffset.UTC.getId()))+"), now = " + now + ","+System.currentTimeMillis()+"("+ Instant.ofEpochMilli(now).atZone(ZoneOffset.of(ZoneOffset.UTC.getId()))+"), eventData = " + eventData);
        statisticsService.add(eventData.getQuoteTime(), priceConf.askPrice, priceConf.askAmount, priceConf.bidPrice, priceConf.bidAmount);
        setSubscribed(true);
    }

    public void unsubscribeCryptoPair(){
        log.info("Unsubscribing from " + cryptoPairConnection.getCommandList());
        cryptoPairConnection.close();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        cryptoPairConnection = null;
        setSubscribed(false);
    }

    public synchronized void setSubscribed(boolean status){
        subscribed = status;
    }

    public synchronized boolean getSubscribed(){
        return subscribed;
    }
}
