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

package edu.cuny.cat.stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.Shout;
import edu.cuny.cat.core.Specialist;
import edu.cuny.cat.core.Trader;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.DayOpenedEvent;
import edu.cuny.cat.event.GameStartingEvent;
import edu.cuny.cat.event.SimulationOverEvent;
import edu.cuny.cat.event.SimulationStartedEvent;
import edu.cuny.cat.market.DuplicateShoutException;
import edu.cuny.cat.market.MaxEfficienciesCalculator;
import edu.cuny.cat.market.matching.ThetaShoutEngine;
import edu.cuny.cat.registry.Registry;
import edu.cuny.cat.server.GameController;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;
import edu.cuny.config.param.Parameterizable;
import edu.cuny.obj.Resetable;
import edu.cuny.util.io.CSVWriter;

/**
 * A report that uses {@link edu.cuny.cat.market.matching.ThetaShoutEngine} to
 * process shouts at specialists to do additional theoretical analysis.
 * 
 * The report uses {@link edu.cuny.cat.market.MaxEfficienciesCalculator} and
 * logs the calculated efficiencies to, e.g, CSV files, a database back end,
 * etc.
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.14 $
 */

public class MoreRevelationReport implements GameReport, Parameterizable,
		Resetable {

	static Logger logger = Logger.getLogger(MoreRevelationReport.class);

	protected ThetaShoutEngine globalShoutEngine;

	protected Map<String, ThetaShoutEngine> shoutEngines;

	protected Trader[] traders;

	/**
	 * The truthful shouts of all traders in the auction.
	 */
	protected ArrayList<Shout> shouts;

	protected Registry registry;

	protected CSVWriter log = null;

	protected int game;

	protected int day;

	public MoreRevelationReport() {
		shouts = new ArrayList<Shout>();
		shoutEngines = Collections
				.synchronizedMap(new HashMap<String, ThetaShoutEngine>());
		globalShoutEngine = new ThetaShoutEngine(1.0);

		registry = GameController.getInstance().getRegistry();
	}

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		log = new CSVWriter();
		log.setAutowrap(false);
		log.setAppend(false);
		log.setup(parameters, base);
		log.open();
	}

	@Override
	public void reset() {
		shouts.clear();

		final String specialistIds[] = GameController.getInstance().getRegistry()
				.getSpecialistIds();
		for (final String specialistId : specialistIds) {
			final ThetaShoutEngine shoutEngine = shoutEngines.get(specialistId);
			if (shoutEngine != null) {
				shoutEngine.reset();
			}
		}

		globalShoutEngine.reset();
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		if (event instanceof SimulationStartedEvent) {
			game = -1;
		} else if (event instanceof SimulationOverEvent) {
			log.close();
		} else if (event instanceof GameStartingEvent) {
			game++;
			final String specialistIds[] = GameController.getInstance().getRegistry()
					.getSpecialistIds();
			for (final String specialistId : specialistIds) {
				shoutEngines.put(specialistId, new ThetaShoutEngine(1.0));
			}
		} else if (event instanceof DayOpenedEvent) {
			traders = GameController.getInstance().getRegistry().getTraders();
			day = event.getDay();
		} else if (event instanceof DayClosedEvent) {
			calculate();
		}
	}

	public void calculate() {
		reset();
		simulateDirectRevelation();

		calculateIndividually();
		calculateGlobally();
	}

	/**
	 * Update the auction state with a truthful shout from each trader.
	 */
	protected void simulateDirectRevelation() {
		for (int i = 0; i < traders.length; i++) {
			final int quantity = traders[i].getEntitlement();
			final double value = traders[i].getPrivateValue();
			final boolean isBid = !traders[i].isSeller();
			final String specialistId = traders[i].getSpecialistId();
			final Shout shout = new Shout(quantity, value, isBid);
			shout.setTrader(traders[i]);
			shouts.add(shout);
			final ThetaShoutEngine shoutEngine = shoutEngines.get(specialistId);

			try {
				if (shoutEngine != null) {
					shoutEngine.newShout(shout);
					globalShoutEngine.newShout(shout);
				}
			} catch (final DuplicateShoutException e) {
				MoreRevelationReport.logger.error(e.getMessage());
				throw new Error(e);
			}
		}
	}

	/**
	 * calculates each individual specialist
	 */
	protected void calculateIndividually() {
		final Specialist specialists[] = registry.getSpecialists();

		for (final Specialist specialist2 : specialists) {

			final ThetaShoutEngine shoutEngine = shoutEngines
					.get(specialist2.getId());
			final MaxEfficienciesCalculator equilsCal = new MaxEfficienciesCalculator(
					shoutEngine);

			logEfficiencies(specialist2.getId(), equilsCal.getEfficiencies());
		}
	}

	/**
	 * calculates the global market including all the specialists
	 */
	protected void calculateGlobally() {

		final MaxEfficienciesCalculator equilsCal = new MaxEfficienciesCalculator(
				globalShoutEngine);

		logEfficiencies("global", equilsCal.getEfficiencies());
	}

	protected void logEfficiencies(String marketId, List<Double> efficiencies) {

		log.newData(game);
		log.newData(day);
		log.newData(marketId);
		for (double eff : efficiencies) {
			log.newData(eff);
		}

		log.endRecord();
		log.flush();
	}

	@Override
	public void produceUserOutput() {
		// do nothing
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
