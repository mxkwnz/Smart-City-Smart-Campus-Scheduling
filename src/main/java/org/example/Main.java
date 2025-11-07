package org.example;

import org.example.graph.TarjanSCC;
import org.example.graph.DamirTopo;
import org.example.graph.ShynggysShortestLongest;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public class Main {
    static class InputEdge { int u, v; long w; }
    static class TasksJson { boolean directed; int n; InputEdge[] edges; Integer source; String weight_model; }
    public static void main(String[] args) throws Exception {
        String path = "tasks.json";
        if (args.length > 0) path = args[0];
        Gson gson = new Gson();
        try (Reader r = Files.newBufferedReader(Paths.get(path))) {
            TasksJson tasks = gson.fromJson(r, TasksJson.class);
            runAll(tasks);
        }
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
        for (int cid = 0; cid < k; cid++) for (int v : comps.get(cid)) compOf[v] = cid;
        long NO_EDGE = Long.MIN_VALUE;
        long[][] compWeight = new long[k][k];
        for (int i = 0; i < k; i++) Arrays.fill(compWeight[i], NO_EDGE);
        for (DagEdge e : edges) {
            int cu = compOf[e.u], cv = compOf[e.v];
            if (cu == cv) continue;
            if (compWeight[cu][cv] == NO_EDGE) compWeight[cu][cv] = e.w;
            else compWeight[cu][cv] = Math.min(compWeight[cu][cv], e.w);
        }
        List<Edge>[] compAdj = new List[k];
        for (int i = 0; i < k; i++) {
            compAdj[i] = new ArrayList<>();
            for (int j = 0; j < k; j++) if (compWeight[i][j] != NO_EDGE) compAdj[i].add(new Edge(j, compWeight[i][j]));
        }
        List<int[]>[] tmpAdj = new List[k];
        for (int i = 0; i < k; i++) {
            tmpAdj[i] = new ArrayList<>();
            for (Edge e : compAdj[i]) tmpAdj[i].add(new int[]{e.to, 0});
        }
        List<Integer> topo = DamirTopo.kahnTopo(tmpAdj);
        int src = tasks.source;
        int srcComp = compOf[src];
        ShyngysShortestLongest.Result shortRes = ShyngysShortestLongest.shortestFrom(compAdj, srcComp, topo);
        ShyngysShortestLongest.Result longRes = ShyngysShortestLongest.longestPath(compAdj, topo);
        System.out.println("SCCs:");
        for (int i = 0; i < k; i++) System.out.println(i + ": " + comps.get(i));
        System.out.println("Topological order: " + topo);
        System.out.println("Shortest distances:");
        for (int i = 0; i < k; i++) System.out.println("comp " + i + " : " + (shortRes.value[i] >= Long.MAX_VALUE/8 ? "INF" : shortRes.value[i]));
        long best = Long.MIN_VALUE; int bestComp = -1;
        for (int i = 0; i < k; i++) if (longRes.value[i] > best) { best = longRes.value[i]; bestComp = i; }
        List<Integer> critPath = ShyngysShortestLongest.reconstructPath(bestComp, longRes.pred);
        System.out.println("Critical path (components): " + critPath);
        System.out.println("Critical path length: " + longRes.value[bestComp]);
    }
    static class DagEdge { final int u,v; final long w; DagEdge(int u,int v,long w){this.u=u;this.v=v;this.w=w;} }
}