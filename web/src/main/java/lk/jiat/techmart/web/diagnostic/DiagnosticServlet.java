package lk.jiat.techmart.web.diagnostic;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.jiat.techmart.api.InventoryManagerLocal;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import jakarta.jms.ConnectionFactory;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/diagnostic/jndi")
public class DiagnosticServlet extends HttpServlet {

    private static final int SAMPLE_ITERATIONS = 100;

    @EJB
    private InventoryManagerLocal inventoryManager;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        StringBuilder json = new StringBuilder("{");

        json.append("\"datasourceLookup\":")
                .append(timeJndiLookup("jdbc/TechMartPool", DataSource.class))
                .append(",");

        json.append("\"jmsConnectionFactoryLookup\":")
                .append(timeJndiLookup("jms/TechMartConnectionFactory", ConnectionFactory.class))
                .append(",");

        json.append("\"injectedEjbCallBaseline\":")
                .append(timeInjectedCall())
                .append(",");

        json.append("\"note\":\"datasourceLookup and jmsConnectionFactoryLookup use explicit InitialContext.lookup() over ")
                .append(SAMPLE_ITERATIONS)
                .append(" iterations, sampled fresh on every call. injectedEjbCallBaseline times a method call on an @EJB reference the container already resolved at injection time. The two are not measuring the same operation: JNDI numbers include directory resolution cost; the injected baseline does not, because the container amortizes that cost once at deploy time. The gap between them is the argument for @EJB/@Inject over manual lookup, not a measurement flaw.\"");

        json.append("}");

        out.write(json.toString());
        out.flush();
    }

    private String timeJndiLookup(String jndiName, Class<?> expectedType) {
        long totalNanos = 0L;
        long minNanos = Long.MAX_VALUE;
        long maxNanos = Long.MIN_VALUE;
        int successCount = 0;
        String lastError = null;

        for (int i = 0; i < SAMPLE_ITERATIONS; i++) {
            long start = System.nanoTime();
            try {
                InitialContext ic = new InitialContext();
                Object resolved = ic.lookup(jndiName);
                long elapsed = System.nanoTime() - start;

                if (expectedType.isInstance(resolved)) {
                    totalNanos += elapsed;
                    minNanos = Math.min(minNanos, elapsed);
                    maxNanos = Math.max(maxNanos, elapsed);
                    successCount++;
                } else {
                    lastError = "Unexpected type returned: " + resolved.getClass().getName();
                }
            } catch (NamingException e) {
                lastError = e.getMessage();
            }
        }

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"jndiName\":\"").append(jndiName).append("\",");
        sb.append("\"iterations\":").append(SAMPLE_ITERATIONS).append(",");
        sb.append("\"successCount\":").append(successCount).append(",");

        if (successCount > 0) {
            sb.append("\"avgElapsedMicros\":").append((totalNanos / successCount) / 1000).append(",");
            sb.append("\"minElapsedMicros\":").append(minNanos / 1000).append(",");
            sb.append("\"maxElapsedMicros\":").append(maxNanos / 1000);
        } else {
            sb.append("\"avgElapsedMicros\":null,");
            sb.append("\"minElapsedMicros\":null,");
            sb.append("\"maxElapsedMicros\":null");
        }

        if (lastError != null) {
            sb.append(",\"lastError\":\"").append(lastError.replace("\"", "'")).append("\"");
        }

        sb.append("}");
        return sb.toString();
    }

    private String timeInjectedCall() {
        long totalNanos = 0L;
        long minNanos = Long.MAX_VALUE;
        long maxNanos = Long.MIN_VALUE;
        int successCount = 0;
        String lastError = null;

        for (int i = 0; i < SAMPLE_ITERATIONS; i++) {
            long start = System.nanoTime();
            try {
                inventoryManager.getCachedSkuCount();
                long elapsed = System.nanoTime() - start;
                totalNanos += elapsed;
                minNanos = Math.min(minNanos, elapsed);
                maxNanos = Math.max(maxNanos, elapsed);
                successCount++;
            } catch (RuntimeException e) {
                lastError = e.getMessage();
            }
        }

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"target\":\"InventoryManagerLocal.getCachedSkuCount() via @EJB\",");
        sb.append("\"iterations\":").append(SAMPLE_ITERATIONS).append(",");
        sb.append("\"successCount\":").append(successCount).append(",");

        if (successCount > 0) {
            sb.append("\"avgElapsedMicros\":").append((totalNanos / successCount) / 1000).append(",");
            sb.append("\"minElapsedMicros\":").append(minNanos / 1000).append(",");
            sb.append("\"maxElapsedMicros\":").append(maxNanos / 1000);
        } else {
            sb.append("\"avgElapsedMicros\":null,");
            sb.append("\"minElapsedMicros\":null,");
            sb.append("\"maxElapsedMicros\":null");
        }

        if (lastError != null) {
            sb.append(",\"lastError\":\"").append(lastError.replace("\"", "'")).append("\"");
        }

        sb.append("}");
        return sb.toString();
    }
}