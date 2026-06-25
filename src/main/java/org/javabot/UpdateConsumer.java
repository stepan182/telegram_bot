package org.javabot;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;

    public UpdateConsumer() {
        this.telegramClient = new OkHttpTelegramClient(
                "8052673046:AAEB_09sfW3Y25OfMh8dbAggLkgfIo0gOt8"
        );
    }

    @SneakyThrows
    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                sendMainMenu(chatId);
            } else if (messageText.equals("/keyboard")) {
                sendReplyKeyboard(chatId);
            } else if (messageText.equals("Hi")) {
                sendMyName(chatId, update.getMessage().getFrom());
            } else if (messageText.equals("Picture")) {
                sendImage(chatId);
            }else {
                sendMessage(chatId, "I don't understand");
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    @SneakyThrows
    private void sendReplyKeyboard(Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("This is an example of a regular keyboard:")
                .build();

        List<KeyboardRow> keyboardRows = List.of(
                new KeyboardRow("Hi", "Picture")
        );

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(keyboardRows);
        message.setReplyMarkup(markup);

        telegramClient.execute(message);
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        var data = callbackQuery.getData();
        var chatId = callbackQuery.getFrom().getId();
        var user = callbackQuery.getFrom();
        switch (data) {
            case "my_name" -> sendMyName(chatId, user);
            case "random" -> sendRandom(chatId);
            case "long_process" -> sendImage(chatId);
            default -> sendMessage(chatId, "Unknown command");
        }
    }

    @SneakyThrows
    private void sendMessage(
            Long chatId,
            String messageText
    ) {
        SendMessage message = SendMessage.builder()
                .text(messageText)
                .chatId(chatId)
                .build();

        telegramClient.execute(message);
    }

    private void sendImage(Long chatId) {
        sendMessage(chatId, "Started uploading the image");
        new Thread(() -> {
            var imageUrl = "https://picsum.photos/200";
            try {
                URL url = new URL(imageUrl);
                var inputStream = url.openStream();

                SendPhoto sendPhoto = SendPhoto.builder()
                        .chatId(chatId)
                        .photo(new InputFile(inputStream, "random.jpg"))
                        .caption("Your random image:")
                        .build();

                telegramClient.execute(sendPhoto);

            } catch (TelegramApiException | IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void sendRandom(Long chatId) {
        var randomInt = ThreadLocalRandom.current().nextInt();
        sendMessage(chatId, "You random number: " + randomInt);
    }

    private void sendMyName(
            Long chatId,
            User user
    ) {
        var text = "Hi!\n\nYour name: %s\nYour nik: @%s"
                .formatted(
                        user.getFirstName() + " " + user.getLastName(),
                        user.getUserName()
                );
        sendMessage(chatId, text);
    }

    @SneakyThrows
    private void sendMainMenu(Long chatId) {
        SendMessage message = SendMessage.builder()
                .text("Welcome! Choose an action:")
                .chatId(chatId)
                .build();

        var button1 = InlineKeyboardButton.builder()
                .text("WHat's my name?")
                .callbackData("my_name")
                .build();

        var button2 = InlineKeyboardButton.builder()
                .text("Random number")
                .callbackData("random")
                .build();

        var button3 = InlineKeyboardButton.builder()
                .text("Long process")
                .callbackData("long_process")
                .build();

        List<InlineKeyboardRow> keyboardRows = List.of(
                new InlineKeyboardRow(button1),
                new InlineKeyboardRow(button2),
                new InlineKeyboardRow(button3)
        );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboardRows);

        message.setReplyMarkup(markup);

        telegramClient.execute(message);
    }
}
