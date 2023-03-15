package usr.ruby.TelegramSpeechBot;

import java.io.IOException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.vosk.Model;

@SpringBootApplication
public class TelegramSpeechBotApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(TelegramSpeechBotApplication.class, args);
	}
}
