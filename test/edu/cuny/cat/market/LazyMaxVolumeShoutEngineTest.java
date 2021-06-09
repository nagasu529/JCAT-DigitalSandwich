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

package edu.cuny.cat.market;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.Shout;
import edu.cuny.cat.market.matching.LazyMaxVolumeShoutEngine;

/**
 * @author Jinzhong Niu
 * @version $Revision: 1.7 $
 */

public class LazyMaxVolumeShoutEngineTest extends AbstractShoutEngineTest {

	protected static Logger logger = Logger
			.getLogger(LazyMaxVolumeShoutEngineTest.class);

	LazyMaxVolumeShoutEngine shoutEngine;

	public LazyMaxVolumeShoutEngineTest(final String name) {
		super(name);
	}

	@Override
	public LazyMaxVolumeShoutEngine getShoutEngine() {
		return shoutEngine;
	}

	@Override
	public void setUp() {
		super.setUp();
		shoutEngine = new LazyMaxVolumeShoutEngine();
	}

	/**
	 * 
	 */
	public void testRandom() {
		System.out.println("\n>>>>>>>>>\t " + "testRandom() \n");

		int matches = 0;

		try {

			Shout testRemoveShout = null, shout = null;

			for (int round = 0; round < 100; round++) {

				for (int num = 0; num < 100; num++) {
					shoutEngine.newShout(shout = randomShout());
					if (testRemoveShout == null && randGenerator.nextDouble() > 0.9) {
						testRemoveShout = shout;
					}
				}

				if (testRemoveShout != null) {
					shoutEngine.removeShout(testRemoveShout);
				}

				final List<Shout> matched = shoutEngine.matchShouts();
				final Iterator<Shout> i = matched.iterator();
				while (i.hasNext()) {
					matches++;
					final Shout bid = i.next();
					final Shout ask = i.next();
					Assert.assertTrue(bid.isBid());
					Assert.assertTrue(ask.isAsk());
					Assert.assertTrue(
							"bid " + bid.getPrice() + " < ask " + ask.getPrice()
									+ " in match !", bid.getPrice() >= ask.getPrice());
					// System.out.print(bid + "/" + ask + " ");
				}
				// System.out.println("");
			}

		} catch (final Exception e) {
			shoutEngine.printState();
			e.printStackTrace();
			Assert.fail();
		}

		System.out.println("Matches = " + matches);

	}

	public void testIterators() {
		checkIterators();
	}

	public void testOrderedMatches() {
		this.checkOrderedMatches(true, true);
	}

	public static void main(final String[] args) {
		junit.textui.TestRunner.run(LazyMaxVolumeShoutEngineTest.suite());
	}

	public static Test suite() {
		return new TestSuite(LazyMaxVolumeShoutEngineTest.class);
	}
}