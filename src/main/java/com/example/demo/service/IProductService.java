package com.example.demo.service;

import com.example.demo.entity.Product;

import java.util.List;

public interface IProductService {
    List<Product> getAllProducts();
    Product getProductById(String id);
    Product createProduct(Product product);
    Product updateProduct(String id, Product productDetails);
    void deleteProduct(String id);
    Product getProductByName(String name);
}
