import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;

import jakarta.json.Json;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import tools.ReadTool;
import tools.WriteTool;

public class App {

    private static final String MODEL = "anthropic/claude-haiku-4.5";

    private final OpenAIClient client;
    private final List<Message> messages;
    private final ReadTool readTool;
    private final WriteTool writeTool;

    public App(OpenAIClient client, List<Message> messages) {
        this.client = client;
        this.messages = messages;
        this.readTool = new ReadTool();
        this.writeTool = new WriteTool();
    }

    public boolean callApi() {
        final var params = ChatCompletionCreateParams.builder()
                                                     .model(MODEL)
                                                     .addTool(readTool.definition())
                                                     .addTool(writeTool.definition());
        messages.forEach(message -> {
            switch (message.role()) {
                case USER -> params.addUserMessage(message.content());
                case TOOL -> {
                    final var msg = ChatCompletionToolMessageParam.builder()
                                                                  .toolCallId(message.toolCallId())
                                                                  .content(message.content())
                                                                  .build();
                    params.addMessage(msg);
                }
                case ASSISTANT -> {
                    final var msg = ChatCompletionAssistantMessageParam.builder()
                                                                       .toolCalls(message.toolCalls())
                                                                       .content(message.content())
                                                                       .build();
                    params.addMessage(msg);
                }
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

        final var optToolCalls = message.toolCalls();
        if (optToolCalls.isEmpty()) {
            System.out.print(messageStr);
            return false;
        }

        final var toolCalls = optToolCalls.get();
        messages.add(new Message(Role.ASSISTANT, messageStr, toolCalls));
        toolCalls.forEach(toolCall -> {
            final var function = toolCall.function();
            final var functionName = function.name();
            final var functionArgs = function.arguments();
            callFunction(
                FunctionName.valueOf(functionName.toUpperCase()),
                functionArgs).ifPresent(result -> messages.add(new Message(Role.TOOL, result, toolCall.id())));
        });
        return true;
    }

    private Optional<String> callFunction(FunctionName functionName, String functionArgs) {
        final var reader = Json.createReader(new StringReader(functionArgs));
        final var jsonObject = reader.readObject();
        reader.close();

        return switch (functionName) {
            case READ -> readTool.exec(jsonObject);
            case WRITE -> writeTool.exec(jsonObject);
        };
    }
}
