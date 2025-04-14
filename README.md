![alt](https://raw.githubusercontent.com/khosbilegt/github-to-mermaid/refs/heads/main/opentopr.svg)
# Github to Mermaid
GitHub to Mermaid turns raw commit statistics from the GitHub API into clean, shareable diagrams.

Whether you're tracking your own contributions or analyzing activity across a team, this tool gives you an intuitive visual overview of repository history.

## 🚀 Why use it?
Numbers are cool, but visuals tell a story. See trends, highlight bursts of activity, or showcase your open source contributions — all at a glance.

## 🔧 Features
- Fetches commit history from GitHub repositories.
- Generates Mermaid.js timelines to visualize commits by date.
- Creates pie charts to represent data distributions.
- Outputs SVG files for easy embedding and sharing.

## 🎯 Roadmap
- Bunch of different statistics that can be extracted from Github API.
- Support for Private Repositories.
- Possibly adapters for different version control systems (Gitlab?).

Any more ideas for charts? Create an issue as a feature request and I'll try to build it!

## 🛠️ Technologies Used
- **Java**: Core programming language.
- **[Github API](https://docs.github.com/en/rest)**: For fetching commit data.
- **[Quarkus](https://quarkus.io/)**: Supersonic Subatomic Java Framework for building reactive applications.
- **[Mermaid.js](https://mermaid.js.org/)**: For generating visual diagrams and charts via CLI.
- **[Puppeteer](https://pptr.dev/)**: Used for rendering Mermaid.js diagrams into SVG files, used by Mermaid CLI.

## 🧔 User ID
To use the API, you need to install the [ghstats-to-mermaid](https://github.com/apps/ghstats-to-mermaid) Github App and get your unique user ID. 
This is necessary for the API to work properly. 

You are assigned 1 unique id every time you install the app. You may uninstall and install again to create another unique id, but previous diagrams will expire.

By using a Github App, it solves the rate limiting issue of other similar projects. By registering an app and using your own unique id, you instead draw from your own rate limit rather than 1 global rate limit.

Strictly for testing purposes, you may use my own User ID used in the examples below (please don't).

## 🖼️ Diagram Examples
### 📆 Timeline View:
```
https://khosbilegt.dev/mermaid/api/[USER_ID]/commit/timeline/[USER]/[REPOSITORY]?count=[COMMIT_COUNT]
```
Example: https://khosbilegt.dev/mermaid/api/608b731d8db64d4c94c040c0f3a12dd3/commit/timeline/khosbilegt/github-to-mermaid

![alt](https://khosbilegt.dev/mermaid/api/608b731d8db64d4c94c040c0f3a12dd3/commit/timeline/khosbilegt/github-to-mermaid)
### 🥧 Pie Chart View:
```
https://khosbilegt.dev/mermaid/api/[USER_ID]/commit/pie/[USER]/[REPOSITORY]?count=[COMMIT_COUNT]
```
Example: https://khosbilegt.dev/mermaid/api/608b731d8db64d4c94c040c0f3a12dd3/commit/pie/khosbilegt/github-to-mermaid

![alt](https://khosbilegt.dev/mermaid/api/608b731d8db64d4c94c040c0f3a12dd3/commit/pie/khosbilegt/github-to-mermaid)

