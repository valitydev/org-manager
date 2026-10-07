package dev.vality.orgmanager.servlet;

import dev.vality.bouncer.ctx.ContextFragment;
import dev.vality.orgmanagement.AuthContextProviderSrv;
import dev.vality.woody.api.trace.ContextUtils;
import dev.vality.woody.api.trace.context.metadata.user.UserIdentityEmailExtensionKit;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebServlet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;

import java.io.IOException;

@WebServlet("/auth-context")
@Slf4j
@RequiredArgsConstructor
public class AuthContextProviderServlet extends GenericServlet {

    private Servlet thriftServlet;

    private final AuthContextProviderSrv.Iface authContextProvider;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        thriftServlet = new THServiceBuilder()
                .build(AuthContextProviderSrv.Iface.class, new EmailRequiringAuthContextProvider(authContextProvider));
    }

    @RequiredArgsConstructor
    static class EmailRequiringAuthContextProvider implements AuthContextProviderSrv.Iface {

        private final AuthContextProviderSrv.Iface delegate;

        @Override
        public ContextFragment getUserContext(String id) throws TException {
            String email = ContextUtils.getCustomMetadataValue(UserIdentityEmailExtensionKit.INSTANCE.getExtension());
            if (email == null) {
                throw new IllegalArgumentException(
                        "Required woody metadata is missing: " + UserIdentityEmailExtensionKit.KEY);
            }
            return delegate.getUserContext(id);
        }

        @Override
        public ContextFragment getPartyContext(String id) throws TException {
            return delegate.getPartyContext(id);
        }
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        thriftServlet.service(req, res);
    }
}
