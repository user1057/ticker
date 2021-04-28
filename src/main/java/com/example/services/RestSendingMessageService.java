package com.example.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RestSendingMessageService {

    private final Logger log = LoggerFactory.getLogger(RestSendingMessageService.class);

    private final RestTemplate restTemplate;

    public RestSendingMessageService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public void sendTelegramMessage(String msg) {
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(() -> {
            for(int i=0;i<3;i++) {
                String url = "https://api.telegram.org/bot755582695:AAEBcMVt8piJHKn6XCm-QENSa2rLCBk24dQ/sendMessage?chat_id=@channelll111&text=" + msg;
                try {
                    this.restTemplate.getForObject(url, String.class);
                    Thread.sleep(5000);
                } catch (Exception e) {
                    log.error("Failed telegram sending");
                    e.printStackTrace();
                }
            }
        });
    }
}