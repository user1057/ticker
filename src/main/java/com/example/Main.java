/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import com.example.conf.PriceConf;
import com.example.sender.EmitterSender;
import com.example.sender.StatusSender;
import com.example.services.DatabaseServices;
import com.example.services.HobiWebsocketService;
import com.example.services.StatisticsService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Controller
@SpringBootApplication
public class Main  implements WebMvcConfigurer {

  private final Logger log = LoggerFactory.getLogger(Main.class);

  @Value("${spring.datasource.url}")
  private String dbUrl;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private PriceConf priceConf;

  @Autowired
  private HobiWebsocketService huobiWebsocketService;

  @Autowired
  private StatisticsService statisticsService;

  @Autowired
  private DatabaseServices databaseServices;

  private static AtomicLong idCounter = new AtomicLong();

  public static void main(String[] args) throws Exception {
    SpringApplication.run(Main.class, args);
  }

  public static String createID()
  {
    return String.valueOf(idCounter.getAndIncrement());
  }

  @RequestMapping(value = "/setAlarmHighLimitPrice/{alarmHighLimitPrice}", method = RequestMethod.GET)
  @ResponseBody
  public String setAlarmHighLimitPrice(@PathVariable("alarmHighLimitPrice") String alarmHighLimitPrice, Map<String, Object> model){
    log.info("Request to set Alarm High Limit Price " + alarmHighLimitPrice);
    databaseServices.setNumericColumn(alarmHighLimitPrice, "alarmHighLimitPrice");
    BigDecimal alarmHighLimitPrice_ = new BigDecimal(alarmHighLimitPrice);
    priceConf.setAlarmHighLimitPrice(alarmHighLimitPrice_);
    return "Alarm High Limit Price set to " + alarmHighLimitPrice_;
  }

  @RequestMapping(value = "/setAmountHolding/{amountHolding}", method = RequestMethod.GET)
  @ResponseBody
  public String setAmountHolding(@PathVariable("amountHolding") String amountHolding, Map<String, Object> model) {
    log.info("Request to set Amount Holding " + amountHolding);
    databaseServices.setNumericColumn(amountHolding, "amountHolding");
    BigDecimal amountHolding_ = new BigDecimal(amountHolding);
    priceConf.setAmountHolding(amountHolding_);
    return "Amount Holding set to " + amountHolding_;
  }

  @RequestMapping(value = "/setStatisticsConfig/{statisticsConfig}", method = RequestMethod.GET)
  @ResponseBody
  public String setStatisticsConfig(@PathVariable("statisticsConfig") String statisticsConfig, Map<String, Object> model){
    log.info("Request to set statistics config " + statisticsConfig);
    databaseServices.setStringColumn(statisticsConfig, "statisticsConfig");
    priceConf.setStatisticsConfig(statisticsConfig);
    log.info("priceConf.getStatisticsConfig() = " + priceConf.getStatisticsConfig());
    return "Statistics config set to " + statisticsConfig;
  }

