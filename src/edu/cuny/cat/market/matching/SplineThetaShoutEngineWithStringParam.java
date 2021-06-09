/*
 * JCAT - TAC Market Design Competition Platform
 * Copyright (C) 2006-2010 Jinzhong Niu, Kai Cai
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

import org.apache.log4j.Logger;

import edu.cuny.cat.comm.Message;
import edu.cuny.cat.comm.MessageException;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;
import edu.cuny.util.Utils;

/**
 * An parameterized version of {@link SplineThetaShoutEngine} where thetas are
 * specified in a string in the parameter database and thetas are separated by
 * comma.
 * 
 * 
 * <p>
 * <b>Parameters </b>
 * 
 * <table>
 * <tr>
 * <td valign=top><i>base </i> <tt>.thetas</tt><br>
 * <font size=-1>a list of comma-separated double values in
 * [-1,1]("-0.5,-0.4,-0.3,0.0,0.0,0.0" by default)</font></td>
 * <td valign=top>(the matching quantity coefficients that evenly span across a
 * trading day, based on which the actual matching quantity coefficient will be
 * calculated.)</td>
 * <tr>
 * 
 * </table>
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.4 $
 */

public class SplineThetaShoutEngineWithStringParam extends
		SplineThetaShoutEngine {

	static Logger logger = Logger
			.getLogger(SplineThetaShoutEngineWithStringParam.class);

	public static final String P_THETAS = "thetas";

	/**
	 * default thetas in the format of comma-separated string.
	 */
	public static final String DEFAULT_THETAS_TEXT = "-0.5,-0.4,-0.3,0.0,0.0,0.0";

	/**
	 * string of thetas read from parameter database.
	 */
	protected String thetasText;

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		super.setup(parameters, base);
		
		final Parameter defBase = new Parameter(ThetaShoutEngine.P_DEF_BASE);

		thetasText = parameters.getStringWithDefault(
				base.push(SplineThetaShoutEngineWithStringParam.P_THETAS),
				defBase.push(SplineThetaShoutEngineWithStringParam.P_THETAS),
				SplineThetaShoutEngineWithStringParam.DEFAULT_THETAS_TEXT);

	}

	@Override
	public void initialize() {
		super.initialize();

		try {
			thetas = Message.parseDoubles(thetasText);
		} catch (MessageException e) {
			logger.error("Invalid theta value list in configuring "
					+ SplineThetaShoutEngineWithStringParam.class.getSimpleName() + " !");
			setDefaultThetas();
		}

		if (thetas.length == 0) {
			logger.error("Invalid theta value list in configuring "
					+ SplineThetaShoutEngineWithStringParam.class.getSimpleName() + " !");
			setDefaultThetas();
		} else if (thetas.length == 1) {
			double value = thetas[0];
			thetas = new double[2];
			thetas[0] = thetas[1] = value;
		}

		for (int i = 0; i < thetas.length; i++) {
			if (thetas[i] < -1) {
				thetas[i] = -1;
			} else if (thetas[i] > 1) {
				thetas[i] = 1;
			}
		}
	}

	public void setThetasText(String thetasText) {
		this.thetasText = thetasText;
	}

	protected void setDefaultThetas() {
		try {
			thetas = Message.parseDoubles(DEFAULT_THETAS_TEXT);
		} catch (MessageException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "\n" + Utils.indent(P_THETAS + ": " + thetasText);
		return s;
	}
}