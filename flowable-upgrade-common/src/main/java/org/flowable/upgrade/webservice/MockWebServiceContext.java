package org.flowable.upgrade.webservice;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

/**
 * @author Filip Hrisafov
 */
public class MockWebServiceContext {

    public static final String WEBSERVICE_MOCK_ADDRESS = "http://localhost:63081/webservicemock";

    protected final WebServiceMock webServiceMock;
    protected final Server server;

    private MockWebServiceContext(WebServiceMock webServiceMock, Server server) {
        this.webServiceMock = webServiceMock;
        this.server = server;
    }

    public void start() {
        server.start();
    }

    public void stopIfStarted() {
        if (server.isStarted()) {
            server.stop();
            server.destroy();
        }
    }

    public static MockWebServiceContext create(String address) {
        WebServiceMock webServiceMock = new WebServiceMockImpl();
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(WebServiceMock.class);
        svrFactory.setAddress(address);
        svrFactory.setServiceBean(webServiceMock);
        svrFactory.getInInterceptors().add(new LoggingInInterceptor());
        svrFactory.getOutInterceptors().add(new LoggingOutInterceptor());
        Server server = svrFactory.create();
        return new MockWebServiceContext(webServiceMock, server);
    }

}
