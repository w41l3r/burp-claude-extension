# Burp Claude AI Analyzer

A Burp Suite extension that integrates Claude AI (Anthropic) directly into your pentesting workflow. Analyze HTTP requests and responses for security vulnerabilities with a single right-click.

## Features

- **Context menu integration** — right-click any request/response in Proxy, Repeater, or Scanner and send it directly to Claude for analysis
- **Dedicated "Claude AI" tab** — dedicated panel inside Burp with request input, analysis output, and configuration
- **Customizable system prompt** — swap the default pentest prompt for any analysis style (recon, code review, IDOR hunting, etc.)
- **Persistent configuration** — API key and model selection are saved between Burp sessions via Burp's native storage
- **Model selector** — choose between Sonnet, Opus, and Haiku depending on speed/depth needs
- **Input safeguard** — automatically truncates payloads larger than 50 KB before sending to the API

## Default Analysis Coverage

The default system prompt instructs Claude to look for:

- SQL Injection, XSS, SSRF, IDOR
- Command Injection, Path Traversal
- Authentication and Authorization flaws
- Information Disclosure
- Insecure HTTP headers
- Business Logic vulnerabilities
- OWASP Top 10 in general

Each finding is reported with: **severity**, **location**, **technical description**, **PoC/exploitation notes**, and **remediation**.

## Requirements

- Burp Suite Pro or Community Edition (2023.x+)
- Java 17+ (JDK for building, JRE for running inside Burp)
- An [Anthropic API key](https://console.anthropic.com/api-keys)

## Building from Source

```bash
# Install dependencies (Debian/Ubuntu/Kali)
sudo apt-get install -y openjdk-21-jdk
sudo snap install gradle --classic

# Clone and build
git clone https://github.com/YOUR_USERNAME/burp-claude-extension.git
cd burp-claude-extension
gradle shadowJar
```

The compiled JAR will be at:
```
build/libs/burp-claude-extension-1.0.0.jar
```

Or just run the helper script:
```bash
bash setup-and-build.sh
```

## Installation in Burp Suite

1. Open Burp Suite
2. Go to **Extensions** → **Installed** → **Add**
3. Set **Extension type** to `Java`
4. Select the JAR file: `build/libs/burp-claude-extension-1.0.0.jar`
5. Click **Next** — the **Claude AI** tab will appear in the Burp toolbar

## Usage

### Via context menu
1. In Proxy, Repeater, or any HTTP message editor — right-click the request/response
2. Click **"Analyze with Claude AI"**
3. The request is sent to the **Claude AI** tab automatically
4. Click **"Analyze with Claude"** to get the analysis

### Via the Claude AI tab
1. Paste any raw HTTP request/response into the left panel
2. Optionally customize the system prompt
3. Click **"Analyze with Claude"**

### Configuration
- Enter your API key (`sk-ant-api03-...`) in the **API Key** field
- Select your preferred model
- Click **Save** to persist the settings

## Models

| Model | Best for |
|---|---|
| `claude-sonnet-4-6` | Balanced — recommended for most findings |
| `claude-opus-4-7` | Deep analysis, complex business logic |
| `claude-haiku-4-5` | Fast triage, high-volume scanning |

## Project Structure

```
burp-claude-extension/
├── build.gradle
├── settings.gradle
├── setup-and-build.sh
└── src/main/java/com/claudeburp/
    ├── ClaudeExtension.java    # Burp entry point (BurpExtension)
    ├── ClaudePanel.java        # Swing UI — "Claude AI" tab
    ├── ClaudeApiClient.java    # Anthropic API HTTP client
    └── ClaudeContextMenu.java  # Right-click context menu handler
```

## License

MIT — see [LICENSE](LICENSE).
