# AI-Powered Study Platform

An intelligent, accessible learning management system that transforms study materials into personalized learning experiences using AI. The platform enables students to upload course documents, automatically generate summaries, flashcards, and practice quizzes, while providing adaptive study modes tailored to different learning styles and accessibility needs.

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Development](#development)

## Features

### Core Functionality

- **Smart Document Processing**: Upload and process various document formats (PDF, DOCX, PPT, PPTX, TXT) with automatic text extraction using Apache Tika
- **AI-Powered Summarization**: Generate intelligent summaries of uploaded materials with key concepts highlighted using OpenAI GPT-3.5
- **Automated Flashcard Generation**: Create personalized flashcards for active recall practice with customizable difficulty levels
- **Adaptive Quiz Creation**: Generate practice quizzes with multiple question types, difficulty levels, scoring, and time limits
- **RAG-Based AI Q&A**: Ask contextual questions about study materials with AI-powered explanations using your own documents as reference
- **Progress Tracking**: Monitor study sessions, quiz scores, and learning progress with detailed analytics and best score tracking

### Accessibility & Adaptive Learning Modes

- **Standard Mode**: Default study experience with traditional layouts
- **Dyslexia-Friendly Mode**: OpenDyslexic font, increased spacing, text-to-speech support
- **ADHD Focus Mode**: Minimal distractions, Pomodoro timer, single-card view, chunked content
- **Visual Learner Mode**: Enhanced visuals, color-coded content, icons, diagrams, and visual progress indicators
- **Auditory Learner Mode**: Text-to-speech, audio cues, spoken explanations, customizable voice selection

### User Experience

- **Light/Dark Theme Support**: System-wide theme switching with persistent preferences
- **Responsive Design**: Mobile-first, fully responsive interface
- **Real-time Updates**: Live document processing status and instant feedback
- **Confetti Animations**: Celebratory feedback for perfect quiz scores (100%)

## Technology Stack

### Frontend
- **Framework**: Next.js 15.5.4 (React 19.1.0)
- **Language**: TypeScript 5
- **Styling**: Tailwind CSS 4 with custom theme system
- **State Management**: React Context API with localStorage persistence
- **Theme Management**: next-themes 0.4.6
- **UI Libraries**: 
  - canvas-confetti 1.9.4 (celebration animations)
  - @fontsource/opendyslexic 5.2.5 (accessibility fonts)

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL (production), H2 (testing)
- **Security**: Spring Security with JWT authentication
- **Document Processing**: Apache Tika 2.9.1 (text extraction from various formats)
- **AI Integration**: OpenAI GPT-3.5 API (via openai-gpt3-java 0.18.2)
- **Build Tool**: Gradle
- **Additional Libraries**: Spring Data JPA, Spring Boot Actuator, OAuth2 Client, JWT

### APIs & Services
- **Web Speech API**: Browser-native text-to-speech for accessibility features
- **Web Audio API**: Custom audio cues for auditory learner mode
- **OpenAI API**: Document summarization, flashcard generation, quiz creation, and Q&A responses

## Prerequisites

Before you begin, ensure you have the following installed:

- **Node.js** (v18 or higher) and **npm**
- **Java JDK** 17 or higher
- **PostgreSQL** 12 or higher
- **Gradle** 7.0 or higher (or use the included Gradle wrapper)
- **OpenAI API Key** (for AI features)

## Getting Started

### Backend Setup

1. **Clone the repository** (if not already done):
   ```bash
   cd 656-project-be
   ```

2. **Set up PostgreSQL database**:
   ```bash
   # Create a new database
   createdb springbootdb
   
   # Or using psql:
   psql -U postgres
   CREATE DATABASE springbootdb;
   ```

3. **Configure environment variables**:
   
   Create a `.env` file in the backend root directory or set the following environment variables:
   
   ```bash
   # Database Configuration
   PGHOST=localhost
   PGPORT=5432
   PGDATABASE=springbootdb
   PGUSER=postgres
   PGPASSWORD=your_password
   
   # JWT Configuration
   JWT_SECRET=your-secret-key-minimum-32-characters-long
   JWT_EXPIRATION=86400000
   
   # OpenAI Configuration (Required for AI features)
   OPENAI_API_KEY=your-openai-api-key
   OPENAI_MODEL=gpt-3.5-turbo
   OPENAI_MAX_TOKENS=1000
   
   # Document Storage (Optional - defaults to ./uploads)
   DOCUMENT_STORAGE_PATH=./uploads
   ```

4. **Build and run the backend**:
   ```bash
   # Using Gradle wrapper (recommended)
   ./gradlew build
   ./gradlew bootRun
   
   # Or using installed Gradle
   gradle build
   gradle bootRun
   ```

   The backend will start on `http://localhost:8080`

5. **Verify backend is running**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### Frontend Setup

1. **Navigate to the frontend directory**:
   ```bash
   cd 656_project_fe
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Configure API endpoint** (if backend is not on localhost:8080):
   
   Update `lib/api.ts` to point to your backend URL:
   ```typescript
   const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
   ```

   Or create a `.env.local` file:
   ```bash
   NEXT_PUBLIC_API_URL=http://localhost:8080
   ```

4. **Run the development server**:
   ```bash
   npm run dev
   ```

   The frontend will start on `http://localhost:3000`

5. **Open your browser**:
   Navigate to `http://localhost:3000` to see the application

## Configuration

### Backend Configuration

The backend uses `application.properties` for configuration. Key settings:

- **Server Port**: Defaults to 8080 (configurable via `PORT` environment variable)
- **Database**: PostgreSQL connection settings via environment variables
- **JWT**: Secret key and expiration time
- **OpenAI**: API key and model configuration
- **File Upload**: Max file size (10MB), allowed extensions (pdf, doc, docx, txt, ppt, pptx)
- **CORS**: Configured for `http://localhost:3000` and production domains

### Frontend Configuration

- **API Base URL**: Configured in `lib/api.ts` or via `NEXT_PUBLIC_API_URL` environment variable
- **Theme**: System-wide theme management via `next-themes`
- **Study Modes**: Global state management via React Context

## Usage

### First Time Setup

1. **Create an account**:
   - Navigate to the signup page
   - Fill in your details (first name, last name, email, password)
   - Click "Sign Up"

2. **Login**:
   - Use your credentials to log in
   - You'll be redirected to the dashboard

### Using the Platform

1. **Upload Documents**:
   - Go to the "Materials" page
   - Click "Upload Document"
   - Select a file (PDF, DOCX, PPT, PPTX, or TXT)
   - Wait for processing to complete (status will update automatically)

2. **View AI Summary**:
   - Click on a document to view details
   - The AI summary will appear once processing is complete
   - Review key concepts and study recommendations

3. **Generate Flashcards**:
   - On the document page, click "Generate Flashcards" in Quick Actions
   - Wait for generation to complete
   - Click "Study Flashcards" to start studying

4. **Generate Quizzes**:
   - On the document page, click "Generate Quiz" in Quick Actions
   - Configure quiz settings (difficulty, time limit)
   - Wait for generation to complete
   - Click "Take Quiz" to start

5. **Study with Adaptive Modes**:
   - Go to Settings
   - Select your preferred study mode:
     - **Standard**: Traditional study experience
     - **Dyslexia-Friendly**: OpenDyslexic font, increased spacing
     - **ADHD Focus**: Pomodoro timer, single-card view
     - **Visual Learner**: Color-coded content, icons, visual indicators
     - **Auditory Learner**: Text-to-speech, audio cues
   - Settings persist across the entire application

6. **Ask AI Questions**:
   - On the document page, click "Ask AI Question"
   - Type your question about the document
   - Receive AI-powered explanations using your document as context

7. **Track Progress**:
   - View your dashboard for overview statistics
   - Check quiz scores and best attempts
   - Monitor study session progress

## Project Structure

### Frontend (`656_project_fe`)

```
app/
├── api/                    # Next.js API routes (proxies to backend)
│   └── documents/
├── components/             # Reusable React components
│   ├── Navigation.tsx     # Sidebar navigation
│   ├── AppShell.tsx       # Main app layout
│   ├── GlobalFooter.tsx   # Global footer with Pomodoro timer
│   └── PomodoroTimer.tsx  # Pomodoro timer component
├── contexts/               # React Context providers
│   └── StudyModeContext.tsx  # Global study mode state
├── dashboard/             # Dashboard page
├── documents/             # Document management pages
│   └── [id]/             # Individual document page
├── study/                 # Study/flashcard/quiz pages
├── settings/              # User settings page
├── login/                 # Login page
├── signup/                # Signup page
├── upload/                # Document upload page
├── materials/             # Materials/library page
├── layout.tsx             # Root layout with theme provider
└── globals.css            # Global styles and theme definitions

lib/
└── api.ts                 # API client and type definitions
```

### Backend (`656-project-be`)

```
src/main/java/com/example/springbootjava/
├── controller/            # REST controllers
│   ├── AuthController.java
│   ├── DocumentController.java
│   ├── FlashcardController.java
│   └── QuizController.java
├── service/              # Business logic layer
│   ├── AIService.java
│   ├── DocumentService.java
│   ├── FlashcardService.java
│   └── QuizService.java
├── repository/           # Data access layer (Spring Data JPA)
│   ├── DocumentRepository.java
│   ├── FlashcardRepository.java
│   └── QuizRepository.java
├── entity/              # JPA entities
│   ├── User.java
│   ├── Document.java
│   ├── Flashcard.java
│   └── Quiz.java
├── dto/                 # Data Transfer Objects
│   ├── UserDto.java
│   ├── QuizResponseDTO.java
│   └── QuizUpdateDTO.java
├── security/            # Security configuration
│   ├── JwtUtils.java
│   └── AuthTokenFilter.java
└── config/              # Application configuration
    ├── SecurityConfig.java
    ├── OpenAIConfig.java
    └── WebConfig.java

src/main/resources/
└── application.properties  # Application configuration
```

## Development

### Running in Development Mode

**Backend**:
```bash
cd 656-project-be
./gradlew bootRun
```

**Frontend**:
```bash
cd 656_project_fe
npm run dev
```

### Building for Production

**Backend**:
```bash
cd 656-project-be
./gradlew build
java -jar build/libs/springboot-java-0.0.1-SNAPSHOT.jar
```

**Frontend**:
```bash
cd 656_project_fe
npm run build
npm start
```

### Testing

**Backend Tests**:
```bash
cd 656-project-be
./gradlew test
```

**Frontend**: Manual testing recommended (no test suite configured)

### Code Style

- **Frontend**: TypeScript with ESLint (if configured)
- **Backend**: Java with standard Spring Boot conventions

### Environment Variables

Required for full functionality:

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key for AI features | (required) |
| `JWT_SECRET` | Secret key for JWT tokens | (required, min 32 chars) |
| `PGHOST` | PostgreSQL host | localhost |
| `PGPORT` | PostgreSQL port | 5432 |
| `PGDATABASE` | Database name | springbootdb |
| `PGUSER` | Database user | postgres |
| `PGPASSWORD` | Database password | (required) |

### Troubleshooting

**Backend won't start**:
- Check PostgreSQL is running: `pg_isready`
- Verify database credentials in environment variables
- Check port 8080 is not in use

**Frontend can't connect to backend**:
- Verify backend is running on `http://localhost:8080`
- Check CORS configuration in `application.properties`
- Verify `NEXT_PUBLIC_API_URL` is set correctly

**AI features not working**:
- Verify `OPENAI_API_KEY` is set correctly
- Check OpenAI API quota/credits
- Review backend logs for API errors

**File upload fails**:
- Check file size (max 10MB)
- Verify file extension is allowed (pdf, doc, docx, txt, ppt, pptx)
- Ensure `uploads/` directory exists and is writable

**Database connection errors**:
- Verify PostgreSQL is running
- Check database credentials
- Ensure database exists: `psql -U postgres -l`

## License

This project is part of a course assignment (656 Project).

## Contributing

This is a course project. For questions or issues, please contact the development team.

---

**Note**: This platform requires an OpenAI API key for full functionality. Without it, AI-powered features (summaries, flashcards, quizzes, Q&A) will not work.
