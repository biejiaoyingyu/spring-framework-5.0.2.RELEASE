/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.transaction.annotation;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AutoProxyRegistrar;
import org.springframework.transaction.config.TransactionManagementConfigUtils;

/**
 * Selects which implementation of {@link AbstractTransactionManagementConfiguration}
 * should be used based on the value of {@link EnableTransactionManagement#mode} on the
 * importing {@code @Configuration} class.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableTransactionManagement
 * @see ProxyTransactionManagementConfiguration
 * @see TransactionManagementConfigUtils#TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME
 *
 *
 * 事务导入的组件
 */
public class TransactionManagementConfigurationSelector extends AdviceModeImportSelector<EnableTransactionManagement> {

	/**
	 * {@inheritDoc}
	 * @return {@link ProxyTransactionManagementConfiguration} or
	 * {@code AspectJTransactionManagementConfiguration} for {@code PROXY} and
	 * {@code ASPECTJ} values of {@link EnableTransactionManagement#mode()}, respectively
	 */


	@Override
	protected String[] selectImports(AdviceMode adviceMode) {
		switch (adviceMode) {
			/**
			 * 导入两个组件
			 * AutoProxyRegistrar ----> 给容器中注册一个 InfrastructureAdvisorAutoProxyCreator 组件
			 * 							利用后置处理器机制在对象创建以后，包装对象，返回一个代理对象（增强器），
			 * 							代理对象执行方法利用拦截器链进行调用；
			 * ProxyTransactionManagementConfiguration -->1.给容器中注册事务增强器,事务增强器要用事务注解的信息，
			 * 												AnnotationTransactionAttributeSource解析事务注解
			 * 											  2.事务拦截器
			 */
			case PROXY:
				return new String[] {AutoProxyRegistrar.class.getName(), ProxyTransactionManagementConfiguration.class.getName()};
			case ASPECTJ:
				return new String[] {TransactionManagementConfigUtils.TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME};
			default:
				return null;
		}
	}

}
