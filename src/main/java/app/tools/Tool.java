package app.tools;

import com.openai.models.chat.completions.ChatCompletionTool;

import jakarta.json.JsonObject;

public interface Tool {

    ToolName toolName();

    ChatCompletionTool definition();

    String exec(JsonObject jsonObject);
}
