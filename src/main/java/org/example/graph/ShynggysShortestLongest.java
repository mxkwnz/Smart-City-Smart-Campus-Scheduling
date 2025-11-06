package org.example.graph;
import java.util.*;
public class ShynggysShortestLongest {
    public static class Edge {
        public final int to;
        public final long w;
        public Edge(int to, long w) { this.to = to; this.w = w; }
    }
    public static Result shortestFrom(List<Edge>[] dag, int src, List<Integer> topo) {
        int n = dag.length;
        long INF = Long.MAX_VALUE / 4;
        long[] dist = new long[n];
        Arrays.fill(dist, INF);
        int[] pred = new int[n];
        Arrays.fill(pred, -1);
        dist[src] = 0;
        for (int u : topo) {
            if (dist[u] == INF) continue;
            for (Edge e : dag[u]) {
                int v = e.to;
                long nd = dist[u] + e.w;
                if (nd < dist[v]) {
                    dist[v] = nd;
                    pred[v] = u;
                }
            }
        }
        return new Result(dist, pred);
    }
    public static Result longestPath(List<Edge>[] dag, List<Integer> topo) {
        int n = dag.length;
        long NEG = Long.MIN_VALUE / 4;
        long[] best = new long[n];
        Arrays.fill(best, NEG);
        int[] pred = new int[n];
        Arrays.fill(pred, -1);
        int[] indeg = new int[n];
        for (int u = 0; u < n; u++) for (Edge e : dag[u]) indeg[e.to]++;
        for (int i = 0; i < n; i++) if (indeg[i] == 0) best[i] = 0L;
        for (int u : topo) {
            if (best[u] == NEG) continue;
            for (Edge e : dag[u]) {
                int v = e.to;
                long candidate = best[u] + e.w;
                if (candidate > best[v]) {
                    best[v] = candidate;
                    pred[v] = u;
                }
            }
        }
        return new Result(best, pred);
    }
    public static class Result {
        public final long[] value;
        public final int[] pred;
        public Result(long[] value, int[] pred) { this.value = value; this.pred = pred; }
    }
    public static List<Integer> reconstructPath(int t, int[] pred) {
        if (t < 0) return Collections.emptyList();
        LinkedList<Integer> path = new LinkedList<>();
        int cur = t;
        while (cur != -1) {
            path.addFirst(cur);
            cur = pred[cur];
        }
        return path;
    }
}