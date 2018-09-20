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

package org.springframework.web.method.annotation;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.ServletException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for resolving method arguments from a named value.
 * Request parameters, request headers, and path variables are examples of named
 * values. Each may have a name, a required flag, and a default value.
 *
 * <p>Subclasses define how to do the following:
 * <ul>
 * <li>Obtain named value information for a method parameter
 * <li>Resolve names into argument values
 * <li>Handle missing argument values when argument values are required
 * <li>Optionally handle a resolved value
 * </ul>
 *
 * <p>A default value string can contain ${...} placeholders and Spring Expression
 * Language #{...} expressions. For this to work a
 * {@link ConfigurableBeanFactory} must be supplied to the class constructor.
 *
 * <p>A {@link WebDataBinder} is created to apply type conversion to the resolved
 * argument value if it doesn't match the method parameter type.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractNamedValueMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Nullable
	private final ConfigurableBeanFactory configurableBeanFactory;

	@Nullable
	private final BeanExpressionContext expressionContext;

	private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache = new ConcurrentHashMap<>(256);


	public AbstractNamedValueMethodArgumentResolver() {
		this.configurableBeanFactory = null;
		this.expressionContext = null;
	}

	/**
	 * @param beanFactory a bean factory to use for resolving ${...} placeholder
	 * and #{...} SpEL expressions in default values, or {@code null} if default
	 * values are not expected to contain expressions
	 */
	public AbstractNamedValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
		this.configurableBeanFactory = beanFactory;
		this.expressionContext =
				(beanFactory != null ? new BeanExpressionContext(beanFactory, new RequestScope()) : null);
	}


	/**
	 * 1. 基于 MethodParameter 构建 NameValueInfo <-- 主要有 name, defaultValue, required
	 * 2. 通过 BeanExpressionResolver(${}占位符解析器) 解析 name
	 * 3. 通过模版方法 resolveName 从 HttpServletRequest, Http Headers, URI template variables 中获取对应的属性值
	 * 4. 对 arg == null 这种情况的处理, 要么使用默认值, 若 required = true && arg == null, 则一般报出异常
	 * 5. 通过 WebDataBinder 将 arg 转换成 Methodparameter.getParameterType() 类型
	 * ----------------------------------------
	 * 子类
	 * 1. 根据 MethodParameter 创建 NameValueInfo
	 * 2. 根据 name 从 HttpServletRequest, Http Headers, URI template variables 获取属性值
	 * 3. 对 arg == null 这种情况的处理
	 * ----------------------------------------
	 * 1. SessionAttributeMethodArgumentResolver
	 *  针对 被 @SessionAttribute 修饰的参数起作用, 参数的获取一般通过 HttpServletRequest.getAttribute(name, RequestAttributes.SCOPE_SESSION)
	 * 2. RequestParamMethodArgumentResolver
	 *  针对被 @RequestParam 注解修饰, 但类型不是 Map, 或类型是 Map, 并且 @RequestParam 中指定 name, 一般通过 MultipartHttpServletRequest | HttpServletRequest 获取数据
	 * 3. RequestHeaderMethodArgumentResolver
	 *  针对 参数被 RequestHeader 注解, 并且 参数不是 Map 类型, 数据通过 HttpServletRequest.getHeaderValues(name) 获取
	 * 4. RequestAttributeMethodArgumentResolver
	 *  针对 被 @RequestAttribute 修饰的参数起作用, 参数的获取一般通过 HttpServletRequest.getAttribute(name, RequestAttributes.SCOPE_REQUEST)
	 * 5. PathVariableMethodArgumentResolver
	 *  解决被注解 @PathVariable 注释的参数 <- 这个注解对应的是 uri 中的数据, 在解析 URI 中已经进行解析好了 <- 在 RequestMappingInfoHandlerMapping.handleMatch -> getPathMatcher().extractUriTemplateVariables
	 * 6. MatrixVariableMethodArgumentResolver
	 *  针对被 @MatrixVariable 注解修饰的参数起作用,  从 HttpServletRequest 中获取去除 ; 的 URI Template Variables 获取数据
	 * 7. ExpressionValueMethodArgumentResolver
	 *  针对被 @Value 修饰, 返回 ExpressionValueNamedValueInfo
	 * 8. ServletCookieValueMethodArgumentResolver
	 *  针对被 @CookieValue 修饰, 通过 HttpServletRequest.getCookies 获取对应数据
	 *
	 * @param parameter the method parameter to resolve. This parameter must
	 * have previously been passed to {@link #supportsParameter} which must
	 * have returned {@code true}.
	 * @param mavContainer the ModelAndViewContainer for the current request
	 * @param webRequest the current request
	 * @param binderFactory a factory for creating {@link WebDataBinder} instances
	 * @return
	 * @throws Exception
	 */
	@Override
	@Nullable
	public final Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		// 创建 MethodParameter 对应的 NamedValueInfo
		NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
		MethodParameter nestedParameter = parameter.nestedIfOptional();
		// 因为此时的 name 可能还是被 ${} 符号包裹, 则通过 BeanExpressionResolver 来进行解析
		Object resolvedName = resolveStringValue(namedValueInfo.name);
		if (resolvedName == null) {
			throw new IllegalArgumentException(
					"Specified name must not resolve to null: [" + namedValueInfo.name + "]");
		}

		// 下面的数据大体通过 HttpServletRequest, Http Headers, URI template variables(URI 模版变量) 获取
		// @PathVariable     --> 通过前期对 uri 解析后得到的 decodedUriVariables 获得
		// @RequestParam     --> 通过 HttpServletRequest.getParameterValues(name) 获取
		// @RequestAttribute --> 通过 HttpServletRequest.getAttribute(name) 获取   <-- 这里的 scope 是 request
		// @RequestHeader    --> 通过 HttpServletRequest.getHeaderValues(name) 获取
		// @CookieValue      --> 通过 HttpServletRequest.getCookies() 获取
		// @SessionAttribute --> 通过 HttpServletRequest.getAttribute(name) 获取 <-- 这里的 scope 是 session
		// 通过 resolvedName 来解决参数的真实数据  <-- 模版方法
		Object arg = resolveName(resolvedName.toString(), nestedParameter, webRequest);
		if (arg == null) {
			if (namedValueInfo.defaultValue != null) {
				// 若 arg == null, 则使用 defaultValue, 这里默认值可能也是通过占位符 ${...} 来进行查找
				arg = resolveStringValue(namedValueInfo.defaultValue);
			}
			else if (namedValueInfo.required && !nestedParameter.isOptional()) {
				// 若 arg == null && defaultValue == null && 非 optional 类型的参数 则通过 handleMissingValue 来进行处理, 一般是报异常
				handleMissingValue(namedValueInfo.name, nestedParameter, webRequest);
			}
			// 对 null 值的处理 一般还是报异常
			arg = handleNullValue(namedValueInfo.name, arg, nestedParameter.getNestedParameterType());
		}
		// 若得到的数据是 "", 则还是使用默认值
		else if ("".equals(arg) && namedValueInfo.defaultValue != null) {
			// 这里的默认值有可能也是 ${} 修饰的, 所以也需要通过 BeanExpressionResolver 来进行解析
			arg = resolveStringValue(namedValueInfo.defaultValue);
		}

		if (binderFactory != null) {
			WebDataBinder binder = binderFactory.createBinder(webRequest, null, namedValueInfo.name);
			try {
				// 通过 WebDataBinder 中的 Converter 将 arg 转换成 parameter.getParameterType() 对应的类型
				// 将 arg 转换成 parameter.getParameterType() 类型, 这里就需要 SimpleTypeConverter
				arg = binder.convertIfNecessary(arg, parameter.getParameterType(), parameter);
			}
			catch (ConversionNotSupportedException ex) {
				throw new MethodArgumentConversionNotSupportedException(arg, ex.getRequiredType(),
						namedValueInfo.name, parameter, ex.getCause());
			}
			catch (TypeMismatchException ex) {
				throw new MethodArgumentTypeMismatchException(arg, ex.getRequiredType(),
						namedValueInfo.name, parameter, ex.getCause());

			}
		}
		// 这里的 handleResolvedValue 一般是空实现, 在PathVariableMethodArgumentResolver中也是存储一下数据到 HttpServletRequest中
		handleResolvedValue(arg, namedValueInfo.name, parameter, mavContainer, webRequest);

		return arg;
	}

	/**
	 * Obtain the named value for the given method parameter.
	 */
	private NamedValueInfo getNamedValueInfo(MethodParameter parameter) {
		NamedValueInfo namedValueInfo = this.namedValueInfoCache.get(parameter);
		if (namedValueInfo == null) {
			namedValueInfo = createNamedValueInfo(parameter);
			namedValueInfo = updateNamedValueInfo(parameter, namedValueInfo);
			this.namedValueInfoCache.put(parameter, namedValueInfo);
		}
		return namedValueInfo;
	}

	/**
	 * Create the {@link NamedValueInfo} object for the given method parameter. Implementations typically
	 * retrieve the method annotation by means of {@link MethodParameter#getParameterAnnotation(Class)}.
	 * @param parameter the method parameter
	 * @return the named value information
	 */
	protected abstract NamedValueInfo createNamedValueInfo(MethodParameter parameter);

	/**
	 * Create a new NamedValueInfo based on the given NamedValueInfo with sanitized values.
	 */
	private NamedValueInfo updateNamedValueInfo(MethodParameter parameter, NamedValueInfo info) {
		String name = info.name;
		if (info.name.isEmpty()) {
			name = parameter.getParameterName();
			if (name == null) {
				throw new IllegalArgumentException(
						"Name for argument type [" + parameter.getNestedParameterType().getName() +
						"] not available, and parameter name information not found in class file either.");
			}
		}
		String defaultValue = (ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
		return new NamedValueInfo(name, info.required, defaultValue);
	}

	/**
	 * Resolve the given annotation-specified value,
	 * potentially containing placeholders and expressions.
	 */
	@Nullable
	private Object resolveStringValue(String value) {
		if (this.configurableBeanFactory == null) {
			return value;
		}
		String placeholdersResolved = this.configurableBeanFactory.resolveEmbeddedValue(value);
		BeanExpressionResolver exprResolver = this.configurableBeanFactory.getBeanExpressionResolver();
		if (exprResolver == null || this.expressionContext == null) {
			return value;
		}
		return exprResolver.evaluate(placeholdersResolved, this.expressionContext);
	}

	/**
	 * Resolve the given parameter type and value name into an argument value.
	 * @param name the name of the value being resolved
	 * @param parameter the method parameter to resolve to an argument value
	 * (pre-nested in case of a {@link java.util.Optional} declaration)
	 * @param request the current request
	 * @return the resolved argument (may be {@code null})
	 * @throws Exception in case of errors
	 */
	@Nullable
	protected abstract Object resolveName(String name, MethodParameter parameter, NativeWebRequest request)
			throws Exception;

	/**
	 * Invoked when a named value is required, but {@link #resolveName(String, MethodParameter, NativeWebRequest)}
	 * returned {@code null} and there is no default value. Subclasses typically throw an exception in this case.
	 * @param name the name for the value
	 * @param parameter the method parameter
	 * @param request the current request
	 * @since 4.3
	 */
	protected void handleMissingValue(String name, MethodParameter parameter, NativeWebRequest request)
			throws Exception {

		handleMissingValue(name, parameter);
	}

	/**
	 * Invoked when a named value is required, but {@link #resolveName(String, MethodParameter, NativeWebRequest)}
	 * returned {@code null} and there is no default value. Subclasses typically throw an exception in this case.
	 * @param name the name for the value
	 * @param parameter the method parameter
	 */
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
		throw new ServletRequestBindingException("Missing argument '" + name +
				"' for method parameter of type " + parameter.getNestedParameterType().getSimpleName());
	}

	/**
	 * A {@code null} results in a {@code false} value for {@code boolean}s or an exception for other primitives.
	 */
	@Nullable
	private Object handleNullValue(String name, @Nullable Object value, Class<?> paramType) {
		if (value == null) {
			if (Boolean.TYPE.equals(paramType)) {
				return Boolean.FALSE;
			}
			else if (paramType.isPrimitive()) {
				throw new IllegalStateException("Optional " + paramType.getSimpleName() + " parameter '" + name +
						"' is present but cannot be translated into a null value due to being declared as a " +
						"primitive type. Consider declaring it as object wrapper for the corresponding primitive type.");
			}
		}
		return value;
	}

	/**
	 * Invoked after a value is resolved.
	 * @param arg the resolved argument value
	 * @param name the argument name
	 * @param parameter the argument parameter type
	 * @param mavContainer the {@link ModelAndViewContainer} (may be {@code null})
	 * @param webRequest the current request
	 */
	protected void handleResolvedValue(@Nullable Object arg, String name, MethodParameter parameter,
			@Nullable ModelAndViewContainer mavContainer, NativeWebRequest webRequest) {
	}


	/**
	 * Represents the information about a named value, including name, whether it's required and a default value.
	 */
	protected static class NamedValueInfo {

		private final String name;

		private final boolean required;

		@Nullable
		private final String defaultValue;

		public NamedValueInfo(String name, boolean required, @Nullable String defaultValue) {
			this.name = name;
			this.required = required;
			this.defaultValue = defaultValue;
		}
	}

}
