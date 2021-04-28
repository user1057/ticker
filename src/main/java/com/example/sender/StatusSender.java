package com.example.sender;

import com.example.conf.PriceConf;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StatusSender implements Runnable {

    private final Logger log = LoggerFactory.getLogger(StatusSender.class);

    private PriceConf priceConf;
    private SseEmitter emitter;
    private String appid;
    private String threadID;

    public StatusSender(SseEmitter emitter, PriceConf priceConf, String appid) {
        this.emitter = emitter;
        this.priceConf = priceConf;
        this.appid = appid;
        this.threadID = RandomStringUtils.randomAlphanumeric(5).toUpperCase();
    }

    public void run() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");

        log.info("StatusSender START[" + appid + ", "+threadID+"]");

        while(true) {
            try {
                Map<String, String> statusData = new HashMap<>();

                statusData.put("alarmHighLimitPrice", priceConf.getDfPrice().format(priceConf.getAlarmHighLimitPrice()));
                statusData.put("alarmLowLimitPrice", priceConf.getDfPrice().format(priceConf.getAlarmLowLimitPrice()));
                statusData.put("amountHolding", priceConf.getDfAmount().format(priceConf.getAmountHolding()));
                statusData.put("huobiWebsocketServiceStarted", Boolean.toString(priceConf.huobiWebsocketServiceStarted));
                statusData.put("msgSentCounter", Integer.toString(priceConf.msgSentCounter));
                statusData.put("statustimestamp", formatter.format(new Date()));
                statusData.put("cryptoPair", priceConf.getCryptoPair());
                statusData.put("statisticsConfig", priceConf.getStatisticsConfig());

                Gson gson = new GsonBuilder().create();
                String data = gson.toJson(statusData);

                emitter.send(data, MediaType.TEXT_EVENT_STREAM);

                Thread.sleep(3000);
            } catch (Exception e) {
                log.info("StatusSender error while sending to client[" + appid + ", "+threadID+"]");
                emitter.completeWithError(e);
                break;
            }
        }
        log.info("StatusSender END[" + appid + ", "+threadID+"]");
    }
}
