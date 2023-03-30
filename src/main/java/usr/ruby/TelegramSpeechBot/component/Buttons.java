package usr.ruby.TelegramSpeechBot.component;

import java.util.List;
import lombok.val;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

public class Buttons {
  private static final InlineKeyboardButton HELP_BUTTON = new InlineKeyboardButton("Help");
  private static final InlineKeyboardButton GITHUB_BUTTON =
      new InlineKeyboardButton("Github\uD83E\uDDD1\u200D\uD83D\uDCBB");
  private static final InlineKeyboardButton VK_BUTTON = new InlineKeyboardButton("VK\uD83E\uDD1D");
  private static final InlineKeyboardButton TG_BUTTON = new InlineKeyboardButton("TG\uD83D\uDCE8");

  public static InlineKeyboardMarkup helpInlineMarkup() {
    HELP_BUTTON.setCallbackData("/help");

    val firstRowInline = List.of(HELP_BUTTON);
    val rowsInLine = List.of(firstRowInline);

    val markupInline = new InlineKeyboardMarkup();
    markupInline.setKeyboard(rowsInLine);

    return markupInline;
  }

  public static InlineKeyboardMarkup aboutInLineMarkUp(){
    GITHUB_BUTTON.setUrl("https://github.com/rubysln");
    VK_BUTTON.setUrl("https://vk.com/rubyrubyrubyrubyrubyruby");
    TG_BUTTON.setUrl("https://t.me/rubyrubyrubyrubyrubyrubyruby");

    val firstRowInLine = List.of(GITHUB_BUTTON, VK_BUTTON, TG_BUTTON);
    val rowsInLine = List.of(firstRowInLine);

    val markupInLine = new InlineKeyboardMarkup();
    markupInLine.setKeyboard(rowsInLine);

    return markupInLine;
  }
}
