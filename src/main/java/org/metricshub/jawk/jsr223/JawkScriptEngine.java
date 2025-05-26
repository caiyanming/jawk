package org.metricshub.jawk.jsr223;

import java.io.ByteArrayInputStream;
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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.metricshub.jawk.Awk;
import org.metricshub.jawk.ExitException;
import org.metricshub.jawk.util.AwkSettings;
import org.metricshub.jawk.util.ScriptSource;

/** Simple JSR-223 script engine for Jawk. */
public class JawkScriptEngine extends AbstractScriptEngine {

	private final ScriptEngineFactory factory;

	public JawkScriptEngine(ScriptEngineFactory factory) {
		this.factory = factory;
	}

	@Override
	public Object eval(Reader scriptReader, ScriptContext context) throws ScriptException {
		try {
			AwkSettings settings = new AwkSettings();
			Object inObj = context.getAttribute("input");
			if (inObj instanceof InputStream) {
				settings.setInput((InputStream) inObj);
			} else if (inObj instanceof String) {
				settings.setInput(new ByteArrayInputStream(((String) inObj).getBytes(StandardCharsets.UTF_8)));
			}
			settings.setDefaultRS("\n");
			settings.setDefaultORS("\n");
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			settings.setOutputStream(new PrintStream(result));
			settings.addScriptSource(new ScriptSource(ScriptSource.DESCRIPTION_COMMAND_LINE_SCRIPT, scriptReader, false));
			Awk awk = new Awk();
			awk.invoke(settings);
			String out = result.toString(StandardCharsets.UTF_8);
			Writer writer = context.getWriter();
			if (writer != null) {
				writer.write(out);
				writer.flush();
			}
			return out;
		} catch (ExitException e) {
			if (e.getCode() != 0) {
				throw new ScriptException(e);
			}
			return "";
		} catch (Exception e) {
			throw new ScriptException(e);
		}
	}

	@Override
	public Object eval(String script, ScriptContext context) throws ScriptException {
		return eval(new StringReader(script), context);
	}

	@Override
	public Bindings createBindings() {
		return new SimpleBindings();
	}

	@Override
	public ScriptEngineFactory getFactory() {
		return factory;
	}
}
