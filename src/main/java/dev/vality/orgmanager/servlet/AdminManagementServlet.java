package dev.vality.orgmanager.servlet;

import dev.vality.orgmanagement.AdminManagementSrv;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebServlet;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@WebServlet("/admin-management")
@RequiredArgsConstructor
public class AdminManagementServlet extends GenericServlet {

    private final AdminManagementSrv.Iface adminManagement;
    private Servlet thriftServlet;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        thriftServlet = new THServiceBuilder()
                .build(AdminManagementSrv.Iface.class, adminManagement);
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        thriftServlet.service(request, response);
    }
}
