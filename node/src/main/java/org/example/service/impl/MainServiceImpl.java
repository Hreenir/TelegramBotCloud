package org.example.service.impl;

import lombok.extern.log4j.Log4j;
import org.example.dao.AppUserDAO;
import org.example.dao.RawDataDAO;
import org.example.entity.AppDocument;
import org.example.entity.AppPhoto;
import org.example.entity.AppUser;
import org.example.entity.RawData;
import org.example.exceptions.UploadFileException;
import org.example.service.AppUSerService;
import org.example.service.FileService;
import org.example.service.MainService;
import org.example.service.enums.LinkType;
import org.example.service.enums.ServiceCommand;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.example.entity.enums.UserState.BASIC_STATE;
import static org.example.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;
import static org.example.service.enums.ServiceCommand.*;

@Service
@Log4j
public class MainServiceImpl implements MainService {
    private final RawDataDAO rawDataDAO;
    private final ProducerServiceImpl producerService;
    private final AppUserDAO appUserDAO;
    private final FileService fileService;
    private final AppUSerService appUSerService;

    public MainServiceImpl(RawDataDAO rawDataDAO, ProducerServiceImpl producerService, AppUserDAO appUserDAO, FileService fileService, AppUSerService appUSerService) {
        this.rawDataDAO = rawDataDAO;
        this.producerService = producerService;
        this.appUserDAO = appUserDAO;
        this.fileService = fileService;
        this.appUSerService = appUSerService;
    }

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";

        var serviceCommand = ServiceCommand.fromValue(text);
        if (CANCEL.equals(serviceCommand)) {
            output = cancelProcess(appUser);
        } else if (BASIC_STATE.equals(userState)) {
            output = processServiceCommand(appUser, text);
        } else if (WAIT_FOR_EMAIL_STATE.equals(userState)) {
           output = appUSerService.setEmail(appUser, text);
        } else {
            log.error("Unknown user state " + userState);
            output = "Неизвестная ошибка! Введите /cancel и попробуйте снова!";
        }

        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);
    }

    @Override
    public void processDocMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }
        try {
            AppDocument doc = fileService.processDoc(update.getMessage());
            String link = fileService.generateLink(doc.getId(), LinkType.GET_DOC);
            var answer = "Документ успешно загружен! Ссылка для скачивания: " + link;
            sendAnswer(answer, chatId);
        } catch (UploadFileException e) {
            log.error(e);
            String error = "К сожалению загрузка файла не удалась. Повторите попытку позже.";
            sendAnswer(error, chatId);
        }
    }
    @Override
    public void processPhotoMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }
        try {
            AppPhoto photo = fileService.processPhoto(update.getMessage());
            String link = fileService.generateLink(photo.getId(), LinkType.GET_PHOTO);
            var answer = "Документ успешно загружен! Ссылка для скачивания: " + link;
            sendAnswer(answer, chatId);
        } catch (UploadFileException e) {
            log.error(e);
            String error = "К сожалению загрузка фото не удалась. Повторите попытку позже.";
            sendAnswer(error, chatId);
        }

    }

    private boolean isNotAllowToSendContent(Long chatId, AppUser appUser) {
        var userState = appUser.getState();
        if (!appUser.getIsActive()) {
            var error = "Зарегистрируйтесь или активируйте свою учетную запись для загрузки контента.";
            sendAnswer(error, chatId);
            return true;
        } else if (!BASIC_STATE.equals(userState)) {
            var error = "Отмените текущую команду с помощью /cancel для отправки файлов.";
            sendAnswer(error, chatId);
            return true;
        }
        return false;
    }

    private void sendAnswer(String output, Long chatId) {
        SendMessage sendMessage = SendMessage.builder()
                .text(output)
                .chatId(chatId)
                .build();
        producerService.produceAnswer(sendMessage);
    }

    private String processServiceCommand(AppUser appUser, String cmd) {
        var serviceCommand = ServiceCommand.fromValue(cmd);
        if (REGISTRATION.equals(serviceCommand)) {
            return appUSerService.registerUser(appUser);
        } else if (HELP.equals(serviceCommand)) {
            return help();
        } else if (START.equals(serviceCommand)) {
            return "Привествую! Чтобы посмотреть список доступных комнад введите /help";
        } else {
            return "Неизвестная комнада! Чтобы посмотреть список доступных комнад введите /help";
        }
    }

    private String help() {
        return "Список доступных команд:\n"
                + "/cancel - отмена выполнения текущей команды;\n"
                + "/registration - регистрация пользователя.";
    }

    private String cancelProcess(AppUser appUser) {
        appUser.setState(BASIC_STATE);
        appUserDAO.save(appUser);
        return "Команда отменена!";
    }

    private AppUser findOrSaveAppUser(Update update) {
        var telegramUser = update.getMessage().getFrom();
        var optional = appUserDAO.findByTelegramUserId(telegramUser.getId());
        if (optional.isEmpty()) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .userName(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .isActive(false)
                    .state(BASIC_STATE)
                    .build();
            return appUserDAO.save(transientAppUser);
        }
        return optional.get();
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDataDAO.save(rawData);
    }
}
