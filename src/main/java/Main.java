import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionTool;

import jakarta.json.Json;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Map;

public class Main {

    private static final String MODEL = "anthropic/claude-haiku-4.5";

    private enum FunctionNames {
        READ;
    }

    public static void main(String[] args) {
        final var client = getClient(args);
        final var messages = new LinkedList<Message>();
        messages.add(new Message(Role.USER, args[1], null));
        boolean gotToolCall = true;
        while (gotToolCall) {
            final var params = ChatCompletionCreateParams.builder()
                                                         .model(MODEL)
                                                         .addTool(readTool());
            messages.forEach(message -> {
                switch (message.role()) {
                    case USER -> params.addUserMessage(message.content());
                    case TOOL -> params.addUserMessage("Tool result: " + message.content());
                    case ASSISTANT -> System.err.println("Don't know how to handle messages for assistant!");
                }
            });
            final var response = client.chat()
                                       .completions()
                                       .create(params.build());

            if (response.choices()
                        .isEmpty()) {
                throw new RuntimeException("no choices in response");
            }

            final var choiceZero = response.choices()
                                           .get(0);
            final var message = choiceZero.message();
            final var messageStr = message.content()
                                          .orElse("");
            messages.add(new Message(Role.ASSISTANT, messageStr, null));
            if (message.toolCalls()
                       .isPresent()) {
                final var toolCalls = message.toolCalls()
                                             .get();
                toolCalls.forEach(toolCall -> {
                    final var function = toolCall.function();
                    final var functionName = function.name();
                    final var functionArgs = function.arguments();
                    final var result = callFunction(FunctionNames.valueOf(functionName.toUpperCase()), functionArgs);
                    messages.add(new Message(Role.TOOL, result, toolCall.id()));
                });
            } else {
                gotToolCall = false;
                System.out.print(messageStr);
            }
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

    private static ChatCompletionTool readTool() {
        return ChatCompletionTool.builder()
                                 .type(JsonValue.from("function"))
                                 .function(JsonValue.from(Map.of(
                                     "type", "object",
                                     "name", "Read",
                                     "description", "Read and return the contents of a file",
                                     "parameters", Map.of(
                                         "type", "object",
                                         "properties", Map.of(
                                             "file_path", Map.of(
                                                 "type", "string",
                                                 "description", "The path to the file to read"
                                             )
                                         ),
                                         "required", java.util.List.of("file_path")
                                     )
                                 )))
                                 .build();
    }

    private static String callFunction(Main.FunctionNames functionName, String functionArgs) {
        final var reader = Json.createReader(new StringReader(functionArgs));
        final var jsonObject = reader.readObject();
        reader.close();

        return switch (functionName) {
            case READ -> readFunction(jsonObject.getString("file_path"));
        };
    }

    private static String readFunction(String filePath) {
        try {
            return Files.readString(Path.of(filePath));
        } catch (IOException e) {
            System.err.println("Couldn't read file: " + filePath);
            System.err.println(e.getMessage());
            return null;
        }
    }
}
