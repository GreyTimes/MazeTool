<div align="center">
  <img src="./MazinLogo.jpg" alt="Logo" width="300" align="center "/>
</div>

# JavaFX Maze Generator & Solver

A modern, feature-rich desktop application built with **JavaFX** for generating, solving, and playing custom labyrinths. It features a sleek, cohesive interface with live theme switching, interactive gameplay, and multiple maze generation and pathfinding algorithms.

## Features

- **Expanded Generation Algorithms**: 
  - *Recursive Backtracker (DFS)* (creates long, winding corridors)
  - *Randomized Prim's Algorithm* & *Randomized Kruskal's Algorithm* (produces highly branched, short corridors)
  - *Aldous-Broder* & *Wilson's Algorithm* (generates uniform spanning trees using random walks)

- **Advanced Pathfinding Solvers**: 
  - *Breadth-First Search (BFS)* (guaranteed absolute shortest path)
  - *Depth-First Search (DFS)*
  - *Dijkstra's Algorithm* (computes weighted shortest paths)

- **Interactive Playable Mode & Fog of War**: Test your puzzle-solving skills by navigating the maze using keyboard controls (`WASD`)[cite: 1]. Enable **Fog of War** to limit visibility based on a customizable player radius, revealing the maze dynamically as you explore[cite: 1].

- **Dynamic Theme Switcher**: Seamlessly toggle between a sleek modern **Dark Mode** and a clean **Light Mode** (adapting both the UI controls and the maze board canvas).

- **Customizable Configuration**: Live-updating sliders for uniform grid size, slow-motion animation delay speed, complexity/dead-end density, and fog visibility radius[cite: 1].

- **UX Polish & Onboarding Tooltips**: Interactive **`ℹ`** info icons and rich descriptive tooltips across all control groups and algorithms.

- **Real-time Statistics**: Track metrics like total cell count, dead ends, and solution path length.

- **Image Export**: Export your generated maze directly as a high-resolution PNG image.

## Getting Started

### For Users (Running the Release)
1. Download the latest release `.zip` package from the [Releases](https://github.com/GreyTimes/Maze-Generator/releases) page.
2. Extract the archive anywhere on your computer.
3. Open the extracted folder, navigate to the **`bin`** directory.
4. Double-click **`mazeGenerator.bat`** (Windows) to launch the application.

#### User Guide
1. **Configure Your Maze**:
   - Use the Generation and Solver Algorithm dropdowns to choose your strategies.
   - Adjust the **Grid Size**, **Animation Delay**, **Complexity/Dead-End Density**, and **Fog Radius** sliders. Hover over any control's **`ℹ`** icon for detailed guidance.
2. **Generate**: Click "Generate Labyrinth" to watch the maze build in real-time.
3. **Solve**: Toggle "Show Solution" at any time to compute and highlight the optimal path using your selected solver.
4. **Play Mode**:
   - Toggle "Playable Mode: ON" and optionally enable "Fog of War: ON".
   - Use your keyboard's **WASD** keys to navigate from the entrance on the left to the exit on the right.
5. **Export & Themes**: Click "Export Image" to save your completed maze as a `.png` file, or toggle the Theme button to switch between Dark and Light appearances.

### For Developers (Running via Gradle)
### Prerequisites
- **Java Development Kit (JDK)**: Version 17 or higher (Java 21+ recommended).
- **Gradle**: Included via Gradle Wrapper (`gradlew`).

#### Clone the repository
```bash
git clone [https://github.com/GreyTimes/Maze-Generator.git](https://github.com/GreyTimes/Maze-Generator.git)
cd mazeGenerator
