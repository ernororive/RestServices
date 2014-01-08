package restservices.publish;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import restservices.RestServices;
import restservices.util.DataWriter;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.ISession;
import com.mendix.systemwideinterfaces.core.IUser;
import communitycommons.StringUtils;

public class RestServiceRequest {
	public static enum ContentType { JSON, XML, HTML }

	HttpServletRequest request;
	HttpServletResponse response;
	private ContentType contentType = ContentType.JSON;
	private IContext context;
	protected DataWriter datawriter;
	private boolean autoLogout;
	private ISession activeSession;

	public RestServiceRequest(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
		
		this.contentType = determineContentType(request);
		setResponseContentType(response, contentType);

		try {
			this.datawriter = new DataWriter(response.getOutputStream(), contentType == ContentType.HTML ? DataWriter.HTML : contentType == ContentType.XML ? DataWriter.XML : DataWriter.JSON);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	boolean authenticateService(PublishedService service, ISession existingSession) {
		if (service.isWorldReadable()) {
			this.context = Core.createSystemContext();
			return true;
		}

		//TODO: constants
		String authHeader = request.getHeader("Authorization");
		String username = null;
		String password = null;
		ISession session = null;
		
		if (authHeader != null && authHeader.trim().startsWith("Basic")) {
			String base64 = StringUtils.base64Decode(authHeader.trim().substring("basic".length()).trim());
			String[] parts = base64.split(":");
			username = parts[0];
			password = parts[1];
		}
		
		try {
			//Check credentials provided by request
			if (username != null) {
				session = Core.login(username, password);
				if (session == null) {
					setStatus(401);
					write("Invalid credentials");
					return false;
				}
				
				//same user as the one in the current session? recylcle the session
				if (existingSession != null && session.getId().equals(existingSession.getId()) && existingSession.getUser().getName().equals(session.getUser().getName())) {
					Core.logout(session);
					session = existingSession;
				}
				else
					this.autoLogout = true;
			}
			
			//check session from cookies
			else if (existingSession != null)
				session = existingSession;
				
			//session found?
			if (session != null && session.getUser() != null && session.getUser().getUserRoleNames().contains(service.getRequiredRole())) {
				this.context = session.createContext().getSudoContext();
				this.activeSession = session;
				return true;
			}

		}
		catch(Exception e) {
			RestServices.LOG.warn("Failed to authenticate '" + username + "'" + e.getMessage(), e);
		}
		
		setStatus(401);
		response.setHeader("WWW-Authenticate", "Basic realm=\"" + service.getName() + "\"");
		write("Unauthorized");
		return false;
	}

	public static ContentType determineContentType(HttpServletRequest request) {
		if (request.getParameter(RestServices.CONTENTTYPE_PARAM) != null)
			return ContentType.valueOf(request.getParameter(RestServices.CONTENTTYPE_PARAM).toUpperCase());
		String ct = request.getHeader(RestServices.ACCEPT_HEADER);
		if (ct != null) {
			if (ct.contains("text/json"))
				return ContentType.JSON;
			if (ct.contains("html"))
				return ContentType.HTML;
			if (ct.contains("xml")) 
				return ContentType.XML;
		}
		return ContentType.JSON; //by default
	}
	
	public static void setResponseContentType(HttpServletResponse response, ContentType contentType) {
		response.setContentType("text/" + contentType.toString().toLowerCase()+ "; charset=UTF-8");
	}
	
	public ContentType getContentType() {
		return this.contentType;
	}
	
	public RestServiceRequest write(String data) {
		try {
			this.response.getOutputStream().write(data.getBytes(RestServices.UTF8));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public IContext getContext() {
		return this.context;
	}

	public void close() {
		try {
			this.response.getOutputStream().close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void startHTMLDoc() {
		this.write("<!DOCTYPE HTML><html><head><style>" + RestServices.STYLESHEET + "</style><head><body>");		
	}


	public void endHTMLDoc() {
		this.write("<p><center><small>View as: <a href='?contenttype=xml'>XML</a> <a href='?contenttype=json'>JSON</a></small></center></p>");
		this.write("<hr /><p><center><small>Generated by the Mendix RestServices module</small></center></body></html>");
	}

	public void startXMLDoc() {
		this.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
	}

	public void setStatus(int status) {
		response.setStatus(status);
	}

	public String getETag() {
		return request.getHeader(RestServices.IFNONEMATCH_HEADER);
	}

	public void dispose() {
		if (autoLogout)
			Core.logout(this.activeSession);		
	}
	
	public IUser getCurrentUser() {
		return activeSession.getUser();
	}
}
