package io.quintkard.quintkardapp.agenttool;

public interface AiTool {

    String name();

    String description();

    Class<?> inputType();

    Object execute(AiToolExecutionRequest request);
}
