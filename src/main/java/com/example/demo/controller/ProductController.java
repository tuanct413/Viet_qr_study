package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.service.IProductService;
import com.example.dto.ResponseMessageDTO;
import com.example.dto.ResponseObjectDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final String SUCCESS_STATUS = "SUCCESS";

    @Autowired
    private IProductService productService;

    @GetMapping("/")
    public ResponseEntity<ResponseObjectDTO<List<Product>>> getAllProducts() {
        log.info("getAllProducts: REST request to get all products at: {}", System.currentTimeMillis());
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(new ResponseObjectDTO<>(SUCCESS_STATUS, products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseObjectDTO<Product>> getProductById(@PathVariable String id) {
        log.info("getProductById: REST request to get product by id : {} at: {}", id, System.currentTimeMillis());
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(new ResponseObjectDTO<>(SUCCESS_STATUS, product));
    }

    @PostMapping("/")
    public ResponseEntity<ResponseMessageDTO> createProduct(@Valid @RequestBody Product product) {
        log.info("START: Processing createProduct request for name: {}", product.getName());
        
        // Kiểm tra nghiệp vụ: Không cho phép trùng tên sản phẩm
        Product existingProduct = productService.getProductByName(product.getName());
        if (existingProduct != null) {
            log.info("VALIDATION_FAILED: Product name '{}' already exists. Aborting creation.", product.getName());
            return new ResponseEntity<>(new ResponseMessageDTO("FAILED", "Product name already exists"), HttpStatus.BAD_REQUEST);
        }

        productService.createProduct(product);
        log.info("END: Product created successfully: {}", product.getName());
        return new ResponseEntity<>(new ResponseMessageDTO(SUCCESS_STATUS, "Product created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessageDTO> updateProduct(@PathVariable String id, @Valid @RequestBody Product productDetails) {
        log.info("updateProduct: REST request to update product id : {} at: {}", id, System.currentTimeMillis());
        productService.updateProduct(id, productDetails);
        return ResponseEntity.ok(new ResponseMessageDTO(SUCCESS_STATUS, "Product updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessageDTO> deleteProduct(@PathVariable String id) {
        log.info("deleteProduct: REST request to delete product id : {} at: {}", id, System.currentTimeMillis());
        productService.deleteProduct(id);
        return ResponseEntity.ok(new ResponseMessageDTO(SUCCESS_STATUS, "Product deleted successfully"));
    }
}
