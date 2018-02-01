package de.mhus.osgi.sop.api.registry;

public class RegistryValue implements Comparable<RegistryValue>{

	private String value;
	private String source;
	private long updated;
	private String path;
	private long timeout;
	private boolean readOnly;
	private boolean persistent;
	
	public RegistryValue(String value, String source, long updated, String path, long timeout, boolean readOnly, boolean persistent) {
		this.value = value;
		this.source = source;
		this.updated = updated;
		this.path = path;
		this.timeout = timeout;
		this.readOnly = readOnly;
		this.persistent = persistent;
	}

	public String getValue() {
		return value;
	}

	public String getSource() {
		return source;
	}

	public long getUpdated() {
		return updated;
	}

	public String getPath() {
		return path;
	}

	public long getTimeout() {
		return timeout;
	}

	public boolean isReadOnly() {
		return readOnly;
	}
	
	public boolean isPersistent() {
		return persistent;
	}

	@Override
	public int compareTo(RegistryValue o) {
		return path.compareTo(o.path);
	}
	
}
