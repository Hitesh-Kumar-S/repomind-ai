# 🚀 RepoMind AI

RepoMind AI is a **Spring Boot–based AI application** that analyzes repository `README.md` files and generates **structured, interview-friendly project insights**.

It helps students and developers quickly:

* understand unfamiliar repositories
* revise projects before interviews
* explain architecture and tech stack confidently
* interact with projects using an AI-powered chatbot

---

# ✨ Features

## 🔍 Intelligent README Analysis

* README-based project understanding
* Structured AI-generated explanations
* No unsupported assumptions
* Honest handling of missing information

## 🌐 Multi-Platform Repository Support

Supports:

* ✅ GitHub
* ✅ GitLab
* ✅ Bitbucket

## 🤖 Context-Aware AI Chatbot

* Ask follow-up questions about analyzed repositories
* Context reuse from README and project structure
* Strict anti-hallucination handling
* Smart Mode + Accurate Mode

## ⚠️ Smart Validation

* Invalid repository URL detection
* Missing README detection
* Weak documentation detection
* Branch fallback handling (`main`, `master`, `develop`)

## 📊 Structured Analysis Output

Generates:

* Project Overview
* Key Features
* Tech Stack
* Architecture / Design Approach
* Interview Explanation
* README Quality Score
* Missing Documentation Sections
* Suggested Features & Enhancements

## 🎨 User Experience

* Markdown rendering
* Dark mode 🌙
* Copy-to-clipboard 📋
* Loading indicators ⏳
* Responsive UI
* Interactive chatbot

---

# 🧠 Motivation

Students and developers often forget:

* architecture decisions
* implementation details
* project features
* tech stack reasoning

especially before:

* interviews
* viva sessions
* project demonstrations

Manually revisiting the codebase is time-consuming.

RepoMind AI solves this by transforming repository documentation into:

* concise explanations
* interview-ready summaries
* AI-assisted project understanding

---

# 🛠️ Tech Stack

## Backend

* **Java** → Core programming language
* **Spring Boot** → Backend framework for REST APIs
* **REST APIs** → Communication layer
* **Maven** → Dependency and build management

## Frontend

* **HTML** → Structure
* **CSS** → Styling
* **JavaScript** → Interactivity
* **Marked.js** → Markdown rendering

## AI & Integrations

* **Groq API** → LLM inference platform
* **LLaMA 3.1 (8B)** → AI model for analysis
* **GitHub REST API** → Fetch repository data
* **GitLab API** → Fetch repository data
* **Bitbucket API** → Fetch repository data

---

# 🏗️ Architecture

## 🔹 Workflow

Repository URL
↓
Platform Detection
↓
GitHub / GitLab / Bitbucket Service
↓
README + Key File Extraction
↓
ContextService
↓
LLMService (LLaMA 3.1 via Groq)
↓
Structured Analysis / Chat Response

---

## 🔹 Design Decisions

* Modular service-based architecture
* Repository abstraction using `RepositoryService`
* Context reuse for chatbot continuity
* Platform-independent repository handling
* Separation of concerns
* Scalable and extensible design

---

# ⚙️ Installation & Setup

## 🔹 Prerequisites

* Java 17+
* Maven
* Git

---

## 🔹 Clone Repository

```bash
git clone https://github.com/Hitesh-Kumar-S/repomind-ai.git
cd repomind-ai
```

---

## 🔹 Configure Environment Variables

### Windows (PowerShell)

```powershell
setx GROQ_API_KEY "your_api_key"
setx OPENROUTER_API_KEY "your_openrouter_api_key"
setx GITHUB_TOKEN "your_github_pat"
```

### Linux / macOS

```bash
export GROQ_API_KEY="your_api_key"
export OPENROUTER_API_KEY="your_openrouter_api_key"
export GITHUB_TOKEN="your_github_pat"
```

---

## 🔹 Configure Application

Inside `application.properties`:

```properties
groq.api.key=${GROQ_API_KEY}
openrouter.api.key=${OPENROUTER_API_KEY}
github.token=${GITHUB_TOKEN}
```

---

## 🔹 Build Project

```bash
mvn clean install
```

---

## 🔹 Run Application

```bash
mvn spring-boot:run
```

---

## 🔹 Access Application

```text
http://localhost:8080
```

---

# 🐳 Docker Support

## 🔹 Build Docker Image

```bash
docker build -t repomind-ai .
```

---

## 🔹 Run Docker Container

```bash
docker run -d -p 8080:8080 \
-e GROQ_API_KEY=your_api_key \
--name repomind-container \
repomind-ai
```

---

# ☁️ AWS Deployment

RepoMind AI has been successfully tested on:

* ✅ AWS EC2
* ✅ Dockerized environment

Deployment includes:

* EC2 instance setup
* Docker containerization
* Environment variable configuration
* Public access via port `8080`

---

# 📌 Usage

## 🔹 Analyze Repository

Example:

```text
https://github.com/spring-projects/spring-boot
```

Generated Output:

* Project Overview
* Features
* Tech Stack
* Architecture
* Interview Explanation
* README Quality Score

---

## 🔹 Ask Questions

Example chatbot questions:

```text
What is the architecture?
Why is Spring Boot used?
What improvements can be made?
Explain the tech stack.
```

---

# 📸 Screenshots

## 🔹 Home Page

![Home Page](<Screenshot 2026-05-23 225306.png>)

## 🔹 Dark Mode UI

![Dark Mode UI](<Screenshot 2026-05-24 012450.png>)

## 🔹 AI Analysis Output

![AI Analysis Output](<Screenshot 2026-05-24 012707.png>)

![AI Analysis Output](<Screenshot 2026-05-23 225453.png>)

 ![AI Analysis Output](<Screenshot 2026-05-23 225823.png>)


---

# ⚠️ Important Notes

* Only public repositories are supported
* Better README quality → better AI analysis
* README-driven analysis reduces hallucination risk
* Large repositories may take longer to analyze
* GitHub API authentication is recommended to avoid rate limiting

---

# 🔮 Future Enhancements

* Private repository authentication
* Repository caching
* Advanced README scoring
* Code-level analysis
* RAG-based repository understanding
* Exportable PDF analysis reports
* Architecture visualization

---

# 🔐 Security

* API keys are NOT stored in code
* Uses environment variables
* Prevents secret exposure
* No sensitive repository storage

---

# 📄 License

This project is intended for:

* Learning
* Demonstration
* Portfolio use

---

# 🙌 Final Thoughts

RepoMind AI focuses on:

* clean architecture
* responsible AI usage
* developer-friendly UX
* interview-focused project understanding

The goal is to help students and developers confidently understand and present software projects without revisiting the entire codebase repeatedly.
