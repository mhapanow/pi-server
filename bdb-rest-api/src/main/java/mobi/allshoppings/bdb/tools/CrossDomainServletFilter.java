package mobi.allshoppings.bdb.tools;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CrossDomainServletFilter implements javax.servlet.Filter {

	@Override
	public void destroy() {
		// Do nothing
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
	    HttpServletResponse response = (HttpServletResponse) resp;

	    // This should be added in response to both the preflight and the actual request
	    response.addHeader("Access-Control-Allow-Origin", "*");
	    response.addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization, Cache-Control");
	    response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");

	    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
	        response.addHeader("Access-Control-Allow-Credentials", "true");
	    }

	    chain.doFilter(req, resp);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// Do nothing
	}

}
