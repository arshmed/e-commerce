package com.example.ecommerce.service.impl;
import com.example.ecommerce.dto.review.ReviewRequest;
import com.example.ecommerce.dto.review.ReviewResponse;
import com.example.ecommerce.exception.ReviewNotFoundException;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.Review;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ReviewRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.service.ReviewService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }


    @Override
    public ReviewResponse createReview(ReviewRequest reviewRequest) {
        if(reviewRepository.findByUserIdAndProductId(reviewRequest.getUserId(), reviewRequest.getProductId()).isEmpty()){
            Review review = reviewRepository.save(mapToReview(reviewRequest));
            review.getProduct().setAverageRating(getAverageRating(review.getProduct()));
            productRepository.save(review.getProduct());
            return mapToReviewResponse(review);
        }
       else{
           return null;
        }
    }

    @Override
    public void deleteReviewById(Long reviewId, Long userId){
        Review review = reviewRepository.findById(reviewId).orElse(null);


        if (review == null)
            throw new ReviewNotFoundException();
        if(!review.getUser().getId().equals(userId)){
            throw new RuntimeException("It is not your review, so you can not delete it.");
        }
        reviewRepository.deleteById(reviewId);
        review.getProduct().setAverageRating(getAverageRating(review.getProduct()));
        productRepository.save(review.getProduct());
    }

    @Override
    public List<ReviewResponse> getAllReviews() {
        List<Review> reviews = reviewRepository.findAll();
        return mapToReviewResponses(reviews);
    }
    @Override
    public ReviewResponse getReviewById(Long reviewId){
        Review reviews=reviewRepository.findById(reviewId).orElseThrow(()-> new ReviewNotFoundException());
        return mapToReviewResponse(reviews);

    }

    @Override
    public List<ReviewResponse> getProductReviews(Long productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId).stream().toList();
        return mapToReviewResponses(reviews);
    }

    @Override
    public List<ReviewResponse> getUserReviews(Long userId) {
        List<Review> reviews = reviewRepository.findByUserId(userId).stream().toList();
        return mapToReviewResponses(reviews);
    }
    @Override
    public Double getAverageRatingByProductId(Long productId){
        Double average= reviewRepository.getAverageRatingByProductId(productId);
        List<Review> reviews = reviewRepository.findByProductId(productId).stream().toList();
        if (reviews.isEmpty()) {
            average=0.0;
            return average;
        }else{
            return average;
        }
    }

    private Review mapToReview(ReviewRequest reviewRequest) {
        Review review = new Review();
        review.setUser(userRepository.getReferenceById(reviewRequest.getUserId()));
        review.setProduct(productRepository.getReferenceById(reviewRequest.getProductId()));
        review.setContent(reviewRequest.getContent());
        review.setRating(reviewRequest.getRating());
        return review;
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        return new ReviewResponse(review.getId(), review.getUser().getId(), review.getUser().getUsername(),
                review.getProduct().getId(), review.getContent(),
                review.getRating(), review.getCreatedDate());
    }

    private List<ReviewResponse> mapToReviewResponses(List<Review> reviews) {
        return reviews.stream()
                .map(this::mapToReviewResponse)
                .collect(Collectors.toList());
    }
    private double getAverageRating(Product product) {

        if (product.getReviews() == null || product.getReviews().isEmpty()) {
            return 0.0;
        }

        int sum = 0;
        for (Review review : product.getReviews()) {
            sum += review.getRating();
        }
        return (double) sum / product.getReviews().size();
    }
    @Override
    public ReviewResponse updateReview(Long reviewId, ReviewRequest reviewRequest) {
        Review existingReview = reviewRepository.findById(reviewId).orElseThrow(ReviewNotFoundException::new);

        if (!existingReview.getUser().getId().equals(reviewRequest.getUserId())) {
            throw new RuntimeException("You are not authorized to update this review");
        }

        existingReview.setContent(reviewRequest.getContent());
        existingReview.setRating(reviewRequest.getRating());

        Review updatedReview = reviewRepository.save(existingReview);

        return mapToReviewResponse(updatedReview);
    }

    @Override
    public ReviewResponse getReviewByUserIdAndProcutId(Long userId,Long productId) {
        if(reviewRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
            Review review = reviewRepository.findByUserIdAndProductId(userId, productId).orElseThrow(ReviewNotFoundException::new);
            return mapToReviewResponse(review);
        }
        throw new ReviewNotFoundException();
    }

}
