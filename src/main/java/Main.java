import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import java.util.LinkedList;

public class Main {

    public static void main(String[] args) {
        final var client = getClient(args);
        final var messages = new LinkedList<Message>();
        messages.add(new Message(Role.USER, args[1], null));
        boolean gotToolCall = true;
        final var app = new App(client, messages);
        while (gotToolCall) {
            gotToolCall = app.callApi();
        }
    }

    private static OpenAIClient getClient(String[] args) {
        if (args.length < 2 || !"-p".equals(args[0])) {
            System.err.println("Usage: program -p <prompt>");
            System.exit(1);
        }

        String apiKey = System.getenv("OPENROUTER_API_KEY");
        String baseUrl = System.getenv("OPENROUTER_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://openrouter.ai/api/v1";
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENROUTER_API_KEY is not set");
        }

        return OpenAIOkHttpClient.builder()
                                 .apiKey(apiKey)
                                 .baseUrl(baseUrl)
                                 .build();
    }
}
