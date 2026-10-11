import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DepGraph - Java dependency analysis and visualization.
 * Pure JDK. No Maven, no Gradle, no libraries. Run with:  java DepGraph.java
 */
public class DepGraph {

    static final Path WEB = Paths.get("web").toAbsolutePath().normalize();
    static final Set<String> SKIP = new HashSet<>(Arrays.asList(
            "target", "build", "node_modules", ".git", ".idea", ".gradle", ".settings", ".vscode"));

    static final Pattern STRIP = Pattern.compile(
            "(?s)\"\"\".*?\"\"\"|/\\*.*?\\*/|//[^\\n]*|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])+'");
    static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    static final Pattern IMPORT = Pattern.compile("(?m)^\\s*import\\s+(static\\s+)?([\\w.]+?)(\\.\\*)?\\s*;");
    static final Pattern HEADER = Pattern.compile("(?m)^\\s*(import|package)\\s[^;]*;");
    static final Pattern IDENT = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    static class Unit {
        String file, pkg, name, fqn, kind = "class";
        int loc;
        List<String[]> imports = new ArrayList<>(); // {name, "wild" or ""}
        Set<String> tokens = new HashSet<>();
        Set<String> ext = new TreeSet<>();
        Set<Unit> deps = new LinkedHashSet<>();
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = null;
        int port = 8080;
        for (; port < 8100; port++) {
            try {
                server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
                break;
            } catch (BindException e) {
                // port busy, try the next one
            }
        }
        if (server == null) {
            System.out.println("Could not find a free port between 8080 and 8099.");
            return;
        }
        server.createContext("/api/analyze", DepGraph::handleAnalyze);
        server.createContext("/", DepGraph::handleStatic);
        server.start();

        String url = "http://localhost:" + port;
        System.out.println("=======================================");
        System.out.println("  DepGraph is running!");
        System.out.println("  Open this in your browser: " + url);
        System.out.println("  (Close this window to stop it)");
        System.out.println("=======================================");
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            System.out.println("Please open the link above manually.");
        }
    }

    // ---------- HTTP ----------

    static void handleAnalyze(HttpExchange ex) throws IOException {
        try {
            String q = ex.getRequestURI().getRawQuery();
            String p = "sample-project";
            if (q != null) {
                for (String kv : q.split("&")) {
                    if (kv.startsWith("path=")) p = URLDecoder.decode(kv.substring(5), StandardCharsets.UTF_8);
                }
            }
            p = p.trim();
            if (p.length() > 1 && p.startsWith("\"") && p.endsWith("\"")) p = p.substring(1, p.length() - 1);
            if (p.isEmpty()) p = "sample-project";
            Path root = Paths.get(p).toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) {
                send(ex, 400, "application/json",
                        "{\"error\":\"Folder not found: " + esc(root.toString()) + "\"}");
                return;
            }
            send(ex, 200, "application/json", analyze(root));
        } catch (Exception e) {
            send(ex, 500, "application/json", "{\"error\":\"" + esc(String.valueOf(e.getMessage())) + "\"}");
        }
    }

    static void handleStatic(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            Path f = WEB.resolve(path.substring(1)).normalize();
            if (!f.startsWith(WEB) || !Files.isRegularFile(f)) {
                send(ex, 404, "text/plain", "Not found");
                return;
            }
            String n = f.getFileName().toString();
            String type = n.endsWith(".html") ? "text/html; charset=utf-8"
                    : n.endsWith(".css") ? "text/css; charset=utf-8"
                    : n.endsWith(".js") ? "application/javascript; charset=utf-8"
                    : n.endsWith(".svg") ? "image/svg+xml" : "application/octet-stream";
            send(ex, 200, type, Files.readAllBytes(f));
        } catch (Exception e) {
            send(ex, 404, "text/plain", "Not found");
        }
    }

    static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        send(ex, code, type.contains("json") ? "application/json; charset=utf-8" : type,
                body.getBytes(StandardCharsets.UTF_8));
    }

    static void send(HttpExchange ex, int code, String type, byte[] b) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }

    // ---------- Analysis ----------

    static String analyze(final Path root) throws IOException {
        final List<Path> files = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes a) {
                if (!d.equals(root)) {
                    String n = String.valueOf(d.getFileName());
                    if (SKIP.contains(n)) return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                if (f.getFileName().toString().endsWith(".java") && files.size() < 5000) files.add(f);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path f, IOException e) {
                return FileVisitResult.CONTINUE;
            }
        });

        List<Unit> units = new ArrayList<>();
        Map<String, Unit> byFqn = new HashMap<>();
        Map<String, List<Unit>> byPkg = new HashMap<>();

        for (Path f : files) {
            String stem = f.getFileName().toString().replaceFirst("\\.java$", "");
            if (!stem.matches("[A-Za-z_$][A-Za-z0-9_$]*")) continue; // skips package-info, module-info
            String text = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
            Unit u = new Unit();
            u.name = stem;
            u.file = root.relativize(f).toString().replace('\\', '/');
            long loc = 0;
            for (String line : text.split("\\R")) if (!line.trim().isEmpty()) loc++;
            u.loc = (int) loc;

            String clean = STRIP.matcher(text).replaceAll(" ");
            Matcher pm = PACKAGE.matcher(clean);
            u.pkg = pm.find() ? pm.group(1) : "(default)";
            u.fqn = u.pkg.equals("(default)") ? stem : u.pkg + "." + stem;

            Matcher km = Pattern.compile("\\b(@interface|class|interface|enum|record)\\s+" + Pattern.quote(stem) + "\\b")
                    .matcher(clean);
            if (km.find()) u.kind = km.group(1).equals("@interface") ? "annotation" : km.group(1);

            Matcher im = IMPORT.matcher(clean);
            while (im.find()) {
                u.imports.add(new String[]{im.group(2), im.group(3) != null ? "wild" : ""});
            }
            String body = HEADER.matcher(clean).replaceAll(" ");
            Matcher tm = IDENT.matcher(body);
            while (tm.find()) u.tokens.add(tm.group());

            if (byFqn.containsKey(u.fqn)) continue;
            byFqn.put(u.fqn, u);
            byPkg.computeIfAbsent(u.pkg, k -> new ArrayList<>()).add(u);
            units.add(u);
        }

        for (Unit u : units) {
            for (String[] imp : u.imports) {
                String name = imp[0];
                if (imp[1].equals("wild")) {
                    List<Unit> inPkg = byPkg.get(name);
                    if (inPkg != null) {
                        for (Unit v : inPkg) if (v != u && u.tokens.contains(v.name)) u.deps.add(v);
                    } else {
                        Unit t = resolve(name, byFqn);
                        if (t != null && t != u) u.deps.add(t);
                        else if (t == null) u.ext.add(externalGroup(name));
                    }
                } else {
                    Unit t = resolve(name, byFqn);
                    if (t != null) {
                        if (t != u) u.deps.add(t);
                    } else {
                        u.ext.add(externalGroup(name));
                    }
                }
            }
            List<Unit> same = byPkg.get(u.pkg);
            if (same != null) {
                for (Unit v : same) if (v != u && u.tokens.contains(v.name)) u.deps.add(v);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"root\":\"").append(esc(root.toString())).append("\",\"files\":").append(files.size());
        sb.append(",\"nodes\":[");
        boolean first = true;
        for (Unit u : units) {
            if (!first) sb.append(',');
            first = false;
            sb.append("{\"id\":\"").append(esc(u.fqn)).append("\",\"name\":\"").append(esc(u.name))
                    .append("\",\"pkg\":\"").append(esc(u.pkg)).append("\",\"kind\":\"").append(u.kind)
                    .append("\",\"loc\":").append(u.loc).append(",\"file\":\"").append(esc(u.file))
                    .append("\",\"ext\":[");
            boolean f2 = true;
            for (String x : u.ext) {
                if (!f2) sb.append(',');
                f2 = false;
                sb.append('"').append(esc(x)).append('"');
            }
            sb.append("]}");
        }
        sb.append("],\"edges\":[");
        first = true;
        for (Unit u : units) {
            for (Unit v : u.deps) {
                if (!first) sb.append(',');
                first = false;
                sb.append("[\"").append(esc(u.fqn)).append("\",\"").append(esc(v.fqn)).append("\"]");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    static Unit resolve(String name, Map<String, Unit> byFqn) {
        String n = name;
        while (true) {
            Unit u = byFqn.get(n);
            if (u != null) return u;
            int i = n.lastIndexOf('.');
            if (i < 0) return null;
            n = n.substring(0, i);
        }
    }

    static String externalGroup(String name) {
        String[] p = name.split("\\.");
        return p.length >= 2 ? p[0] + "." + p[1] : name;
    }

    static String esc(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.toString();
    }
}
