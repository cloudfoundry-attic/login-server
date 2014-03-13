package org.cloudfoundry.identity.uaa.login;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.MultiValueMap;

public class AutologinRequestConverter extends AbstractHttpMessageConverter<AutologinRequest> {

	private FormHttpMessageConverter converter = new FormHttpMessageConverter();

	public AutologinRequestConverter() {
		setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_FORM_URLENCODED));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return AutologinRequest.class.isAssignableFrom(clazz);
	}

	@Override
	protected AutologinRequest readInternal(Class<? extends AutologinRequest> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		MultiValueMap<String, String> map = converter.read(null, inputMessage);
		String username = map.getFirst("username");
		String password = map.getFirst("password");
		AutologinRequest result = new AutologinRequest();
		result.setUsername(username);
		result.setPassword(password);
		return result;
	}

	@Override
	protected void writeInternal(AutologinRequest t, HttpOutputMessage outputMessage) throws IOException,
			HttpMessageNotWritableException {
		MultiValueMap<String, String> map = new LinkedMaskingMultiValueMap<String, String>("password");
		if (t.getUsername() != null) {
			map.set("username", t.getUsername());
		}
		if (t.getPassword() != null) {
			map.set("password", t.getPassword());
		}
		converter.write(map, MediaType.APPLICATION_FORM_URLENCODED, outputMessage);
	}
}