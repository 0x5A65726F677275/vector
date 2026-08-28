# Art of Vector

**Art of Vector** is an open-source **Java desktop workbench** for **Linux debugging**, **source editing**, and **visual command workflows**. It combines a code editor, a **ptrace debugger** (with disassembly, registers, hex dump, and breakpoints), and an n8n-style **workflow canvas** that runs shell commands such as `nmap` from a chosen working folder.

Use it as a lightweight **GUI debugger for Linux**, a **Kali Linux reverse-engineering helper**, or a visual pipeline for pentest and binary-analysis commands.

- **Language:** Java 17+
- **UI:** Swing desktop app
- **Real attach:** Linux `ptrace` (JNA)
- **Elsewhere:** simulated debug session (Windows / macOS)
- **Build:** Gradle Wrapper 9.1.0

---

## What is Art of Vector?

Art of Vector (package `com.artofvector`) is a three-pane workbench:

| Module | What it does |
| --- | --- |
| **Editor** | Syntax-highlighted code editor (Java, C/C++, Python, ASM, JSON) with file tree, tabs, and save |
| **Debugger** | Attach to a PID, continue / pause / step, INT3 breakpoints, Capstone or fallback disassembly, hex + ASCII memory, registers, stack |
| **Workflow** | Drag **Node**, use `$ip` for the target, chain stdout with `{in}`, run from the workspace folder |

On **Linux**, the debugger uses `PTRACE_ATTACH`, `PEEKTEXT` / `POKETEXT`, `GETREGS` / `SETREGS`, single-step, and continue. On other operating systems it falls back to a **simulated debug session** so you can still explore the UI.

It is **not** a replacement for GDB/LLDB feature-for-feature. It is a compact Java GUI around ptrace plus an editor and a command workflow canvas.

---

## Who is this for?

- People learning **Linux ptrace**, **x86-64 registers**, and **INT3 breakpoints**
- **Kali Linux** / Debian users who want a GUI next to `nmap`, `gdb`, and a hex view
- Reverse-engineering and malware-lab workflows that mix **source**, **disassembly**, and **shell commands**
- Anyone who wants a **Java Swing IDE-style debugger** they can build from source with Gradle

Search-friendly topics this project covers: Java ptrace debugger, Linux process attach GUI, Capstone disassembly Java, Kali nmap workflow canvas, visual pentest pipeline, hex dump debugger, breakpoint gutter editor.

---

## Features

### Code editor
- Multi-tab editor with RSyntaxTextArea (dark theme)
- File tree with type icons (folder, Java, Python, C, ASM, JSON, binary)
- **Open Folder** sets the workspace root (also used as the command working directory)
- Font size: **View → Increase / Decrease / Reset**, `Ctrl` `+` / `-` / `0`, or **Ctrl + mouse wheel**
- Last folder and font size are remembered

### Terminal
- Bottom **Terminal** tab runs a shell in the Open Folder directory (bash on Linux, cmd on Windows)
- `Ctrl` `` ` `` focuses the terminal. Kill / Restart are on the header.

### Linux debugger
- Attach by **PID**, or leave empty for the **simulated target**
- Run, pause, step into, step over, stop
- Disassembly table, hex dump, stack, registers, breakpoint list
- Optional **libcapstone** for full disassembly; otherwise a simple fallback decoder

### Workflow canvas
- One node type: **Node**
- Double-click a node to write the shell line (`nmap -sn $ip`, `pwd`, `ls`, …)
- Toolbar **$ip** replaces `$ip` / `{ip}` in every node at once
- Toggle **On / Off** on a node to skip it at run time without deleting it
- Pipe previous stdout with `{in}`, `{out}`, or `{stdout}`
- Commands run in the **Open Folder** working directory
- Save / load graphs as JSON

---

## Requirements

| Requirement | Notes |
| --- | --- |
| **JDK 17 or newer** | Java 21 and Java 25 are fine. Gradle 9.1.0 runs on Java 17–25. |
| **A display** | Swing GUI. Use a desktop session or `ssh -X`. |
| **Linux (optional, for real attach)** | `ptrace` backend. Same user as the target process, or root. |
| **libcapstone (optional)** | Better disassembly. Without it, a fallback decoder is used. |

**Java 25 + old Gradle:** class file major version 69 means the JDK is 25 and Gradle is too old. This repo pins **Gradle 9.1.0**, which supports Java 25.

Kali Rolling may **not** ship `openjdk-17-jdk`. Install `openjdk-21-jdk` or `openjdk-25-jdk` instead. Do not set `JAVA_HOME` to a path that does not exist.

---

## Install and run (Linux / Kali)

```bash
git clone https://github.com/YOUR_USER/vector.git
cd vector
chmod +x gradlew
```

### Install a JDK

```bash
# See what your distro actually has
java -version
apt-cache search openjdk | grep jdk   # Debian / Ubuntu / Kali

