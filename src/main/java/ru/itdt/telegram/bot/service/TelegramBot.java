package ru.itdt.telegram.bot.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.vosk.Model;
import org.vosk.Recognizer;
import ru.itdt.telegram.bot.component.Buttons;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
  private final List<BotCommand> BOT_COMMAND_LIST =
      List.of(
          new BotCommand("/start", "Старт бота."),
          new BotCommand("/help", "Помощь."),
          new BotCommand("/about", "Ссылки на разработчика."));
  private long chatId;

  private final Model model = new Model("src/main/resources/models/ru/vosk-model-small-ru-0.22");
  private final Charset UTF_8 = StandardCharsets.UTF_8;

  public TelegramBot() throws IOException {
    try {
      this.execute(new SetMyCommands(BOT_COMMAND_LIST, new BotCommandScopeDefault(), null));
    } catch (TelegramApiException telegramApiException) {
      throw new RuntimeException(telegramApiException);
    }
  }

  @Override
  public String getBotUsername() {
    return "TelegramSpeechBot";
  }

  @Override
  public String getBotToken() {
    return "6228458119:AAHoXxcc5Eb-oh9H32HOZDqeTv_O76eq3t4";
  }

  @Override
  public void onUpdateReceived(Update update){
    if (update.hasMessage() && update.getMessage().hasText()) {
      val messageText = update.getMessage().getText();
      this.chatId = update.getMessage().getChatId();
      val username = update.getMessage().getChat().getFirstName();

      try {
        getBotAnswerUtils(messageText, username);
      } catch (TelegramApiException exception) {
        throw new RuntimeException(exception);
      }
    } else if (update.hasCallbackQuery()) {
      val messageText = update.getCallbackQuery().getData();
      this.chatId = update.getCallbackQuery().getFrom().getId();
      val username = update.getCallbackQuery().getFrom().getFirstName();

      try {
        getBotAnswerUtils(messageText, username);
      } catch (TelegramApiException exception) {
        throw new RuntimeException(exception);
      }
    } else if (update.getMessage().hasVoice()) {
      val voice = update.getMessage().getVoice();
      this.chatId = update.getMessage().getChatId();

      try {
        uploadFiles(voice.getFileId());
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }

  private void getBotAnswerUtils(String receivedMessage, String userName)
      throws TelegramApiException {
    switch (receivedMessage) {
      case "/start" -> startCommandReceived(userName);
      case "/help" -> helpCommandReceived();
      case "/about" -> aboutCommandReceived();
      default -> sendMessage("Sorry " + userName + "! This command was not recognized.", (byte) 1);
    }
  }

  private void aboutCommandReceived() throws TelegramApiException {
    val answer = "Привет! Спасибо что пользуешься моим телеграмм ботом!";

    sendMessage(answer, (byte) 2);
  }

  private void startCommandReceived(String userName) throws TelegramApiException {
    val answer = "\uD83D\uDC4B" + " Привет, " + userName + "!\nОтправь мне голосовое сообщение!";

    sendMessage(answer, (byte) 0);
  }

  private void helpCommandReceived() throws TelegramApiException {
    val answer =
        """
Данный бот был разработан для распознавания ваших голосовых сообщений!
Команды:
/about - Об авторе!
""";

    sendMessage(answer, (byte) 0);
  }

  private void sendMessage(String answer, byte whichMarkup) throws TelegramApiException {
    val message = new SendMessage();
    message.setChatId(chatId);
    message.setText(answer);
    switch (whichMarkup) {
      case 1:
        message.setReplyMarkup(Buttons.helpInlineMarkup());
        break;
      case 2:
        message.setReplyMarkup(Buttons.aboutInLineMarkUp());
      default:
        break;
    }

    execute(message);
  }

  private void uploadFiles(String fileId)
      throws IOException,
          UnsupportedAudioFileException,
          InterruptedException,
          TelegramApiException {
    val url =
        new URL("https://api.telegram.org/bot" + getBotToken() + "/getFile?file_id=" + fileId);

    try (val bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()))) {
      val getFileResponse = bufferedReader.readLine();

      val jsonResult = new JSONObject(getFileResponse);
      val path = jsonResult.getJSONObject("result");
      val file = path.getString("file_path");

      val localFile = new File("src/main/resources/uploadFiles/" + file);
      try (val inputStream =
          new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + file).openStream()) {
        FileUtils.copyInputStreamToFile(inputStream, localFile);
        recodeFile(localFile);
      }
    }
  }

  private void recodeFile(File file)
      throws IOException,
          UnsupportedAudioFileException,
          InterruptedException,
          TelegramApiException {
    val outputFilePath = file.getAbsolutePath() + ".mp3";

    ProcessBuilder processBuilder = new ProcessBuilder();
    val osName = System.getProperty("os.name").toLowerCase();
    val command =
        new String[] {
          new File("src/main/resources/uploadFiles/voice/ffmpeg.exe").getAbsolutePath(),
          "-i",
          file.getAbsolutePath(),
          file.getAbsolutePath() + ".mp3"
        };

    if (osName.contains("win")) {
      processBuilder.command("cmd", "/c", command[0], command[1], command[2], command[3]);
    } else if (osName.contains("mac")) {
      processBuilder.command(
          "open", "-a", "Terminal", command[0], command[1], command[2], command[3]);
    } else if (osName.contains("linux")) {
      processBuilder.command("ffmpeg", command[1], command[2], command[3]);
    } else {
      throw new UnsupportedOperationException("This operating system is not supported.");
    }

    Process process = processBuilder.start();
    process.waitFor();

    val outputFile = new File(outputFilePath);
    try (val inFileStream = AudioSystem.getAudioInputStream(outputFile)) {
      val baseFormat = inFileStream.getFormat();
      val decodedFormat =
          new AudioFormat(
              AudioFormat.Encoding.PCM_SIGNED,
              baseFormat.getSampleRate(),
              16,
              baseFormat.getChannels(),
              baseFormat.getChannels() * 2,
              baseFormat.getSampleRate(),
              false);

      try (val audioInputStream = AudioSystem.getAudioInputStream(decodedFormat, inFileStream)) {

        val recognizer = new Recognizer(model, audioInputStream.getFormat().getSampleRate());

        int inputWave;
        val bytes = new byte[4096];
        while ((inputWave = audioInputStream.read(bytes)) >= 0) {
          if (recognizer.acceptWaveForm(bytes, inputWave)) {
            recognizer.getResult();
          } else {
            recognizer.getPartialResult();
          }
        }
        var recognizeResult = recognizer.getFinalResult();
        recognizeResult = new String(recognizeResult.getBytes("Windows-1251"), UTF_8);

        val jsonObjectResult = new JSONObject(recognizeResult);
        String stringResult = jsonObjectResult.getString("text");

        if (stringResult.length() > 0) {
          sendMessage("\uD83D\uDD0A: " + stringResult, (byte) 0);
        } else {
          sendMessage(
              "Вы отправили пустое голосовое сообщение либо оно не обработалось!", (byte) 0);
        }
      }
    }
    file.delete();
    outputFile.delete();
  }
}
