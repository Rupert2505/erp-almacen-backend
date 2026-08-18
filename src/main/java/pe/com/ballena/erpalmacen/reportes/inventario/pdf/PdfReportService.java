package pe.com.ballena.erpalmacen.reportes.inventario.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class PdfReportService {

    private final TemplateEngine templateEngine;

    public PdfReportService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] render(String template, Map<String, Object> model) {
        Context context = new Context();
        context.setVariables(model);
        String html = templateEngine.process(template, context);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }
}
