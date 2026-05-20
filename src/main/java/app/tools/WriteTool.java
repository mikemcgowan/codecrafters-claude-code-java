package app.tools;

import static app.tools.ToolName.WRITE;

import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionTool;

import jakarta.json.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class WriteTool implements Tool {

    private static final String PARAM1 = "file_path";
    private static final String PARAM2 = "content";

    @Override
    public ToolName toolName() {
        return WRITE;
    }

    @Override
    public ChatCompletionTool definition() {
        return ChatCompletionTool.builder()
                                 .type(JsonValue.from("function"))
                                 .function(JsonValue.from(Map.of(
                                     "type", "object",
                                     "name", "Write",
                                     "description", "Write content to a file",
                                     "parameters", Map.of(
                                         "type", "object",
                                         "properties", Map.of(
                                             PARAM1, Map.of(
                                                 "type", "string",
                                                 "description", "The path to the file to write to"
                                             ),
                                             PARAM2, Map.of(
                                                 "type", "string",
                                                 "description", "The content to write to the file"
                                             )
                                         ),
                                         "required", java.util.List.of(PARAM1, PARAM2)
                                     )
                                 )))
                                 .build();
    }

    @Override
    public String exec(JsonObject jsonObject) {
        final var filePath = jsonObject.getString(PARAM1);
        final var content = jsonObject.getString(PARAM2);
        try {
            final var path = Path.of(filePath);
            final var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content);
            return "File written successfully";
        } catch (IOException _) {
            return "Couldn't write file: " + filePath;
        }
    }
}
