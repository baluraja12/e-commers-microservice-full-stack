import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ProductService } from '../../services/product.service';
import { Product } from '../../models/product.model';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './products.component.html'
})
export class ProductsComponent implements OnInit {
  products: Product[] = [];
  loading = true;
  error = '';

  constructor(
    private productService: ProductService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.error = '';
    this.cdr.detectChanges(); // force "Loading..." to show immediately

    this.productService.getAllProducts().subscribe({
      next: (data: any) => {
        console.log('API returned:', data);
        this.products = Array.isArray(data) ? data : [];
        this.loading = false;
        this.cdr.detectChanges(); // FORCE UI UPDATE
      },
      error: (err: any) => {
        console.error('API error:', err);
        this.error = 'Failed to load products.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  viewDetails(id: number | undefined): void {
    if (!id) return;
    this.router.navigate(['/products', id]);
  }

  addToCart(product: Product): void {
    let cart = JSON.parse(localStorage.getItem('cart') || '[]');
    const existing = cart.find((item: any) => item.product.id === product.id);
    if (existing) {
      existing.quantity++;
    } else {
      cart.push({ product: product, quantity: 1 });
    }
    localStorage.setItem('cart', JSON.stringify(cart));
    alert(product.name + ' added to cart!');
  }
}