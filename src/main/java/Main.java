import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionTool;

import jakarta.json.Json;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Main {

    private enum FunctionNames {
        READ;
    }

    public static void main(String[] args) {
        if (args.length < 2 || !"-p".equals(args[0])) {
            System.err.println("Usage: program -p <prompt>");
            System.exit(1);
        }

        String prompt = args[1];

        String apiKey = System.getenv("OPENROUTER_API_KEY");
        String baseUrl = System.getenv("OPENROUTER_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://openrouter.ai/api/v1";
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENROUTER_API_KEY is not set");
        }

        OpenAIClient client = OpenAIOkHttpClient.builder()
                                                .apiKey(apiKey)
                                                .baseUrl(baseUrl)
                                                .build();

        ChatCompletion response = client.chat()
                                        .completions()
                                        .create(
                                            ChatCompletionCreateParams.builder()
                                                                      .model("anthropic/claude-haiku-4.5")
                                                                      .addUserMessage(prompt)
                                                                      .addTool(readTool())
                                                                      .build()
                                        );

        if (response.choices()
                    .isEmpty()) {
            throw new RuntimeException("no choices in response");
        }

        final var choiceZero = response.choices()
                                       .get(0);
        final var message = choiceZero.message();
        message.toolCalls()
               .ifPresentOrElse(toolCalls -> {
                                    final var toolCallZero = toolCalls.get(0);
                                    final var function = toolCallZero.function();
                                    final var functionName = function.name();
                                    final var functionArgs = function.arguments();
                                    callFunction(FunctionNames.valueOf(functionName), functionArgs);
                                }, () -> System.out.print(choiceZero.message()
                                                                    .content()
                                                                    .orElse(""))
               );
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

    private static void callFunction(FunctionNames functionName, String functionArgs) {
        final var reader = Json.createReader(new StringReader(functionArgs));
        final var jsonObject = reader.readObject();
        reader.close();

        switch (functionName) {
            case READ -> readFunction(jsonObject.getString("file_path"));
            default -> throw new RuntimeException("Unknown function: " + functionName);
        }
    }

    private static void readFunction(String filePath) {
        try {
            System.out.println(Files.readString(Path.of(filePath)));
        } catch (IOException e) {
            System.err.println("Couldn't read file: " + filePath);
        }
    }
}
