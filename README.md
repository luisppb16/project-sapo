# Project Sapo

**Project Sapo** is a smart vulnerability scanner for your project dependencies, integrated directly into IntelliJ IDEA. It leverages the [OSV.dev](https://osv.dev) database to provide real-time alerts and detailed remediation advice without leaving your workflow.

## 🚀 Key Features

*   **Automated Scanning**: Automatically detects vulnerabilities in Gradle and Maven projects.
*   **Real-time Alerts**: Get instant notifications about security risks in your dependencies.
*   **Detailed Reports**: View comprehensive vulnerability details, including summaries, severity levels, and fixed versions.
*   **Integrated Tool Window**: A dedicated "Project Sapo" tool window for managing and reviewing scan results.
*   **Direct References**: One-click access to official security advisories (CVE, GHSA, etc.).
*   **Visual Indicators**: Clear visual cues for safe vs. vulnerable packages.

## 🛠 Installation

1.  Open **Settings/Preferences** > **Plugins** > **Marketplace**.
2.  Search for "Project Sapo".
3.  Click **Install** and restart the IDE.

Alternatively, you can install it manually from disk if you have the `.jar` or `.zip` file.

## 📖 Usage

1.  Open your project in IntelliJ IDEA.
2.  Navigate to the **Project Sapo** tool window (usually located at the bottom sidebar).
3.  The plugin will scan your dependencies (Gradle or Maven).
4.  Review the list of dependencies. Vulnerable ones will be highlighted.
5.  Click on a specific dependency to see detailed security information and remediation steps.

## 📋 Requirements

*   IntelliJ IDEA (Platform)
*   Gradle Plugin
*   Maven Plugin

## 📝 Change Log

### Initial Release
*   First public release of Project Sapo.
*   Full support for parsing dependencies from both Gradle and Maven build systems.
*   Connects to the Open Source Vulnerabilities database for up-to-date security info.
*   Added a dedicated Tool Window with sorting, filtering, and detailed HTML reports.

## 👤 Author

**Luis Pepe**
*   Email: ironkrozz@gmail.com

## 📄 License

Copyright (c) 2026 Luis Paolo Pepe Barra (@LuisPPB16). All rights reserved.

---
*Built with ❤️ for secure coding.*
