package org.example.controller;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j;
import org.example.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


@Component
@Log4j
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig config;
    private UpdateController updateController;

    public TelegramBot(BotConfig config, UpdateController updateController) {
        this.config = config;
        this.updateController = updateController;
    }
    @PostConstruct
    public void init(){
        updateController.registerBot(this);
    }
    @Override
    public void onUpdateReceived(Update update) {
        var originalMessage = update.getMessage();
        updateController.processUpdate(update);
    }

    public void sendAnswerMessage(SendMessage message){
        if (message != null){
            try {
                execute(message);
            } catch (TelegramApiException e){
                log.error(e);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return config.getName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }
}
