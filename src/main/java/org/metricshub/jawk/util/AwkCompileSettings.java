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

import java.util.List;

/**
 * Settings used during compilation of AWK scripts.
 */
public interface AwkCompileSettings {

	/**
	 * Script sources meta info.
	 *
	 * @return the script sources to compile
	 */
	List<ScriptSource> getScriptSources();

	/**
	 * @return {@code true} to enable additional functions
	 */
	boolean isAdditionalFunctions();

	/**
	 * @return {@code true} to enable additional type functions
	 */
	boolean isAdditionalTypeFunctions();

	/**
	 * @return {@code true} to dump the syntax tree
	 */
	boolean isDumpSyntaxTree();

	/**
	 * @return {@code true} to dump the intermediate code
	 */
	boolean isDumpIntermediateCode();

	/**
	 * @return {@code true} to write intermediate code to a file
	 */
	boolean isWriteIntermediateFile();

	/**
	 * @param defaultFileName default file name to use
	 * @return the output file name
	 */
	String getOutputFilename(String defaultFileName);
}
