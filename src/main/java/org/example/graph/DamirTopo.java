package org.example.graph;
import java.util.*;
public class DamirTopo {
    public static List<Integer> kahnTopo(List<int[]>[] adjacency) {
        int n = adjacency.length;
        int[] indeg = new int[n];
        Arrays.fill(indeg, 0);
        for (int u = 0; u < n; u++) for (int[] e : adjacency[u]) indeg[e[0]]++;
        Deque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (indeg[i] == 0) q.add(i);
        List<Integer> order = new ArrayList<>();
        while (!q.isEmpty()) {
            int u = q.remove();
            order.add(u);
            for (int[] e : adjacency[u]) {
                int v = e[0];
                indeg[v]--;
                if (indeg[v] == 0) q.add(v);
            }
        }
        if (order.size() != n) throw new IllegalStateException("Not a DAG");
        return order;
    }
}