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

/**
 * Use this super class to implement a node without data. The node only implements
 * actions.
 * 
 * @author mikehummel
 *
 */
public abstract class VoidNode extends SingleObjectNode<Void>  {

	@Override
	public Node lookup(List<String> parts, CallContext callContext)
			throws Exception {
		
		if (parts.size() < 1) return this;
		return callContext.lookup(parts, getNodeId());
	}
	
	@Override
	public final Class<Void> getManagedClass() {
		return Void.class;
	}

	@Override
	protected final Void getObject(CallContext context) throws Exception {
		return null;
	}

}