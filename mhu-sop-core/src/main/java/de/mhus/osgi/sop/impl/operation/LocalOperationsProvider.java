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
package de.mhus.osgi.sop.impl.operation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import de.mhus.lib.core.IProperties;
import de.mhus.lib.core.MApi;
import de.mhus.lib.core.MLog;
import de.mhus.lib.core.cfg.CfgBoolean;
import de.mhus.lib.core.strategy.DefaultTaskContext;
import de.mhus.lib.core.strategy.NotSuccessful;
import de.mhus.lib.core.strategy.Operation;
import de.mhus.lib.core.strategy.OperationDescription;
import de.mhus.lib.core.strategy.OperationResult;
import de.mhus.lib.core.util.VersionRange;
import de.mhus.lib.errors.AccessDeniedException;
import de.mhus.lib.errors.NotFoundException;
import de.mhus.osgi.sop.api.aaa.AccessApi;
import de.mhus.osgi.sop.api.jms.JmsApi;
import de.mhus.osgi.sop.api.operation.OperationAddress;
import de.mhus.osgi.sop.api.operation.OperationDescriptor;
import de.mhus.osgi.sop.api.operation.OperationException;
import de.mhus.osgi.sop.api.operation.OperationUtil;
import de.mhus.osgi.sop.api.operation.OperationsProvider;

@Component(immediate=true,provide=OperationsProvider.class,properties="provider=local")
public class LocalOperationsProvider extends MLog implements OperationsProvider {

	static final String PROVIDER_NAME = "local";
	public static final CfgBoolean RELAXED = new CfgBoolean(JmsApi.class, "aaaRelaxed", true);

	private BundleContext context;
	private ServiceTracker<Operation,Operation> nodeTracker;
	private HashMap<String, LocalOperationDescriptor> register = new HashMap<>();
	public static LocalOperationsProvider instance;

	@Activate
	public void doActivate(ComponentContext ctx) {
		context = ctx.getBundleContext();
		nodeTracker = new ServiceTracker<>(context, Operation.class, new MyServiceTrackerCustomizer() );
		nodeTracker.open(true);
		instance = this;
	}

	@Deactivate
	public void doDeactivate(ComponentContext ctx) {
		instance  = null;
		context = null;
	}

	@Reference
	public void getAccessApi(AccessApi api) {}
	
	private class MyServiceTrackerCustomizer implements ServiceTrackerCustomizer<Operation,Operation> {

		@Override
		public Operation addingService(
				ServiceReference<Operation> reference) {

			Operation service = context.getService(reference);
			if (service != null) {
				OperationDescription desc = service.getDescription();
				if (desc != null && desc.getPath() != null) {
					log().i("register",desc);
					synchronized (register) {
						LocalOperationDescriptor descriptor = createDescriptor(reference, service);
						if (register.put(desc.getPath() + ":" + desc.getVersionString(), descriptor ) != null)
							log().w("Operation already defined",desc.getPath());
					}
				} else {
					log().i("no description found, not registered",reference.getProperty("objectClass"));
				}
			}
			return service;
		}

		private LocalOperationDescriptor createDescriptor(ServiceReference<Operation> reference, Operation service) {
			TreeSet<String> tags = new TreeSet<>();
			Object tagsStr = reference.getProperty("tags");
			if (tagsStr instanceof String[]) {
				for (String item : (String[])tagsStr)
					tags.add(item);
			} else
			if (tagsStr instanceof String) {
				for (String item : ((String)tagsStr).split(";"))
					tags.add(item);
			}
			service.getDescription().getForm();
			OperationDescription desc = service.getDescription();
			
			Object tagsStr2 = desc.getParameters() == null ? null : desc.getParameters().get(OperationDescription.TAGS);
			if (tagsStr2 != null)
				for (String item : String.valueOf(tagsStr2).split(";"))
					tags.add(item);
			
			String acl = OperationUtil.getOption(tags, OperationDescriptor.TAG_DEFAULT_ACL, "");
			try {
				AccessApi aaa = MApi.lookup(AccessApi.class);
				if (aaa != null)
					acl = aaa.getResourceAccessAcl(aaa.getCurrenAccount(), "local.operation", desc.getPath(), "execute", acl);
				else
					log().w("AccessApi not found",desc,acl);
			} catch (Throwable t) {
				log().i(t);
			}
			return new LocalOperationDescriptor(OperationAddress.create(PROVIDER_NAME,desc), desc,tags, acl, service);
		}