  @RequestMapping(value = "/setCryptoPair/{cryptoPair}", method = RequestMethod.GET)
  @ResponseBody
  public String setCryptoPair(@PathVariable("cryptoPair") String cryptoPair, Map<String, Object> model) {
    log.info("Request to set Crypto Pair " + cryptoPair);

    huobiWebsocketService.unsubscribeCryptoPair();
    try {
      Thread.sleep(15000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    databaseServices.setStringColumn(cryptoPair, "cryptoPair");

    databaseServices.initializeConfig();

    statisticsService.reset();
    huobiWebsocketService.subscribeCryptoPair();
    return "Crypto Pair set to " + cryptoPair;
  }



  @RequestMapping(value = "/setAlarmLowLimitPrice/{alarmLowLimitPrice}", method = RequestMethod.GET)
  @ResponseBody
  public String setAlarmLowLimitPrice(@PathVariable("alarmLowLimitPrice") String alarmLowLimitPrice, Map<String, Object> model){
    log.info("Request to set Alarm Low Limit Price " + alarmLowLimitPrice);
    databaseServices.setNumericColumn(alarmLowLimitPrice, "alarmLowLimitPrice");
    BigDecimal alarmLowLimitPrice_ = new BigDecimal(alarmLowLimitPrice);
    priceConf.setAlarmLowLimitPrice(alarmLowLimitPrice_);
    return "Alarm Low Limit Price set to " + alarmLowLimitPrice_;
  }

  @RequestMapping(value = "/start/{source}")
  @ResponseBody
  public String startSubscription(@PathVariable("source") String source, Map<String, Object> model){
    //huobiWebsocketService.unsubscribe();
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    if(priceConf.huobiWebsocketServiceStarted == false){
      databaseServices.initializeConfig();
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      huobiWebsocketService.subscribeAll();
      priceConf.huobiWebsocketServiceStarted = true;
      log.info("Huobi subscription started, request source = " + source);
      return "Huobi subscription started, request source = " + source;
    }else{
      log.info("Huobi subscription already started, request source = " + source);
      return "Huobi subscription already started, request source = " + source;
    }
  }

  @RequestMapping(value = "/stop/{source}")
  @ResponseBody
  public String stopSubscription(@PathVariable("source") String source, Map<String, Object> model){
    log.info("Request to STOP subscription, source = " + source);
    //huobiWebsocketService.unsubscribe();
    priceConf.huobiWebsocketServiceStarted = false;
    return "Subscription stopped";
  }

  @RequestMapping(value = "/resetmessagesentcounter/{source}")
  @ResponseBody
  public String resetMessageSentCounter(@PathVariable("source") String source, Map<String, Object> model){
    log.info("Request to RESET messages sent counter, source = " + source);
    priceConf.msgSentCounter = 0;
    return "Messages sent counter is reset";
  }

  @RequestMapping("/")
  String index() {
    return "index";
  }

  @RequestMapping("/login")
  String login() {
    return "login";
  }

  @RequestMapping("/tickerUpdate/{appid}")
  public ResponseBodyEmitter tickerUpdate (@PathVariable("appid") String appid) {
    final SseEmitter emitter = new SseEmitter();
    appid += "(ticker)";
    if(priceConf.huobiWebsocketServiceStarted) {
      ExecutorService service = Executors.newSingleThreadExecutor();
      log.info("Starting Live Data ExecutorService[" + appid + "]");
      service.execute(new EmitterSender(emitter, priceConf, appid, statisticsService));
      return emitter;
    }else{
      log.info("Huobi websocket service not started[" + appid + "]");
      return null;
    }
  }

  @RequestMapping("/statusUpdate/{appid}")
  public ResponseBodyEmitter statusUpdate (@PathVariable("appid") String appid) {
    final SseEmitter emitter = new SseEmitter();
    appid += "(status)";
    ExecutorService service = Executors.newSingleThreadExecutor();
    log.info("Starting Status Update ExecutorService[" + appid + "]");
    service.execute(new StatusSender(emitter, priceConf, appid));
    return emitter;
  }

  @RequestMapping("/ticker")
  public String ticker(Map<String, Object> model){
    model.put("alarmHighLimitPrice", priceConf.getDfPrice().format(priceConf.getAlarmHighLimitPrice()));
    model.put("alarmLowLimitPrice", priceConf.getDfPrice().format(priceConf.getAlarmLowLimitPrice()));
    model.put("amountHolding", priceConf.getDfAmount().format(priceConf.getAmountHolding()));
    model.put("huobiWebsocketServiceStarted", priceConf.huobiWebsocketServiceStarted);
    return "ticker";
  }

  @Bean
  public DataSource dataSource() throws SQLException {
    if (dbUrl == null || dbUrl.isEmpty()) {
      return new HikariDataSource();
    } else {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(dbUrl);
      return new HikariDataSource(config);
    }
  }

  @Override
  public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    configurer.setTaskExecutor(mvcTaskExecutor());
    configurer.setDefaultTimeout(720_000);
  }

  @Bean
  public ThreadPoolTaskExecutor mvcTaskExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setThreadNamePrefix("mvc-task-");
    return taskExecutor;
  }

}
