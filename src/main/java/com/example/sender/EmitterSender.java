package com.example.sender;

import com.example.conf.PriceConf;
import com.example.services.StatisticsService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EmitterSender implements Runnable {

    private final Logger log = LoggerFactory.getLogger(EmitterSender.class);

    private PriceConf priceConf;
    private SseEmitter emitter;
    private String appid;
    private String threadID;
    private StatisticsService statisticsService;

    public EmitterSender(SseEmitter emitter, PriceConf priceConf, String appid, StatisticsService statisticsService) {
        this.emitter = emitter;
        this.priceConf = priceConf;
        this.appid = appid;
        this.threadID = RandomStringUtils.randomAlphanumeric(5).toUpperCase();
        this.statisticsService = statisticsService;
    }

    public void run() {
        log.info("EmitterSender START[" + appid + ", "+threadID+"]");
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");
        while (priceConf.huobiWebsocketServiceStarted) {
            try {
                Map<String, String> emitterData = new HashMap<>();

                emitterData.put("askAmount", priceConf.getDfAmount().format(priceConf.askAmount));
                emitterData.put("askPrice", priceConf.getDfPrice().format(priceConf.askPrice));

                emitterData.put("bidAmount", priceConf.getDfAmount().format(priceConf.bidAmount));
                emitterData.put("bidPrice", priceConf.getDfPrice().format(priceConf.bidPrice));

                emitterData.put("states", "<pre style='color:white; text-align: left; white-space: pre-line;'>count = " + statisticsService.getTicks().size() + "\n" + statisticsService.getStatesFormated()+"</pre>");
                emitterData.put("ticks", "<pre style='color:white; text-align: left; white-space: pre-line;'>" + statisticsService.getTicksFormated()+"</pre>");

                double amount = priceConf.getAmountHolding().doubleValue();
                double usd = 6.14;
                double eur = 7.52;

                BigDecimal btcInEur = priceConf.bidPriceBtc
                        .multiply(BigDecimal.valueOf(usd))
                        .divide(BigDecimal.valueOf(eur), 2, RoundingMode.HALF_UP);
                emitterData.put("bidPriceBtcInEur", priceConf.getDfAmount().format(btcInEur));

                BigDecimal net = priceConf.bidPrice
                        .multiply(BigDecimal.valueOf(amount))
                        .multiply(priceConf.bidPriceBtc)
                        .multiply(BigDecimal.valueOf(usd))
                        .divide(BigDecimal.valueOf(eur), 2, RoundingMode.HALF_UP);
                emitterData.put("net", priceConf.getDfAmount().format(net));

                emitterData.put("timestamp", formatter.format(new Date()));

                Gson gson = new GsonBuilder().create();
                String data = gson.toJson(emitterData);

                emitter.send(data, MediaType.TEXT_EVENT_STREAM);

                Thread.sleep(1000);
            } catch (Exception e) {
                log.info("EmitterSender error while sending to client[" + appid + ", "+threadID+"]");
                emitter.completeWithError(e);
                break;
            }
        }
        if(!priceConf.huobiWebsocketServiceStarted) {
            emitter.complete();
        }
        log.info("EmitterSender END[" + appid + ", "+threadID+"]");
    }
}