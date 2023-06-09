package com.example.ecommerce.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name")
    private String productName;
    @Column(name = "author_name")
    private String authorName;

    @Column(name = "description")
    @Lob
    @Size(max = 5000)
    private String description;

    @ManyToMany
    @JoinTable(name = "product_category",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<Category> categories;

    @Column(name = "price")
    private double price;
    @Column(name = "ISBN")
    private String ISBN;
    @Column(name = "number_of_pages")
    private long numberOfPages;

    @Column(name = "image_url")
    private String imageUrl;
    @Column(name = "number_of_sales")
    private int numberOfSales;
    @Column(name = "publisher")
    private String publisher;
    @Column(name = "language")
    private String language;

    @Column(name = "is_featured")
    private boolean isFeatured;

    @Column(name = "is_available")
    private boolean isAvailable;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "published_date")
    private int publishedDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "stock")
    private long stock;
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews;
    @Column(name="average_rating")
    private Double averageRating;


}
