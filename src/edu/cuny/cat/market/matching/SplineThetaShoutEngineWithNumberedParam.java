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

import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.cuny.cat.comm.Message;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;
import edu.cuny.util.Utils;

/**
 * An parameterized version of {@link SplineThetaShoutEngine} where thetas are
 * enumerated as numeric values separately in the parameter database.
 * 
 * 
 * <p>
 * <b>Parameters </b>
 * 
 * <table>
 * <tr>
 * <td valign=top><i>base </i> <tt>.n</tt><br>
 * <font size=-1>3 <= int</font></td>
 * <td valign=top>the number of knot points for interpolating</td>
 * </tr>
 * 
 * <tr>
 * <td valign=top><i>base </i> <tt>.i</tt><br>
 * <font size=-1>-1<=double<=1</font></td>
 * <td valign=top>the ith knot point for interpolating</td>
 * </tr>
 * 
 * </table>
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.4 $
 */

public class SplineThetaShoutEngineWithNumberedParam extends
		SplineThetaShoutEngine {

	static Logger logger = Logger
			.getLogger(SplineThetaShoutEngineWithNumberedParam.class);

	public static final String P_NUM = "n";

	protected double paramThetas[];

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		super.setup(parameters, base);
		
		// read the theta list
		int n = parameters.getInt(base.push(P_NUM), null, 1);
		paramThetas = new double[n];
		for (int i = 0; i < paramThetas.length; i++) {
			paramThetas[i] = parameters.getDoubleWithDefault(base.push(String.valueOf(i)), null,
					-1);
		}
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (paramThetas.length == 0) {
			logger.error("Invalid theta value list in configuring "
					+ SplineThetaShoutEngineWithNumberedParam.class.getSimpleName()
					+ " !");
		} else if (paramThetas.length == 1) {
			thetas = new double[2];
			thetas[0] = thetas[1] = paramThetas[0];
		}

		thetas = Arrays.copyOf(paramThetas, paramThetas.length);
		for (int i = 0; i < thetas.length; i++) {
			if (thetas[i] < -1) {
				thetas[i] = -1;
			} else if (thetas[i] > 1) {
				thetas[i] = 1;
			}
		}
	}
	
	public void setParamThetas(double paramThetas[]) {
		this.paramThetas = paramThetas;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "\n"
				+ Utils.indent("thetas: " + paramThetas.length + " | "
						+ Message.concatenate(paramThetas));
		return s;
	}
}