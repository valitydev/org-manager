package dev.vality.orgmanager.servlet;

import dev.vality.orgmanagement.AdminManagementSrv;
import dev.vality.orgmanagement.InvitationNotFound;
import dev.vality.orgmanagement.MemberNotFound;
import dev.vality.orgmanagement.MemberRoleNotFound;
import dev.vality.orgmanagement.OrganizationNotFound;
import dev.vality.orgmanagement.UnknownUser;
import dev.vality.woody.api.flow.error.ErrorLogPolicy;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebServlet;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;

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
                .withErrorLogPolicy(ErrorLogPolicy.builder()
                        .levelWithoutStacktrace(OrganizationNotFound.class, Level.INFO)
                        .levelWithoutStacktrace(MemberNotFound.class, Level.INFO)
                        .levelWithoutStacktrace(MemberRoleNotFound.class, Level.INFO)
                        .levelWithoutStacktrace(InvitationNotFound.class, Level.INFO)
                        .levelWithoutStacktrace(UnknownUser.class, Level.INFO)
                        .build())
                .build(AdminManagementSrv.Iface.class, adminManagement);
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        thriftServlet.service(request, response);
    }
}
