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

package edu.cuny.cat.market.core;

import org.apache.log4j.Logger;

/**
 * A class that extends {@link SpecialistInfo} and includes even more
 * information about a specialist itself so that the specialist is able to make
 * decisions.
 * 
 * This class was added mainly to address the need of AstonCat-Plus, which
 * calculates and stores additional information for its policies.
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.88 $
 * 
 */

public class ExtendedSpecialistInfo extends SpecialistInfo {

	static Logger logger = Logger.getLogger(ExtendedSpecialistInfo.class);

	public ExtendedSpecialistInfo(final String id) {
		super(id);
	}

	@Override
	public void dayOpening() {
		super.dayOpening();
	}
}
