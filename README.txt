DEPGRAPH - Java Dependency Visualizer
=====================================

WHAT YOU NEED
  Only Java (JDK 11 or newer - you already have JDK 17 for Spring Boot).
  NO Maven. NO Node. NO npm install. NO internet.

HOW TO RUN (Windows) - 3 steps
  1. Right-click depgraph.zip -> "Extract All..." -> Extract.
  2. Open the extracted "depgraph" folder and double-click  start.bat
     A black window opens and says "DepGraph is running!"
     Your browser opens by itself. (If not, open http://localhost:8080)
  3. KEEP the black window open while you use the app.
     To stop DepGraph, close the black window.

HOW TO USE
  - It opens with a built-in sample project, so you see a graph right away.
  - To see YOUR project: open your project folder in File Explorer, click the
    address bar at the top, copy the path, paste it into the box at the top of
    DepGraph, then click "Analyze".
    (Point to the folder that contains your .java files, e.g. your "src" folder
    or the whole project folder - both work.)
  - Click a bubble  -> see what it uses and who uses it.
  - Drag a bubble   -> move it.  Drag empty space -> move around.
  - Mouse wheel     -> zoom.   The fit button fits everything on screen.
  - "Classes / Packages" switch at the top changes the level of detail.
  - Red ring = circular dependency (two classes depend on each other).
  - Export PNG saves a picture of the graph.

IF SOMETHING GOES WRONG
  "Java was not found"      -> install JDK 17 from https://adoptium.net
  Browser shows nothing     -> make sure the black window is still open
  Port busy                 -> DepGraph automatically tries 8081, 8082 ...
                               read the address shown in the black window
  Folder not found          -> re-copy the path from File Explorer
