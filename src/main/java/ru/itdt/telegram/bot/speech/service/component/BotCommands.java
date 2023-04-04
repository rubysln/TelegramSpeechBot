package ru.itdt.telegram.bot.speech.service.component;

import java.util.List;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

public interface BotCommands {
  List<BotCommand> BOT_COMMAND_LIST =
      List.of(
          new BotCommand("/start", "Старт бота."), new BotCommand("/help", "Помощь."), new BotCommand("/about", "Ссылки на разработчика."));
}
