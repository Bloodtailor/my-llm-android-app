# Personal LLM Android App

A personal Android application that connects to my local LLM server, enabling private AI interactions directly from my mobile device. This project was built for my specific setup but will work for others with similar configurations.

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

### 🔧 **Smart Functionality**
- **Multiple Model Support**: Easy switching between different LLM models
- **Dynamic Model Management**: Load/unload models remotely to manage server memory
- **Custom Context Length**: Configure context window size for different use cases
- **Context Usage Monitoring**: Real-time token counting and usage visualization
- **Raw Prompt Mode**: Send prompts exactly as typed without auto-formatting

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
    - Optionally set a custom context length
    - Tap "Load Model"

3. **Start using**:
    - Type your prompt in the text field
    - Tap "Send" to get a response
    - Use the copy button to save responses

## 📱 How to Use

### Main Interface
- **Prompt Input**: Type your questions here
- **Context Usage Bar**: Shows token usage (appears when model is loaded)
- **Send Button**: Submits your prompt to the LLM
- **Response Area**: Scrollable display with copy functionality
- **Status Indicators**: Shows connection and model status

### Settings (⚙️ icon)
- **Server URL**: Must match the IP shown by your MyLLMServer
- **Auto-connect**: Tests connection when you save

### Model Settings (💾 icon)
- **Model Selection**: Choose from models configured on your server
- **Context Length**: Set custom context window (optional)
- **Load/Unload**: Manage server memory usage
- **Refresh Models**: Update list if you add models to the server

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
2. **Try smaller model** or reduced context length
3. **Unload current model first** if switching models

### Still Having Problems?

1. **Check server setup** - go back to [MyLLMServer](https://github.com/yourusername/MyLLMServer) and verify everything is working
2. **Restart both** - restart the Android app and the server
3. **Check logs** - look at server logs and Android Studio logcat for errors

## 🗺️ Future Improvements

Based on my [development roadmap](https://docs.google.com/document/d/1qfkpCG09e5sAiYSWzC6XOS0YzZI-8z87H2Bz03ZL88U/edit):

### Planned Features (in order of difficulty):
- [ ] **Text Box Expansion Controls**: Allow expanding prompt and response boxes for better visibility
- [ ] **Screen Rotation Text Persistence**: Prevent prompt text from being cleared when screen rotates
- [ ] **Model Parameter Prefix/Suffix Buttons**: Buttons to append model's formatting to prompt text
- [ ] **Full Model Parameter Editing**: Edit all model parameters (temperature, top_p, etc.) with validation
- [ ] **Saved Prompts with Persistence**: Save/load/edit/delete named prompts that persist across app restarts


## 📄 License

This project is licensed under the MIT License. See the LICENSE file for details.

## 🔗 Essential Links

- **[MyLLMServer](https://github.com/yourusername/MyLLMServer)** - Required server component (set this up first!)
- **[Development Notes](https://docs.google.com/document/d/1qfkpCG09e5sAiYSWzC6XOS0YzZI-8z87H2Bz03ZL88U/edit)** - My development process and roadmap

---

**Remember**: This app is useless without the [MyLLMServer](https://github.com/yourusername/MyLLMServer). Set up the server first, then come back here!


