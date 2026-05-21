package app;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;

import jakarta.json.Json;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import app.tools.BashTool;
import app.tools.ReadTool;
import app.tools.Tool;
import app.tools.ToolName;
import app.tools.WriteTool;

public class App {

    private static final String MODEL = "anthropic/claude-haiku-4.5";
    private static final int MAX_ITERATIONS = 100;

    private final OpenAIClient client;
    private final List<Message> messages;
    private final List<Tool> tools;

    public App(OpenAIClient client, List<Message> messages) {
        this.client = client;
        this.messages = messages;
        this.tools = List.of(new ReadTool(), new WriteTool(), new BashTool());
    }

    public void run() {
        int iterations = 0;
        while (iterations < MAX_ITERATIONS) {
            ++iterations;
            callApi(iterations);
        }
        System.err.println("Too many agent loop iterations");
        System.exit(1);
    }

    private void callApi(int iteration) {
        System.err.println("\ncallApi(" + iteration + "):");
        final var response = client.chat()
                                   .completions()
                                   .create(prepareParams());

        if (response.choices()
                    .isEmpty()) {
            System.err.println("No choices in response");
            System.exit(1);
        }

        final var choiceZero = response.choices()
                                       .getFirst();
        final var message = choiceZero.message();
        final var messageStr = message.content()
                                      .orElse("");
        System.err.println("Message content from API call is: " + messageStr);

        final var optToolCalls = message.toolCalls();
        if (optToolCalls.isEmpty()) {
            System.out.print(messageStr);
            System.exit(0);
        }

        final var toolCalls = optToolCalls.get();
        messages.add(new Message(Role.ASSISTANT, messageStr, toolCalls));
        processToolCalls(toolCalls);
    }

    private ChatCompletionCreateParams prepareParams() {
        final var params = ChatCompletionCreateParams.builder()
                                                     .model(MODEL);
        tools.forEach(tool -> params.addTool(tool.definition()));
        messages.forEach(message -> {
            System.err.println(message.role() + ": " + message.content());
            switch (message.role()) {
                case USER -> params.addUserMessage(message.content());
                case TOOL -> params.addMessage(ChatCompletionToolMessageParam.builder()
                                                                             .toolCallId(message.toolCallId())
                                                                             .content(message.content())
                                                                             .build());
                case ASSISTANT -> params.addMessage(ChatCompletionAssistantMessageParam.builder()
                                                                                       .toolCalls(message.toolCalls())
                                                                                       .content(message.content())
                                                                                       .build());
            }
        });
        return params.build();
    }

    private void processToolCalls(List<ChatCompletionMessageToolCall> toolCalls) {
        toolCalls.forEach(toolCall -> {
            final var function = toolCall.function();
            final var functionName = function.name();
            final var functionArgs = function.arguments();
            callFunction(
                ToolName.valueOf(functionName.toUpperCase()),
                functionArgs).ifPresent(result -> messages.add(new Message(Role.TOOL, result, toolCall.id())));
        });
    }

    private Optional<String> callFunction(ToolName toolName, String functionArgs) {
        final var reader = Json.createReader(new StringReader(functionArgs));
        final var jsonObject = reader.readObject();
        reader.close();

        return tools.stream()
                    .filter(tool -> tool.toolName()
                                        .equals(toolName))
                    .map(tool -> tool.exec(jsonObject))
                    .findFirst();
    }
}
