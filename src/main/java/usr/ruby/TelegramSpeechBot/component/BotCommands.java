package usr.ruby.TelegramSpeechBot.component;

import java.util.List;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

public interface BotCommands {
	List<BotCommand> BOT_COMMAND_LIST = List.of(
			new BotCommand("/start", "start bot"),
			new BotCommand("/help", "help")
	);
}
