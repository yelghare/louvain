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

package com.github.neiljustice.louvain.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Sparse square matrix using hashmap.
 */
public class SparseIntMatrix {
	private Map<Long, Integer> map;
	private final long size;
	private boolean compressed = false;

	public SparseIntMatrix(int size) {
		this.size = (long) size;
		map = new HashMap<>();
	}

	public SparseIntMatrix(SparseIntMatrix m) {
		this.size = (long) m.size();
		map = m.copyMap();
	}

	public int get(int x, int y) {
		long key = (long) x * size + (long) y;
		if (map.containsKey(key))
			return map.get(key);
		return 0;
	}

	public void set(int x, int y, int val) {
		map.put((long) x * size + (long) y, val);
		compressed = false;
	}

	public void add(int x, int y, int val) {
		set(x, y, get(x, y) + val);
	}

	private boolean isNonZero(long key, int val) {
		if (val == 0)
			return false;
		return true;
	}

	private void compress() {
		Map<Long, Integer> newMap = new HashMap<Long, Integer>();
		for (Map.Entry<Long, Integer> iter : map.entrySet()) {
			if (isNonZero(iter.getKey(), iter.getValue())) {
				newMap.put(iter.getKey(), iter.getValue());
			}
		}
		map = newMap;
		return;
	}

	public int size() {
		return (int) size;
	}

	public SparseIntMatrix.MyIterator iterator() {
		if (compressed == false) {
			compress();
			compressed = true;
		}
		return new MyIterator();
	}

	public HashMap<Long, Integer> copyMap() {
		return new HashMap<Long, Integer>(map);
	}

	public boolean isSymmetric() {
		for (SparseIntMatrix.MyIterator it = iterator(); it.hasNext();) {
			it.advance();
			if (it.value() != get(it.x(), it.y()))
				return false;
		}
		return true;
	}

	public class MyIterator {
		private final Iterator<Map.Entry<Long, Integer>> iterator;
		private Map.Entry<Long, Integer> nextValue;

		MyIterator() {
			iterator = map.entrySet().iterator();
		}

		public void advance() {
			nextValue = iterator.next();
		}

		public boolean hasNext() {
			return iterator.hasNext();
		}

		public int value() {
			return nextValue.getValue();
		}

		public int x() {
			return (int) (nextValue.getKey() % size);
		}

		public int y() {
			return (int) (nextValue.getKey() / size);
		}
	}
}
