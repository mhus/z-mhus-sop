package de.mhus.osgi.sop.impl.operation;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import de.mhus.lib.core.IProperties;
import de.mhus.lib.core.MApi;
import de.mhus.lib.core.MLog;
import de.mhus.lib.core.MThread;
import de.mhus.lib.core.MTimeInterval;
import de.mhus.lib.core.base.service.TimerFactory;
import de.mhus.lib.core.base.service.TimerIfc;
import de.mhus.lib.core.strategy.Operation;
import de.mhus.lib.core.strategy.OperationDescription;
import de.mhus.lib.core.strategy.OperationResult;
import de.mhus.lib.core.util.VersionRange;
import de.mhus.lib.errors.NotFoundException;
import de.mhus.osgi.sop.api.operation.OperationAddress;
import de.mhus.osgi.sop.api.operation.OperationApi;
import de.mhus.osgi.sop.api.operation.OperationDescriptor;
import de.mhus.osgi.sop.api.operation.OperationsProvider;

@Component(immediate=true,provide=OperationApi.class)
public class OperationApiImpl extends MLog implements OperationApi {

	private ServiceTracker<OperationsProvider,OperationsProvider> nodeTracker;
	private HashMap<String, OperationsProvider> register = new HashMap<>();
	private BundleContext context;
	public static OperationApiImpl instance;
	private TimerIfc timer;

	@Activate
	public void doActivate(ComponentContext ctx) {
		context = ctx.getBundleContext();
		nodeTracker = new ServiceTracker<>(context, OperationsProvider.class, new MyServiceTrackerCustomizer() );
		nodeTracker.open();
		instance = this;
		
		timer = MApi.lookup(TimerFactory.class).getTimer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				synchronize();
			}
			
		}, MTimeInterval.MINUTE_IN_MILLISECOUNDS); // TODO configurable and disable

	}

	@Deactivate
	public void doDeactivate(ComponentContext ctx) {
		
		if (timer != null)
			timer.cancel();
		timer = null;

		instance  = null;
		context = null;
	}

	private class MyServiceTrackerCustomizer implements ServiceTrackerCustomizer<OperationsProvider,OperationsProvider> {

		@Override
		public OperationsProvider addingService(
				ServiceReference<OperationsProvider> reference) {

			OperationsProvider service = context.getService(reference);
			if (service != null) {
				String name = String.valueOf(reference.getProperty("provider"));
				log().d("register",name);
				synchronized (register) {
					OperationsProvider o = register.put(name, service);
					if (o != null)
						log().w("Provider was already registered",name);
				}
			}
			return service;
		}

		@Override
		public void modifiedService(
				ServiceReference<OperationsProvider> reference,
				OperationsProvider service) {

			if (service != null) {
				String name = String.valueOf(reference.getProperty("provider"));
				log().i("modified",name);
				synchronized (register) {
					register.put(name,service);
				}
			}
			
		}

		@Override
		public void removedService(
				ServiceReference<OperationsProvider> reference,
				OperationsProvider service) {
			
			if (service != null) {
				String name = String.valueOf(reference.getProperty("provider"));
				log().i("unregister",name);
				synchronized (register) {
					register.remove(name);
				}
			}			
		}
		
	}

	public OperationsProvider getProvider(String name) {
		synchronized (register) {
			return register.get(name);
		}
	}
	
	public String[] getProviderNames() {
		synchronized (register) {
			return register.keySet().toArray(new String[register.size()]);
		}
	}
	
	public OperationsProvider[] getProviders() {
		synchronized (register) {
			return register.values().toArray(new OperationsProvider[register.size()]);
		}
	}
	
	@Override
	public OperationDescriptor getOperation(OperationAddress addr) throws NotFoundException {
		OperationsProvider provider = getProvider(addr.getProvider());
		return provider.getOperation(addr);
	}
	
	@Override
	public List<OperationDescriptor> findOperations(String filter, VersionRange version,
			Collection<String> providedTags) {
		LinkedList<OperationDescriptor> list = new LinkedList<>();
		for (OperationsProvider provider : getProviders())
			try {
				provider.collectOperations(list, filter, version, providedTags);
			} catch (Throwable t) {
				log().d(filter,version,providedTags,t);
			}
		return list;
	}

	@Override
	public OperationDescriptor findOperation(String filter, VersionRange version, Collection<String> providedTags) throws NotFoundException {
		LinkedList<OperationDescriptor> list = new LinkedList<>();
		for (OperationsProvider provider : getProviders()) {
			try {
				provider.collectOperations(list, filter, version, providedTags);
			} catch (Throwable t) {
				log().d(filter,version,providedTags,t);
			}
			if (list.size() > 0)
				return list.getFirst();
		}
		throw new NotFoundException("operation not found",filter,version,providedTags);
	}

	@Override
	public OperationResult doExecute(String filter, VersionRange version, Collection<String> providedTags,
			IProperties properties, String... executeOptions) throws NotFoundException {
		for (OperationsProvider provider : getProviders()) {
			try {
				return provider.doExecute(filter, version, providedTags, properties, executeOptions);
			} catch (NotFoundException nfe) {}
		}

		throw new NotFoundException("operation not found",filter,version,providedTags, executeOptions);
	}

	@Override
	public OperationResult doExecute(OperationDescriptor desc, IProperties properties, String ... executeOptions) throws NotFoundException {
		OperationsProvider provider = getProvider(desc.getProvider());
		if (provider == null) throw new NotFoundException("provider for operation not found",desc, executeOptions);
		return provider.doExecute(desc, properties);
	}

	@Override
	public void synchronize() {
		for (OperationsProvider provider : getProviders()) {
			try {
				provider.synchronize();
			} catch (Throwable e) {
				log().d(provider,e);
			}
		}
	}

}