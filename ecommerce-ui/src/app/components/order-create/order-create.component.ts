import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { CartItem, Order, OrderItem } from '../../models/order.model';

@Component({
  selector: 'app-order-create',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './order-create.component.html'
})
export class OrderCreateComponent implements OnInit {
  cartItems: CartItem[] = [];
  total = 0;
  loading = false;
  error = '';
  success = '';

  constructor(private orderService: OrderService, private router: Router) {}

  ngOnInit(): void {
    this.loadCart();
  }

  loadCart(): void {
    this.cartItems = JSON.parse(localStorage.getItem('cart') || '[]');
    this.total = this.cartItems.reduce((sum, item) => sum + (item.product.price * item.quantity), 0);
  }

  placeOrder(): void {
    if (this.cartItems.length === 0) return;

    const orderItems: OrderItem[] = this.cartItems.map(item => ({
      productId: item.product.id!,
      productName: item.product.name,
      quantity: item.quantity,
      unitPrice: item.product.price,
      subtotal: item.product.price * item.quantity
    }));

    const order: Order = {
      orderItems: orderItems
    };

    this.loading = true;
    this.error = '';

    this.orderService.createOrder(order).subscribe({
      next: (response) => {
        this.success = 'Order placed successfully! Order #' + response.id;
        localStorage.removeItem('cart');
        this.loading = false;
        setTimeout(() => this.router.navigate(['/orders']), 2000);
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to place order. Please try again.';
        this.loading = false;
      }
    });
  }
}