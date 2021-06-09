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
import edu.cuny.cat.event.GameStartingEvent;
import edu.cuny.cat.event.RoundOpenedEvent;
import edu.cuny.cat.market.matching.FourHeapShoutEngine;
import edu.cuny.cat.market.matching.LazyMaxVolumeShoutEngine;
import edu.cuny.cat.market.matching.ShoutEngine;
import edu.cuny.cat.market.matching.SplineThetaShoutEngine;
import edu.cuny.cat.market.matching.SplineThetaShoutEngineWithNumberedParam;
import edu.cuny.cat.market.matching.SplineThetaShoutEngineWithStringParam;
import edu.cuny.math.MathUtil;
import edu.cuny.util.Utils;

/**
 * @author Jinzhong Niu
 * @version $Revision: 1.6 $
 */

public class SplineThetaShoutEngineTest extends AbstractShoutEngineTest {

	static Logger logger = Logger.getLogger(SplineThetaShoutEngineTest.class);

	SplineThetaShoutEngine stShoutEngine;

	SplineThetaShoutEngineWithNumberedParam stnpShoutEngine;

	SplineThetaShoutEngineWithStringParam stspShoutEngine;

	FourHeapShoutEngine equShoutEngine;

	LazyMaxVolumeShoutEngine maxShoutEngine;

	public SplineThetaShoutEngineTest(final String name) {
		super(name);
	}

	@Override
	public SplineThetaShoutEngine getShoutEngine() {
		return stShoutEngine;
	}

	@Override
	public void setUp() {
		super.setUp();
		stShoutEngine = new SplineThetaShoutEngine();

		stnpShoutEngine = new SplineThetaShoutEngineWithNumberedParam();

		stspShoutEngine = new SplineThetaShoutEngineWithStringParam();

		equShoutEngine = new FourHeapShoutEngine();

		maxShoutEngine = new LazyMaxVolumeShoutEngine();
	}

