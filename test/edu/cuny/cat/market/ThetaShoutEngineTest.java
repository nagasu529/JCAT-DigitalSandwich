/*
 * JCAT - TAC Market Design Competition Platform
 * Copyright (C) 2006-2013 Jinzhong Niu, Kai Cai
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
/*
 * JASA Java Auction Simulator API
 * Copyright (C) 2001-2005 Steve Phelps
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package edu.cuny.cat.market;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.Shout;
import edu.cuny.cat.market.matching.FourHeapShoutEngine;
import edu.cuny.cat.market.matching.LazyMaxVolumeShoutEngine;
import edu.cuny.cat.market.matching.ShoutEngine;
import edu.cuny.cat.market.matching.ThetaShoutEngine;

/**
 * @author Jinzhong Niu
 * @version $Revision: 1.6 $
 */

public class ThetaShoutEngineTest extends AbstractShoutEngineTest {

	protected static Logger logger = Logger.getLogger(ThetaShoutEngineTest.class);

	ThetaShoutEngine thetaShoutEngine;

	FourHeapShoutEngine equShoutEngine;

	LazyMaxVolumeShoutEngine maxShoutEngine;

	public ThetaShoutEngineTest(final String name) {
		super(name);
	}

	@Override
	public ThetaShoutEngine getShoutEngine() {
		return thetaShoutEngine;
	}

	@Override
	public void setUp() {
		super.setUp();
		thetaShoutEngine = new ThetaShoutEngine();

		equShoutEngine = new FourHeapShoutEngine();

		maxShoutEngine = new LazyMaxVolumeShoutEngine();
	}

	/**
	 * 
	 */
	public void testEquivalence() {
		System.out.println("\n>>>>>>>>>\t " + "testEquivalence() \n");

		Shout shout = null;

		try {

			for (int round = 0; round < 10; round++) {

				for (int num = 0; num < 100; num++) {
					shout = randomShout();
					equShoutEngine.newShout((Shout) shout.clone());
					maxShoutEngine.newShout((Shout) shout.clone());
					thetaShoutEngine.newShout(shout);
				}

				int maxQ = maxShoutEngine.getMatchedVolume();
				int equQ = equShoutEngine.getMatchedVolume();

				checkEqualMatchedVolume(
						thetaShoutEngine.calculateEquilibriumQuantity(), equQ,
						"Failed with equQ");

				thetaShoutEngine.setTheta(1.0D);
				checkEqualMatchedVolume(thetaShoutEngine.getMatchedVolume(), maxQ,
						"Failed with theta=1");
				checkEqualShouts(thetaShoutEngine, maxShoutEngine,
						"Failed with theta=1");

				thetaShoutEngine.setTheta(0.0D);
				checkEqualMatchedVolume(thetaShoutEngine.getMatchedVolume(), equQ,
						"Failed with theta=0");
				checkEqualShouts(thetaShoutEngine, equShoutEngine,
						"Failed with theta=0");

				thetaShoutEngine.setTheta(-1.0D);
				checkEqualMatchedVolume(thetaShoutEngine.getMatchedVolume(), 0,
						"Failed with theta=-1");

				System.out.println("round: " + round + " okay");
			}

		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	protected void checkEqualMatchedVolume(int obtained, int expected, String msg) {
		if (obtained != expected) {
			System.out.println(msg);
			System.out.println("theta-ed: " + thetaShoutEngine.getMatchedVolume()
					+ "; max: " + maxShoutEngine.getMatchedVolume() + " equ: "
					+ equShoutEngine.getMatchedVolume());
		}
		Assert.assertTrue("ThetaShoutEngine calculated a matching volume "
				+ obtained + " different from expected !" + expected + " !",
				obtained == expected);
	}

	class IntPtr {
		int count = 0;

		public IntPtr(int count) {
			this.count = count;
		}

		public void inc(int num) {
			this.count += num;
		}

		@Override
		public String toString() {
			return String.valueOf(count);
		}
	}

	protected void checkEqualShouts(ShoutEngine se1, ShoutEngine se2, String msg) {
		Iterator<Shout> itor1 = se1.matchedBidIterator();
		Iterator<Shout> itor2 = se2.matchedBidIterator();

		List<IntPtr> qs1, qs2;
		List<Integer> ps1, ps2;

		qs1 = new LinkedList<IntPtr>();
		qs2 = new LinkedList<IntPtr>();
		ps1 = new LinkedList<Integer>();
		ps2 = new LinkedList<Integer>();

		countQuantities(itor1, qs1, ps1);
		countQuantities(itor2, qs2, ps2);

		Assert.assertTrue(
				"Same number of prices expected in two matching sets !\n" + msg + "\n"
						+ qs1.toString() + "\n" + ps1.toString() + "\n---\n"
						+ qs2.toString() + "\n" + ps2.toString(), qs1.size() == qs2.size()
						&& ps1.size() == ps2.size());

		for (int i = 0; i < qs1.size(); i++) {
			Assert.assertTrue(
					"Same quantity at a price expected in two matching sets !",
					qs1.get(i).count == qs2.get(i).count);
		}
	}

	protected void countQuantities(Iterator<Shout> itor, List<IntPtr> qs,
			List<Integer> ps) {
		int price = -1;
		Shout shout;
		while (itor.hasNext()) {
			shout = itor.next();
			if ((int) shout.getPrice() != price) {
				qs.add(new IntPtr(shout.getQuantity()));
				price = (int) shout.getPrice();
				ps.add(price);
			} else {
				qs.get(qs.size() - 1).inc(shout.getQuantity());
			}
		}
	}

	public void testOrderedMatches() {

		for (int i = 0; i < 5; i++) {
			thetaShoutEngine.setTheta(randGenerator.nextDouble());
			thetaShoutEngine.setOrderly(true);
			logger.info(thetaShoutEngine.toString() + " ...");
			assertTrue("shout engine should be empty!",
					thetaShoutEngine.getMatchedVolume() == 0);
			checkOrderedMatches(true, true);
			thetaShoutEngine.reset();
		}

		for (int i = 0; i < 5; i++) {
			thetaShoutEngine.setTheta(randGenerator.nextDouble() * -0.5);
			thetaShoutEngine.setOrderly(false);
			logger.info(thetaShoutEngine.toString() + " ...");
			assertTrue("shout engine should be empty!",
					thetaShoutEngine.getMatchedVolume() == 0);
			checkOrderedMatches(false, true);
			thetaShoutEngine.reset();
		}
	}

	public static void main(final String[] args) {
		junit.textui.TestRunner.run(ThetaShoutEngineTest.suite());
	}

	public static Test suite() {
		return new TestSuite(ThetaShoutEngineTest.class);
	}
}
