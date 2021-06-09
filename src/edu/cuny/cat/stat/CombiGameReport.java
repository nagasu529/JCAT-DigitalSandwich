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

import java.util.LinkedList;
import java.util.List;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;
import edu.cuny.config.param.Parameterizable;
import edu.cuny.stat.CombiReport;

/**
 * <p>
 * A report that combines several different reports.
 * </p>
 * 
 * <p>
 * <b>Parameters</b>
 * </p>
 * <table>
 * <tr>
 * <td valign=top><i>base</i><tt>.n</tt><br>
 * <font size=-1>int &gt;= 1</font></td>
 * <td valign=top>(the number of different reports to configure)</td>
 * <tr>
 * 
 * <tr>
 * <td valign=top><i>base</i><tt>.</tt><i>n</i><br>
 * <font size=-1>name of class, inheriting {@link GameReport}</font></td>
 * <td valign=top>(the <i>n</i>th game report)</td>
 * <tr>
 * 
 * </table>
 * 
 * <p>
 * <b>Default Base</b>
 * </p>
 * <table>
 * <tr>
 * <td valign=top><tt>combi_game_report</tt></td>
 * </tr>
 * </table>
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.21 $
 */

public class CombiGameReport extends CombiReport<AuctionEvent> implements
		GameReport {

	protected List<GameReport> reports = null;

	public static final String P_DEF_BASE = "combi_game_report";

	public static final String P_NUM = "n";

	public CombiGameReport(final List<GameReport> reports) {
		this.reports = reports;
	}

	public CombiGameReport() {
		reports = new LinkedList<GameReport>();
	}

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {

		final Parameter defBase = new Parameter(CombiGameReport.P_DEF_BASE);

		final int numLoggers = parameters.getIntWithDefault(
				base.push(CombiGameReport.P_NUM), defBase.push(CombiGameReport.P_NUM),
				0);

		for (int i = 0; i < numLoggers; i++) {
			final GameReport report = parameters.getInstanceForParameter(
					base.push(i + ""), defBase.push(i + ""), GameReport.class);
			if (report instanceof Parameterizable) {
				((Parameterizable) report).setup(parameters, base.push(i + ""));
			}
			addReport(report);
		}
	}

//	public Map<ReportVariable, ?> getVariables() {
//		final Map<ReportVariable, Object> variableMap = new HashMap<ReportVariable, Object>();
//		final Iterator<GameReport> i = reports.iterator();
//		while (i.hasNext()) {
//			final GameReport report = i.next();
//			variableMap.putAll(report.getVariables());
//		}
//		return variableMap;
//	}
}