package ru.itdt.telegram.bot.config;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.itdt.telegram.bot.service.TelegramBot;

@Component
@RequiredArgsConstructor
public class BotInitializer {

  private final TelegramBot bot;

  @EventListener({ContextRefreshedEvent.class})
  public void init() throws TelegramApiException {
    new TelegramBotsApi(DefaultBotSession.class).registerBot(bot);
  }
}
