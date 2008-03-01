package com.wideplay.warp.hibernate;

import com.wideplay.warp.persist.UnitOfWork;
import org.hibernate.context.ManagedSessionContext;

import javax.servlet.*;
import java.io.IOException;

import net.jcip.annotations.ThreadSafe;

/**
 * Created with IntelliJ IDEA.
 * On: 2/06/2007
 *
 * <p>
 * Apply this filter in web.xml to enable the HTTP Request unit of work.
 * </p>
 *
 * @author Dhanji R. Prasanna <a href="mailto:dhanji@gmail.com">email</a>
 * @since 1.0
 * @see com.wideplay.warp.persist.UnitOfWork
 */
@ThreadSafe
public class SessionPerRequestFilter implements Filter {
    private static volatile UnitOfWork unitOfWork;

    public void destroy() {}

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (!UnitOfWork.REQUEST.equals(unitOfWork))
            throw new ServletException("UnitOfWork *must* be REQUEST to use this filter (did you mean to use jpa instead)?");

        //open session;
        ManagedSessionContext.bind(SessionFactoryHolder.getCurrentSessionFactory().openSession());

        try {
            //continue operations
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            //close up session when done
            SessionFactoryHolder.getCurrentSessionFactory().getCurrentSession().close();
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {}


    static void setUnitOfWork(UnitOfWork unitOfWork) {
        SessionPerRequestFilter.unitOfWork = unitOfWork;
    }

    static UnitOfWork getUnitOfWork() {
        return unitOfWork;
    }
}
