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

package edu.cuny.cat.market.matching;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.log4j.Logger;

import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.GameStartingEvent;
import edu.cuny.cat.event.RoundOpenedEvent;

/**
 * An adaptive version of {@link ThetaShoutEngine} in which <code>theta</code>
 * changes according to an interpolated cubic spline. This aims to allow the
 * shout engine to adjust the level of matching dynamically within a trading
 * day.
 * 
 * This class is not based on configuration from parameter files but from
 * setters. To that end, some subclasses of this class should be used.
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.4 $
 */

public class SplineThetaShoutEngine extends ThetaShoutEngine {

	static Logger logger = Logger.getLogger(SplineThetaShoutEngine.class);

	/**
	 * thetas to generated spline interpolation.
	 */
	protected double thetas[];

	/**
	 * cubic spline function generated based on {@link #thetas}.
	 */
	protected PolynomialSplineFunction spline;

	/**
	 * 
	 * @param thetas
	 */
	public void setThetas(double thetas[]) {
		this.thetas = thetas;
	}

	/**
	 * initialize the spline interpolation function with the given number of
	 * rounds within a day.
	 * 
	 * @param dayLen number of rounds per day
	 */
	protected void setupSpline(int dayLen) {
		if (thetas != null) {
			double times[] = new double[thetas.length];
			times[0] = 0;
			times[thetas.length - 1] = dayLen - 1;
			for (int i = 1; i < thetas.length - 1; i++) {
				times[i] = (((double) dayLen - 1) * i) / (thetas.length - 1);
			}

			SplineInterpolator interpolator = new SplineInterpolator();
			spline = interpolator.interpolate(times, thetas);
		}
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		if (event instanceof GameStartingEvent) {
			setupSpline(((GameStartingEvent) event).getDayLen());
		} else if (event instanceof RoundOpenedEvent) {
			int round = ((RoundOpenedEvent) event).getRound();
			if (spline != null) {
				setTheta(spline.value(round));
			}
		}
	}

	@Override
	public String toString() {
		String s = super.toString();
		return s;
	}
}