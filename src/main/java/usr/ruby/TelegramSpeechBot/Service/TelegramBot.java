package usr.ruby.TelegramSpeechBot.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.sound.sampled.AudioFileFormat;
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
import org.telegram.telegrambots.meta.api.objects.Voice;
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
		if(update.hasMessage() && update.getMessage().hasText()){
			String messageText = update.getMessage().getText();
			long chatId = update.getMessage().getChatId();
			String username = update.getMessage().getChat().getFirstName();

			try {
				botAnswerUtils(messageText, chatId, username);
			} catch (TelegramApiException e) {
				throw new RuntimeException(e);
			}
		}
		else if(update.hasCallbackQuery()){
			String messageText = update.getCallbackQuery().getData();
			long chatId = update.getCallbackQuery().getFrom().getId();
			String username = update.getCallbackQuery().getFrom().getFirstName();

			try {
				botAnswerUtils(messageText, chatId, username);
			} catch (TelegramApiException e) {
				throw new RuntimeException(e);
			}
		}
		else if(update.getMessage().hasVoice()){
			Voice voice = update.getMessage().getVoice();
			long chatId = update.getMessage().getChatId();
			System.out.println("https://api.telegram.org/bot" + getBotToken() + "/getFile?file_id=" + voice.getFileId());

			try {
				uploadFiles(voice.getFileId(), chatId);
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			} catch (UnsupportedAudioFileException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void botAnswerUtils(String receivedMessage, long chatId, String userName) throws TelegramApiException{
		switch (receivedMessage){
			case "/start":
				startCommandReceived(chatId, userName);
				break;
			case "/help":
				helpCommandReceived(chatId, userName);
				break;
			default: sendMessage(chatId, "Sorry " + userName + "! This command was not recognized.");
		}
	}

	private void startCommandReceived(long chatId, String userName) throws TelegramApiException {
		String answer = "Привет! " + userName + "! Отправь мне голосовое сообщение!.";

		sendMessage(chatId, answer);
	}

	private void helpCommandReceived(long chatId, String userName) throws TelegramApiException{
		String answer = "Привет " + userName + "! Отправь мне голосовое сообщение!";

		sendMessage(chatId, answer);
	}

	private void sendMessage(long chatId, String answer) throws TelegramApiException {
		SendMessage message = new SendMessage();
		message.setChatId(chatId);
		message.setText(answer);
		message.setReplyMarkup(Buttons.inlineMarkup());

		execute(message);
	}

	private void uploadFiles(String fileId, long chatId)
			throws IOException, UnsupportedAudioFileException, InterruptedException {
		URL url = new URL("https://api.telegram.org/bot" + getBotToken() + "/getFile?file_id=" + fileId);

		val bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
		String getFileResponse = bufferedReader.readLine();

		JSONObject jsonResult = new JSONObject(getFileResponse);
		JSONObject path = jsonResult.getJSONObject("result");
		String file = path.getString("file_path");
		System.out.println(file);

		File localFile = new File("src/main/resources/uploadFiles/" + file);
		InputStream inputStream = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + file).openStream();

		FileUtils.copyInputStreamToFile(inputStream, localFile);

		bufferedReader.close();
		inputStream.close();

		recodeFile(localFile, chatId);
	}

	private void recodeFile(File file, long chatId)
			throws IOException, UnsupportedAudioFileException, InterruptedException {
		String outputFilePath = file.getAbsolutePath() + ".wav";
		val processBuilder = new ProcessBuilder(FFMPEG_FILE_DIR.getAbsolutePath(), "-i", file.getAbsolutePath(), outputFilePath);
		Process process = processBuilder.start();
		process.waitFor();

		File outputFile = new File(outputFilePath);

		AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(outputFile);
		AudioFormat audioFormat = audioFileFormat.getFormat();
		Float sampleRate = audioFormat.getSampleRate();


		try (InputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(outputFile)))){
			Recognizer recognizer = new Recognizer(model, sampleRate);

			int nbytes;
			byte[] b = new byte[4096];
			while ((nbytes = ais.read(b)) >= 0) {
				if (recognizer.acceptWaveForm(b, nbytes)) {
					System.out.println(recognizer.getResult());
				} else {
					System.out.println(recognizer.getPartialResult());
				}
			}
			String recognizeResult = recognizer.getFinalResult();
			Charset utf8 = StandardCharsets.UTF_8;
			recognizeResult = new String(recognizeResult.getBytes("Windows-1251"), utf8);
			System.out.println(recognizeResult);

			JSONObject jsonResult = new JSONObject(recognizeResult);
			String result = jsonResult.getString("text");

			if(result.length() > 0){
				sendMessage(chatId, result);
			}
			else {
				sendMessage(chatId, "Вы отправили пустое голосовое сообщение либо оно не обработалось!");
			}
	} catch (TelegramApiException e) {
			throw new RuntimeException(e);
		}
		file.delete();
		outputFile.delete();
	}
}

