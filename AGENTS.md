# Agents skill triage

Agents using this repository should consult the skill documents stored locally in `./skills/<skill-id>/SKILL.md`.

Before producing work, the agent should fully understand the referenced skill document relevant to the task and always prioritise direct user instruction when a prompt specifies a particular skill.

## Android delivery

- **`android-product-shaping`** – This skill is used to turn Android app ideas into small, well-bounded product slices with clear value, ready for UX and implementation.
- **`android-ux-flows`** – This skill is used to design Android user flows and screen structures that match the existing app patterns and keep forms, lists and navigation clear.
- **`android-dev-standards`** – Standards, architecture patterns, and best practices for Android app development with Kotlin, Jetpack Compose, and Android Jetpack libraries using clean architecture and MVVM. Use for any Android coding, review, refactor, or design task, especially when acting as an AI coding agent that must follow established project conventions.
- **`android-engineering-core`** – This skill is used to implement Android features within the existing Kotlin, Compose, Room, Hilt and Navigation architecture, including data, navigation and background work.
- **`android-ui-compose`** – This skill is used to implement Android UI in Jetpack Compose based on an existing UX flow, focusing on clear hierarchy, list vs form separation and discoverable navigation.
- **`android-qa-verification`** – This skill is used to verify Android features against acceptance criteria, catch regressions and define tests that reflect real device behaviour.

## Design & UX

- **`brand-guidelines`** – Applies Anthropic's official brand colors and typography to any sort of artifact that may benefit from having Anthropic's look-and-feel. Use it when brand colors or style guidelines, visual formatting, or company design standards apply.
- **`frontend-design`** – Create distinctive, production-grade frontend interfaces with high design quality. Use this skill when the user asks to build web components, pages, or applications. Generates creative, polished code that avoids generic AI aesthetics.

## Writing & comms

- **`internal-comms`** – A set of resources to help me write all kinds of internal communications, using the formats that my company likes to use. Claude should use this skill whenever asked to write some sort of internal communications (status reports, leadership updates, 3P updates, company newsletters, FAQs, incident reports, project updates, etc.).

## Skills meta

- **`mcp-builder`** – Guide for creating high-quality MCP (Model Context Protocol) servers that enable LLMs to interact with external services through well-designed tools. Use when building MCP servers to integrate external APIs or services, whether in Python (FastMCP) or Node/TypeScript (MCP SDK).
- **`skill-creator`** – Guide for creating effective skills. This skill should be used when users want to create a new skill (or update an existing skill) that extends Claude's capabilities with specialized knowledge, workflows, or tool integrations.
- **`template-skill`** – Replace with description of the skill and when Claude should use it.

## Project Status

**Current Phase:** Phase 6 (Polish & Release) - **COMPLETED**

### Completed
*   **Project Setup**: Gradle, Basic Structure, Hilt, Room.
*   **Local Data Layer**: Room DB, Entities, DAOs.
*   **Network Layer**: ClickUp API Integration.
*   **Domain Layer**: Clean Architecture, Use Cases.
*   **Presentation Layer**: Home, Details, Settings, Kanban Screens, Insights Screen.
*   **Core Features**:
    *   **Background Sync**: WorkManager.
    *   **NLP Engine**: Date, Priority, and Recurrence parsing.
    *   **Notifications**: AlarmManager integration for due dates.
    *   **Calendar Integration**: 2-way sync with Android Calendar.
    *   **Kanban Board**: Drag-and-drop workflow visualization.
    *   **Recurring Tasks**: Logic for repeating tasks upon completion.
    *   **List Management**: List selection UI and multi-list support.
    *   **Dynamic Theming**: Custom color palettes and theme modes.
    *   **AI Suggestions**: Suggest tasks based on history.
    *   **Insights**: Task statistics and charts.
    *   **Webhooks**: Android-side FCM integration.
    *   **Search & Filtering**: Task search by title/description with Status and Priority filtering.
*   **Phase 6 (Polish & Release)**:
    *   Code quality improvements (warnings resolution).
    *   UI modernization (deprecated API replacements).
    *   Robust error handling (Calendar logging).
    *   Full feature verification.

### Pending / Next Steps
*   **Release Candidate Preparation**: Prepare signed build.
*   **Store Listing**: Prepare assets and descriptions.
