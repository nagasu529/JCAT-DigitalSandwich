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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.cuny.cat.market.charging.ChargingPolicy;
import edu.cuny.cat.registry.Registry;
import edu.cuny.cat.server.GameController;
import edu.cuny.stat.ReportVariable;
import edu.cuny.stat.ReportVariableNameDecoder;
import edu.cuny.stat.ReportVariableNamePattern;

/**
 * The class that allows to use patterns in {@link ReportVariable} names and
 * provides the replacements for each pattern to obtain actual variable names.
 * 
 * <p>
 * Supported patterns include <code>&lt;trader&gt</code>,
 * <code>&lt;specialist&gt</code>, <code>&lt;fee&gt</code>, and
 * <code>&lt;shout&gt</code>, which in turn represents trader's name,
 * specialist's name, fee names, and shout types.
 * </p>
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.5 $
 */

public class CatReportVariableNameDecoder implements ReportVariableNameDecoder {

	static Logger logger = Logger.getLogger(CatReportVariableNameDecoder.class);

	public static String[] TEMPLATES = { GameReport.TRADER,
			GameReport.SPECIALIST, GameReport.FEE, GameReport.SHOUT };

	static String[] SHOUTS = { "ask", "bid" };

	static String[] FEES = null;

	static {
		CatReportVariableNameDecoder.FEES = new String[ChargingPolicy.P_FEES.length];
		for (int i = 0; i < CatReportVariableNameDecoder.FEES.length; i++) {
			CatReportVariableNameDecoder.FEES[i] = GameReport.FEE
					+ ReportVariable.SEPARATOR + ChargingPolicy.P_FEES[i];
		}
	}

	protected List<ReportVariableNamePattern> patterns;

	public static CatReportVariableNameDecoder getInstance() {

		// this would lead to a decoder containing out-of-date information in unit
		// tests!
		// CatReportVariableNameDecoder decoder = Galaxy.getInstance()
		// .getDefaultTyped(CatReportVariableNameDecoder.class);
		// if (decoder == null) {
		// decoder = new CatReportVariableNameDecoder();
		// decoder.preparePatterns();
		// Galaxy.getInstance().putDefault(CatReportVariableNameDecoder.class,
		// decoder);
		// }

		CatReportVariableNameDecoder decoder = new CatReportVariableNameDecoder();
		decoder.preparePatterns();

		return decoder;
	}

	public CatReportVariableNameDecoder() {
		patterns = new ArrayList<ReportVariableNamePattern>();
	}

	@Override
	public List<ReportVariableNamePattern> patterns() {
		return patterns;
	}

	/**
	 * prepares the patterns that this decoder supports. This should be invoked
	 * after the simulation started as only until then the names of traders and
	 * specialists become available.
	 * 
	 */
	protected void preparePatterns() {

		for (int i = 0; i < TEMPLATES.length; i++) {
			ReportVariableNamePattern pattern = new ReportVariableNamePattern(
					getPattern(CatReportVariableNameDecoder.TEMPLATES[i]),
					Arrays
							.asList(getReplacements(CatReportVariableNameDecoder.TEMPLATES[i])));
			patterns.add(pattern);
		}
	}

	protected Pattern getPattern(final String template) {
		final String prefix = "<";
		final String postfix = ">";

		return Pattern.compile(prefix + template + postfix,
				Pattern.CASE_INSENSITIVE);
	}

	protected String[] getReplacements(final String template) {
		final Registry registry = GameController.getInstance().getRegistry();

		if (template.equalsIgnoreCase(GameReport.TRADER)) {
			return registry.getTraderIds();
		} else if (template.equalsIgnoreCase(GameReport.SPECIALIST)) {
			return registry.getSpecialistIds();
		} else if (template.equalsIgnoreCase(GameReport.SHOUT)) {
			return CatReportVariableNameDecoder.SHOUTS;
		} else if (template.equalsIgnoreCase(GameReport.FEE)) {
			return CatReportVariableNameDecoder.FEES;
		} else {
			CatReportVariableNameDecoder.logger
					.fatal("Unsupported template for report variable: " + template);
			return null;
		}
	}
}