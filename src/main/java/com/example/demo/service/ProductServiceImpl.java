package com.example.demo.service;

import com.example.demo.entity.Product;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@Service
public class ProductServiceImpl implements IProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public List<Product> getAllProducts() {
        try {
            return productRepository.findAll();
        } catch (Exception e) {
            log.error("SERVICE_ERROR: Failed to fetch all products. Detail: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Product getProductById(String id) {
        try {
            return productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("SERVICE_ERROR: Error retrieving product by id: {}. Detail: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Product createProduct(Product product) {
        try {
            log.debug("createProduct: Processing product creation for: name={}, price={}", product.getName(),
                    product.getPrice());
            return productRepository.save(product);
        } catch (Exception e) {
            log.error("SERVICE_ERROR: Failed to save product: {}. Detail: {}", product.getName(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Product updateProduct(String id, Product productDetails) {
        try {
            Product product = getProductById(id);
            product.setName(productDetails.getName());
            product.setPrice(productDetails.getPrice());
            product.setDescription(productDetails.getDescription());
            product.setCategory(productDetails.getCategory());
            return productRepository.save(product);
        } catch (Exception e) {
            log.error("SERVICE_ERROR: Failed to update product id: {}. Detail: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteProduct(String id) {
        try {
            Product product = getProductById(id);
            productRepository.delete(product);
        } catch (Exception e) {
            log.error("SERVICE_ERROR: Failed to delete product id: {}. Detail: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Product getProductByName(String name) {
        try {
            return productRepository.findByName(name).orElse(null);
        } catch (Exception e) {
            log.error("SERVICE_ERROR: Error searching product by name: {}. Detail: {}", name, e.getMessage(), e);
            throw e;
        }
    }
}
