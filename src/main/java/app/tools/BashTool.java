package app.tools;

import static app.tools.ToolName.BASH;

import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionTool;

import jakarta.json.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

public class BashTool implements Tool {

    @Override
    public ToolName functionName() {
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
                                             "command", Map.of(
                                                 "type", "string",
                                                 "description", "The command to execute"
                                             )
                                         ),
                                         "required", java.util.List.of("command")
                                     )
                                 )))
                                 .build();
    }

    @Override
    public String exec(JsonObject jsonObject) {
        final var command = jsonObject.getString("command");
        try {
            String[] parts = command.split("\\s+");
            final var processBuilder = new ProcessBuilder(parts);
            final var process = processBuilder.start();
            final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            final var lines = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println(line);
                lines.add(line);
            }
            final var exitCode = process.waitFor();
            System.err.println("Exited with code: " + exitCode);
            return lines.toString();
        } catch (Exception e) {
            final var msg = "Couldn't execute command: " + command;
            System.err.println(msg);
            System.err.println(e.getMessage());
            return msg;
        }
    }
}
