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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Convenient superclass for application objects that want to be aware of
 * the application context, e.g. for custom lookup of collaborating beans
 * or for context-specific resource access. It saves the application
 * context reference and provides an initialization callback method.
 * Furthermore, it offers numerous convenience methods for message lookup.
 *
 * <p>There is no requirement to subclass this class: It just makes things
 * a little easier if you need access to the context, e.g. for access to
 * file resources or to the message source. Note that many application
 * objects do not need to be aware of the application context at all,
 * as they can receive collaborating beans via bean references.
 * <p>Many framework classes are derived from this class, particularly
 * within the web support.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.context.support.WebApplicationObjectSupport
 */

/**
 * spring ioc容器初始化的过程
 * 1、web应用程序启动时，tomcat会读取web.xml文件中的context-parm（含有配置文件的路径）和listener节点，
 * 		接着会为应用程序创建一个ServletContext，为全局共享，Spring ioc容器就是存储在这里
 * 2、tomcat将context-param节点转换为键值对，写入到ServletContext中
 * 3、创建listener节点中的ContextLoaderListener实例，调用该实例，初始化webapplicationContext，
 * 		这是一个接口，其实现类为XmlWebApplicationContext（即spring的IOC容器），
 * 		其通过ServletContext.getinitialParameter（"contextConfigLoaction"）从ServletContext中获取
 * 		context-param中的值（即spring ioc容器配置文件的路径），这就是为什么要有第二步的原因。接着根据配置文件
 * 		的路径加载配置文件信息（其中含有Bean的配置信息）到WebApplicationContext（即spring ioc容器）中，
 * 		将WebApplicationContext以WebApplicationContext.ROOTWEBAPPLICATIONCONTEXTATTRIBUTE为属性Key，
 * 		将其存储到ServletContext中，便于获取。至此，spring ioc容器初始化完毕
 * 4、容器初始化web.xml中配置的servlet，为其初始化自己的上下文信息servletContext，并加载其设置的配置信息到
 *  	该上下文中。将WebApplicationContext（即spring ioc容器）设置为它的父容器。其中便有SpringMVC（假设配
 *  	置了SpringMVC），这就是为什么spring ioc是springmvc ioc的父容器的原因
 *
 * 5、SpringMVC通过web.xml文件中servlet标签下的DispatcherServlet类完成自身的初始化
 */
public abstract class ApplicationObjectSupport implements ApplicationContextAware {

	/** Logger that is available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** ApplicationContext this object runs in */
	@Nullable
	private ApplicationContext applicationContext;

	/** MessageSourceAccessor for easy message access */
	@Nullable
	private MessageSourceAccessor messageSourceAccessor;


	@Override
	public final void setApplicationContext(@Nullable ApplicationContext context) throws BeansException {
		if (context == null && !isContextRequired()) {
			// Reset internal context state.
			this.applicationContext = null;
			this.messageSourceAccessor = null;
		}
		else if (this.applicationContext == null) {
			// Initialize with passed-in context.
			if (!requiredContextClass().isInstance(context)) {
				throw new ApplicationContextException(
						"Invalid application context: needs to be of type [" + requiredContextClass().getName() + "]");
			}
			this.applicationContext = context;
			this.messageSourceAccessor = new MessageSourceAccessor(context);
			initApplicationContext(context);
		}
		else {
			// Ignore reinitialization if same context passed in.
			if (this.applicationContext != context) {
				throw new ApplicationContextException(
						"Cannot reinitialize with different application context: current one is [" +
						this.applicationContext + "], passed-in one is [" + context + "]");
			}
		}
	}

	/**
	 * Determine whether this application object needs to run in an ApplicationContext.
	 * <p>Default is "false". Can be overridden to enforce running in a context
	 * (i.e. to throw IllegalStateException on accessors if outside a context).
	 * @see #getApplicationContext
	 * @see #getMessageSourceAccessor
	 */
	protected boolean isContextRequired() {
		return false;
	}

	/**
	 * Determine the context class that any context passed to
	 * {@code setApplicationContext} must be an instance of.
	 * Can be overridden in subclasses.
	 * @see #setApplicationContext
	 */
	protected Class<?> requiredContextClass() {
		return ApplicationContext.class;
	}

	/**
	 * Subclasses can override this for custom initialization behavior.
	 * Gets called by {@code setApplicationContext} after setting the context instance.
	 * <p>Note: Does </i>not</i> get called on reinitialization of the context
	 * but rather just on first initialization of this object's context reference.
	 * <p>The default implementation calls the overloaded {@link #initApplicationContext()}
	 * method without ApplicationContext reference.
	 * @param context the containing ApplicationContext
	 * @throws ApplicationContextException in case of initialization errors
	 * @throws BeansException if thrown by ApplicationContext methods
	 * @see #setApplicationContext
	 */
	protected void initApplicationContext(ApplicationContext context) throws BeansException {
		initApplicationContext();
	}

	/**
	 * Subclasses can override this for custom initialization behavior.
	 * <p>The default implementation is empty. Called by
	 * {@link #initApplicationContext(org.springframework.context.ApplicationContext)}.
	 * @throws ApplicationContextException in case of initialization errors
	 * @throws BeansException if thrown by ApplicationContext methods
	 * @see #setApplicationContext
	 */
	protected void initApplicationContext() throws BeansException {
	}


	/**
	 * Return the ApplicationContext that this object is associated with.
	 * @throws IllegalStateException if not running in an ApplicationContext
	 */
	@Nullable
	public final ApplicationContext getApplicationContext() throws IllegalStateException {
		if (this.applicationContext == null && isContextRequired()) {
			throw new IllegalStateException(
					"ApplicationObjectSupport instance [" + this + "] does not run in an ApplicationContext");
		}
		return this.applicationContext;
	}

	/**
	 * Obtain the ApplicationContext for actual use.
	 * @return the ApplicationContext (never {@code null})
	 * @throws IllegalStateException in case of no ApplicationContext set
	 * @since 5.0
	 */
	protected final ApplicationContext obtainApplicationContext() {
		ApplicationContext applicationContext = getApplicationContext();
		Assert.state(applicationContext != null, "No ApplicationContext");
		return applicationContext;
	}

	/**
	 * Return a MessageSourceAccessor for the application context
	 * used by this object, for easy message access.
	 * @throws IllegalStateException if not running in an ApplicationContext
	 */
	@Nullable
	protected final MessageSourceAccessor getMessageSourceAccessor() throws IllegalStateException {
		if (this.messageSourceAccessor == null && isContextRequired()) {
			throw new IllegalStateException(
					"ApplicationObjectSupport instance [" + this + "] does not run in an ApplicationContext");
		}
		return this.messageSourceAccessor;
	}

}
