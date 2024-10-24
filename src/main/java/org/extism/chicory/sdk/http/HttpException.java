
package org.extism.chicory.sdk.http;

import com.workoss.boot.util.exception.BootException;

/**
 * @author workoss
 */
public class HttpException extends BootException {

	public HttpException(String errcode) {
		super(errcode);

	}

	public HttpException(String errcode, String errmsg) {
		super(errcode, errmsg);
	}

	public HttpException(Throwable throwable) {
		super(throwable);
	}

	public HttpException(String errcode, Throwable throwable) {
		super(errcode, throwable);

	}

}
