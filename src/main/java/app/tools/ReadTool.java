package app.tools;

import static app.tools.ToolName.READ;

import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionTool;

import jakarta.json.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ReadTool implements Tool {

    private static final String PARAM = "file_path";

    @Override
    public ToolName functionName() {
        return READ;
    }

    @Override
    public ChatCompletionTool definition() {
        return ChatCompletionTool.builder()
                                 .type(JsonValue.from("function"))
                                 .function(JsonValue.from(Map.of(
                                     "type", "object",
                                     "name", "Read",
                                     "description", "Read and return the contents of a file",
                                     "parameters", Map.of(
                                         "type", "object",
                                         "properties", Map.of(
                                             PARAM, Map.of(
                                                 "type", "string",
                                                 "description", "The path to the file to read"
                                             )
                                         ),
                                         "required", java.util.List.of(PARAM)
                                     )
                                 )))
                                 .build();
    }

    @Override
    public String exec(JsonObject jsonObject) {
        final var filePath = jsonObject.getString(PARAM);
        try {
            return Files.readString(Path.of(filePath));
        } catch (IOException _) {
            return "Couldn't read file: " + filePath;
        }
    }
}
