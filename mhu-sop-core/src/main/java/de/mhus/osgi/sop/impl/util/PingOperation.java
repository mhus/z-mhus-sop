package de.mhus.osgi.sop.impl.util;

import aQute.bnd.annotation.component.Component;
import de.mhus.lib.core.MApi;
import de.mhus.lib.core.MSystem;
import de.mhus.lib.core.strategy.AbstractOperation;
import de.mhus.lib.core.strategy.Operation;
import de.mhus.lib.core.strategy.OperationDescription;
import de.mhus.lib.core.strategy.OperationResult;
import de.mhus.lib.core.strategy.Successful;
import de.mhus.lib.core.strategy.TaskContext;
import de.mhus.osgi.sop.api.aaa.AaaContext;
import de.mhus.osgi.sop.api.aaa.AccessApi;
import de.mhus.osgi.sop.api.util.SopUtil;

@Component(provide=Operation.class, properties="tags=acl=*")
public class PingOperation extends AbstractOperation {

	@Override
	protected OperationResult doExecute2(TaskContext context) throws Exception {
		log().i("PING PONG", context.getParameters() );
		String user = "";
		boolean admin = false;
		try {
			AccessApi aaa = MApi.lookup(AccessApi.class);
			AaaContext c = aaa.getCurrentOrGuest();
			user = c.getAccountId();
			admin = c.isAdminMode();
		} catch (Throwable t) {}
		
		String ident = SopUtil.getServerIdent();
		String pid = MSystem.getPid();
		String host = MSystem.getHostname();
		String free = MSystem.freeMemoryAsString();
		
		return new Successful(this, "ok", "user", user, "admin", ""+admin, "ident", ident, "pid", pid, "host", host, "free", free);
	}

	@Override
	protected OperationDescription createDescription() {
		return new OperationDescription(PingOperation.class, this, "Ping");
	}



}
