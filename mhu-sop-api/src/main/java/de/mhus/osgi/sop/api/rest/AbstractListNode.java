/**
 * Copyright 2018 Mike Hummel
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
package de.mhus.osgi.sop.api.rest;

import java.util.List;

public abstract class AbstractListNode<T> extends AbstractNode<T> {

	public static final String ID 		= "_id";
	public static final String OBJECT 	= "_obj";
	public static final String SOURCE 	= "source";
	public static final String INTERNAL_PREFIX = "_";

	@Override
	public Node lookup(List<String> parts, CallContext callContext)
			throws Exception {
		if (parts.size() < 1) return this;

		String id = parts.get(0);
		parts.remove(0);
		
		T obj = getObjectForId(callContext, id);

		if (obj == null) return null;
		
		callContext.put(getManagedClass().getCanonicalName() + ID, id);
		callContext.put(getManagedClass().getCanonicalName() + OBJECT, obj);

		if (parts.size() < 1) return this;

		return callContext.lookup(parts, getNodeId());
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getObjectFromContext(CallContext callContext, Class<T> clazz) {
		return (T) callContext.get(clazz.getCanonicalName() + OBJECT);
	}

	@SuppressWarnings("unchecked")
	protected T getObjectFromContext(CallContext callContext) {
		return (T) callContext.get(getManagedClass().getCanonicalName() + OBJECT);
	}
 
	/**
	 * Return a the managed class as class
	 * @return x
	 */
	public abstract Class<T> getManagedClass();

	protected String getIdFromContext(CallContext callContext) {
		return (String) callContext.get(getManagedClass().getCanonicalName() + ID);
	}
	
	public static <T> String getIdFromContext(CallContext callContext, Class<T> clazz) {
		return (String) callContext.get(clazz.getCanonicalName() + ID);
	}
	
	protected abstract T getObjectForId(CallContext context, String id) throws Exception;

}