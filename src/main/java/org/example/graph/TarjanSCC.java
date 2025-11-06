package org.example.graph;
import java.util.*;
public class TarjanSCC {
    private final int n;
    private final List<Integer>[] adj;
    private int index = 0;
    private final int[] indices, low;
    private final boolean[] onStack;
    private final Deque<Integer> stack = new ArrayDeque<>();
    private final List<List<Integer>> components = new ArrayList<>();
    private final int[] componentOf;
    @SuppressWarnings("unchecked")
    public TarjanSCC(int n) {
        this.n = n;
        adj = new List[n];
        for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();
        indices = new int[n];
        Arrays.fill(indices, -1);
        low = new int[n];
        onStack = new boolean[n];
        componentOf = new int[n];
        Arrays.fill(componentOf, -1);
    }
    public void addEdge(int u, int v) { adj[u].add(v); }
    public List<List<Integer>> run() {
        for (int v = 0; v < n; v++) if (indices[v] == -1) strongConnect(v);
        for (int cid = 0; cid < components.size(); cid++) for (int v : components.get(cid)) componentOf[v] = cid;
        return components;
    }
    private void strongConnect(int v) {
        indices[v] = index;
        low[v] = index;
        index++;
        stack.push(v);
        onStack[v] = true;
        for (int w : adj[v]) {
            if (indices[w] == -1) {
                strongConnect(w);
                low[v] = Math.min(low[v], low[w]);
            } else if (onStack[w]) low[v] = Math.min(low[v], indices[w]);
        }
        if (low[v] == indices[v]) {
            List<Integer> comp = new ArrayList<>();
            while (true) {
                int w = stack.pop();
                onStack[w] = false;
                comp.add(w);
                if (w == v) break;
            }
            components.add(comp);
        }
    }
    public int componentOf(int v) { return componentOf[v]; }
    public int componentCount() { return components.size(); }
    public List<List<Integer>> components() { return components; }
}
