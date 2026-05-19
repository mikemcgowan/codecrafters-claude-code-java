public record Message(Role role, String content, String toolCallId) {
    public Message(Role role, String content) {
        this(role, content, null);
    }
}
