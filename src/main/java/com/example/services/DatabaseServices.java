package com.example.services;

import com.example.domain.ConfigData;
import com.example.conf.PriceConf;
import com.example.domain.State;
import com.example.domain.Tick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*

-- DROP TABLE public.config;

CREATE TABLE public.config (
	paramname varchar(100) NOT NULL,
	numval numeric(18,10) NULL,
	stringval varchar(100) NULL,
	CONSTRAINT paramname_pkey PRIMARY KEY (paramname)
);

INSERT INTO public.config
(paramname, numval, stringval)
VALUES('amountHolding', 267200.0000000000, NULL);
INSERT INTO public.config
(paramname, numval, stringval)
VALUES('alarmHighLimitPrice', 0.0000001700, NULL);
INSERT INTO public.config
(paramname, numval, stringval)
VALUES('alarmLowLimitPrice', 0.0000001550, NULL);
INSERT INTO public.config
(paramname, numval, stringval)
VALUES('cryptoPair', NULL, 'datxbtc');
INSERT INTO public.config
(paramname, numval, stringval)
VALUES('statisticsConfig', NULL, '60,100,100');

PARAMNAME                          NUMVAL     STRINGVAL
amountHolding           267200.0000000000
alarmHighLimitPrice          0.0000001700
alarmLowLimitPrice	         0.0000001550
cryptoPair		                                datxbtc
statisticsConfig                                60,100;120,100

 */

@Service
public class DatabaseServices {

    private final Logger log = LoggerFactory.getLogger(DatabaseServices.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PriceConf priceConf;

    public void setNumericColumn(String price, String property) {
        try (Connection connection = dataSource.getConnection()) {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("UPDATE config SET numval = " + price + " WHERE paramname = '" + property + "'");
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setStringColumn(String data, String property) {
        try (Connection connection = dataSource.getConnection()) {
            Statement stmt = connection.createStatement();
            String updateStatement = "UPDATE config SET stringval = '" + data + "' WHERE paramname = '" + property + "'";
            log.info(updateStatement);
            stmt.executeUpdate(updateStatement);
            log.info("DONE " + updateStatement);
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initializeConfig() {
        try (Connection connection = dataSource.getConnection()) {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT paramname, numval, stringval FROM config");

            List<ConfigData> configDataList = new ArrayList<>();
            while (rs.next()) {
                String paramname = rs.getString("paramname");
                BigDecimal numval = rs.getBigDecimal("numval");
                String stringval = rs.getString("stringval");
                ConfigData configData = new ConfigData(paramname, numval, stringval);
                configDataList.add(configData);
                log.info("Config data loading, paramname({}), numval({}), stringval({})", configData.getParamname(), configData.getNumval(), configData.getStringval());
            }
            for (ConfigData configData : configDataList) {
                switch (configData.getParamname()) {
                    case "alarmHighLimitPrice":
                        priceConf.setAlarmHighLimitPrice(configData.getNumval());
                        break;
                    case "alarmLowLimitPrice":
                        priceConf.setAlarmLowLimitPrice(configData.getNumval());
                        break;
                    case "amountHolding":
                        priceConf.setAmountHolding(configData.getNumval());
                        break;
                    case "cryptoPair":
                        priceConf.setCryptoPair(configData.getStringval());
                        break;
                    case "statisticsConfig":
                        priceConf.setStatisticsConfig(configData.getStringval());
                        break;
                    default:
                        break;
                }
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveTicks(Map<Long, Tick> ticks) {

        log.info("STARTING saveTicks");;

        synchronized (this) {
            try (Connection connection = dataSource.getConnection()) {

                Iterator<Map.Entry<Long, Tick>> iterator = ticks.entrySet().iterator();
                Statement batchStmt = connection.createStatement();

                long stateid = -1L;

                while (iterator.hasNext()) {

                    Map.Entry<Long, Tick> entry = iterator.next();
                    Tick tick = entry.getValue();
                    State state = tick.getState();

                    if (state != null) {
                        log.info("STATE INSERT START");
                        Statement stmt = connection.createStatement();
                        String updateStatement = "INSERT INTO public.states" +
                                "(statetriggered, minbid, minbidsize, maxbid, maxbidsize, minask, minasksize, maxask, maxasksize, period, biddiff, askdiff)" +
                                "VALUES(" + state.isStateTriggered() + ", " + state.getMinBid() + ", " + state.getMinBidSize() + ", " + state.getMaxBid() + ", " + state.getMaxBidSize() +
                                ", " + state.getMinAsk() + ", " + state.getMinAskSize() + ", " + state.getMaxAsk() + ", " + state.getMaxAskSize() + ", " + state.getPeriod() + ", " + state.getBidDiff() + ", " + state.getAskDiff() + ");";
                        stmt.executeUpdate(updateStatement, Statement.RETURN_GENERATED_KEYS);
                        try (ResultSet keys = stmt.getGeneratedKeys()) {
                            log.info("keys.getFetchSize() = " + keys.getFetchSize());
                            if (keys.next()) {
                                stateid = keys.getLong(1);
                                log.info("STATE INSERT END, stateid = " + stateid);
                                break;
                            }
                        }
                        stmt.close();
                    }
                }

                Iterator<Map.Entry<Long, Tick>> iterator2 = ticks.entrySet().iterator();
                while (iterator2.hasNext()) {
                    Map.Entry<Long, Tick> entry = iterator2.next();
                    Tick tick = entry.getValue();

                    String batchStatement = "INSERT INTO public.ticks" +
                            "(timestamp, bidprice, bidsize, askprice, asksize, trigger, stateid)" +
                            "VALUES("+tick.getTimestamp()+", "+tick.getBidPrice()+", "+tick.getBidSize()+", "+tick.getAskPrice()+", "+tick.getAskSize()+", "+tick.isTrigger()+", "+stateid+");";
                    batchStmt.addBatch(batchStatement);
                }
                log.info("SAVING BATCH");
                int[] arr = batchStmt.executeBatch();
                log.info("BATCH SAVED size = " + arr.length);
                batchStmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
