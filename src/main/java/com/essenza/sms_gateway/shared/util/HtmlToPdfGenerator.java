//package com.essenza.sms_gateway.shared.util;
//
//import com.comfenalco.servicios.dracoFuncionarios.modules.compras.licitaciones.domain.dto.LicitacionDetalleCustomDto;
//import com.github.jknack.handlebars.Handlebars;
//import com.github.jknack.handlebars.Plantilla;
//import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
//
//import java.io.*;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class HtmlToPdfGenerator {
//
//   /* public static File
//        generarOrdenCompraPdf(String plantillaHtml, LicitacionDetalleCustomDto dto) throws Exception {
//        //String plantillaHtml = cargarPlantilla("/templates/orden_compra.html");
//
//        Map<String, Object> context = new HashMap<>();
//        context.put("ordenCompra", dto.getOrdenCompraId());
//        context.put("licitacion", dto.getLicitacionId());
//        context.put("solicitud", dto.getSolicitudAreaId());
//        context.put("cotizacion", dto.getCotizacionId());
//        context.put("proveedor", dto.getProveedorId());
//
//        Handlebars handlebars = new Handlebars();
//        Plantilla template = handlebars.compileInline(plantillaHtml);
//        String htmlRenderizado = template.apply(context);
//
//        File archivoPdf = Files.createTempFile("orden_compra_", ".pdf").toFile();
//
//        try (OutputStream os = new FileOutputStream(archivoPdf)) {
//            PdfRendererBuilder builder = new PdfRendererBuilder();
//            builder.useFastMode();
//            builder.withHtmlContent(htmlRenderizado, null);
//            builder.toStream(os);
//            builder.run();
//        }
//
//        System.out.println("✅ PDF generado en: " + archivoPdf.getAbsolutePath());
//        return archivoPdf;
//    }*/
//
//    public static File generarOrdenCompraPdf(String plantillaHtml, List<LicitacionDetalleCustomDto> detalles) throws Exception {
//        LicitacionDetalleCustomDto encabezado = detalles.get(0);
//
//        Map<String, Object> ordenCompra = new HashMap<>();
//        ordenCompra.put("id", encabezado.getOrdenCompraId());
//
//        Map<String, Object> licitacion = new HashMap<>();
//        licitacion.put("id", encabezado.getLicitacionId());
//        licitacion.put("nombre", encabezado.getLicitacionNombre());
//        licitacion.put("vigencia", encabezado.getLicitacionVigencia());
//
//        Map<String, Object> proveedor = new HashMap<>();
//        proveedor.put("nit", encabezado.getNit());
//        proveedor.put("razonSocial", encabezado.getProveedorNombre());
//
//        Map<String, Object> cotizacion = new HashMap<>();
//        cotizacion.put("formaPago", encabezado.getFormaPago());
//        cotizacion.put("id", encabezado.getCotizacionId());
//        cotizacion.put("diasEntrega", encabezado.getDiasEntrega());
//        cotizacion.put("garantia", encabezado.getGarantia());
//        cotizacion.put("validezOferta", encabezado.getValidezOferta());
//        double totalCotizado = detalles.stream().mapToDouble(LicitacionDetalleCustomDto::getTotal).sum();
//        cotizacion.put("totalCotizado", totalCotizado);
//
//        List<Map<String, Object>> detallesProductos = new ArrayList<>();
//        for (LicitacionDetalleCustomDto d : detalles) {
//            Map<String, Object> prod = new HashMap<>();
//            prod.put("item", d.getItemNombre());
//            prod.put("cantidad", d.getCantidadRequerida());
//            prod.put("precioUnidad", d.getPrecioUnidad());
//            prod.put("iva", d.getIva());
//            prod.put("total", d.getTotal());
//            detallesProductos.add(prod);
//        }
//        cotizacion.put("detalles", detallesProductos);
//
//        Map<String, Object> solicitud = new HashMap<>();
//        solicitud.put("estado", Map.of("nombre", encabezado.getSolicitudEstado() != null ? encabezado.getSolicitudEstado() : ""));
//        solicitud.put("responsable", encabezado.getUsrCrea());
//        solicitud.put("categoria", Map.of("nombre", encabezado.getNombre() != null ? encabezado.getNombre() : ""));
//        solicitud.put("descripcion", encabezado.getSolicitudDescripcion());
//
//        Map<String, Object> context = new HashMap<>();
//        context.put("ordenCompra", ordenCompra);
//        context.put("licitacion", licitacion);
//        context.put("proveedor", proveedor);
//        context.put("cotizacion", cotizacion);
//        context.put("solicitud", solicitud);
//
//        Handlebars handlebars = new Handlebars();
//        Plantilla template = handlebars.compileInline(plantillaHtml);
//        String htmlRenderizado = template.apply(context);
//
//        File archivoPdf = Files.createTempFile("orden_compra_", ".pdf").toFile();
//
//        try (OutputStream os = new FileOutputStream(archivoPdf)) {
//            PdfRendererBuilder builder = new PdfRendererBuilder();
//            builder.useFastMode();
//            builder.withHtmlContent(htmlRenderizado, null);
//            builder.toStream(os);
//            builder.run();
//        }
//
//        System.out.println("✅ PDF generado en: " + archivoPdf.getAbsolutePath());
//        return archivoPdf;
//    }
//
//    private static String cargarPlantilla(String rutaRelativa) throws IOException {
//        try (InputStream is = HtmlToPdfGenerator.class.getResourceAsStream(rutaRelativa)) {
//            if (is == null) throw new FileNotFoundException("No se encontró la plantillaEntity: " + rutaRelativa);
//            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
//        }
//    }
//
//}
