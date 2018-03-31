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
package de.mhus.osgi.sop.api.operation;

public class OperationException extends Exception {

	private static final long serialVersionUID = 1L;
	private long returnCode;
	private String caption;

	public OperationException(long rc) {
		this(rc,"", null, null);
	}
	
	public OperationException(long rc, String msg) {
		this(rc, msg, null, null);
	}
	
	public OperationException(long rc, String msg, Throwable cause) {
		super(msg, cause);
		this.returnCode = rc;
	}

	public OperationException(long rc, String msg, String caption) {
		this(rc, msg, caption, null);
	}
	
	public OperationException(long rc, String msg, String caption, Throwable cause) {
		super(msg, cause);
		this.returnCode = rc;
		this.caption = caption;
	}
	
	public long getReturnCode() {
		return returnCode;
	}

	@Override
	public String toString() {
		return returnCode + " " + super.toString();
	}
	public String getCaption() {
		return caption;
	}
	
}
