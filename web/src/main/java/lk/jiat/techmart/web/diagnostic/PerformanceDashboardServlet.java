package lk.jiat.techmart.web.diagnostic;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.jiat.techmart.api.InventoryManagerLocal;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;

@WebServlet("/diagnostic/dashboard")
public class PerformanceDashboardServlet extends HttpServlet {

    @EJB
    private InventoryManagerLocal inventoryManager;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();

        int cachedSkus = inventoryManager.getCachedSkuCount();

        StringBuilder json = new StringBuilder("{");
        json.append("\"heapUsedMB\":").append(heap.getUsed() / (1024 * 1024)).append(",");
        json.append("\"heapCommittedMB\":").append(heap.getCommitted() / (1024 * 1024)).append(",");
        json.append("\"heapMaxMB\":").append(heap.getMax() / (1024 * 1024)).append(",");
        json.append("\"nonHeapUsedMB\":").append(nonHeap.getUsed() / (1024 * 1024)).append(",");
        json.append("\"liveThreadCount\":").append(threadMXBean.getThreadCount()).append(",");
        json.append("\"peakThreadCount\":").append(threadMXBean.getPeakThreadCount()).append(",");
        json.append("\"daemonThreadCount\":").append(threadMXBean.getDaemonThreadCount()).append(",");
        json.append("\"inventoryCacheSize\":").append(cachedSkus);
        json.append("}");

        out.write(json.toString());
        out.flush();
    }
}