	/**
	 * test with identical thetas in SplineThetaShoutEngine.
	 */
	public void testIdenticalSplineThetas() {
		System.out.println("\n>>>>>>>>>\t " + "testIdenticalSplineThetas() \n");
		try {

			for (int round = 0; round < 10; round++) {
				double theta = randGenerator.nextDouble() * 2 - 1;
				int dayLen = randGenerator.nextInt(5) + 10;

				double thetas[] = Utils.newDuplicateArray(theta, 5);

				stShoutEngine.setThetas(thetas);
				GameStartingEvent gsEvent = new GameStartingEvent();
				gsEvent.setDayLen(dayLen);
				stShoutEngine.eventOccurred(gsEvent);

				for (int i = 0; i < dayLen; i++) {
					RoundOpenedEvent roEvent = new RoundOpenedEvent();
					roEvent.setTime(new int[] { 0, i, -1 });
					stShoutEngine.eventOccurred(roEvent);

					checkEquals("Unexpected theta value: " + stShoutEngine.getTheta()
							+ ".", theta, stShoutEngine.getTheta(), MathUtil.DEFAULT_ERROR);
				}

				logger.info("round " + round + " okay");

			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/**
	 * test with identical thetas for parameterized SplineThetaShoutEngine
	 * variants.
	 */
	public void testIdenticalParamThetas() {
		System.out.println("\n>>>>>>>>>\t " + "testIdenticalParamThetas() \n");
		try {

			for (int round = 0; round < 10; round++) {
				double theta = randGenerator.nextDouble() * 2 - 1;
				int dayLen = randGenerator.nextInt(5) + 10;

				double thetas[] = Utils.newDuplicateArray(theta, 5);

				stnpShoutEngine.setParamThetas(thetas);
				stnpShoutEngine.initialize();
				stspShoutEngine.setThetasText(Utils.concatenate(thetas));
				stspShoutEngine.initialize();
				GameStartingEvent gsEvent = new GameStartingEvent();
				gsEvent.setDayLen(dayLen);
				stnpShoutEngine.eventOccurred(gsEvent);
				stspShoutEngine.eventOccurred(gsEvent);

				for (int i = 0; i < dayLen; i++) {
					RoundOpenedEvent roEvent = new RoundOpenedEvent();
					roEvent.setTime(new int[] { 0, i, -1 });
					stnpShoutEngine.eventOccurred(roEvent);
					stspShoutEngine.eventOccurred(roEvent);

					checkEquals("Unexpected theta value: " + stnpShoutEngine.getTheta()
							+ ".", theta, stnpShoutEngine.getTheta(), MathUtil.DEFAULT_ERROR);
					checkEquals("Unexpected theta value: " + stspShoutEngine.getTheta()
							+ ".", theta, stspShoutEngine.getTheta(), MathUtil.DEFAULT_ERROR);
				}

				logger.info("round " + round + " okay");

			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/**
	 * 
	 */
	public void testSplineShoutEngines() {
		System.out.println("\n>>>>>>>>>\t " + "testSplineShoutEngines() \n");
		try {

			for (int round = 0; round < 10; round++) {

				// initialize thetas
				int num = randGenerator.nextInt(5) + 3;
				double thetas[] = new double[num];
				for (int i = 0; i < num; i++) {
					thetas[i] = randGenerator.nextDouble() * 2 - 1;
				}
				stShoutEngine.setThetas(thetas);

				// to mimic game starting event so as to set up spline
				int dayLen = randGenerator.nextInt(5) + 10;
				GameStartingEvent gsEvent = new GameStartingEvent();
				gsEvent.setDayLen(dayLen);
				stShoutEngine.eventOccurred(gsEvent);

				// test matching volumes
				for (int i = 0; i < dayLen; i++) {
					RoundOpenedEvent roEvent = new RoundOpenedEvent();
					roEvent.setTime(new int[] { 0, i, -1 });
					stShoutEngine.eventOccurred(roEvent);

					stShoutEngine.reset();
					equShoutEngine.reset();
					maxShoutEngine.reset();
					for (int s = 0; s < 100; s++) {
						Shout shout = randomShout();
						stShoutEngine.newShout((Shout) shout.clone());
						equShoutEngine.newShout((Shout) shout.clone());
						maxShoutEngine.newShout((Shout) shout.clone());
					}

					double theta = stShoutEngine.getTheta();

					int volume = 0;
					if (theta >= 0) {
						volume = (int) Math.round((1 - theta)
								* equShoutEngine.getMatchedVolume() + theta
								* maxShoutEngine.getMatchedVolume());
					} else {
						volume = (int) Math.round((1 + theta)
								* equShoutEngine.getMatchedVolume());
					}

					logger.info("testing matching volume in shout engine with thetas "
							+ Utils.concatenate(thetas) + ": " + volume);

					checkEquals(
							"Unexpected matching volume: " + volume + " [0, "
									+ equShoutEngine.getMatchedVolume() + ", "
									+ maxShoutEngine.getMatchedVolume() + "].", volume,
							stShoutEngine.getMatchedVolume());

				}

				logger.info("round " + round + " okay");

			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/**
	 * 
	 */
	// public void testEquivalence() {
	// System.out.println("\n>>>>>>>>>\t " + "testEquivalence() \n");
	//
	// Shout shout = null;
	//
	// try {
	//
	// for (int round = 0; round < 10; round++) {
	//
	// for (int num = 0; num < 100; num++) {
	// shout = randomShout();
	// equShoutEngine.newShout((Shout) shout.clone());
	// maxShoutEngine.newShout((Shout) shout.clone());
	// stShoutEngine.newShout(shout);
	// }
	//
	// int maxQ = maxShoutEngine.getMatchedVolume();
	// int equQ = equShoutEngine.getMatchedVolume();
	//
	//
	// System.out.println("round: " + round + " okay");
	// }
	//
	// } catch (final Exception e) {
	// e.printStackTrace();
	// Assert.fail();
	// }
	// }

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

	public static void main(final String[] args) {
		junit.textui.TestRunner.run(SplineThetaShoutEngineTest.suite());
	}

	public static Test suite() {
		return new TestSuite(SplineThetaShoutEngineTest.class);
	}
}
