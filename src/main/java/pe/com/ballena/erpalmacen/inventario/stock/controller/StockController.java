package pe.com.ballena.erpalmacen.inventario.stock.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.ballena.erpalmacen.inventario.stock.dto.StockActualResponse;
import pe.com.ballena.erpalmacen.inventario.stock.dto.StockBajoMinimoResponse;
import pe.com.ballena.erpalmacen.inventario.stock.service.StockConsultaService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/inventario/stock")
public class StockController {

    private final StockConsultaService stockConsultaService;

    public StockController(StockConsultaService stockConsultaService) {
        this.stockConsultaService = stockConsultaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<StockActualResponse>>> listar(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long ubicacionId,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean soloConStock,
            @PageableDefault(sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                stockConsultaService.listar(productoId, almacenId, ubicacionId, texto, soloConStock, pageable)
        ));
    }

    @GetMapping("/producto/{productoId}")
    @PreAuthorize("hasAuthority('STOCK_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<StockActualResponse>>> listarPorProducto(
            @PathVariable Long productoId,
            @PageableDefault(sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(stockConsultaService.listarPorProducto(productoId, pageable)));
    }

    @GetMapping("/almacen/{almacenId}")
    @PreAuthorize("hasAuthority('STOCK_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<StockActualResponse>>> listarPorAlmacen(
            @PathVariable Long almacenId,
            @PageableDefault(sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(stockConsultaService.listarPorAlmacen(almacenId, pageable)));
    }

    @GetMapping("/bajo-minimo")
    @PreAuthorize("hasAuthority('STOCK_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<StockBajoMinimoResponse>>> listarBajoMinimo(
            @RequestParam(required = false) Long almacenId,
            @PageableDefault(sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(stockConsultaService.listarBajoMinimo(almacenId, pageable)));
    }
}
