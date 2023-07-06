package org.example.service.impl;

import org.example.dao.AppUserDAO;
import org.example.dao.RawDataDAO;
import org.example.entity.AppUser;
import org.example.entity.RawData;
import org.example.entity.enums.UserState;
import org.example.service.MainService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.example.entity.enums.UserState.BASIC_STATE;

@Service
public class MainServiceImpl implements MainService {
    private final RawDataDAO rawDataDAO;
    private final ProducerServiceImpl producerService;
    private final AppUserDAO appUserDAO;

    public MainServiceImpl(RawDataDAO rawDataDAO, ProducerServiceImpl producerService, AppUserDAO appUserDAO) {
        this.rawDataDAO = rawDataDAO;
        this.producerService = producerService;
        this.appUserDAO = appUserDAO;
    }

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);
        var textMessage = update.getMessage();
        var telegramUser = textMessage.getFrom();
        var appUser = findOfSaveAppUser(telegramUser);

        SendMessage sendMessage = SendMessage.builder()
                .text("Hello from NODE")
                .chatId(update.getMessage().getChatId())
                .build();
        producerService.produceAnswer(sendMessage);
    }

    private AppUser findOfSaveAppUser(User telegramUser) {
        AppUser persistentAppUser = appUserDAO.findAppUserByTelegramUserId(telegramUser.getId());
        if (persistentAppUser == null) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .userName(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .isActive(true)
                    .state(BASIC_STATE)
                    .build();
            return appUserDAO.save(transientAppUser);
        }
        return appUserDAO.save(persistentAppUser);
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDataDAO.save(rawData);
    }
}
