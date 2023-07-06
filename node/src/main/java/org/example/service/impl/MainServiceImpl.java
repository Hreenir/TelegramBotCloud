package org.example.service.impl;

import org.example.dao.RawDataDAO;
import org.example.entity.RawData;
import org.example.service.MainService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class MainServiceImpl implements MainService {
    private final RawDataDAO rawDataDAO;
    private final ProducerServiceImpl producerService;

    public MainServiceImpl(RawDataDAO rawDataDAO, ProducerServiceImpl producerService) {
        this.rawDataDAO = rawDataDAO;
        this.producerService = producerService;
    }

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);

        SendMessage sendMessage = SendMessage.builder()
                .text("Hello from NODE")
                .chatId(update.getMessage().getChatId())
                .build();
        producerService.produceAnswer(sendMessage);
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDataDAO.save(rawData);
    }
}
