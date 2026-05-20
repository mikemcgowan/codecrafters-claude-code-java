package app.tools;

public enum ToolName {
    READ,
    WRITE,
    BASH;

    public String ucFirst() {
        return name().charAt(0)
               + name().substring(1)
                       .toLowerCase();
    }
}
