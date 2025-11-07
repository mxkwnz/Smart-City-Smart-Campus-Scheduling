package org.example;

import org.example.graph.TarjanSCC;
import org.example.graph.DamirTopo;
import org.example.graph.ShynggysShortestLongest;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Main {
    static class InputEdge { int u, v; long w; }
    static class TasksJson { boolean directed; int n; InputEdge[] edges; Integer source; String weight_model; }

    static class DagEdge {
        final int u, v;
        final long w;
        DagEdge(int u, int v, long w) {
            this.u = u;
            this.v = v;
            this.w = w;
        }
    }

    static class CondensedEdge {
        final int toComp;
        final long weight;
        final int fromVertex;
        final int toVertex;
        CondensedEdge(int toComp, long weight, int fromVertex, int toVertex) {
            this.toComp = toComp;
            this.weight = weight;
            this.fromVertex = fromVertex;
            this.toVertex = toVertex;
        }
    }

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "src/data/small_dag_1.json";
        Path resolved = resolveInputPath(path);
        TasksJson tasks = loadTasks(resolved);
        runAll(tasks);
    }

    private static Path resolveInputPath(String path) {
        Path candidate = Paths.get(path);
        if (Files.exists(candidate)) return candidate;
        Path inData = Paths.get("src", "data", path);
        if (Files.exists(inData)) return inData;
        Path justData = Paths.get("data", path);
        if (Files.exists(justData)) return justData;
        throw new IllegalArgumentException("Cannot locate input file: " + path);
    }

    private static TasksJson loadTasks(Path path) throws IOException {
        String json = Files.readString(path);
        TasksJson tasks = new TasksJson();
        tasks.directed = extractBoolean(json, "directed", false);
        tasks.n = extractInt(json, "n");
        tasks.source = extractOptionalInt(json, "source");
        tasks.weight_model = extractString(json, "weight_model");
        tasks.edges = extractEdges(json).toArray(new InputEdge[0]);
        return tasks;
    }

    private static boolean extractBoolean(String json, String key, boolean defaultValue) {
        Matcher m = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(true|false)").matcher(json);
        return m.find() ? Boolean.parseBoolean(m.group(1)) : defaultValue;
    }

    private static int extractInt(String json, String key) {
        Matcher m = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!m.find()) throw new IllegalArgumentException("Missing required integer field '" + key + "'");
        return Integer.parseInt(m.group(1));
    }

    private static Integer extractOptionalInt(String json, String key) {
        Matcher m = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(-?\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private static String extractString(String json, String key) {
        Matcher m = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static List<InputEdge> extractEdges(String json) {
        Matcher arrayMatcher = Pattern.compile("\\\"edges\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(json);
        if (!arrayMatcher.find()) throw new IllegalArgumentException("Missing required array field 'edges'");
        String body = arrayMatcher.group(1);
        Matcher edgeMatcher = Pattern.compile("\\{\\s*\\\"u\\\"\\s*:\\s*(-?\\d+)\\s*,\\s*\\\"v\\\"\\s*:\\s*(-?\\d+)\\s*,\\s*\\\"w\\\"\\s*:\\s*(-?\\d+)\\s*}(,|\\s|$)", Pattern.DOTALL).matcher(body);
        List<InputEdge> edges = new ArrayList<>();
        while (edgeMatcher.find()) {
            InputEdge e = new InputEdge();
            e.u = Integer.parseInt(edgeMatcher.group(1));
            e.v = Integer.parseInt(edgeMatcher.group(2));
            e.w = Long.parseLong(edgeMatcher.group(3));
            edges.add(e);
        }
        if (edges.isEmpty()) throw new IllegalArgumentException("No edges parsed from 'edges' array");
        return edges;
    }

    private static void runAll(TasksJson tasks) {
        int n = tasks.n;
        List<DagEdge> edges = new ArrayList<>();
        for (InputEdge ie : tasks.edges) edges.add(new DagEdge(ie.u, ie.v, ie.w));

        TarjanSCC tarjan = new TarjanSCC(n);
        for (DagEdge e : edges) tarjan.addEdge(e.u, e.v);
        List<List<Integer>> comps = tarjan.run();
        int k = comps.size();

        int[] compOf = new int[n];
        for (int cid = 0; cid < k; cid++)
            for (int v : comps.get(cid))
                compOf[v] = cid;

        @SuppressWarnings("unchecked")
        List<ShynggysShortestLongest.Edge>[] compAdj = new List[k];
        @SuppressWarnings("unchecked")
        List<CondensedEdge>[] condensed = new List[k];
        @SuppressWarnings("unchecked")
        Set<Integer>[] topoEdges = new Set[k];
        for (int i = 0; i < k; i++) {
            compAdj[i] = new ArrayList<>();
            condensed[i] = new ArrayList<>();
            topoEdges[i] = new LinkedHashSet<>();
        }
        for (DagEdge e : edges) {
            int cu = compOf[e.u];
            int cv = compOf[e.v];
            if (cu == cv) continue;
            compAdj[cu].add(new ShynggysShortestLongest.Edge(cv, e.w, e.u, e.v));
            condensed[cu].add(new CondensedEdge(cv, e.w, e.u, e.v));
            topoEdges[cu].add(cv);
        }

        @SuppressWarnings("unchecked")
        List<int[]>[] tmpAdj = new List[k];
        for (int i = 0; i < k; i++) {
            tmpAdj[i] = new ArrayList<>();
            for (Integer to : topoEdges[i]) tmpAdj[i].add(new int[]{to, 0});
        }

        List<Integer> topo = DamirTopo.kahnTopo(tmpAdj);

        if (tasks.source == null)
            throw new IllegalArgumentException("tasks.json is missing required field 'source'");
        int src = tasks.source;
        int srcComp = compOf[src];

        ShynggysShortestLongest.Result shortRes = ShynggysShortestLongest.shortestFrom(compAdj, srcComp, topo);
        ShynggysShortestLongest.Result longRes = ShynggysShortestLongest.longestPath(compAdj, topo);

        System.out.println("SCCs:");
        for (int i = 0; i < k; i++) {
            List<Integer> compNodes = new ArrayList<>(comps.get(i));
            Collections.sort(compNodes);
            System.out.println(i + " (size=" + compNodes.size() + "): " + compNodes);
        }

        System.out.println("Condensation graph adjacency (component -> [to (weight, u->v)]):");
        for (int i = 0; i < k; i++) {
            if (condensed[i].isEmpty()) {
                System.out.println(i + " -> []");
                continue;
            }
            List<String> desc = new ArrayList<>();
            for (CondensedEdge ce : condensed[i]) {
                desc.add(ce.toComp + "(w=" + ce.weight + ", " + ce.fromVertex + "->" + ce.toVertex + ")");
            }
            System.out.println(i + " -> " + desc);
        }

        System.out.println("Topological order: " + topo);
        List<Integer> derivedOrder = new ArrayList<>();
        for (int comp : topo) {
            List<Integer> nodes = new ArrayList<>(comps.get(comp));
            Collections.sort(nodes);
            derivedOrder.addAll(nodes);
        }
        System.out.println("Derived task order: " + derivedOrder);

        System.out.println("Shortest distances:");
        for (int i = 0; i < k; i++)
            if (shortRes.value[i] >= Long.MAX_VALUE / 8) {
                System.out.println("comp " + i + " : INF");
            } else {
                List<Integer> compPath = ShynggysShortestLongest.reconstructPath(i, shortRes.pred);
                List<Integer> taskPath = reconstructTaskPath(compPath, shortRes, comps, src);
                System.out.println("comp " + i + " : " + shortRes.value[i] + " path=" + taskPath);
            }

        long best = Long.MIN_VALUE;
        int bestComp = -1;
        for (int i = 0; i < k; i++) {
            if (longRes.value[i] > best) {
                best = longRes.value[i];
                bestComp = i;
            }
        }

        if (bestComp == -1) {
            System.out.println("Critical path (components): []");
            System.out.println("Critical path (tasks): []");
            System.out.println("Critical path length: undefined");
        } else {
            List<Integer> critCompPath = ShynggysShortestLongest.reconstructPath(bestComp, longRes.pred);
            List<Integer> critTaskPath = reconstructTaskPath(critCompPath, longRes, comps, null);
            System.out.println("Critical path (components): " + critCompPath);
            System.out.println("Critical path (tasks): " + critTaskPath);
            System.out.println("Critical path length: " + longRes.value[bestComp]);
        }
    }

    private static List<Integer> reconstructTaskPath(List<Integer> componentPath,
                                                     ShynggysShortestLongest.Result res,
                                                     List<List<Integer>> components,
                                                     Integer explicitStart) {
        if (componentPath.isEmpty()) return Collections.emptyList();
        LinkedList<Integer> nodes = new LinkedList<>();
        if (explicitStart != null) nodes.add(explicitStart);
        else nodes.add(selectRepresentative(components.get(componentPath.get(0))));
        for (int idx = 1; idx < componentPath.size(); idx++) {
            int comp = componentPath.get(idx);
            int fromVertex = res.predBridgeFrom[comp];
            int toVertex = res.predBridgeTo[comp];
            if (fromVertex != -1) {
                if (nodes.isEmpty() || nodes.getLast() != fromVertex) nodes.add(fromVertex);
            } else {
                int fallback = selectRepresentative(components.get(componentPath.get(idx - 1)));
                if (nodes.isEmpty() || nodes.getLast() != fallback) nodes.add(fallback);
            }
            if (toVertex != -1) {
                if (nodes.isEmpty() || nodes.getLast() != toVertex) nodes.add(toVertex);
            } else {
                int fallback = selectRepresentative(components.get(comp));
                if (nodes.isEmpty() || nodes.getLast() != fallback) nodes.add(fallback);
            }
        }
        return nodes;
    }

    private static int selectRepresentative(List<Integer> nodes) {
        if (nodes.isEmpty()) return -1;
        List<Integer> copy = new ArrayList<>(nodes);
        Collections.sort(copy);
        return copy.get(0);
    }
}