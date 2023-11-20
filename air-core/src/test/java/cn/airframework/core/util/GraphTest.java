/*
 * Copyright (c) 2023 looly(loolly@aliyun.com)
 * Hutool is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          https://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package cn.airframework.core.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * test for {@link Graph}
 */
public class GraphTest {

	@Test
	public void testPutEdge() {
		final Graph<Integer> graph = new Graph<>();
		graph.putEdge(0, 1);
		graph.putEdge(1, 2);
		graph.putEdge(2, 0);

		Assert.assertEquals(asSet(1, 2), graph.get(0));
		Assert.assertEquals(asSet(0, 2), graph.get(1));
		Assert.assertEquals(asSet(0, 1), graph.get(2));
	}

	@Test
	public void testContainsEdge() {
		// 0 -- 1
		// |    |
		// 3 -- 2
		final Graph<Integer> graph = new Graph<>();
		graph.putEdge(0, 1);
		graph.putEdge(1, 2);
		graph.putEdge(2, 3);
		graph.putEdge(3, 0);

		Assert.assertTrue(graph.containsEdge(0, 1));
		Assert.assertTrue(graph.containsEdge(1, 0));

		Assert.assertTrue(graph.containsEdge(1, 2));
		Assert.assertTrue(graph.containsEdge(2, 1));

		Assert.assertTrue(graph.containsEdge(2, 3));
		Assert.assertTrue(graph.containsEdge(3, 2));

		Assert.assertTrue(graph.containsEdge(3, 0));
		Assert.assertTrue(graph.containsEdge(0, 3));

		Assert.assertFalse(graph.containsEdge(1, 3));
	}

	@Test
	public void removeEdge() {
		final Graph<Integer> graph = new Graph<>();
		graph.putEdge(0, 1);
		Assert.assertTrue(graph.containsEdge(0, 1));

		graph.removeEdge(0, 1);
		Assert.assertFalse(graph.containsEdge(0, 1));
	}

	@Test
	public void testContainsAssociation() {
		// 0 -- 1
		// |    |
		// 3 -- 2
		final Graph<Integer> graph = new Graph<>();
		graph.putEdge(0, 1);
		graph.putEdge(1, 2);
		graph.putEdge(2, 3);
		graph.putEdge(3, 0);

		Assert.assertTrue(graph.containsAssociation(0, 2));
		Assert.assertTrue(graph.containsAssociation(2, 0));

		Assert.assertTrue(graph.containsAssociation(1, 3));
		Assert.assertTrue(graph.containsAssociation(3, 1));

		Assert.assertFalse(graph.containsAssociation(-1, 1));
		Assert.assertFalse(graph.containsAssociation(1, -1));
	}

	@Test
	public void testGetAssociationPoints() {
		// 0 -- 1
		// |    |
		// 3 -- 2
		final Graph<Integer> graph = new Graph<>();
		graph.putEdge(0, 1);
		graph.putEdge(1, 2);
		graph.putEdge(2, 3);
		graph.putEdge(3, 0);

		Assert.assertEquals(asSet(0, 1, 2, 3), graph.getAssociatedPoints(0, true));
		Assert.assertEquals(asSet(1, 2, 3), graph.getAssociatedPoints(0, false));

		Assert.assertEquals(asSet(1, 2, 3, 0), graph.getAssociatedPoints(1, true));
		Assert.assertEquals(asSet(2, 3, 0), graph.getAssociatedPoints(1, false));

		Assert.assertEquals(asSet(2, 3, 0, 1), graph.getAssociatedPoints(2, true));
		Assert.assertEquals(asSet(3, 0, 1), graph.getAssociatedPoints(2, false));

		Assert.assertEquals(asSet(3, 0, 1, 2), graph.getAssociatedPoints(3, true));
		Assert.assertEquals(asSet(0, 1, 2), graph.getAssociatedPoints(3, false));

		Assert.assertTrue(graph.getAssociatedPoints(-1, false).isEmpty());
	}

	@Test
	public void testGetAdjacentPoints() {
		// 0 -- 1
		// |    |
		// 3 -- 2
		final Graph<Integer> graph = new Graph<>();
		graph.putEdge(0, 1);
		graph.putEdge(1, 2);
		graph.putEdge(2, 3);
		graph.putEdge(3, 0);

		Assert.assertEquals(asSet(1, 3), graph.getAdjacentPoints(0));
		Assert.assertEquals(asSet(2, 0), graph.getAdjacentPoints(1));
		Assert.assertEquals(asSet(1, 3), graph.getAdjacentPoints(2));
		Assert.assertEquals(asSet(2, 0), graph.getAdjacentPoints(3));
	}

	@Test
	public void testRemovePoint() {
		// 0 -- 1
		// |    |
		// 3 -- 2
		final Graph<Integer> graph = new Graph<>();
		graph.putEdge(0, 1);
		graph.putEdge(1, 2);
		graph.putEdge(2, 3);
		graph.putEdge(3, 0);

		// 0
		// |
		// 3 -- 2
		graph.removePoint(1);

		Assert.assertEquals(asSet(3), graph.getAdjacentPoints(0));
		Assert.assertTrue(graph.getAdjacentPoints(1).isEmpty());
		Assert.assertEquals(asSet(3), graph.getAdjacentPoints(2));
		Assert.assertEquals(asSet(2, 0), graph.getAdjacentPoints(3));
	}

	@SafeVarargs
	private static <T> Set<T> asSet(final T... ts) {
		return new LinkedHashSet<>(Arrays.asList(ts));
	}

}
