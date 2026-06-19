package lk.jiat.techmart.web.diagnostic;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import jakarta.jms.ConnectionFactory;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/diagnostic/jndi")
public class DiagnosticServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        StringBuilder json = new StringBuilder("{");

        long dsStart = System.nanoTime();
        try {
            InitialContext ic = new InitialContext();
            DataSource ds = (DataSource) ic.lookup("jdbc/TechMartPool");
            long dsElapsedNanos = System.nanoTime() - dsStart;
            json.append("\"datasourceLookup\":{")
                    .append("\"jndiName\":\"jdbc/TechMartPool\",")
                    .append("\"success\":true,")
                    .append("\"elapsedMicros\":").append(dsElapsedNanos / 1000)
                    .append("},");
        } catch (NamingException e) {
            long dsElapsedNanos = System.nanoTime() - dsStart;
            json.append("\"datasourceLookup\":{")
                    .append("\"jndiName\":\"jdbc/TechMartPool\",")
                    .append("\"success\":false,")
                    .append("\"error\":\"").append(e.getMessage()).append("\",")
                    .append("\"elapsedMicros\":").append(dsElapsedNanos / 1000)
                    .append("},");
        }

        long jmsStart = System.nanoTime();
        try {
            InitialContext ic = new InitialContext();
            ConnectionFactory cf = (ConnectionFactory) ic.lookup("jms/TechMartConnectionFactory");
            long jmsElapsedNanos = System.nanoTime() - jmsStart;
            json.append("\"jmsConnectionFactoryLookup\":{")
                    .append("\"jndiName\":\"jms/TechMartConnectionFactory\",")
                    .append("\"success\":true,")
                    .append("\"elapsedMicros\":").append(jmsElapsedNanos / 1000)
                    .append("}");
        } catch (NamingException e) {
            long jmsElapsedNanos = System.nanoTime() - jmsStart;
            json.append("\"jmsConnectionFactoryLookup\":{")
                    .append("\"jndiName\":\"jms/TechMartConnectionFactory\",")
                    .append("\"success\":false,")
                    .append("\"error\":\"").append(e.getMessage()).append("\",")
                    .append("\"elapsedMicros\":").append(jmsElapsedNanos / 1000)
                    .append("}");
        }

        json.append(",\"note\":\"Explicit InitialContext.lookup() used here only, to contrast against @EJB/@Inject used elsewhere in the application\"");
        json.append("}");

        out.write(json.toString());
        out.flush();
    }
}