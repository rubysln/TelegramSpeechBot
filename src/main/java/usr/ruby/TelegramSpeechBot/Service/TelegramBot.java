package usr.ruby.TelegramSpeechBot.service;

import it.sauronsoftware.jave.EncoderException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.vosk.Model;
import org.vosk.Recognizer;
import usr.ruby.TelegramSpeechBot.component.BotCommands;
import usr.ruby.TelegramSpeechBot.component.Buttons;
import usr.ruby.TelegramSpeechBot.config.BotConfig;

@Component
public class TelegramBot extends TelegramLongPollingBot implements BotCommands {
  private final BotConfig config;

  private final Model model = new Model("src/main/resources/models/ru/vosk-model-small-ru-0.22");
  private final File FFMPEG_FILE_DIR = new File("src/main/resources/uploadFiles/voice/ffmpeg.exe");

  public TelegramBot(BotConfig config) throws IOException {
    this.config = config;
  }

  @Override
  public String getBotUsername() {
    return config.getBotName();
  }

  @Override
  public String getBotToken() {
    return config.getBotToken();
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {
      val messageText = update.getMessage().getText();
      val chatId = update.getMessage().getChatId();
      val username = update.getMessage().getChat().getFirstName();

      try {
        botAnswerUtils(messageText, chatId, username);
      } catch (TelegramApiException e) {
        throw new RuntimeException(e);
      }
    } else if (update.hasCallbackQuery()) {
      val messageText = update.getCallbackQuery().getData();
      val chatId = update.getCallbackQuery().getFrom().getId();
      val username = update.getCallbackQuery().getFrom().getFirstName();

      try {
        botAnswerUtils(messageText, chatId, username);
      } catch (TelegramApiException e) {
        throw new RuntimeException(e);
      }
    } else if (update.getMessage().hasVoice()) {
      val voice = update.getMessage().getVoice();
      val chatId = update.getMessage().getChatId();
      System.out.println(
          "https://api.telegram.org/bot" + getBotToken() + "/getFile?file_id=" + voice.getFileId());

      try {
        uploadFiles(voice.getFileId(), chatId);
      } catch (IOException
          | InterruptedException
          | UnsupportedAudioFileException
          | EncoderException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void botAnswerUtils(String receivedMessage, long chatId, String userName)
      throws TelegramApiException {
    switch (receivedMessage) {
      case "/start":
        startCommandReceived(chatId, userName);
        break;
      case "/help":
        helpCommandReceived(chatId, userName);
        break;
      default:
        sendMessage(chatId, "Sorry " + userName + "! This command was not recognized.");
    }
  }

  private void startCommandReceived(long chatId, String userName) throws TelegramApiException {
    val answer = "Привет! " + userName + "! Отправь мне голосовое сообщение!.";

    sendMessage(chatId, answer);
  }

  private void helpCommandReceived(long chatId, String userName) throws TelegramApiException {
    val answer = "Привет " + userName + "! Отправь мне голосовое сообщение!";

    sendMessage(chatId, answer);
  }

  private void sendMessage(long chatId, String answer) throws TelegramApiException {
    val message = new SendMessage();
    message.setChatId(chatId);
    message.setText(answer);
    message.setReplyMarkup(Buttons.inlineMarkup());

    execute(message);
  }

  private void uploadFiles(String fileId, long chatId)
      throws IOException, UnsupportedAudioFileException, InterruptedException, EncoderException {
    val url =
        new URL("https://api.telegram.org/bot" + getBotToken() + "/getFile?file_id=" + fileId);

    val bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
    val getFileResponse = bufferedReader.readLine();

    val jsonResult = new JSONObject(getFileResponse);
    val path = jsonResult.getJSONObject("result");
    val file = path.getString("file_path");
    System.out.println(file);

    val localFile = new File("src/main/resources/uploadFiles/" + file);
    val inputStream =
        new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + file).openStream();

    FileUtils.copyInputStreamToFile(inputStream, localFile);

    bufferedReader.close();
    inputStream.close();

    recodeFile(localFile, chatId);
  }

  private void recodeFile(File file, long chatId)
      throws IOException, UnsupportedAudioFileException, InterruptedException, EncoderException {
    val outputFilePath = file.getAbsolutePath() + ".mp3";
    try {
      val processBuilder =
          new ProcessBuilder("ffmpeg", "-i", file.getAbsolutePath(), outputFilePath);
      val process = processBuilder.start();
      process.waitFor();
    } catch (IOException exception) {
      val processBuilder =
          new ProcessBuilder(
              FFMPEG_FILE_DIR.getAbsolutePath(), "-i", file.getAbsolutePath(), outputFilePath);
      val process = processBuilder.start();
      process.waitFor();
    }

    val outputFile = new File(outputFilePath);
    System.out.println("Created converted file: " + outputFile.getName());
    val inFileStream = AudioSystem.getAudioInputStream(outputFile);
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
      val utf8 = StandardCharsets.UTF_8;
      recognizeResult = new String(recognizeResult.getBytes("Windows-1251"), utf8);
      System.out.println(recognizeResult);

      val jsonObjectResult = new JSONObject(recognizeResult);
      val stringResult = jsonObjectResult.getString("text");

      if (stringResult.length() > 0) {
        sendMessage(chatId, stringResult);
      } else {
        sendMessage(chatId, "Вы отправили пустое голосовое сообщение либо оно не обработалось!");
      }
    } catch (TelegramApiException exception) {
      throw new RuntimeException(exception);
    }
    if (file.delete()) {
      System.out.println("Input voice file deleted!");
    }
    ;
    if (outputFile.delete()) {
      System.out.println("Converted voice file deleted!");
    }
    ;
  }
}
