package com.example.services;

import com.example.conf.PriceConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AlarmLimitPriceCheckService {
    private final Logger log = LoggerFactory.getLogger(AlarmLimitPriceCheckService.class);

    @Autowired
    private PriceConf priceConf;

    @Autowired
    private RestSendingMessageService restSendingMessageService;

    @Scheduled(fixedRate = 1000)
    public void priceCheck(){
        if(priceConf.bidPrice!=null && priceConf.getAlarmHighLimitPrice()!=null) {
            if (priceConf.getAlarmHighLimitPrice().compareTo(priceConf.bidPrice) < 0 && priceConf.msgSentCounter < priceConf.msgSentLoopCount) {
                String message = "High Alarm - " + priceConf.getDfPrice().format(priceConf.bidPrice);
                restSendingMessageService.sendTelegramMessage(message);
                priceConf.msgSentCounter++;
                log.info("Message sent counter is " + priceConf.msgSentCounter + ", message sent: " + message);
            }
        }

        if(priceConf.askPrice!=null && priceConf.getAlarmLowLimitPrice()!=null) {
            if (priceConf.getAlarmLowLimitPrice().compareTo(priceConf.askPrice) > 0 && priceConf.msgSentCounter < priceConf.msgSentLoopCount) {
                String message = "Low Alarm - " + priceConf.getDfPrice().format(priceConf.askPrice);
                restSendingMessageService.sendTelegramMessage(message);
                priceConf.msgSentCounter++;
                log.info("Message sent counter is " + priceConf.msgSentCounter + ", message sent: " + message);
            }
        }
    }
}
