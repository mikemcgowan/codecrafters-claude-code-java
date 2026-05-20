package app.tools;

import static app.tools.ToolName.BASH;

import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionTool;

import jakarta.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

public class BashTool implements Tool {

    private static final String PARAM = "command";

    @Override
    public ToolName toolName() {
        return BASH;
    }

    @Override
    public ChatCompletionTool definition() {
        return ChatCompletionTool.builder()
                                 .type(JsonValue.from("function"))
                                 .function(JsonValue.from(Map.of(
                                     "type", "object",
                                     "name", "Bash",
                                     "description", "Execute a command shell",
                                     "parameters", Map.of(
                                         "type", "object",
                                         "properties", Map.of(
                                             PARAM, Map.of(
                                                 "type", "string",
                                                 "description", "The command to execute"
                                             )
                                         ),
                                         "required", java.util.List.of(PARAM)
                                     )
                                 )))
                                 .build();
    }

    @Override
    public String exec(JsonObject jsonObject) {
        final var command = jsonObject.getString(PARAM);
        try {
            final var parts = command.split("\\s+");
            final var processBuilder = new ProcessBuilder(parts);
            final var process = processBuilder.start();
            final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            final var lines = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines.toString();
        } catch (IOException _) {
            return "Couldn't execute command: " + command;
        }
    }
}
