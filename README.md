# Personal LLM Android App

A personal Android application that connects to my local LLM server, enabling private AI interactions with full parameter control directly from my mobile device. This project was built for my specific setup but will work for others with similar configurations.

## 🎯 Personal Project Notice

**This is a personal project** designed specifically for my use case:
- **Windows 11 PC** with NVIDIA GPU and CUDA support
- **Android phone** on the same WiFi network
- **Local AI inference** without cloud dependencies

While the code is open source and others can use it, **it's optimized for my specific setup**. If you have a similar configuration (Windows 11 + NVIDIA GPU + Android), it should work for you too!

## ⚠️ Important: Requires MyLLMServer

**This Android app does nothing by itself.** It's only a client that connects to my separate [MyLLMServer](https://github.com/yourusername/MyLLMServer) repository.

**You MUST set up the server first** by following the complete setup instructions in the [MyLLMServer](https://github.com/yourusername/MyLLMServer) repository. The server handles all the AI processing, model loading, and CUDA acceleration.

## ✨ Features

### 🎨 **Modern UI/UX**
- **Material Design 3**: Clean interface with dark/light theme support
- **Real-time Streaming**: Live token streaming for immediate response feedback
- **Connection Status**: Visual indicators for server connectivity
- **One-tap Copy**: Easy copying of AI responses to clipboard
- **Full-Screen Modes**: Expandable prompt editor and response viewer
- **Swipeable Navigation**: Smooth navigation between Chat and Parameters screens

### 🔧 **Advanced Model Management**
- **Multiple Model Support**: Easy switching between different LLM models
- **Dynamic Model Loading**: Load/unload models remotely to manage server memory
- **Custom Loading Parameters**: Full control over n_ctx, n_gpu_layers, n_threads, memory settings
- **Model-Specific Defaults**: Each model can have its own parameter configurations
- **Real-time Parameter Validation**: Client-side validation with server-side verification

### 🎛️ **Comprehensive Parameter Control**
- **Inference Parameters Screen**: Dedicated screen for tuning generation parameters
- **Real-time Sliders**: Interactive controls for temperature, top_p, top_k, repeat_penalty, min_p, max_tokens
- **Parameter Persistence**: Settings saved across app restarts and screen rotations
- **Live Parameter Testing**: Send test prompts with current parameter settings
- **Visual Feedback**: Modified parameters highlighted with visual indicators
- **Quick Reset**: One-tap reset to model defaults for any parameter

### 🚀 **Smart Functionality**
- **Context Usage Monitoring**: Real-time token counting and usage visualization
- **Raw Prompt Mode**: Send prompts exactly as typed without auto-formatting
- **Model Prefix/Suffix Integration**: Append model-specific formatting strings to prompts
- **Rotation Persistence**: All data persists across screen rotations
- **Auto-connect**: Automatic server connection with saved settings

## 🏗️ Project Architecture

The app follows a clean MVVM architecture with proper separation of concerns:

### 1. **Network Layer** (`network/`)
- **ApiService.kt** - Handles all HTTP communications with enhanced parameter support
- **Data Classes** - Type-safe models for loading parameters, inference parameters, context usage
- **Result Handling** - Kotlin Result types for robust error handling

### 2. **Data Layer** (`data/`)
- **LlmRepository.kt** - Single source of truth for data operations
- **SharedPreferences Integration** - Persistent storage for server settings
- **Parameter Management** - Handles loading and inference parameter operations

### 3. **ViewModel Layer** (`viewmodel/`)
- **LlmViewModel.kt** - Manages UI state and business logic with SavedStateHandle
- **Parameter State Management** - Handles loading and inference parameter values
- **Lifecycle-aware Operations** - Survives configuration changes

### 4. **UI Layer** (`ui/`)

#### **Screens** (`screens/`)
- **ChatScreen.kt** - Main chat interface with prompt input and response display
- **ParametersScreen.kt** - Dedicated inference parameters management screen
- **FullScreenScreens.kt** - Full-screen prompt editor and response viewer

#### **Components** (`components/`)
- **ModelComponents.kt** - Model selection and loading parameter controls
- **PromptComponents.kt** - Enhanced prompt input with context usage display
- **ResponseComponents.kt** - Response display with copy functionality
- **StatusComponents.kt** - Status messages and context usage indicators
- **PrefixSuffixComponents.kt** - Model parameter string append dialogs
- **DialogComponents.kt** - Settings and model configuration dialogs

#### **Navigation** (`navigation/`)
- **AppNavigation.kt** - HorizontalPager-based swipeable navigation
- **NavigationState.kt** - Navigation state management
- **BottomNavigation.kt** - Custom bottom navigation bar

#### **Layout** (`layout/`)
- **AppScaffold.kt** - Main app structure with top/bottom bars
- **TopAppBar.kt** - Connection status and action buttons

#### **State Management** (`state/`, `dialogs/`)
- **UiState.kt** - Centralized UI state management
- **DialogState.kt** - Dialog visibility and coordination

### 5. **Application Layer**
- **LlmApplication.kt** - Application class for global state initialization
- **MainActivity.kt** - Minimal activity handling only lifecycle and ViewModel setup

## 🚀 Setup Instructions

### Prerequisites

**You need all of these**:
- **Windows 11 PC** with NVIDIA GPU and CUDA support
- **Android device** with API level 24+ (Android 7.0+)
- **Android Studio** (latest version)
- **Same WiFi network** for phone and PC
- **[MyLLMServer](https://github.com/yourusername/MyLLMServer) set up and running**

### Step 1: Set Up the Server FIRST

**This app won't work without the server!**

1. Go to the [MyLLMServer](https://github.com/yourusername/MyLLMServer) repository
2. Follow ALL the setup instructions there completely
3. Make sure the server is running and shows your IP address
4. Test the server works by visiting `http://YOUR_PC_IP:5000/server/ping` in a browser

### Step 2: Install the Android App

1. **Clone this repository**:
   ```bash
   git clone https://github.com/yourusername/my-llm-android-app.git
   cd my-llm-android-app
   ```

2. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory
   - Wait for Gradle sync to complete

3. **Build and install**:
   - Connect your Android device via USB (enable Developer Options & USB Debugging)
   - Click "Run" or press `Ctrl+F10`
   - Select your device from the list

### Step 3: Connect to Your Server

1. **Configure connection**:
   - Open the app on your Android device
   - Tap the ⚙️ Settings icon in the top bar
   - Enter your PC's IP address exactly as shown by the server (e.g., `http://192.168.1.100:5000`)
   - Tap "Save & Connect"
   - You should see "Connected" status

2. **Load a model**:
   - Tap the 💾 Model Settings icon
   - Select a model from the dropdown (configured in your server)
   - Optionally adjust loading parameters (context length, GPU layers, etc.)
   - Tap "Load Model"

3. **Configure inference parameters** (Optional):
   - Swipe to the "Parameters" tab or tap it in bottom navigation
   - Adjust sliders for temperature, top_p, top_k, repeat_penalty, min_p, max_tokens
   - Parameters are automatically saved and used for all subsequent prompts
   - Tap "Test" to send a test prompt with current settings

4. **Start using**:
   - Return to the Chat tab
   - Type your prompt in the text field
   - Tap "Send" to get a response
   - Use the copy button to save responses

## 📱 How to Use

### Main Chat Interface
- **Prompt Input**: Type your questions with full-screen expand option
- **Context Usage Bar**: Shows real-time token usage when model is loaded
- **Send Button**: Submits prompt with current inference parameter settings
- **Response Area**: Scrollable display with copy functionality and expand option
- **Status Indicators**: Shows connection and model status in top bar

### Parameters Screen (New!)
- **Interactive Sliders**: Real-time adjustment of all inference parameters
- **Visual Feedback**: Modified parameters highlighted with different colors
- **Parameter Info**: Shows current value, default, min/max ranges, and descriptions
- **Text Input Mode**: Tap parameter values for precise numeric input
- **Quick Reset**: Individual parameter reset or reset all to defaults
- **Test Function**: Send test prompts with current parameter settings
- **Auto-save**: All changes automatically saved and persist across app restarts

### Settings (⚙️ icon)
- **Server URL**: Must match the IP shown by your MyLLMServer
- **Auto-connect**: Tests connection when you save settings

### Model Settings (💾 icon)
- **Model Selection**: Choose from models configured on your server
- **Loading Parameters**: Full control over model loading behavior:
   - **Context Length (n_ctx)**: Memory window size (128-32768)
   - **GPU Layers**: How many layers to run on GPU (-1 for all)
   - **CPU Threads**: Processing threads (1-32)
   - **Memory Settings**: mlock, mmap, and f16_kv options
- **Load/Unload**: Manage server memory usage
- **Parameter Status**: See exactly which parameters were used to load current model
- **Refresh Models**: Update list if you add models to the server

### Full-Screen Modes
- **Prompt Editor**: Expand prompt input for long text with navigation controls
- **Response Viewer**: Full-screen response reading with text selection
- **Model Parameters**: Append model-specific prefix/suffix strings to prompts

## 🔍 Troubleshooting

### Connection Issues

**"Not Connected" Status**:
1. **Verify your MyLLMServer is running** - you should see the IP address in the console
2. **Check WiFi** - both devices must be on same network
3. **Test in browser** - visit `http://YOUR_PC_IP:5000/server/ping`
4. **Check firewall** - Windows may be blocking port 5000
5. **Double-check IP address** - make sure it matches exactly what the server shows

### Model Issues

**No Models Available**:
1. **Server configuration** - verify you have models configured in the server's `config.py`
2. **Tap "Refresh Models"** in Model Settings
3. **Check server logs** for model loading errors

**Model Won't Load**:
1. **Check server memory** - you may need more RAM/VRAM
2. **Adjust loading parameters** - reduce n_ctx or n_gpu_layers
3. **Try smaller model** first to test connection
4. **Check server logs** for specific error messages

**Parameter Issues**:
1. **Invalid parameter values** - app validates ranges, check error messages
2. **Server parameter mismatch** - ensure server and app are same version
3. **Reset to defaults** - use reset buttons if parameters get corrupted

### Performance Issues

**Slow Responses**:
1. **Adjust inference parameters** - lower temperature and max_tokens for faster responses
2. **Check server resources** - monitor CPU/GPU usage
3. **Optimize loading parameters** - adjust n_threads and n_gpu_layers

**App Crashes or Freezes**:
1. **Check Android logs** - use Android Studio logcat for error details
2. **Clear app data** - reset to fresh state if needed
3. **Restart both app and server** - fresh connection often helps

### Still Having Problems?

1. **Check server setup** - go back to [MyLLMServer](https://github.com/yourusername/MyLLMServer) and verify everything is working
2. **Review server logs** - check `logs/` directory for detailed error information
3. **Test server endpoints** - use browser or curl to test individual API endpoints
4. **Android Studio debugging** - check logcat for detailed error messages

## 🗺️ Development Roadmap

### ✅ **Completed Features**
- [x] **Text Box Expansion Controls** - Full-screen prompt editor and response viewer
- [x] **Screen Rotation Text Persistence** - All data persists across rotations using SavedStateHandle
- [x] **Model Parameter Prefix/Suffix Buttons** - Append model formatting strings to prompts
- [x] **Full Model Parameter Editing** - Complete inference parameter control with validation
- [x] **Advanced Loading Parameters** - Full control over model loading behavior
- [x] **Real-time Parameter Validation** - Client and server-side validation with error handling

### 🔄 **Future Planned Features**
- [ ] **Enhanced Parameter Presets** - Save and load custom parameter configurations
- [ ] **Conversation History** - Local conversation storage and management
- [ ] **Saved Prompts with Persistence** - Save/load/edit/delete named prompts that persist across app restarts
- [ ] **Advanced Response Management** - Response templates, formatting options, and export features
- [ ] **Performance Monitoring** - Real-time server performance metrics and optimization suggestions

## 🔧 Technical Implementation Details

### Parameter Management System

The app implements a comprehensive parameter management system with three levels:

1. **Loading Parameters** - Control how models are loaded into memory
2. **Inference Parameters** - Control text generation behavior in real-time
3. **Model-Specific Defaults** - Each model can override global defaults

### State Management Architecture

- **SavedStateHandle Integration** - All critical state survives process death and screen rotations
- **Reactive UI Updates** - Parameter changes immediately reflect in UI with visual feedback
- **Validation Pipeline** - Client-side validation before server submission with fallback error handling
- **Persistent Storage** - Server URL and parameter preferences saved locally

### Network Architecture

- **Coroutine-based Networking** - All network operations use structured concurrency
- **Result Type Error Handling** - Type-safe error handling throughout the application
- **Streaming Response Processing** - Real-time token streaming with proper backpressure handling
- **Connection State Management** - Automatic reconnection and robust error recovery

## 📚 Code Examples

### Using the Parameter System

```kotlin
// Update an inference parameter
viewModel.updateInferenceParameter("temperature", 0.8f)

// Load model with custom loading parameters
val loadingParams = mapOf(
    "n_ctx" to 4096,
    "n_gpu_layers" to -1,
    "n_threads" to 8
)
viewModel.loadModelWithParameters("MyMainLLM")

// Send prompt with current inference settings
viewModel.sendPrompt(
    prompt = "Your question here",
    systemPrompt = "You are a helpful assistant"
)
```

### Server API Integration

```kotlin
// The app automatically handles parameter validation
apiService.loadModelWithParameters(
    modelName = "MyMainLLM",
    loadingParams = mapOf(
        "n_ctx" to 4096,
        "n_gpu_layers" to -1,
        "use_mlock" to true
    )
)

// Inference parameters sent with each query
apiService.sendStreamingPrompt(
    prompt = prompt,
    systemPrompt = systemPrompt,
    modelName = modelName,
    inferenceParams = mapOf(
        "temperature" to 0.8f,
        "max_tokens" to 500f,
        "top_p" to 0.9f
    )
)
```

## 🎯 Best Practices

### Parameter Tuning Guidelines

**For Creative Writing:**
- Temperature: 0.8-1.2
- Top_p: 0.9-0.95
- Top_k: 40-80
- Repeat_penalty: 1.0-1.1

**For Technical/Factual Content:**
- Temperature: 0.1-0.5
- Top_p: 0.8-0.9
- Top_k: 20-40
- Repeat_penalty: 1.1-1.2

**For Code Generation:**
- Temperature: 0.2-0.4
- Top_p: 0.85-0.9
- Top_k: 30-50
- Repeat_penalty: 1.05-1.15

### Loading Parameter Optimization

**For Large Models (7B+):**
- Start with n_gpu_layers: -1 (all layers on GPU)
- Reduce if VRAM insufficient
- Use higher n_ctx for longer conversations
- Enable memory optimizations (mlock, mmap)

**For Smaller Models (3B-):**
- Can run with fewer GPU layers
- Higher n_threads for CPU processing
- Larger context windows possible

## 🔒 Privacy & Security

- **Local Processing**: All AI processing happens on your local server
- **No Cloud Dependency**: No data sent to external services
- **Network Security**: Communication only within your local WiFi network
- **Data Persistence**: All settings and conversations stored locally on device

## 📊 Performance Considerations

### Memory Usage
- **App Memory**: Typically 50-100MB for the Android app
- **Server Memory**: Depends on model size and loading parameters
- **Context Management**: Monitor token usage to stay within model limits

### Network Performance
- **Streaming Responses**: Optimized for real-time token delivery
- **Parameter Validation**: Client-side validation reduces server load
- **Connection Pooling**: Efficient HTTP connection reuse

### Battery Optimization
- **Background Processing**: Minimal background activity
- **Network Efficiency**: Optimized request/response cycles
- **UI Updates**: Efficient state management reduces battery drain

## 🐛 Known Issues & Limitations

### Current Limitations
1. **Single Server Connection**: App connects to one server at a time
2. **No Conversation Branching**: Linear conversation flow only
3. **Limited Model Formats**: GGUF models only (server limitation)
4. **WiFi Dependency**: Requires local network connection

### Workarounds
- **Server Switching**: Change server URL in settings for different servers
- **Conversation Management**: Use copy/paste for conversation branching
- **Model Format**: Convert models to GGUF format for compatibility
- **Network Issues**: Ensure stable WiFi or use mobile hotspot

## 🤝 Contributing

While this is a personal project, contributions are welcome:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'Add amazing feature'`)
4. **Push to the branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

### Development Guidelines
- Follow Android development best practices
- Maintain MVVM architecture
- Add comprehensive error handling
- Include parameter validation
- Test with multiple model configurations

## 📄 License

This project is licensed under the MIT License. See the LICENSE file for details.

## 🔗 Essential Links

- **[MyLLMServer](https://github.com/yourusername/MyLLMServer)** - Required server component (set this up first!)
- **[Development Notes](https://docs.google.com/document/d/1qfkpCG09e5sAiYSWzC6XOS0YzZI-8z87H2Bz03ZL88U/edit)** - Development process and roadmap
- **[llama-cpp-python](https://github.com/abetlen/llama-cpp-python)** - Core LLM inference library used by server
- **[Material Design 3](https://m3.material.io/)** - Design system used for UI

## 🙏 Acknowledgments

- **llama-cpp-python** for the excellent LLM inference library
- **Material Design 3** for the beautiful design system
- **Android Jetpack Compose** for the modern UI framework
- **OkHttp** for reliable networking
- **Kotlin Coroutines** for structured concurrency

---

**Remember**: This app is useless without the [MyLLMServer](https://github.com/yourusername/MyLLMServer). Set up the server first, then come back here!

**Quick Start Summary:**
1. 🖥️ Set up [MyLLMServer](https://github.com/yourusername/MyLLMServer) on your PC
2. 📱 Install this Android app
3. ⚙️ Configure server connection in settings
4. 💾 Load a model with your preferred parameters
5. 🎛️ Tune inference parameters in the Parameters tab
6. 💬 Start chatting with your local AI!