		@Override
		public void modifiedService(
				ServiceReference<Operation> reference,
				Operation service) {

			if (service != null) {
				OperationDescription desc = service.getDescription();
				if (desc != null && desc.getPath() != null) {
					log().i("modified",desc);
					synchronized (register) {
						LocalOperationDescriptor descriptor = createDescriptor(reference, service);
						register.put(desc.getPath() + ":" + desc.getVersionString(), descriptor);
					}
				}
			}
			
		}

		@Override
		public void removedService(
				ServiceReference<Operation> reference,
				Operation service) {
			
			if (service != null) {
				OperationDescription desc = service.getDescription();
				if (desc != null && desc.getPath() != null) {
					log().i("unregister",desc);
					synchronized (register) {
						register.remove(desc.getPath() + ":" + desc.getVersionString());
					}
				}
			}			
		}
		
	}

	@Override
	public void collectOperations(List<OperationDescriptor> list, String filter, VersionRange version, Collection<String> providedTags) {
		synchronized (register) {
			for (OperationDescriptor desc : register.values()) {
				if (OperationUtil.matches(desc, filter, version, providedTags))
					list.add(desc);
			}
		}
	}

	@Override
	public OperationResult doExecute(String filter, VersionRange version, Collection<String> providedTags, IProperties properties, String ... executeOptions)
			throws NotFoundException {
		OperationDescriptor d = null;
		synchronized (register) {
			for (OperationDescriptor desc : register.values()) {
				if (OperationUtil.matches(desc, filter, version, providedTags)) {
						d = desc;
						break;
				}
			}
		}
		if (d == null) throw new NotFoundException("operation not found",filter,version,providedTags);
		return doExecute(d, properties);
	}

	@Override
	public OperationResult doExecute(OperationDescriptor desc, IProperties properties, String ... executeOptions) throws NotFoundException {
		Operation operation = null;
		if (desc instanceof LocalOperationDescriptor) {
			operation = ((LocalOperationDescriptor)desc).operation;
		}
		if (operation == null) {
			if (!PROVIDER_NAME.equals(desc.getProvider()))
				throw new NotFoundException("description is from another provider",desc);
			synchronized (register) {
				LocalOperationDescriptor local = register.get(desc.getPath() + ":" + desc.getVersionString());
				if (local != null)
					operation = local.operation;
			}
		}
		if (operation == null)
			throw new NotFoundException("operation not found", desc);
		
		AccessApi aaa = MApi.lookup(AccessApi.class);
		if (aaa != null) {
			try {
				String acl = OperationUtil.getOption(desc.getTags(), OperationDescriptor.TAG_DEFAULT_ACL, "");
				if (!aaa.hasResourceAccess(aaa.getCurrenAccount(), "local.operation", desc.getPath(), "execute", acl))
					throw new AccessDeniedException("access denied");
			} catch (Throwable t) {
				throw new AccessDeniedException("internal error", t);
			}
		} else
		if (!RELAXED.value())
			throw new AccessDeniedException("Access api not found");
		
		DefaultTaskContext taskContext = new DefaultTaskContext(getClass());
		taskContext.setParameters(properties);
		try {
			return operation.doExecute(taskContext);
		} catch (OperationException e) {
			log().w(desc,properties,e);
			return new NotSuccessful(operation,e.getMessage(), e.getCaption(), e.getReturnCode());
		} catch (Exception e) {
			log().w(desc,properties,e);
			return new NotSuccessful(operation,e.toString(), OperationResult.INTERNAL_ERROR);
		}
	}

	private class LocalOperationDescriptor extends OperationDescriptor {

		private Operation operation;

		public LocalOperationDescriptor(OperationAddress address, OperationDescription description,
				Collection<String> tags, String acl, Operation operation) {
			super(address, description, tags, acl);
			this.operation = operation;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public <T> T adaptTo(Class<T> ifc) {
			if (ifc == Operation.class) return (T) operation;
			return super.adaptTo(ifc);
		}

		
	}

	@Override
	public OperationDescriptor getOperation(OperationAddress addr) throws NotFoundException {
		synchronized (register) {
			LocalOperationDescriptor ret = register.get(addr.getPath() + ":" + addr.getVersionString());
			if (ret == null) throw new NotFoundException("operation not found", addr);
			return ret;
		}
	}

	@Override
	public void synchronize() {
		// already up to date
	}
	
}
