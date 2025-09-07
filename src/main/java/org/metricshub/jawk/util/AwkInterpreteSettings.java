package org.metricshub.jawk.util;

/*-
 * ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲
 * Jawk
 * ჻჻჻჻჻჻
 * Copyright (C) 2006 - 2025 MetricsHub
 * ჻჻჻჻჻჻
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * ╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱
 */

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Settings used during interpretation of AWK tuples.
 */
public interface AwkInterpreteSettings {

	InputStream getInput();

	Map<String, Object> getVariables();

	List<String> getNameValueOrFileNames();

	String getFieldSeparator();

	boolean isUseSortedArrayKeys();

	boolean isCatchIllegalFormatExceptions();

	boolean isAdditionalFunctions();

	PrintStream getOutputStream();

	Locale getLocale();

	String getDefaultRS();

	String getDefaultORS();
}
