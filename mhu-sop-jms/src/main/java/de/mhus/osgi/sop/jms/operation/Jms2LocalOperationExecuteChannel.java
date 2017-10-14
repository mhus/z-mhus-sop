package de.mhus.osgi.sop.jms.operation;

import java.util.LinkedList;
import java.util.List;

import org.osgi.service.component.ComponentContext;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import de.mhus.lib.core.IProperties;
import de.mhus.lib.core.MApi;
import de.mhus.lib.core.cfg.CfgString;
import de.mhus.lib.core.service.ServerIdent;
import de.mhus.lib.core.strategy.Operation;
import de.mhus.lib.core.strategy.OperationDescription;
import de.mhus.lib.core.strategy.OperationResult;
import de.mhus.lib.core.util.VersionRange;
import de.mhus.lib.errors.NotFoundException;
import de.mhus.lib.karaf.jms.JmsDataChannel;
import de.mhus.lib.karaf.jms.JmsManagerService;
import de.mhus.osgi.sop.api.jms.AbstractJmsOperationExecuteChannel;
import de.mhus.osgi.sop.api.jms.JmsApi;
import de.mhus.osgi.sop.api.jms.TicketAccessInterceptor;
import de.mhus.osgi.sop.api.operation.OperationApi;
import de.mhus.osgi.sop.api.operation.OperationDescriptor;

@Component(provide=JmsDataChannel.class,immediate=true)
public class Jms2LocalOperationExecuteChannel extends AbstractJmsOperationExecuteChannel {

	public static CfgString queueName = new CfgString(Jms2LocalOperationExecuteChannel.class, "queue", "sop.operation." + MApi.lookup(ServerIdent.class));
	static Jms2LocalOperationExecuteChannel instance;
	
	@Override
	@Activate
	public void doActivate(ComponentContext ctx) {
		super.doActivate(ctx);
		if (MApi.getCfg(Jms2LocalOperationExecuteChannel.class).getBoolean("accessControl", true))
			getServer().setInterceptorIn(new TicketAccessInterceptor());
		instance = this;
	}	
	
	@Override
	@Deactivate
	public void doDeactivate(ComponentContext ctx) {
		instance = null;
		super.doDeactivate(ctx);
	}
	
	@Override
	@Reference
	public void setJmsManagerService(JmsManagerService manager) {
		super.setJmsManagerService(manager);
	}

	@Override
	protected void doAfterReset() {
		if (getServer() != null && getServer().getInterceptorIn() == null)
			getServer().setInterceptorIn(new TicketAccessInterceptor()); // for authentication
	}

	@Override
	protected String getQueueName() {
		return  queueName.value();
	}

	@Override
	protected String getJmsConnectionName() {
		return MApi.lookup(JmsApi.class).getDefaultConnectionName();
	}

	@Override
	protected OperationResult doExecute(String path, VersionRange version, IProperties properties) throws NotFoundException {

		log().d("execute operation",path,properties);
		
		OperationApi admin = MApi.lookup(OperationApi.class);
		OperationResult res = admin.doExecute(path, version, null, properties);
		
		log().d("operation result",path,res, res == null ? "" : res.getResult());
		return res;
	}

	@Override
	protected List<String> getPublicOperations() {
		LinkedList<String> out = new LinkedList<String>();
		OperationApi admin = MApi.lookup(OperationApi.class);
		for (OperationDescriptor desc :  admin.findOperations("*", null, null)) {
			if (!JmsOperationApiImpl.PROVIDER_NAME.equals(desc.getProvider())) {
				try {
					out.add(desc.getPath() + ":" + desc.getVersionString());
				} catch (Throwable t) {
					log().d(desc,t);
				}
			}
		}
			
		return out;
	}

	@Override
	protected OperationDescriptor getOperationDescription(String path, VersionRange version) throws NotFoundException {
		OperationApi admin = MApi.lookup(OperationApi.class);
		OperationDescriptor desc = admin.findOperation(path, version, null);
		if (desc == null) return null;
		return desc;
	}

}
