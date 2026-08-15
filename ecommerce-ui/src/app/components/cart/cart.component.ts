import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Product } from '../../models/product.model';

interface CartItem {
  product: Product;
  quantity: number;
}

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './cart.component.html'
})
export class CartComponent implements OnInit {
  cartItems: CartItem[] = [];
  total = 0;

  ngOnInit(): void {
    this.loadCart();
  }

  loadCart(): void {
    this.cartItems = JSON.parse(localStorage.getItem('cart') || '[]');
    this.calculateTotal();
  }

  calculateTotal(): void {
    this.total = this.cartItems.reduce((sum, item) => sum + (item.product.price * item.quantity), 0);
  }

  increaseQuantity(item: CartItem): void {
    item.quantity++;
    this.saveCart();
  }

  decreaseQuantity(item: CartItem): void {
    if (item.quantity > 1) {
      item.quantity--;
      this.saveCart();
    }
  }

  removeItem(productId: number): void {
    this.cartItems = this.cartItems.filter(item => item.product.id !== productId);
    this.saveCart();
  }

  saveCart(): void {
    localStorage.setItem('cart', JSON.stringify(this.cartItems));
    this.calculateTotal();
  }

  clearCart(): void {
    this.cartItems = [];
    localStorage.removeItem('cart');
    this.total = 0;
  }
}