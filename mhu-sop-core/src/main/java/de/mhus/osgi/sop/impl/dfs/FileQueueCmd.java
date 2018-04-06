package de.mhus.osgi.sop.impl.dfs;

import java.io.File;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import de.mhus.lib.core.M;
import de.mhus.lib.core.MApi;
import de.mhus.lib.core.MDate;
import de.mhus.lib.core.MFile;
import de.mhus.lib.core.MProperties;
import de.mhus.lib.core.MString;
import de.mhus.lib.core.MTimeInterval;
import de.mhus.lib.core.console.ConsoleTable;
import de.mhus.lib.core.util.MUri;
import de.mhus.osgi.sop.api.dfs.FileInfo;
import de.mhus.osgi.sop.api.dfs.FileQueueApi;
import de.mhus.osgi.sop.api.dfs.FileQueueOperation;

@Command(scope = "sop", name = "dfq", description = "Distributed File Queue actions")
@Service
public class FileQueueCmd implements Action {

	@Argument(index=0, name="cmd", required=true, description=
			"Command:\n"
			+ " load <uri>           - load a file from remote or local\n"
			+ " list [ident]         - list queued files\n"
			+ " touch <id> [ttl]     - touch file and extend expire time\n"
			+ " info <id>            - print file properties\n"
			+ " create <name>        - create a queued file\n"
			+ " append <id> <content as string> - append content to the file\n"
			+ " close <id>           - close the queued file, now the file is available in the network\n"
			+ " print <id>           - print the file content\n"
			+ " delete <id>          - delete the queued file\n"
			+ " copy <path to file>  - copy the file from file system into file queue\n"
			+ " move <path to file>  - move a file from file system into file queue\n"
			+ " set <id> <key=value> - change a propertie for a queue file\n"
			+ " providers            - list of available providers")
	String cmd;

	@Argument(index=1, name="parameters", required=false, description="More Parameters", multiValued=true)
    String[] parameters;

	@Option(name="-f", aliases="--full", description="Full output",required=false)
	boolean full = false;

	@Override
	public Object execute() throws Exception {
		
		FileQueueApi api = MApi.lookup(FileQueueApi.class);
		
		switch(cmd) {
		case "load": {
			File id = api.loadFile(MUri.toUri(parameters[0]));
			System.out.println(id.getName());
		} break;
		case "list": {
			ConsoleTable table = new ConsoleTable(full);

			if (parameters == null) {
				table.setHeaderValues("ID","Name","Size","Modified", "TTL", "Source");
				for (UUID id : FileQueueApiImpl.instance.getQueuedIdList(true)) {
					FileInfo info = api.getFileInfo(id);
					MProperties prop = FileQueueApiImpl.instance.getProperties(id);
					String ttl = MTimeInterval.getIntervalAsString( prop.getLong("expires", 0) - System.currentTimeMillis());
					table.addRowValues(id, info.getName(), MString.toByteDisplayString(info.getSize()), MDate.toIso8601(info.getModified()), ttl, prop.getString("source", ""));
				}
			} else {
				table.setHeaderValues("ID","Name","Size","Modified");
				FileQueueOperation operation = FileQueueApiImpl.instance.getOperation(parameters[0]);
				for (UUID id : operation.getQueuedIdList()) {
					FileInfo info = operation.getFileInfo(id);
					table.addRowValues(id, info.getName(), MString.toByteDisplayString(info.getSize()), MDate.toIso8601(info.getModified()));
				}
				
			}
			table.print(System.out);
		} break;
		case "touch": {
			UUID id = UUID.fromString(parameters[0]);
			api.touchFile(id, parameters.length > 1 ? M.c(parameters[1], 0) : 0);
			System.out.println("OK");
		} break;
		case "info": {
			UUID id = UUID.fromString(parameters[0]);
			MProperties prop = FileQueueApiImpl.instance.getProperties(id);
			for (Entry<String, Object> entry : prop.entrySet()) {
				System.out.println(entry.getKey() + "=" + entry.getValue());
			}
		} break;
		case "create": {
			UUID id = api.createQueueFile(parameters[0], 0);
			System.out.println("File Created with id: " + id);
		} break;
		case "append": {
			UUID id = UUID.fromString(parameters[0]);
			byte[] content = parameters[1].getBytes();
			long size = api.appendQueueFileContent(id, content);
			System.out.println("New file size: " + size);
		} break;
		case "close": {
			UUID id = UUID.fromString(parameters[0]);
			long size = api.closeQueueFile(id);
			System.out.println("File size: " + size);
		} break;
		case "print": {
			UUID id = UUID.fromString(parameters[0]);
			File file = api.loadFile(id);
			System.out.println( MFile.readFile(file) );
		} break;
		case "delete": {
			UUID id = UUID.fromString(parameters[0]);
			FileQueueApiImpl.instance.delete(id);
			System.out.println("OK");
		} break;
		case "copy": {
			File file = new File(parameters[0]);
			UUID id = api.takeFile(file, true, 0);
			System.out.println("Created File: " + id);
		} break;
		case "move": {
			File file = new File(parameters[0]);
			UUID id = api.takeFile(file, false, 0);
			System.out.println("Created File: " + id);
		} break;
		case "set": {
			UUID id = UUID.fromString(parameters[0]);
			String key = MString.beforeIndex(parameters[1], '=');
			String val = MString.afterIndex(parameters[1], '=');
			FileQueueApiImpl.instance.setParameter(id, key, val);
			System.out.println("OK");
		} break;
		case "providers": {
			System.out.println(" Ident");
			System.out.println("------------------------");
			for (String provider : FileQueueApiImpl.instance.listProviders()) {
				System.out.println(provider);
			}
		} break;
		default:
			System.out.println("Unknown command");
		}
		
		return null;
	}

}