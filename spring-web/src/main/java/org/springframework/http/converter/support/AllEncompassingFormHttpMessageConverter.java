/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.converter.support;

import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.ClassUtils;

/**
 * Extension of {@link org.springframework.http.converter.FormHttpMessageConverter},
 * adding support for XML and JSON-based parts.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public class AllEncompassingFormHttpMessageConverter extends FormHttpMessageConverter {

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader());

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader()) &&
			ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader());

	private static final boolean jackson2XmlPresent =
			ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader());

	private static final boolean jackson2SmilePresent =
			ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader());

	private static final boolean gsonPresent =
			ClassUtils.isPresent("com.google.gson.Gson",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader());

	private static final boolean jsonbPresent =
			ClassUtils.isPresent("javax.json.bind.Jsonb",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader());


	/**
	 * 添加一些转化器
	 */
	public AllEncompassingFormHttpMessageConverter() {

		addPartConverter(new SourceHttpMessageConverter<>());
		//如果Classpath下面有javax.xml.bind.Binder类，
		//没有com.fasterxml.jackson.dataformat.xml.XmlMapper类的话
		//则添加Jaxb2RootElementHttpMessageConverter转换器
		if (jaxb2Present && !jackson2XmlPresent) {
			addPartConverter(new Jaxb2RootElementHttpMessageConverter());
		}
		//如果Classpath下有com.fasterxml.jackson.databind.ObjectMapper
		//和com.fasterxml.jackson.core.JsonGenerator的话，则添加
		//MappingJackson2HttpMessageConverter转换器
		if (jackson2Present) {
			addPartConverter(new MappingJackson2HttpMessageConverter());
		}
		//如果Classpath下面有com.google.gson.Gson类的话，则添加
		//GsonHttpMessageConverter转换器
		else if (gsonPresent) {
			addPartConverter(new GsonHttpMessageConverter());
		}
		//如果Classpath下有com.fasterxml.jackson.dataformat.xml.XmlMapper类的话，
		//则添加MappingJackson2XmlHttpMessageConverter转换器
		else if (jsonbPresent) {
			addPartConverter(new JsonbHttpMessageConverter());
		}

		if (jackson2XmlPresent) {
			addPartConverter(new MappingJackson2XmlHttpMessageConverter());
		}

		if (jackson2SmilePresent) {
			addPartConverter(new MappingJackson2SmileHttpMessageConverter());
		}
	}

}
