/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.artefacts;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.Assert;
import step.core.deployment.JacksonMapperProvider;

public class MapDeserializerTest {

	@Test
	public void test() throws IOException {
		ObjectMapper m = JacksonMapperProvider.createMapper();
		
		TestBean b = new TestBean();
		b.getMap().put("key1", new TestBean2());
		b.getMap().put("key2", new TestBean2());
		b.getMap().put("key3", null);

		TestBean b2 = m.readValue(m.writeValueAsString(b), TestBean.class);
		Assert.assertEquals("Test", ((TestBean2)b2.getMap().get("key1")).getTest());
		Assert.assertEquals("Test", ((TestBean2)b2.getMap().get("key2")).getTest());
		Assert.assertNull(b2.getMap().get("key3"));
	}

}
