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

package edu.cuny.cat.stat;

import org.apache.log4j.Logger;

import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayStatPassEvent;
import edu.cuny.cat.event.GameStartingEvent;
import edu.cuny.cat.event.SimulationOverEvent;
import edu.cuny.cat.event.SimulationStartedEvent;
import edu.cuny.stat.AbstractReportVariableWriterReport;
import edu.cuny.util.io.CSVWriter;

/**
 * <p>
 * This class writes values of specified report variables to the specified
 * <code>DataWriter</code> objects, and thus can be used to log data to, e.g., CSV
 * files, a database back end, etc.
 * </p>
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.26 $
 */

public class ReportVariableWriterReport extends
		AbstractReportVariableWriterReport<AuctionEvent> implements GameReport {

	static Logger logger = Logger.getLogger(ReportVariableWriterReport.class);

	protected int gameNum;

	protected int day;

	public ReportVariableWriterReport() {
	}

	public ReportVariableWriterReport(final CSVWriter log) {
		super(log);
	}

	@Override
	protected void generateHeader() {
		
		final String headers[] = { "game", "day" };

		for (final String header : headers) {
			log.newData(header);
		}

		super.generateHeader();
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {

		if (event instanceof SimulationStartedEvent) {
			applyDecoder(CatReportVariableNameDecoder.getInstance());

			gameNum = -1;
			generateHeader();
			super.closeRecord();
		} else if (event instanceof GameStartingEvent) {
			gameNum++;
		} else if (event instanceof DayStatPassEvent) {
			if (((DayStatPassEvent) event).getPass() == DayStatPassEvent.THIRD_PASS) {
				day = event.getDay();
				updateData();
				super.closeRecord();
			}
		} else if (event instanceof SimulationOverEvent) {
			log.close();
		}
	}

	@Override
	protected void updateData() {

		log.newData(gameNum);
		log.newData(day);

		super.updateData();
	}

	@Override
	public void produceUserOutput() {
	}
}