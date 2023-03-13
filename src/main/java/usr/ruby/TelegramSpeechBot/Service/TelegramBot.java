package usr.ruby.TelegramSpeechBot.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import usr.ruby.TelegramSpeechBot.component.BotCommands;
import usr.ruby.TelegramSpeechBot.component.Buttons;
import usr.ruby.TelegramSpeechBot.config.BotConfig;

@Component
public class TelegramBot extends TelegramLongPollingBot implements BotCommands {
	private final BotConfig config;

	public TelegramBot(BotConfig config) {
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
				uploadFiles(voice.getFileId());
			} catch (IOException e) {
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

	private void uploadFiles(String fileId) throws IOException {
		URL url = new URL("https://api.telegram.org/bot" + getBotToken() + "/getFile?file_id=" + fileId);

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
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

		//recodeFile(localFile);
	}

	private void recodeFile(File file){
		//...

		file.delete();
	}
}
