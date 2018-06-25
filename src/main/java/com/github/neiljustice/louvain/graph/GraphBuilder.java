
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

package com.github.neiljustice.louvain.graph;

import com.github.neiljustice.louvain.util.*;

import java.util.*;

public class GraphBuilder {
	private SparseIntMatrix matrix;
	private ArrayList<Integer>[] adjList;
	private Map<Integer, Integer> index, reverseIndex;
	private int[] degrees;
	private int order = 0;
	private int numNodes = 0;
	private int sizeDbl = 0;
	private int layer = 0;

	/* The size parameter is needed to initialize the adjacency list */
	public GraphBuilder(int size) {
		numNodes = size;
		initialize();
	}
	
	public void addNode(Integer id) {
		if (!index.containsKey(id)) {
			index.put(id, order);
			reverseIndex.put(order, id);
			order++;
		}
	}

	/* Add symmetrical edge. Make sure the nodes were added to the graph via the *addNode* method 
	 * before calling this method */
	public void addEdgeSym(int n1, int n2, int weight) {
		if (!index.containsKey(n1)) {
			throw new Error("Node " + n1 + " does not exist in the graph");
		}
		if (!index.containsKey(n2)) {
			throw new Error("Node " + n2 + " does not exist in the graph");
		}
		n1 = index.get(n1);
		n2 = index.get(n2);
		insertEdge(n1, n2, weight);
		if (n1 != n2)
			insertEdge(n2, n1, weight);
	}

	private void insertEdge(int n1, int n2, int weight) {
		matrix.set(n1, n2, weight);
		adjList[n1].add(n2);
		degrees[n1] += weight;
		sizeDbl += weight;
	}

	private void initialize() {
		index = new HashMap<Integer, Integer>();
		reverseIndex = new HashMap<Integer, Integer>();
		matrix = new SparseIntMatrix(numNodes);
		degrees = new int[numNodes];
		adjList = (ArrayList<Integer>[]) new ArrayList[numNodes];
		for (int i = 0; i < numNodes; i++) {
			adjList[i] = new ArrayList<Integer>();
		}
	}

	public Graph coarseGrain(Graph g, Map<Integer, Integer> map) {
		this.order = g.partitioning().numComms();
		this.layer = g.layer() + 1;
		initialize();
		int sum = 0;

		for (SparseIntMatrix.MyIterator it = g.partitioning().commWeightIterator(); it.hasNext();) {
			it.advance();
			int weight = it.value();
			if (weight != 0) {
				int n1 = map.get((int) it.x());
				int n2 = map.get((int) it.y());
				insertEdge(n1, n2, weight);
				sum += weight;
			}
		}

		if (!matrix.isSymmetric())
			throw new Error("asymmetric matrix");
		if (sum != g.size() * 2)
			throw new Error("builder recieved wrong weights: " + sum + " " + (g.size() * 2));
		if (sum != sizeDbl)
			throw new Error("Coarse-grain error: " + sum + " != " + sizeDbl);
		return build();
	}

	public Graph fromCommunity(Graph g, ArrayList<Integer> members) {
		this.order = members.size();
		initialize();

		for (int newNode = 0; newNode < order; newNode++) {
			int oldNode = members.get(newNode);
			for (int i = 0; i < g.neighbours(oldNode).size(); i++) {
				int oldNeigh = g.neighbours(oldNode).get(i);
				int newNeigh = -1;
				if ((newNeigh = members.indexOf(oldNeigh)) != -1) {
					insertEdge(newNode, newNeigh, g.weight(oldNode, oldNeigh));
				}
			}
		}
		if (!matrix.isSymmetric())
			throw new Error("asymmetric matrix");
		return build();
	}

	public Graph erdosRenyi(int order, double prob) {
		this.order = order;
		Random rnd = new Random();
		initialize();

		for (int n1 = 0; n1 < order; n1++) {
			for (int n2 = 0; n2 < order; n2++) {
				if (matrix.get(n2, n1) == 0 && n1 != n2 && rnd.nextDouble() < prob) {
					addEdgeSym(n1, n2, 1);
				}
			}
		}
		if (!matrix.isSymmetric())
			throw new Error("asymmetric matrix");
		return build();
	}

	public SparseIntMatrix matrix() {
		return matrix;
	}

	public ArrayList<Integer>[] adjList() {
		return adjList;
	}

	public int[] degrees() {
		return degrees;
	}

	public int sizeDbl() {
		return sizeDbl;
	}

	public int order() {
		return order;
	}

	public int layer() {
		return layer;
	}

	public Map<Integer, Integer> index() {
		return index;
	}

	public Map<Integer, Integer> reverseIndex() {
		return reverseIndex;
	}

	public Graph build() {
		return new Graph(this);
	}
}