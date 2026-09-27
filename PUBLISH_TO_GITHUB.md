# Publish MioTimer to GitHub

These instructions assume the project has been extracted to:

```text
C:\publicGithub\MioTimer
```

Target repository:

<https://github.com/fraschizzato/MioTimer>

## Prerequisites

Install Git and GitHub CLI (`gh`), then authenticate once:

```powershell
gh auth login
```

If Git identity has not already been configured on this PC:

```powershell
git config --global user.name "Francesco Raschi"
git config --global user.email "4145029+fraschizzato@users.noreply.github.com"
```

## Publish with the helper script

```powershell
cd C:\publicGithub\MioTimer
.\publish_github.ps1
```

The script initializes the local Git repository, creates the public GitHub repository, pushes `main`, and configures description and topics.

## Manual commands

Run the following one by one:

```powershell
cd C:\publicGithub\MioTimer
git init
git branch -M main
git add .
git status
git commit -m "Initial public release"
gh repo create fraschizzato/MioTimer --public --source=. --remote=origin --push
gh repo edit fraschizzato/MioTimer --description "Offline Android timer with random intervals, countdowns, reusable workout sequences and a one-tap home-screen widget."
gh repo edit fraschizzato/MioTimer --add-topic android --add-topic kotlin --add-topic jetpack-compose --add-topic timer --add-topic countdown --add-topic workout-timer --add-topic offline-first --add-topic android-widget
git remote -v
git status
gh repo view fraschizzato/MioTimer --web
```
