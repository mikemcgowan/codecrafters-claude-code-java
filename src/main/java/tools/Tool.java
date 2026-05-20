package tools;

import com.openai.models.chat.completions.ChatCompletionTool;

import jakarta.json.JsonObject;

import java.util.Optional;

public interface Tool {

    FunctionName functionName();

    ChatCompletionTool definition();

    String exec(JsonObject jsonObject);
}
