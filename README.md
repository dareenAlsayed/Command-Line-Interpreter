# 🖥️ Java Command Line Interpreter

> A custom terminal emulator built in Java that replicates core Unix/Linux shell commands with file system manipulation, path handling, compression utilities, and output redirection.
This project simulates a lightweight Command Line Interface (CLI) capable of executing common terminal operations while interacting directly with the operating system’s file structure.

<br>

## ✨ Supported Commands

| Command | Description |
|---|---|
| `pwd` | Display current working directory |
| `cd` | Navigate between directories |
| `ls` | List directory contents |
| `mkdir` | Create directories |
| `rmdir` | Remove empty directories |
| `touch` | Create files |
| `rm` | Delete files |
| `cp` | Copy files |
| `cp -r` | Copy directories recursively |
| `cat` | Display file contents |
| `wc` | Count lines, words, and characters |
| `echo` | Print text output |
| `>` | Overwrite output redirection |
| `>>` | Append output redirection |
| `zip` | Compress files and directories |
| `unzip` | Extract zip archives |
| `exit` | Terminate the terminal |

<br>

## ⚙️ Features
- Real file system interaction
- Relative and absolute path support
- Recursive directory operations
- Output redirection handling
- ZIP compression and extraction
- Custom command parser
- Error handling and validation
- Interactive terminal loop

<br>

## 🧠 Parser Design
The terminal uses a dedicated `Parser` class responsible for:
- Separating commands from arguments
- Detecting output redirection operators
- Parsing overwrite (`>`) and append (`>>`) operations
- Preparing command execution data for the terminal engine

<br>

## 📂 Example Session

```bash
> mkdir projects
Directory created: projects

> cd projects

> touch notes.txt
File created: notes.txt

> echo Operating Systems CLI > notes.txt

> cat notes.txt
Operating Systems CLI

> wc notes.txt
1  3  23  notes.txt

> zip backup.zip notes.txt
zip created: backup.zip
```

<br>

## 🛠️ Technologies
```text
Java
File I/O
Path Manipulation
ZIP Streams
Object-Oriented Programming
Command Parsing
```
