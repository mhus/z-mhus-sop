package de.mhus.osgi.sop.jms.operation;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;

import org.osgi.service.component.ComponentContext;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import de.mhus.lib.core.MCollection;
import de.mhus.lib.core.strategy.OperationDescription;
import de.mhus.lib.jms.JmsChannel;
import de.mhus.lib.jms.JmsDestination;
import de.mhus.lib.jms.ServerJms;
import de.mhus.lib.karaf.jms.AbstractJmsDataChannel;
import de.mhus.lib.karaf.jms.JmsDataChannel;
import de.mhus.osgi.sop.api.jms.JmsApi;
import de.mhus.osgi.sop.api.operation.OperationAddress;
import de.mhus.osgi.sop.jms.operation.JmsApiImpl.JmsOperationDescriptor;

@Component(immediate=true,provide=JmsDataChannel.class)
public class JmsRegisterServer extends AbstractJmsDataChannel {

	private JmsApi jmsApi;

	@Override
	@Activate
	public void doActivate(ComponentContext ctx) {
		super.doActivate(ctx);
	}

	@Override
	@Deactivate
	public void doDeactivate(ComponentContext ctx) {
		super.doDeactivate(ctx);
	}

	@Reference
	public void setJmsApi(JmsApi api) {
		this.jmsApi = api;
	}

	@Override
	protected JmsChannel createChannel() {
		return new ServerJms(new JmsDestination(JmsApi.REGISTRY_TOPIC, true)) {
			
			@Override
			public void receivedOneWay(Message msg) throws JMSException {
				if (msg instanceof MapMessage && 
					!Jms2LocalOperationExecuteChannel.queueName.value().equals(msg.getStringProperty("queue")) // do not process my own messages
				   ) {
					
					MapMessage m = (MapMessage)msg;
					String type = m.getStringProperty("type");
					if ("request".equals(type) ) {
						JmsApiImpl.instance.lastRegistryRequest = System.currentTimeMillis();
						jmsApi.sendLocalOperations();
					}
					if ("operations".equals(type)) {
						String queue = m.getStringProperty("queue");
						String connection = jmsApi.getDefaultConnectionName(); //TODO
						int cnt = 0;
						String path = null;
						synchronized (JmsApiImpl.instance.register) {
							long now = System.currentTimeMillis();
							while (m.getString("operation" + cnt) != null) {
								path = m.getString("operation" + cnt);
								String version = m.getString("version" + cnt);
								String tags = m.getString("tags" + cnt);
								String title = m.getString("title" + cnt);
								String form = m.getString("form" + cnt);
								cnt++;
								String ident = connection + "," + queue + "," + path + "," + version;
								JmsOperationDescriptor desc = JmsApiImpl.instance.register.get(ident);
								if (desc == null) {
									OperationAddress a = new OperationAddress(JmsOperationProvider.PROVIDER_NAME + "://" + path + ":" + version + "/" + queue + "/" + connection);
									OperationDescription d = new OperationDescription(a.getGroup(),a.getName(),a.getVersion(),null,title);
									// TODO transform string to form
									// d.setForm(form);
									desc = new JmsOperationDescriptor(a,d,tags == null ? null : MCollection.toList(tags.split(",")));
									JmsApiImpl.instance.register.put(ident, desc);
								}
								desc.setLastUpdated();
							}
							// remove stare
							JmsApiImpl.instance.register.entrySet().removeIf(
									entry -> entry.getValue().getAddress().getPart(0).equals(queue) && 
											 entry.getValue().getLastUpdated() < now
									);
						}
					}
				}
			}
			
			@Override
			public Message received(Message msg) throws JMSException {
				receivedOneWay(msg);
				return null;
			}
		};
	}

	@Override
	protected String getJmsConnectionName() {
		return jmsApi.getDefaultConnectionName();
	}

}
