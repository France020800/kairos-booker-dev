package com.francesco.marchini.kairosbookerdev.bot;

import com.francesco.marchini.kairosbookerdev.db.devUser.DevUser;
import com.francesco.marchini.kairosbookerdev.db.devUser.DevUserRepository;
import com.francesco.marchini.kairosbookerdev.db.lessonToBook.LessonToBookRepository;
import com.francesco.marchini.kairosbookerdev.db.user.KairosUser;
import com.francesco.marchini.kairosbookerdev.db.user.UserRepository;
import com.github.kshashov.telegram.api.TelegramMvcController;
import com.github.kshashov.telegram.api.bind.annotation.BotController;
import com.github.kshashov.telegram.api.bind.annotation.BotPathVariable;
import com.github.kshashov.telegram.api.bind.annotation.request.MessageRequest;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;

@BotController
@Slf4j
public class KairosBotDevRequestHandler implements TelegramMvcController {

    private final UserRepository userRepository;
    private final LessonToBookRepository lessonToBookRepository;
    private final DevUserRepository devUserRepository;
    private final TelegramBot bot;
    private final String PASSWORD;

    @Value("${bot.token}")
    private String token;

    /**
     * Constructor with autowired fields
     *
     * @param userRepository Repository to store user credentials
     * @param lessonToBookRepository Repository to store lessons which will be auto-booked
     */
    @Autowired
    public KairosBotDevRequestHandler(UserRepository userRepository, LessonToBookRepository lessonToBookRepository, DevUserRepository devUserRepository, @Value("${bot.token.official}") String officialToken, @Value("${bot.password}") String PASSWORD) {
        this.userRepository = userRepository;
        this.lessonToBookRepository = lessonToBookRepository;
        this.devUserRepository = devUserRepository;
        this.PASSWORD = PASSWORD;
        this.bot = new TelegramBot(officialToken);
    }

    @Override
    public String getToken() {
        return token;
    }

    /**
     * Starting method for bot
     *
     * @param chat The representation of the chat with the user
     */
    @MessageRequest("/start")
    public String welcomeUser(Chat chat) {
        log.info(chat.id() + " user logged.");
        final Optional<DevUser> optionalDevUser = devUserRepository.findByChatId(chat.id());
        if (optionalDevUser.isPresent())
            return "Bentornato su sviluppatore!";
        final DevUser devUser = devUserRepository.findByChatId(chat.id())
                .orElse(DevUser.builder()
                        .chatId(chat.id())
                        .username(chat.username())
                        .isDevUser(false)
                        .build());
        devUserRepository.save(devUser);
        return loginMessage();
    }

    /**
     * Method that display the connected user
     *
     * @param chat The representation of the chat with the user
     */
    @MessageRequest("/user")
    public String getUser(Chat chat) {
        log.info("/user command");
        Optional<DevUser> optionalDevUser = devUserRepository.findByChatId(chat.id());
        if (optionalDevUser.isEmpty())
            return "Utente non registrato reinizializza il bot con /start";
        final DevUser devUser = optionalDevUser.get();
        if (!devUser.getIsDevUser())
            return loginMessage();
        List<KairosUser> kairosUsers = userRepository.findAll();
        String out = "";
        for (KairosUser user : kairosUsers) {
            out += kairosUsers.size() + " utenti attivi.\n" +
                    "Name: " + user.getUsername() + ".\n" +
                    "Matricola: " + user.getMatricola() + ", ChatId: " + user.getChadId() + "\n" +
                    "-----------------------\n";
        }
        return out;
    }

    /**
     * Method that delete a KairosUser from repository
     *
     * @param chatId The user's chat id we want to remove
     * @param chat The representation of the chat with the user
     */
    @MessageRequest("/delete_user {chatId}")
    public String removeUser(Chat chat, @BotPathVariable("chatId") Long chatId) {
        log.info("/delete_user command");
        Optional<DevUser> optionalDevUser = devUserRepository.findByChatId(chat.id());
        if (optionalDevUser.isEmpty())
            return "Utente non registrato, reinizializza il bot con /start";
        final DevUser devUser = optionalDevUser.get();
        if (!devUser.getIsDevUser())
            return loginMessage();
        final Optional<KairosUser> optionalKairosUser = userRepository.findByChadId(chatId);
        if (optionalKairosUser.isEmpty())
            return "Nessun utente da rimuovere trovato.";
        userRepository.delete(optionalKairosUser.get());
        return "Utente " + optionalKairosUser.get().getMatricola() + " rimosso correttamente";
    }

    /**
     * Method to send a message to all registered user
     *
     * @param message The message that we want to send.
     */
    @MessageRequest("/send_to_all {message}")
    public String sendToAll(Chat chat, @BotPathVariable("message") String message) {
        log.info("/send_to_all command");
        Optional<DevUser> optionalDevUser = devUserRepository.findByChatId(chat.id());
        if (optionalDevUser.isEmpty())
            return "Utente non registrato, reinizializza il bot con /start";
        final DevUser devUser = optionalDevUser.get();
        if (!devUser.getIsDevUser())
            return loginMessage();
        userRepository.findAll().forEach(u -> bot.execute(new SendMessage(u.getChadId(), message)));
        return "Messaggi inviati con successo";
    }

    /**
     * Method to send a message to a specific user
     *
     * @param chatId The user's chat id we want to send a message
     * @param message The message we want to send
     */
    @MessageRequest("/send_to {chatId} {message}")
    public String sendTo(Chat chat, @BotPathVariable("chatId") Long chatId, @BotPathVariable("message") String message) {
        log.info("/send_to command");
        Optional<DevUser> optionalDevUser = devUserRepository.findByChatId(chat.id());
        if (optionalDevUser.isEmpty())
            return "Utente non registrato, reinizializza il bot con /start";
        final DevUser devUser = optionalDevUser.get();
        if (!devUser.getIsDevUser())
            return loginMessage();
        bot.execute(new SendMessage(chatId, message));
        return "Messaggio inviato con successo";
    }

    @MessageRequest("{message}")
    public String genericMessageHandler(Chat chat, @BotPathVariable("message") String message) {
        Optional<DevUser> optionalDevUser = devUserRepository.findByChatId(chat.id());
        if (optionalDevUser.isEmpty())
            return "Utente non registrato, reinizializza il bot con /start";
        final DevUser devUser = optionalDevUser.get();
        if (message.equals(PASSWORD) && !devUser.getIsDevUser()) {
            devUser.setIsDevUser(true);
            devUserRepository.save(devUser);
            return "Autenticazione avvenuta con successo.\n" +
                    "Benvenuto sviluppatore!";
        } else {
            return "Comando non riconosciuto";
        }
    }

    private String loginMessage() {
        return "Autenticazione richiesta per utilizzare questo bot.\n" +
                "Prego inserire la password.";
    }
}
