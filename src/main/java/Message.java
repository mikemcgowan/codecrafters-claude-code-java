import com.openai.models.chat.completions.ChatCompletionMessageToolCall;

import java.util.List;

public record Message(Role role, String content, String toolCallId, List<ChatCompletionMessageToolCall> toolCalls) {

    public Message(Role role, String content) {
        this(role, content, null, null);
    }

    public Message(Role role, String content, String toolCallId) {
        this(role, content, toolCallId, null);
    }

    public Message(Role role, String content, List<ChatCompletionMessageToolCall> toolCalls) {
        this(role, content, null, toolCalls);
    }
}
