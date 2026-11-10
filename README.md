# SnapTool 

**SnapTool** is a lightweight, high-performance, Windows-style screenshot and annotation utility built entirely from scratch using standard Java JDK (Swing & AWT). No external libraries, no Maven/Gradle setup, and zero bloat—just compile and run!

---

##  Features

### Phase 1 — Core Snipping Engine
* **Virtual Screen Multi-Monitor Support:** Automatically calculates full virtual screen bounds across multiple displays.
* **HiDPI & Scaling Safe:** Accurate mapping between physical Robot captures and screen coordinates.
* **Snipping Overlay:** Fullscreen semi-transparent dim overlay with drag-to-select rectangles and red highlight borders.
* **Quick Escapes:** Press `Escape` at any time during selection to cancel.

### Phase 2 — Advanced Annotation Editor
* **Multi-Tool Suite:** Draw directly on your captures using **Pen**, **Arrows**, **Rectangles**, or **Highlights**.
* **Stack Management:** Full **Undo** and **Clear All** capabilities for your drawings.
* **Flattened Export:** Seamlessly merges annotations onto your cropped image before copying or saving.

### Phase 3 — Desktop Utility & Tray Integration
* **System Tray Mode:** Minimizes quietly to the Windows system tray with a right-click quick menu (`New Snip`, `Open`, `Exit`).
* **Session History Sidebar:** Keeps track of your recent captures in a side drawer—click any history item to instantly reload it into the editor.
* **Configurable Storage:** Auto-saves every snip to a dedicated pictures directory with fully customizable save locations via the Settings dialog.

---

## Getting Started

### Prerequisites
* **Java Development Kit (JDK 11 or higher)** installed on your machine.

### Compilation & Execution
Open your terminal (PowerShell, Command Prompt, or bash) in the directory containing `SnapTool.java`, and run:

```bash
# Compile the source file
javac SnapTool.java

# Run the application
java SnapTool
