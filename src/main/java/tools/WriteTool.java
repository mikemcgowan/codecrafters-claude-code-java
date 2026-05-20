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
            final var path = Path.of(filePath);
            final var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content);
            return Optional.of("File written successfully");
        } catch (IOException e) {
            final var msg = "Couldn't write file: " + filePath;
            System.err.println(msg);
            System.err.println(e.getMessage());
            return Optional.of(msg);
        }
    }
}
