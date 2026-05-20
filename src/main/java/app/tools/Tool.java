package app.tools;

import com.openai.models.chat.completions.ChatCompletionTool;

import jakarta.json.JsonObject;

public interface Tool {

    ToolName functionName();

    ChatCompletionTool definition();

    String exec(JsonObject jsonObject);
}
