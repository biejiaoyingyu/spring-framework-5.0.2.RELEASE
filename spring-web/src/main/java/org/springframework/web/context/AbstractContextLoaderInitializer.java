/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.lang.Nullable;
import org.springframework.web.WebApplicationInitializer;

/**
 * Convenient base class for {@link WebApplicationInitializer} implementations
 * that register a {@link ContextLoaderListener} in the servlet context.
 *
 * <p>The only method required to be implemented by subclasses is
 * {@link #createRootApplicationContext()}, which gets invoked from
 * {@link #registerContextLoaderListener(ServletContext)}.
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.2
 *
 * 创建根容器
 */
public abstract class AbstractContextLoaderInitializer implements WebApplicationInitializer {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * 首先创建根容器，注册父容器
	 * 多态
	 * @param servletContext the {@code ServletContext} to initialize
	 * @throws ServletException
	 */
	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		registerContextLoaderListener(servletContext);
	}

	/**
	 * Register a {@link ContextLoaderListener} against the given servlet context. The
	 * {@code ContextLoaderListener} is initialized with the application context returned
	 * from the {@link #createRootApplicationContext()} template method.
	 * @param servletContext the servlet context to register the listener against
	 *
	 * ServletContextListener 是 Servlet 中比较重要的一个接口:监听 Servlet 容器的启动和销毁事件.
	 * 所以在 ContextLoaderListener 中：contextInitialized ：参数为所要监听的ServletContextEvent，
	 * 也就是Tomcat启动加载完web.xml会产生的事件，ServletContextEvent 持有从web.xml加载的初始化
	 * 配置的 ServletContext 上下文contextDestroyed ：在Tomcat关闭的时候执行该方法
	 *
	 * 当Servlet容器启动事件发生时，将被ContextLoaderLister 监听。此时 ContextLoaderListener
	 * 会调用实现 ServletContextListener 接口后实现的 contextInitialized 方法，并把在web.xml加
	 * 载初始化后获取的 ServletContext 传入initWebApplicationContext方法中进行IoC容器的初始化
	 *
	 */
	protected void registerContextLoaderListener(ServletContext servletContext) {
		WebApplicationContext rootAppContext = createRootApplicationContext();
		if (rootAppContext != null) {
			//看一下这个的初始化过程，监听tomcat启动
			//调用public void contextInitialized(ServletContextEvent event)
			ContextLoaderListener listener = new ContextLoaderListener(rootAppContext);
			listener.setContextInitializers(getRootApplicationContextInitializers());
			servletContext.addListener(listener);
		}
		else {
			logger.debug("No ContextLoaderListener registered, as " +
					"createRootApplicationContext() did not return an application context");
		}
	}

	/**
	 * Create the "<strong>root</strong>" application context to be provided to the
	 * {@code ContextLoaderListener}.
	 * <p>The returned context is delegated to
	 * {@link ContextLoaderListener#ContextLoaderListener(WebApplicationContext)} and will
	 * be established as the parent context for any {@code DispatcherServlet} application
	 * contexts. As such, it typically contains middle-tier services, data sources, etc.
	 * @return the root application context, or {@code null} if a root context is not
	 * desired
	 * @see org.springframework.web.servlet.support.AbstractDispatcherServletInitializer
	 */
	@Nullable
	protected abstract WebApplicationContext createRootApplicationContext();

	/**
	 * Specify application context initializers to be applied to the root application
	 * context that the {@code ContextLoaderListener} is being created with.
	 * @since 4.2
	 * @see #createRootApplicationContext()
	 * @see ContextLoaderListener#setContextInitializers
	 */
	@Nullable
	protected ApplicationContextInitializer<?>[] getRootApplicationContextInitializers() {
		return null;
	}

}
