# LLM Mobile App Architecture Guide

This document explains the restructured architecture of the LLM Mobile App, which has been refactored to follow the MVVM (Model-View-ViewModel) pattern.

## Project Structure

The app has been split into the following components:

### 1. Network Layer
- **ApiService.kt** - Handles all HTTP communications with the LLM server
- Contains methods for fetching models, checking status, loading/unloading models, and sending prompts
- Uses OkHttp for networking and returns Kotlin Result types for better error handling

### 2. Data Layer
- **LlmRepository.kt** - Acts as a single source of truth for data
- Manages the ApiService and coordinates data operations
- Handles local data persistence (SharedPreferences)
- Abstracts network operations from the ViewModel

### 3. ViewModel Layer
- **LlmViewModel.kt** - Manages UI-related data and business logic
- Holds UI state (selected model, context length, responses, etc.)
- Coordinates between UI and repository
- Survives configuration changes and manages lifecycle

### 4. UI Layer
- **UiComponents.kt** - Contains reusable UI components
- Each component is a separate composable function
- Components include: ModelSelector, ContextLengthInput, PromptInput, ResponseDisplay, etc.
- **MainActivity.kt** - Main entry point for the app
- Assembles UI components and manages navigation

### 5. Application Layer
- **LlmApplication.kt** - Application class for global state
- Initializes repository and provides application-scoped ViewModels

## Data Flow

1. User interacts with UI components in MainActivity
2. Interactions trigger ViewModel methods
3. ViewModel delegates to Repository
4. Repository uses ApiService for network operations
5. Results flow back through the same chain
6. ViewModel updates its state
7. UI automatically updates based on ViewModel state

## Key Benefits of This Structure

1. **Separation of Concerns**: Each class has a single responsibility
2. **Testability**: Components can be tested in isolation
3. **Maintainability**: Smaller files are easier to understand and modify
4. **Reusability**: UI components can be reused across different screens

## How to Add Features

When adding new features, follow these guidelines:

1. **Add API Methods**: If the feature requires server communication, add methods to ApiService.kt
2. **Update Repository**: Add methods to access the new API functionality
3. **Add ViewModel Logic**: Implement the business logic in LlmViewModel.kt
4. **Create UI Components**: Add new composable functions to UiComponents.kt
5. **Update MainActivity**: Assemble the new components in MainActivity.kt

## Example: Adding a History Feature

Here's how you might implement a history feature to save past conversations:

1. Create a data class `ConversationHistory` in the data layer
2. Add methods to `LlmRepository` to save and retrieve history
3. Add state and methods to `LlmViewModel` to manage history
4. Create UI components for displaying and interacting with history
5. Update MainActivity to incorporate the new components

## Conclusion

This architecture provides a solid foundation for building complex features while keeping the codebase organized and maintainable. Each layer can evolve independently, making it easier to respond to changing requirements.