# Debian / Ubuntu (if Java 17 exists)
sudo apt update
sudo apt install openjdk-17-jdk libcapstone-dev

# Kali Linux Rolling / newer Debian (typical)
sudo apt update
sudo apt install openjdk-21-jdk
# or: sudo apt install openjdk-25-jdk
sudo apt install libcapstone-dev     # optional
```

If `JAVA_HOME` was set to a missing directory:

```bash
unset JAVA_HOME
java -version
ls /usr/lib/jvm
```

Point it at a real JDK only if needed:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # adjust to ls output
```

### Start the app

```bash
./gradlew --stop    # if a previous Gradle daemon failed
./gradlew run
```

Install a runnable copy:

```bash
./gradlew installDist
./build/install/art-of-vector/bin/art-of-vector
```

Tests:

```bash
./gradlew test
```

---

## Install and run (Windows / macOS)

You need **JDK 17+** on `PATH` or `JAVA_HOME`.

```bat
git clone https://github.com/YOUR_USER/vector.git
cd vector
gradlew.bat run
```

On Windows and macOS the debugger uses the **simulated session**. The editor, file tree, and Node workflow still work. Real process attach is **Linux-only**.

---

## First-time usage

1. **File → Open Folder…** (`Ctrl+K`) or the **Open Folder** button — this is the workspace and the cwd for workflow commands.
2. Open a file from the tree (double-click) or **File → Open File…**.
3. Debugger: **Attach**. Empty / `0` = simulated target. A real PID = `ptrace` on Linux.
4. Workflow: drag **Node**, set toolbar **$ip**, keep `$ip` in the shell line, click **Run**. Off nodes are skipped.
5. Chain nodes: connect output → input and use `{in}` in the next command.

### ptrace attach on Linux (Yama)

If attach fails, check:

```bash
cat /proc/sys/kernel/yama/ptrace_scope
```

`1` is common (only dumpable children). For a lab VM you can temporarily allow more:

```bash
echo 0 | sudo tee /proc/sys/kernel/yama/ptrace_scope
```

Attach as the **same user** as the target, or as root. This is a debugger: only attach to processes you are allowed to debug.

---

## Project layout

```
src/main/java/com/artofvector/
  ArtOfVectorApp.java          # entry point
  editor/                      # code editor, file tree
  debugger/                    # ptrace + simulated session, disasm, UI
  workflow/                    # Node canvas, engine
  ui/                          # main window, theme, icons
  workspace/                   # folder + font preferences
```

- **Main class:** `com.artofvector.ArtOfVectorApp`
- **Gradle application name:** `art-of-vector`
- **Version:** 0.1.0

---

## Tech stack

- Java 17 bytecode, Gradle 9.1.0 wrapper
- Swing + RSyntaxTextArea
- JNA for `ptrace` / libc
- Optional native **Capstone** via JNA
- Jackson for workflow JSON
- JUnit 5 tests

---

## FAQ

**Does Art of Vector work on Kali Linux?**  
Yes. Install a current OpenJDK (`openjdk-21-jdk` or `openjdk-25-jdk`), then `./gradlew run`. Use a graphical session.

**Can I debug on Windows?**  
You can run the app and the simulated debugger. Live `ptrace` attach is Linux-only.

**How do I run nmap from the GUI?**  
Open a folder, add a **Node**, set the shell line to your nmap command, Run. The process cwd is that folder.

**Unsupported class file major version 69?**  
You are on **Java 25** with an old Gradle. This repository already uses Gradle **9.1.0**. Pull the latest wrapper, run `./gradlew --stop`, then `./gradlew run`.

**Why is the file tree empty?**  
Use **Open Folder**. The tree follows the workspace root; workflow commands use the same folder.

---

## License and use

This is a local workbench for **your own** binaries and processes. Do not attach to software or systems you do not have permission to debug.

---

## 한국어 요약

**Art of Vector**는 자바 스윙 데스크톱 앱입니다. 코드 에디터, 리눅스 **ptrace 디버거**(디스어셈블, 레지스터, 헥스, 브레이크포인트), 셸 명령을 연결하는 **Node 워크플로**를 한 창에서 씁니다.

```bash
git clone https://github.com/YOUR_USER/vector.git
cd vector
chmod +x gradlew
sudo apt install openjdk-21-jdk    # Kali에는 openjdk-17-jdk가 없을 수 있음
./gradlew run
```

- 폴더 열기: 파일 트리 + 명령 작업 디렉터리
- 아래 **Terminal** 탭에서 같은 폴더의 셸. `Ctrl`+`` ` ``
- 글자 크기: View 메뉴 / `Ctrl` `+` `-` `0` / Ctrl+휠
- 실제 PID attach는 Linux만. Windows는 시뮬레이터
- 워크플로 노드는 **Node** 하나. `$ip`는 툴바에서 일괄 변경. Off면 실행만 건너뜀
- Java 25면 Gradle 9.1.0 필요 (이 저장소에 포함)
