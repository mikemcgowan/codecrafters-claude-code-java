import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import app.App;
import app.Message;
import app.Role;

void main(String[] args) {
    validateArgs(args);
    final var client = getClient();
    final var messages = new LinkedList<Message>();
    messages.add(new Message(Role.USER, args[1]));
    new App(client, messages).run();
}

private static void validateArgs(String[] args) {
    if (args.length < 2 || !"-p".equals(args[0])) {
        System.err.println("Usage: program -p <prompt>");
        System.exit(1);
    }
}

private static OpenAIClient getClient() {
    String apiKey = System.getenv("OPENROUTER_API_KEY");
    String baseUrl = System.getenv("OPENROUTER_BASE_URL");
    if (baseUrl == null || baseUrl.isEmpty()) {
        baseUrl = "https://openrouter.ai/api/v1";
    }
    if (apiKey == null || apiKey.isEmpty()) {
        System.err.println("OPENROUTER_API_KEY is not set");
        System.exit(1);
    }
    return OpenAIOkHttpClient.builder()
                             .apiKey(apiKey)
                             .baseUrl(baseUrl)
                             .build();
}
