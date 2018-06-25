
/* MIT License

Copyright (c) 2018 Neil Justice

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE. */

package com.github.neiljustice.louvain.clustering;

import com.github.neiljustice.louvain.graph.*;
import java.util.*;

/**
 * Given a list of graphs where each node in graph n + 1 is a community im graph
 * n, maps the partitionings of all graphs (except the first) as if they were
 * partitionings of the first graph.
 */
public class LayerMapper {
	private final List<Graph> graphs = new ArrayList<Graph>();
	// maps between communities on L and nodes on L + 1:
	private final List<Map<Integer, Integer>> layerMaps = new ArrayList<>();
	private int layer = 0;

	// map from community -> node on layer above
	protected Map<Integer, Integer> createLayerMap(Graph g) {
		int count = 0;
		layer++;
		boolean[] isFound = new boolean[g.order()];
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		// Arrays.sort(communities);

		for (int node = 0; node < g.order(); node++) {
			int comm = g.partitioning().community(node);
			if (!isFound[comm]) {
				map.put(comm, count);
				isFound[comm] = true;
				count++;
			}
		}
		if (map.size() != g.partitioning().numComms())
			throw new Error("Map creation failed: " + g.partitioning().numComms() + " != " + map.size());
		layerMaps.add(map);
		graphs.add(g);
		return map;
	}

	// uses the layer maps to assign a community from each layer to the base layer
	// graph.
	protected List<int[]> run() {
		List<int[]> rawComms = new ArrayList<int[]>();
		List<int[]> communities = new ArrayList<int[]>();
		communities.add(graphs.get(0).partitioning().communities());

		for (int i = 0; i < layer; i++) {
			rawComms.add(graphs.get(i).partitioning().communities());
		}

		for (int i = 0; i < layer - 1; i++) {
			communities.add(mapToBaseLayer(i, rawComms));
		}

		return communities;
	}

	// maps layers to each other until the specified layer has been mapped to the
	// base layer
	private int[] mapToBaseLayer(int layer, List<int[]> rawComms) {
		int[] a = mapToNextLayer(graphs.get(layer), layerMaps.get(layer), rawComms.get(layer + 1));
		layer--;

		while (layer >= 0) {
			a = mapToNextLayer(graphs.get(layer), layerMaps.get(layer), a);
			layer--;
		}

		return a;
	}

	// maps each node in a layer to its community on the layer above it
	private int[] mapToNextLayer(Graph g, Map<Integer, Integer> map, int[] commsL2) {
		int[] commsL1 = g.partitioning().communities();
		int[] NL1toCL2 = new int[g.order()];

		for (int nodeL1 = 0; nodeL1 < g.order(); nodeL1++) {
			int commL1 = commsL1[nodeL1];
			int nodeL2 = map.get(commL1);
			int commL2 = commsL2[nodeL2];
			NL1toCL2[nodeL1] = commL2;
		}

		return NL1toCL2;
	}
}
