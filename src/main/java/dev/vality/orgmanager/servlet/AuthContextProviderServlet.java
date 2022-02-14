package dev.vality.orgmanager.servlet;

import dev.vality.orgmanagement.AuthContextProviderSrv;
import dev.vality.woody.api.trace.context.metadata.user.UserIdentityEmailExtensionKit;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;

import java.io.IOException;
import java.util.List;

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
                .withMetaExtensions(
                        List.of(UserIdentityEmailExtensionKit.INSTANCE)
                )
                .build(AuthContextProviderSrv.Iface.class, authContextProvider);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        thriftServlet.service(req, res);
    }
}
