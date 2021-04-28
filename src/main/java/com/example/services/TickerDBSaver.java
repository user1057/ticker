package com.example.services;

import com.example.conf.PriceConf;
import com.example.domain.State;
import com.example.domain.Tick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TickerDBSaver{ // implements Runnable {

    private final Logger log = LoggerFactory.getLogger(TickerDBSaver.class);

    @Autowired
    private DatabaseServices databaseServices;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private PriceConf priceConf;

    private State state;

    public TickerDBSaver init(State state){
        this.state = state;
        return this;
    }

    public Runnable newRunnable() {

        return new Runnable() {

            public void run() {
                log.info("STARTING TickerDBSaver");

                long sleepperiod = 60 * 1000;

                long moreHistory = 60 * 1000;

                log.info("state.getPeriod() = " + state.getPeriod());

                log.info("sleepperiod = " + sleepperiod);

                log.info("ticks in Runnable1 = " + statisticsService.getTicksFormated());

                //wait sleepperiod seconds after trigger
                try {
                    Thread.sleep(sleepperiod);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //save last state.period secs of data to DB
                long startMilis = System.currentTimeMillis();

                synchronized(this) {
                    Map<Long, Tick> ticks = new TreeMap();
                    log.info("ticks in Runnable2 = " + statisticsService.getTicksFormated());
                    Iterator<Map.Entry<Long, Tick>> iterator = statisticsService.getTicks().entrySet().iterator();
                    int i=0;
                    while (iterator.hasNext()) {
                        Map.Entry<Long, Tick> entry = iterator.next();
                        Long timestamp = entry.getKey();
                        Tick tick = entry.getValue();

                        if ((startMilis - timestamp) < (state.getPeriod()*1000 + sleepperiod + moreHistory)) {
                            log.info("["+i+"]startMilis("+startMilis+","+Instant.ofEpochMilli(startMilis).atZone(ZoneOffset.of(ZoneOffset.UTC.getId())).format(DateTimeFormatter.ofPattern("hh:mm:ss.SSS"))+") - timestamp("+timestamp+","+ Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.of(ZoneOffset.UTC.getId())).format(DateTimeFormatter.ofPattern("hh:mm:ss.SSS"))+") = " + (startMilis - timestamp));
                            ticks.put(timestamp, tick);
                        }else{
                            break;
                        }
                        i++;
                    }
                    log.info("SAVING {} ticks to DB", ticks.size());
                    databaseServices.saveTicks(ticks);
                    priceConf.msgSentCounter = 0;
                }
            }
        };
    }
}
