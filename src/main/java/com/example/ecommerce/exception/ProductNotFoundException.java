package com.example.ecommerce.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends RuntimeException{
    public ProductNotFoundException(Long productId) {
        super("Product with " + productId + " not found");
    }

    public ProductNotFoundException(String productName) {
        super(productName +" not found");
    }
}