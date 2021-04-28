package com.example.services;

import com.example.conf.PriceConf;
import com.example.domain.State;
import com.example.domain.Tick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class StatisticsService {

    private final Logger log = LoggerFactory.getLogger(StatisticsService.class);

    private SortedMap<Long, Tick> ticks;

    private Map<String, State> states;

    @Autowired
    private PriceConf priceConf;

    @Autowired
    private RestSendingMessageService restSendingMessageService;

    @Autowired
    private HobiWebsocketService hobiWebsocketService;

    @Autowired
    private TickerDBSaver tickerDBSaver;

    public StatisticsService() {
        reset();
    }

    public void reset() {
        resetTicks();
        resetStates();
    }

    public void resetTicks() {
        ticks = Collections.synchronizedSortedMap(new TreeMap<>(Collections.reverseOrder()));
    }

    public void resetStates() {
        states = Collections.synchronizedMap(new HashMap<>());
    }

    public SortedMap<Long, Tick> getTicks() {
        return ticks;
    }

    public synchronized Map<String, State> getStates() {
        return states;
    }

    public synchronized String getStatesFormated() {
        String output = "";
        for (State state : states.values()) {
            output += state.toString();
        }
        return output;
    }

    public synchronized String getTicksFormated() {
        String output = "";
        for (Tick tick : ticks.values()) {
            output = tick.toString() + "\n" + output;
        }
        return output;
    }

    public synchronized void add(Long timestamp, BigDecimal askPrice, BigDecimal askSize, BigDecimal bidPrice, BigDecimal bidSize) {
        Tick tick = new Tick();
        tick.setTimestamp(timestamp);
        tick.setAskPrice(askPrice);
        tick.setAskSize(askSize);
        tick.setBidPrice(bidPrice);
        tick.setBidSize(bidSize);
        ticks.put(timestamp, tick);
    }

    public synchronized void initStates() {
        try {
            String statisticsConfig = priceConf.getStatisticsConfig();
            if (statisticsConfig != null) {
                String[] configs = statisticsConfig.split("-");
                List<String> periods = new ArrayList<>();

                //log.info("STATES ****** = " + states);

                for (String config : configs) {
                    State state = new State();

                    state.setMaxBid(BigDecimal.valueOf(0));
                    state.setMaxBidSize(BigDecimal.valueOf(0));
                    state.setMinBid(BigDecimal.valueOf(1));
                    state.setMinBidSize(BigDecimal.valueOf(0));

                    state.setMaxAsk(BigDecimal.valueOf(0));
                    state.setMaxAskSize(BigDecimal.valueOf(0));
                    state.setMinAsk(BigDecimal.valueOf(1));
                    state.setMinAskSize(BigDecimal.valueOf(0));

                    Long period = Long.valueOf(config.split(",")[0]);
                    state.setPeriod(period);

                    Integer bidDiff = Integer.valueOf(config.split(",")[1]);
                    state.setBidDiff(bidDiff);

                    Integer askDiff = Integer.valueOf(config.split(",")[2]);
                    state.setAskDiff(askDiff);

                    if (states.get(period.toString()) != null && states.get(period.toString()).isStateTriggered()) {
                        state.setStateTriggered(true);
                        if (!(priceConf.msgSentCounter >= priceConf.msgSentLoopCount)) {
                            state.setStateTriggered(false);
                        }
                    }

                    //log.info("STATE = " + state);
                    states.put(period.toString(), state);
                    periods.add(period.toString());
                }

                //log.info("STATES --------- = " + states);

                Iterator<Map.Entry<String, State>> iterator = states.entrySet().iterator();
                while (iterator.hasNext()) {
                    if (!periods.contains(iterator.next().getKey())) {
                        iterator.remove();
                    }
                }
            } else {
                resetStates();
            }
        }catch(Exception e){
            log.error("initStates FAILED: " + e.getMessage());
            resetStates();
        }
    }

    @Scheduled(fixedRate = 100)
    public void check() {
        long startMilis = System.currentTimeMillis();

        initStates();

        synchronized (this) {
            if(!states.isEmpty() && !(priceConf.msgSentCounter >= priceConf.msgSentLoopCount)) {
                Iterator<Map.Entry<Long, Tick>> iterator = ticks.entrySet().iterator();

                boolean triggerDoneRecently = false;
                while (iterator.hasNext()) {
                    Map.Entry<Long, Tick> entry = iterator.next();
                    Long timestamp = entry.getKey();
                    Tick tick = entry.getValue();

                    if(tick.getState()!=null){
                        triggerDoneRecently = true;
                    }

                    if(!triggerDoneRecently){
                        int statecnt = 0;
                        for (State state : states.values()) {
                            if (state.checkState(startMilis, tick, tickerDBSaver)) {
                                log.info("[" + statecnt + "]ticks in StatisticsService2 = " + getTicksFormated());
                                if (priceConf.msgSentCounter < priceConf.msgSentLoopCount) {
                                    priceConf.msgSentCounter++;
                                    String message = "[" + statecnt + "] Statistics Alarm - period " + state.getPeriod() + ", diff " + state.getBidDiff() + ", maxBid " + priceConf.getDfPrice().format(state.getMaxBid()) + ", minBid " + priceConf.getDfPrice().format(state.getMinBid());
                                    //restSendingMessageService.sendTelegramMessage(message);
                                }
                            }
                            statecnt++;
                        }
                    }

                    if ((startMilis - timestamp) > 300000) {
                        iterator.remove();
                    }
                }
            }
        }
    }

}
