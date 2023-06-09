package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.cart.CartItemDto;
import com.example.ecommerce.dto.cart.CartResponse;
import com.example.ecommerce.exception.InvalidArgumentException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.Cart;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.service.CartService;
import com.example.ecommerce.service.ProductService;
import com.example.ecommerce.service.UserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final UserService userService;
    private final CartRepository cartRepository;
    private final ProductService productService;

    public CartServiceImpl(UserService userService, CartRepository cartRepository, ProductService productService) {
        this.userService = userService;
        this.cartRepository = cartRepository;
        this.productService = productService;
    }

    @Override
    public CartResponse addToCart(Long productId, Integer amount) {

        User user = userService.getUser();
        Cart cart = user.getCart();

        if (Objects.nonNull(cart) && Objects.nonNull(cart.getCartItemList()) && !cart.getCartItemList().isEmpty()) {
            Optional<CartItem> cartItem = cart.getCartItemList()
                    .stream()
                    .filter(ci -> ci.getProduct().getId().equals(productId)).findFirst();
            if (cartItem.isPresent()) {
                if (cartItem.get().getProduct().getStock() < (cartItem.get().getAmount() + amount)) {
                    throw new InvalidArgumentException("Product does not have desired stock.");
                }
                cartItem.get().setAmount(cartItem.get().getAmount() + amount);
                Cart updatedCart = calculatePrice(cart);
                cartRepository.save(updatedCart);
                return cartResponseConverter(updatedCart);
            }
        }

        if(Objects.isNull(cart))
            cart = createCart(user);

        Product product = productService.getProductById(productId);

        if (product == null)
            throw new InvalidArgumentException("There is no such product!");

        if(product.getStock() < amount)
            throw new InvalidArgumentException("Product does not have desired stock.");

        CartItem cartItem = new CartItem();
        cartItem.setAmount(amount);
        cartItem.setCart(cart);
        cartItem.setProduct(product);

        if(Objects.isNull(cart.getCartItemList()))
            cart.setCartItemList(new ArrayList<>());

        cart.getCartItemList().add(cartItem);

        Cart updatedCart = calculatePrice(cart);
        cartRepository.save(updatedCart);
        return cartResponseConverter(updatedCart);
    }

    @Override
    public CartResponse incrementCartItem(Long cartItemId, Integer amount) {
        User user = userService.getUser();
        Cart cart = user.getCart();
        if (Objects.isNull(cart) || Objects.isNull(cart.getCartItemList()) || cart.getCartItemList().isEmpty()) {
            throw new ResourceNotFoundException("Empty cart");
        }

        CartItem cartItem = cart.getCartItemList()
                .stream()
                .filter(ci -> ci.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem not found"));

        if (cartItem.getProduct().getStock() < (cartItem.getAmount() + amount)) {
            throw new InvalidArgumentException("Product does not have desired stock.");
        }

        cartItem.setAmount(cartItem.getAmount() + amount);
        cart = calculatePrice(cart);
        Cart updatedCart = cartRepository.save(cart);
        return cartResponseConverter(updatedCart);
    }

    @Override
    public CartResponse decrementCartItem(Long cartItemId, Integer amount){

        User user = userService.getUser();
        Cart cart = user.getCart();

        if (Objects.isNull(cart) || Objects.isNull(cart.getCartItemList()) || cart.getCartItemList().isEmpty()) {
            throw new ResourceNotFoundException("Empty cart");
        }

        CartItem cartItem = cart.getCartItemList()
                .stream()
                .filter(ci -> ci.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem not found"));

        if(cartItem.getAmount() <= amount){
            List<CartItem> cartItemList = cart.getCartItemList();
            cartItemList.remove(cartItem);

            if (Objects.isNull(cart.getCartItemList()) || cart.getCartItemList().isEmpty()) {
                user.setCart(null);
                userService.saveUser(user);
                return null;
            }
            cart.setCartItemList(cartItemList);
            cart = calculatePrice(cart);
            cart = cartRepository.save(cart);
            return cartResponseConverter(cart);
        }

        cartItem.setAmount(cartItem.getAmount() - amount);
        cart = calculatePrice(cart);
        cartRepository.save(cart);
        return cartResponseConverter(cart);
    }

    @Override
    public CartResponse removeFromCart(Long cartItemId){

        User user = userService.getUser();
        Cart cart = user.getCart();

        if (Objects.isNull(cart) || Objects.isNull(cart.getCartItemList()) || cart.getCartItemList().isEmpty()) {
            throw new ResourceNotFoundException("Cart or CartItem not found");
        }

        List<CartItem> cartItemList = cart.getCartItemList();
        Optional<CartItem> cartItem = cart.getCartItemList()
                .stream()
                .filter(ci -> ci.getId().equals(cartItemId)).findFirst();

        if(cartItem.isEmpty())
            throw new ResourceNotFoundException("CartItem not found");

        cartItemList.remove(cartItem.get());

        if(Objects.isNull(cart.getCartItemList()) || cart.getCartItemList().isEmpty()){

            user.setCart(null);
            userService.saveUser(user);
            return null;
        }

        cart.setCartItemList(cartItemList);
        cart = calculatePrice(cart);
        cartRepository.save(cart);
        return cartResponseConverter(cart);

    }

    @Override
    public CartResponse getCart(){

        Cart cart = userService.getUser().getCart();
        if (cart != null){
            return cartResponseConverter(cart);
        }
        else
            return null;
    }

    @Override
    public void emptyCart(){
        User user = userService.getUser();
        user.setCart(null);
        userService.saveUser(user);
    }

    @Override
    public Cart calculatePrice(Cart cart){

        cart.setTotalCartPrice(0F);
        cart.setTotalPrice(0F);

        cart.getCartItemList().forEach(cartItem -> {

            cart.setTotalCartPrice((float) (cart.getTotalCartPrice() + (cartItem.getProduct().getPrice()) * cartItem.getAmount()));
            // some other prices (like cargo) will be added
            cart.setTotalPrice(cart.getTotalCartPrice());
        });

        cart.setTotalPrice(cart.getTotalPrice());
        return cart;
    }

    private Cart createCart(User user){

        Cart cart = new Cart();
        cart.setUser(user);
        return cart;
    }

    private CartResponse cartResponseConverter(Cart cart){

        CartResponse cartResponse = new CartResponse();

        cartResponse.setCartItems(cart.getCartItemList()
                .stream()
                .map(cartItem -> CartItemDto
                        .builder()
                        .id(cartItem.getId())
                        .amount(cartItem.getAmount())
                        .productName(cartItem.getProduct().getProductName())
                        .authorName(cartItem.getProduct().getAuthorName())
                        .price(cartItem.getProduct().getPrice())
                        .stock(cartItem.getProduct().getStock())
                        .imageUrl(cartItem.getProduct().getImageUrl())
                        .ISBN(cartItem.getProduct().getISBN())
                        .publisher(cartItem.getProduct().getPublisher())
                        .language(cartItem.getProduct().getLanguage())
                        .productId(cartItem.getProduct().getId())
                        .build())
                .collect(Collectors.toList()));

        cartResponse.setTotalCartPrice(cart.getTotalCartPrice());
        cartResponse.setTotalPrice(cart.getTotalPrice());
        return cartResponse;
    }

}
