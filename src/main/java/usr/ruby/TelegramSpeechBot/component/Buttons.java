package usr.ruby.TelegramSpeechBot.component;

import java.util.List;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

public class Buttons {
	private static final InlineKeyboardButton HELP_BUTTON = new InlineKeyboardButton("Help");

	public static InlineKeyboardMarkup inlineMarkup() {
		HELP_BUTTON.setCallbackData("/help");

		List<InlineKeyboardButton> rowInline = List.of(HELP_BUTTON);
		List<List<InlineKeyboardButton>> rowsInLine = List.of(rowInline);

		InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
		markupInline.setKeyboard(rowsInLine);

		return markupInline;
	}
}
