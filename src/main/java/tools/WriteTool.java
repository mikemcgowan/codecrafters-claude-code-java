package tools;

import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionTool;

import jakarta.json.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class WriteTool implements Tool {

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
                                             "file_path", Map.of(
                                                 "type", "string",
                                                 "description", "The path to the file to write to"
                                             ),
                                             "content", Map.of(
                                                 "type", "string",
                                                 "description", "The content to write to the file"
                                             )
                                         ),
                                         "required", java.util.List.of("file_path", "content")
                                     )
                                 )))
                                 .build();
    }

    public Optional<String> exec(JsonObject jsonObject) {
        final var filePath = jsonObject.getString("file_path");
        final var content = jsonObject.getString("content");
        try {
            Path path = Path.of(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        } catch (IOException e) {
            System.err.println("Couldn't write file: " + filePath);
            System.err.println(e.getMessage());
        }
        return Optional.empty();
    }
}
